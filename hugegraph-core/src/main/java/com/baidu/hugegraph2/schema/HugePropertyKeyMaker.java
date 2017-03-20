package com.baidu.hugegraph2.schema;

import com.baidu.hugegraph2.DataType;
import com.baidu.hugegraph2.Store.SchemaStore;
import com.baidu.hugegraph2.schema.base.maker.PropertyKeyMaker;
import com.baidu.hugegraph2.schema.base.PropertyKey;
import com.baidu.hugegraph2.schema.base.SchemaType;

/**
 * Created by jishilei on 17/3/17.
 */
public class HugePropertyKeyMaker implements PropertyKeyMaker {

    private SchemaStore schemaStore;
    private HugePropertyKey propertyKey;
    private String name;



    public HugePropertyKeyMaker(SchemaStore schemaStore,String name){
        propertyKey = new HugePropertyKey(name);
        this.name = name;
        this.schemaStore = schemaStore;
    }


    public PropertyKey getPropertyKey(){
        return propertyKey;
    }



    @Override
    public PropertyKeyMaker Text() {
        this.propertyKey.setDataType(DataType.TEXT);
        return this;
    }

    @Override
    public PropertyKeyMaker Int() {
        this.propertyKey.setDataType(DataType.INT);
        return this;
    }


    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public PropertyKey create() {
        schemaStore.addPropertyKey(propertyKey);
        return propertyKey;
    }

    @Override
    public SchemaType save() {
        schemaStore.removePropertyKey(name);
        schemaStore.addPropertyKey(propertyKey);
        return propertyKey;
    }

    @Override
    public void remove() {

        schemaStore.removePropertyKey(name);
    }
}
