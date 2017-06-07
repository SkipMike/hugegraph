package com.baidu.hugegraph.backend.store.cassandra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.dht.ByteOrderedPartitioner;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.dht.OrderPreservingPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.dht.Token.TokenFactory;

import com.baidu.hugegraph.backend.BackendException;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.TokenRange;
import com.google.common.collect.ImmutableMap;

public class CassandraSplit {

    public static long MIN_SPLIT_SIZE = 1024 * 1024; // 1M

    private CassandraSessionPool.Session session;
    private String keyspace;
    private String table;

    private IPartitioner partitioner;

    public CassandraSplit(CassandraSessionPool.Session session,
            String keyspace,
            String table) {
        this.session = session;
        this.keyspace = keyspace;
        this.table = table;

        this.partitioner = new Murmur3Partitioner();
    }

    /**
     * Get splits of a table
     * @param splitPartitions: expected partitions count per split
     * @param splitSize: expected size(bytes) per split,
     *        splitPartitions will be ignored if splitSize is passed
     * @return a list of Split
     */
    public List<Split> getSplits(long splitPartitions, long splitSize) {
        // canonical ranges, split into pieces
        // fetching the splits in parallel
        ExecutorService executor = new ThreadPoolExecutor(0, 128, 60L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        List<Split> splits = new ArrayList<>();

        try {
            List<Future<List<Split>>> futures = new ArrayList<>();

            // canonical ranges and nodes holding replicas
            Map<TokenRange, Set<Host>> masterRangeNodes = getRangeMap();

            for (TokenRange range : masterRangeNodes.keySet()) {
                // for each token range, pick a live owner and ask it to
                // compute bite-sized splits
                futures.add(executor.submit(new SplitCallable(range,
                        splitPartitions, splitSize)));
            }

            // wait until we have all the results back
            for (Future<List<Split>> future : futures) {
                try {
                    splits.addAll(future.get());
                } catch (Exception e) {
                    throw new BackendException("Can't get cassandra splits", e);
                }
            }
            assert splits.size() > masterRangeNodes.size();
        } finally {
            executor.shutdownNow();
        }

        Collections.shuffle(splits, new Random(System.nanoTime()));
        return splits;
    }

    // NOTE: maybe we don't need this method
    public List<Split> getSplits(String start, String end,
            int splitPartitions, int splitSize) {

        ExecutorService executor = new ThreadPoolExecutor(0, 128, 60L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        List<Split> splits = new ArrayList<>();

        try {
            List<Future<List<Split>>> futures = new ArrayList<>();
            TokenFactory tokenFactory = this.partitioner.getTokenFactory();
            TokenRange tokenRange = rangeToTokenRange(new Range<>(
                    tokenFactory.fromString(start),
                    tokenFactory.fromString(end)));

            // canonical ranges and nodes holding replicas
            Map<TokenRange, Set<Host>> masterRangeNodes = getRangeMap();

            for (TokenRange range : masterRangeNodes.keySet()) {
                for (TokenRange r : range.intersectWith(tokenRange)) {
                    // for each tokenRange, pick a live owner and ask it
                    // to compute bite-sized splits
                    futures.add(executor.submit(new SplitCallable(r,
                            splitPartitions, splitSize)));
                }
            }

            // wait until we have all the results back
            for (Future<List<Split>> future : futures) {
                try {
                    splits.addAll(future.get());
                } catch (Exception e) {
                    throw new BackendException("Can't get cassandra splits", e);
                }
            }
            assert splits.size() >= masterRangeNodes.size();
        } finally {
            executor.shutdownNow();
        }

        Collections.shuffle(splits, new Random(System.nanoTime()));
        return splits;
    }

    private boolean isPartitionerOpp() {
        return (this.partitioner instanceof OrderPreservingPartitioner
                || this.partitioner instanceof ByteOrderedPartitioner);
    }

    private TokenRange rangeToTokenRange(Range<Token> range) {
        TokenFactory tokenFactory = this.partitioner.getTokenFactory();
        Metadata metadata = this.session.metadata();
        return metadata.newTokenRange(
                metadata.newToken(tokenFactory.toString(range.left)),
                metadata.newToken(tokenFactory.toString(range.right)));
    }

    private Map<TokenRange, Long> getSubSplits(
            TokenRange tokenRange,
            long splitPartitions,
            long splitSize) {
        try {
            return describeSplits(this.session,
                    this.keyspace, this.table,
                    splitPartitions, splitSize,
                    tokenRange);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<TokenRange, Set<Host>> getRangeMap() {
        Metadata metadata = this.session.metadata();
        return metadata.getTokenRanges().stream().collect(Collectors.toMap(
                p -> p,
                p -> metadata.getReplicas('"' + this.keyspace + '"', p)));
    }

    private static Map<TokenRange, Long> describeSplits(
            CassandraSessionPool.Session session,
            String keyspace,
            String table,
            long splitPartitions,
            long splitSize,
            TokenRange tokenRange) {
        String query = String.format(
                "SELECT mean_partition_size, partitions_count FROM %s.%s "
                + "WHERE keyspace_name = ? AND table_name = ? AND "
                + "range_start = ? AND range_end = ?",
                SchemaConstants.SYSTEM_KEYSPACE_NAME,
                SystemKeyspace.SIZE_ESTIMATES);

        ResultSet resultSet = session.execute(query,
                keyspace, table,
                tokenRange.getStart().toString(),
                tokenRange.getEnd().toString());
        Row row = resultSet.one();

        long meanPartitionSize = 0;
        long partitionsCount = 0;
        long splitCount = 0;

        if (row != null) {
            meanPartitionSize = row.getLong("mean_partition_size");
            partitionsCount = row.getLong("partitions_count");
            assert splitSize <= 0 || splitSize >= 1024 * 1024;
            splitCount = splitSize > 0
                    ? (meanPartitionSize * partitionsCount / splitSize)
                    : (partitionsCount / splitPartitions);
        }

        // If we have no data on this split or the size estimate is 0,
        // return the full split i.e., do not sub-split
        // Assume smallest granularity of partition count available from
        // CASSANDRA-7688
        if (splitCount == 0) {
            return ImmutableMap.of(tokenRange, (long) 128);
        }

        List<TokenRange> ranges = tokenRange.splitEvenly((int) splitCount);
        Map<TokenRange, Long> rangesWithLength = new HashMap<>();
        for (TokenRange range : ranges) {
            // sub-range (with its partitions count per sub-range)
            rangesWithLength.put(range, partitionsCount / splitCount);
        }
        return rangesWithLength;
    }

    /**
     * Gets a token tokenRange and splits it up according to the suggested size
     * into input splits that Hugegraph can use.
     */
    class SplitCallable implements Callable<List<Split>> {

        private final TokenRange tokenRange;
        private final long splitPartitions;
        private final long splitSize;

        public SplitCallable(TokenRange tokenRange,
                long splitPartitions, long splitSize) {
            if (splitSize <= 0) {
                if (splitPartitions <= 0) {
                    throw new IllegalArgumentException(String.format(
                            "The split-partitions must be > 0, but got %s",
                            splitPartitions));
                }
            } else if (splitSize < MIN_SPLIT_SIZE) {
                // splitSize at least 1M if passed
                throw new IllegalArgumentException(String.format(
                        "The split-size must be >= %s bytes, but got %s",
                        MIN_SPLIT_SIZE, splitSize));
            }

            this.tokenRange = tokenRange;
            this.splitPartitions = splitPartitions;
            this.splitSize = splitSize;
        }

        @Override
        public List<Split> call() throws Exception {
            ArrayList<Split> splits = new ArrayList<>();

            Map<TokenRange, Long> subSplits = getSubSplits(this.tokenRange,
                    this.splitPartitions, this.splitSize);
            for (Map.Entry<TokenRange, Long> entry : subSplits.entrySet()) {
                List<TokenRange> ranges = entry.getKey().unwrap();
                for (TokenRange subrange : ranges) {
                    Split split = new Split();
                    split.start = isPartitionerOpp()
                            ? subrange.getStart().toString().substring(2)
                            : subrange.getStart().toString();
                    split.end = isPartitionerOpp()
                            ? subrange.getEnd().toString().substring(2)
                            : subrange.getEnd().toString();
                    split.length = entry.getValue();
                    splits.add(split);
                }
            }
            return splits;
        }
    }

    public static class Split {
        public String start; // token range start
        public String end; // token range end
        public long length; // partitions count in this range

        @Override
        public String toString() {
            return String.format("Split{start=%s, end=%s, length=%s}",
                    this.start, this.end, this.length);
        }
    }
}
