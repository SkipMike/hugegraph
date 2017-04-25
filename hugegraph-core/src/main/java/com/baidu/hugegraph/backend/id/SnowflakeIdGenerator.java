package com.baidu.hugegraph.backend.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph.HugeException;
import com.baidu.hugegraph.schema.SchemaElement;
import com.baidu.hugegraph.structure.HugeEdge;
import com.baidu.hugegraph.structure.HugeIndex;
import com.baidu.hugegraph.structure.HugeVertex;
import com.baidu.hugegraph.util.TimeUtil;

public class SnowflakeIdGenerator extends IdGenerator {

    private IdWorker idWorker = null;

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        this.idWorker = new IdWorker(workerId, datacenterId);
    }

    public Id generate() {
        if (this.idWorker == null) {
            throw new HugeException("Please initialize before using it");
        }
        return generate(this.idWorker.nextId());
    }

    @Override
    public Id generate(SchemaElement entry) {
        return this.generate();
    }

    @Override
    public Id generate(HugeVertex entry) {
        return this.generate();
    }

    @Override
    public Id generate(HugeEdge entry) {
        return this.generate();
    }

    @Override
    public Id generate(HugeIndex index) {
        return this.generate();
    }

    @Override
    public Id[] split(Id id) {
        throw new RuntimeException("Not supported");
    }

    static class IdWorker {

        protected static final Logger LOG = LoggerFactory.getLogger(IdWorker.class);

        private long workerId;
        private long datacenterId;
        private long sequence = 0L;

        private long workerIdBits = 5L;
        private long datacenterIdBits = 5L;
        private long maxWorkerId = -1L ^ (-1L << this.workerIdBits);
        private long maxDatacenterId = -1L ^ (-1L << this.datacenterIdBits);
        private long sequenceBits = 12L;

        private long workerIdShift = this.sequenceBits;
        private long datacenterIdShift = this.sequenceBits + this.workerIdBits;
        private long timestampLeftShift = this.sequenceBits + this.workerIdBits + this.datacenterIdBits;
        private long sequenceMask = -1L ^ (-1L << this.sequenceBits);

        private long lastTimestamp = -1L;

        public IdWorker(long workerId, long datacenterId) {
            // sanity check for workerId
            if (workerId > this.maxWorkerId || workerId < 0) {
                throw new IllegalArgumentException(String.format(
                        "worker Id can't be greater than %d or less than 0",
                        this.maxWorkerId));
            }
            if (datacenterId > this.maxDatacenterId || datacenterId < 0) {
                throw new IllegalArgumentException(String.format(
                        "datacenter Id can't be greater than %d or less than 0",
                        this.maxDatacenterId));
            }
            this.workerId = workerId;
            this.datacenterId = datacenterId;
            LOG.info(String.format(
                    "worker starting. timestamp left shift %d,"
                            + "datacenter id bits %d, worker id bits %d,"
                            + "sequence bits %d, workerid %d",
                    this.timestampLeftShift,
                    this.datacenterIdBits,
                    this.workerIdBits,
                    this.sequenceBits,
                    workerId));
        }

        public synchronized long nextId() {
            long timestamp = TimeUtil.timeGen();

            if (timestamp < this.lastTimestamp) {
                LOG.error(String.format("clock is moving backwards."
                                + "Rejecting requests until %d.",
                        this.lastTimestamp));
                throw new RuntimeException(String.format("Clock moved backwards."
                                + "Refusing to generate id for %d milliseconds",
                        this.lastTimestamp - timestamp));
            }

            if (this.lastTimestamp == timestamp) {
                this.sequence = (this.sequence + 1) & this.sequenceMask;
                if (this.sequence == 0) {
                    timestamp = TimeUtil.tilNextMillis(this.lastTimestamp);
                }
            } else {
                this.sequence = 0L;
            }

            this.lastTimestamp = timestamp;

            return (timestamp << this.timestampLeftShift)
                    | (this.datacenterId << this.datacenterIdShift)
                    | (this.workerId << this.workerIdShift)
                    | this.sequence;
        }

    }
}
