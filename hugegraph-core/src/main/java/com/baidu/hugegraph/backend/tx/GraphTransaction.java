package com.baidu.hugegraph.backend.tx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.AbstractThreadedTransaction;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;

import com.baidu.hugegraph.HugeGraph;
import com.baidu.hugegraph.backend.id.Id;
import com.baidu.hugegraph.backend.query.Query;
import com.baidu.hugegraph.backend.store.BackendEntry;
import com.baidu.hugegraph.backend.store.BackendStore;
import com.baidu.hugegraph.schema.SchemaManager;
import com.baidu.hugegraph.structure.HugeElement;
import com.baidu.hugegraph.structure.HugeVertex;
import com.baidu.hugegraph.type.HugeTypes;
import com.baidu.hugegraph.type.schema.VertexLabel;
import com.baidu.hugegraph.util.CollectionUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class GraphTransaction extends AbstractTransaction {

    private Set<HugeVertex> vertexes;

    public GraphTransaction(final HugeGraph graph, BackendStore store) {
        super(graph, store);
        this.vertexes = new LinkedHashSet<HugeVertex>();
    }

    @Override
    protected void prepareCommit() {
        // ensure all the target vertexes (of out edges) are in this.vertexes
        for (HugeVertex source : this.vertexes) {
            Iterator<Vertex> targets = source.vertices(Direction.OUT);
            while (targets.hasNext()) {
                HugeVertex target = (HugeVertex) targets.next();
                this.vertexes.add(target);
            }
        }

        // serialize and add into super.additions
        for (HugeVertex v : this.vertexes) {
            this.addEntry(this.serializer.writeVertex(v));
        }

        this.vertexes.clear();
    }

    public Vertex addVertex(HugeVertex vertex) {
        return this.vertexes.add(vertex) ? vertex : null;
    }

    public Vertex addVertex(Object... keyValues) {
        ElementHelper.legalPropertyKeyValueArray(keyValues);

        Id id = HugeVertex.getIdValue(keyValues);
        Object label = HugeVertex.getLabelValue(keyValues);

        // Vertex id must be null now
        if (id != null) {
            String msg = "User defined id of Vertex is not supported";
            throw new IllegalArgumentException(msg);
        }

        if (label == null) {
            // Preconditions.checkArgument(label != null, "Vertex label must be not null");
            throw Element.Exceptions.labelCanNotBeNull();
        } else if (label instanceof String) {
            SchemaManager schema = this.graph.openSchemaManager();
            label = schema.vertexLabel((String) label);
        }

        // create HugeVertex
        assert (label instanceof VertexLabel);

        // check keyValues whether contain primaryKey in definition of vertexLabel.
        Preconditions.checkArgument(
                CollectionUtil.containsAll(ElementHelper.getKeys(keyValues), ((VertexLabel) label)
                        .primaryKeys()), "the primary key must "
                        + "set in 'addVertex' method, you can refer to the definition of vertexLabel.");

        HugeVertex vertex = new HugeVertex(this.graph, id, (VertexLabel) label);
        // set properties
        ElementHelper.attachProperties(vertex, keyValues);

        // generate an id and assign it if not exists
        if (id == null) {
            vertex.assignId();
        }

        return this.addVertex(vertex);
    }

    public Iterator<Vertex> queryVertices(Object... vertexIds) {
        List<Vertex> list = new ArrayList<Vertex>(vertexIds.length);

        for (Object vertexId : vertexIds) {
            Id id = HugeElement.getIdValue(T.id, vertexId);
            BackendEntry entry = this.get(HugeTypes.VERTEX, id);
            Vertex vertex = this.serializer.readVertex(entry);
            assert vertex != null;
            list.add(vertex);
        }

        return list.iterator();
    }

    public Iterator<Vertex> queryVertices(Query q) {
        // TODO Auto-generated method stub
        return null;
    }

    public Iterator<Edge> queryEdges(Object... edgeIds) {
        List<Edge> list = new ArrayList<Edge>(edgeIds.length);

        for (Object vertexId : edgeIds) {
            Id id = HugeElement.getIdValue(T.id, vertexId);
            BackendEntry entry = this.get(HugeTypes.EDGE, id);
            Vertex vertex = this.serializer.readVertex(entry);
            assert vertex != null;
            list.addAll(ImmutableList.copyOf(vertex.edges(Direction.BOTH)));
        }

        return list.iterator();
    }

    public Iterator<Edge> queryEdges(Query q) {
        // TODO Auto-generated method stub
        return null;
    }

    public org.apache.tinkerpop.gremlin.structure.Transaction tx() {
        return new AbstractThreadedTransaction(this.graph) {
            @Override
            public void doOpen() {
                // NOTE: we assume that a Transaction is opened as long as
                // the object exists
            }

            @Override
            public void doCommit() {
                GraphTransaction.this.commit();
            }

            @Override
            public void doRollback() {
                GraphTransaction.this.rollback();
            }

            @Override
            public <R> Workload<R> submit(Function<Graph, R> graphRFunction) {
                throw new UnsupportedOperationException(
                        "HugeGraph does not support nested transactions. "
                                + "Call submit on a HugeGraph not an individual transaction.");
            }

            @Override
            public <G extends Graph> G createThreadedTx() {
                throw new UnsupportedOperationException(
                        "HugeGraph does not support nested transactions.");
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void doClose() {
                // calling super will clear listeners
                super.doClose();
            }
        };
    }
}
