package com.baidu.hugegraph2.schema;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph2.backend.BackendException;
import com.baidu.hugegraph2.backend.tx.SchemaTransaction;
import com.baidu.hugegraph2.type.HugeType;
import com.baidu.hugegraph2.type.Namifiable;
import com.baidu.hugegraph2.type.schema.PropertyKey;

/**
 * Created by liningrui on 2017/3/27.
 */
public abstract class SchemaElement implements Namifiable, HugeType {

    private static final Logger logger = LoggerFactory.getLogger(SchemaElement.class);

    protected String name;
    protected String schema;
    // TODO:This is a questionable place，mutual reference
    protected SchemaTransaction transaction;
    protected Map<String, PropertyKey> properties;

    public SchemaElement(String name, SchemaTransaction transaction) {
        this.name = name;
        this.transaction = transaction;
        this.properties = null;
    }

    public Map<String, PropertyKey> properties() {
        return properties;
    }

    public SchemaElement properties(String... propertyNames) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        for (String propertyName : propertyNames) {
            properties.put(propertyName, new HugePropertyKey(propertyName, transaction));
        }
        return this;
    }

    public String propertiesSchema() {
        String props = "";
        if (properties != null) {
            for (String propertyName : properties.keySet()) {
                props += "\"";
                props += propertyName;
                props += "\",";
            }
        }
        int endIdx = props.lastIndexOf(",") > 0 ? props.length() - 1 : props.length();
        return "properties(" + props.substring(0, endIdx) + ")";
    }

    public boolean commit() {
        try {
            this.transaction.commit();
            return true;
        } catch (BackendException e) {
            logger.error("Failed to commit schema changes: {}", e.getMessage());
            try {
                this.transaction.rollback();
            } catch (BackendException e2) {
                logger.error("Failed to rollback schema changes: {}", e2.getMessage());
            }
        }
        return false;
    }

    @Override
    public String name() {
        return name;
    }

    public abstract String schema();

    public abstract void create();

    public abstract void remove();
}
