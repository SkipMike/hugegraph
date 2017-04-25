package com.baidu.hugegraph.backend.tx;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph.HugeGraph;
import com.baidu.hugegraph.backend.id.Id;
import com.baidu.hugegraph.backend.query.Condition;
import com.baidu.hugegraph.backend.query.ConditionQuery;
import com.baidu.hugegraph.backend.store.BackendEntry;
import com.baidu.hugegraph.backend.store.BackendStore;
import com.baidu.hugegraph.schema.HugeEdgeLabel;
import com.baidu.hugegraph.schema.HugeIndexLabel;
import com.baidu.hugegraph.schema.HugePropertyKey;
import com.baidu.hugegraph.schema.HugeVertexLabel;
import com.baidu.hugegraph.schema.SchemaElement;
import com.baidu.hugegraph.type.HugeTypes;
import com.baidu.hugegraph.type.define.HugeKeys;
import com.baidu.hugegraph.type.schema.EdgeLabel;
import com.baidu.hugegraph.type.schema.IndexLabel;
import com.baidu.hugegraph.type.schema.PropertyKey;
import com.baidu.hugegraph.type.schema.VertexLabel;

public class SchemaTransaction extends AbstractTransaction {

    private static final Logger logger = LoggerFactory.getLogger(SchemaTransaction.class);

    public SchemaTransaction(HugeGraph graph, BackendStore store) {
        super(graph, store);
        // TODO Auto-generated constructor stub
    }

    public List<HugePropertyKey> getPropertyKeys(String... names) {
        // TODO:to be checked

        List<HugePropertyKey> propertyKeys = new ArrayList<HugePropertyKey>();

        Condition c = Condition.none();
        for (String name : names) {
            c = c.or(Condition.eq(HugeKeys.NAME, name));
        }

        ConditionQuery q = new ConditionQuery(HugeTypes.PROPERTY_KEY);
        q.query(c);

        Iterable<BackendEntry> entries = query(q);
        entries.forEach(item -> {
            propertyKeys.add((HugePropertyKey) this.serializer.readPropertyKey(item));
        });
        return propertyKeys;
    }

    public void getVertexLabels() {
        // todo:to be implemented
    }

    public void getEdgeLabels() {
        // todo:to be implemented
    }

    public void addPropertyKey(PropertyKey propertyKey) {
        logger.debug("SchemaTransaction add property key, "
                + "name: " + propertyKey.name() + ", "
                + "dataType: " + propertyKey.dataType() + ", "
                + "cardinality: " + propertyKey.cardinality());

        this.addEntry(this.serializer.writePropertyKey(propertyKey));
    }

    public PropertyKey getPropertyKey(String name) {
        BackendEntry entry = querySchema(new HugePropertyKey(name, null));
        return this.serializer.readPropertyKey(entry);
    }

    public void removePropertyKey(String name) {
        logger.debug("SchemaTransaction remove property key " + name);

        this.removeSchema(new HugePropertyKey(name, null));
    }

    public void addVertexLabel(VertexLabel vertexLabel) {
        logger.debug("SchemaTransaction add vertex label, "
                + "name: " + vertexLabel.name());

        this.addEntry(this.serializer.writeVertexLabel(vertexLabel));
    }

    public VertexLabel getVertexLabel(String name) {
        BackendEntry entry = querySchema(new HugeVertexLabel(name, null));
        return this.serializer.readVertexLabel(entry);
    }

    public void removeVertexLabel(String name) {
        logger.info("SchemaTransaction remove vertex label " + name);

        this.removeSchema(new HugeVertexLabel(name, null));
    }

    public void addEdgeLabel(EdgeLabel edgeLabel) {
        logger.debug("SchemaTransaction add edge label, "
                + "name: " + edgeLabel.name() + ", "
                + "multiplicity: " + edgeLabel.multiplicity() + ", "
                + "frequency: " + edgeLabel.frequency());

        this.addEntry(this.serializer.writeEdgeLabel(edgeLabel));
    }

    public EdgeLabel getEdgeLabel(String name) {
        BackendEntry entry = querySchema(new HugeEdgeLabel(name, null));
        return this.serializer.readEdgeLabel(entry);
    }

    public void removeEdgeLabel(String name) {
        logger.info("SchemaTransaction remove edge label " + name);

        this.removeSchema(new HugeEdgeLabel(name, null));
    }

    public void addIndexLabel(IndexLabel indexLabel) {
        logger.debug("SchemaTransaction add index label, "
                + "name: " + indexLabel.name() + ", "
                + "base-type: " + indexLabel.baseType() + ", "
                + "base-value:" + indexLabel.baseValue() + ", "
                + "indexType: " + indexLabel.indexType() + ", "
                + "fields: " + indexLabel.indexFields());

        this.addEntry(this.serializer.writeIndexLabel(indexLabel));
    }

    public IndexLabel getIndexLabel(String name) {
        BackendEntry entry = querySchema(new HugeIndexLabel(name));
        return this.serializer.readIndexLabel(entry);
    }

    public void removeIndexLabel(String name) {
        logger.info("SchemaTransaction remove index label " + name);
        // TODO: need check index data exists
        this.removeSchema(new HugeIndexLabel(name));
    }

    private BackendEntry querySchema(SchemaElement schemaElement) {
        Id id = this.idGenerator.generate(schemaElement);
        return this.query(schemaElement.type(), id);
    }

    private void removeSchema(SchemaElement schema) {
        Id id = this.idGenerator.generate(schema);
        this.removeEntry(schema.type(), id);
    }


    //****************************   update operation *************************** //
    public void updateSchemaElement(HugeTypes baseType, String baseValue, String indexName) {
        switch (baseType) {
            case VERTEX_LABEL:
                VertexLabel vertexLabel = getVertexLabel(baseValue);
                vertexLabel.indexNames(indexName);
                addVertexLabel(vertexLabel);
                break;
            case EDGE_LABEL:
                EdgeLabel edgeLabel = getEdgeLabel(baseValue);
                edgeLabel.indexNames(indexName);
                addEdgeLabel(edgeLabel);
                break;
            case PROPERTY_KEY:
                PropertyKey propertyKey = getPropertyKey(baseValue);
                propertyKey.indexNames(indexName);
                addPropertyKey(propertyKey);
                break;
        }
    }
}
