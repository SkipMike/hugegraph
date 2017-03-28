package com.baidu.hugegraph2.schema;

import com.baidu.hugegraph2.backend.tx.SchemaTransaction;
import com.baidu.hugegraph2.type.define.Cardinality;
import com.baidu.hugegraph2.type.define.DataType;
import com.baidu.hugegraph2.type.schema.PropertyKey;

/**
 * Created by jishilei on 17/3/17.
 */
public class HugePropertyKey extends PropertyKey {

    private DataType dataType;
    private Cardinality cardinality;

    public HugePropertyKey(String name, SchemaTransaction transaction) {
        super(name, transaction);
        this.dataType = DataType.OBJECT;
        this.cardinality = Cardinality.SINGLE;
    }

    @Override
    public DataType dataType() {
        return dataType;
    }

    public void dataType(DataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public Cardinality cardinality() {
        return cardinality;
    }

    public void cardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    @Override
    public PropertyKey asText() {
        this.dataType(DataType.TEXT);
        return this;
    }

    @Override
    public PropertyKey asInt() {
        this.dataType(DataType.INT);
        return this;
    }

    @Override
    public PropertyKey asTimestamp() {
        this.dataType(DataType.TIMESTAMP);
        return this;
    }

    @Override
    public PropertyKey asUuid() {
        this.dataType(DataType.UUID);
        return this;
    }

    @Override
    public PropertyKey single() {
        this.cardinality(Cardinality.SINGLE);
        return this;
    }

    @Override
    public PropertyKey multiple() {
        this.cardinality(Cardinality.MULTIPLE);
        return this;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, dataType=%s, cardinality=%s}",
                name, dataType.toString(), cardinality.toString());
    }

    public String schema() {
        String schema = "schema.propertyKey(\"" + name + "\")"
                + "." + dataType.schema() + "()"
                + "." + cardinality.schema() + "()"
                + "." + propertiesSchema()
                + ".create();";
        return schema;
    }

    public void create() {
        transaction.addPropertyKey(this);
    }

    public void remove() {
        transaction.removePropertyKey(name);
    }
}
