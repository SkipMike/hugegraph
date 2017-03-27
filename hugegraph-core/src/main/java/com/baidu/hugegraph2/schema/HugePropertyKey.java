package com.baidu.hugegraph2.schema;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.baidu.hugegraph2.backend.tx.SchemaTransaction;
import com.baidu.hugegraph2.type.define.Cardinality;
import com.baidu.hugegraph2.type.define.DataType;
import com.baidu.hugegraph2.type.schema.PropertyKey;
import com.baidu.hugegraph2.type.schema.SchemaType;
import com.baidu.hugegraph2.util.StringUtil;

/**
 * Created by jishilei on 17/3/17.
 */
public class HugePropertyKey implements PropertyKey {

    private String name;
    private SchemaTransaction transaction;

    private DataType dataType;
    private Cardinality cardinality;
    // propertykey可能还有properties
    private Set<String> properties;


    public HugePropertyKey(String name, SchemaTransaction transaction) {
        this.name = name;
        this.transaction = transaction;
        this.dataType = DataType.OBJECT;
        this.cardinality = Cardinality.SINGLE;
        this.properties = null;
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
    public Set<String> properties() {
        return properties;
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
    public PropertyKey asTimeStamp() {
        this.dataType(DataType.TIMESTAMP);
        return this;
    }

    @Override
    public PropertyKey asUUID() {
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
    public SchemaType properties(String... propertyNames) {
        if (properties == null) {
            properties = new HashSet<>();
        }
        properties.addAll(Arrays.asList(propertyNames));
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("{name=%s, dataType=%s, cardinality=%s}",
                name, dataType.toString(), cardinality.toString());
    }

    @Override
    public String schema() {
        return "schema.propertyKey(\"" + name + "\")"
                + "." + cardinality.toString().toLowerCase() + "()"
                + ".as" + StringUtil.captureName(dataType.toString().toLowerCase()) + "()"
                + ".create();";
    }

    @Override
    public void create() {
        transaction.addPropertyKey(this);
    }

    @Override
    public void remove() {
        transaction.removePropertyKey(name);
    }
}
