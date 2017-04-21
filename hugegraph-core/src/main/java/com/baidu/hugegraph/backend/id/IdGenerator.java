package com.baidu.hugegraph.backend.id;

import com.baidu.hugegraph.schema.SchemaElement;
import com.baidu.hugegraph.structure.HugeEdge;
import com.baidu.hugegraph.structure.HugeVertex;
import com.baidu.hugegraph.type.HugeTypes;
import com.baidu.hugegraph.util.NumericUtil;
import com.baidu.hugegraph.util.StringEncoding;

public abstract class IdGenerator {

    /****************************** id type ******************************/

    public static enum IdType {
        LONG,
        STRING;
    }

    // this could be set by conf
    public static IdType ID_TYPE = IdType.STRING;

    /****************************** id generate ******************************/

    public abstract Id generate(SchemaElement entry);
    public abstract Id generate(HugeVertex entry);
    public abstract Id generate(HugeEdge entry);

    public abstract Id[] split(Id id);

    // generate a string id
    public Id generate(String id) {
        switch (ID_TYPE) {
            case LONG:
                return new LongId(Long.parseLong(id));
            case STRING:
                return new StringId(id);
            default:
                assert false;
                return null;
        }
    }

    // generate a long id
    public Id generate(long id) {
        switch (ID_TYPE) {
            case LONG:
                return new LongId(id);
            case STRING:
                return new StringId(String.valueOf(id));
            default:
                assert false;
                return null;
        }
    }

    // parse an id from bytes
    public Id parse(byte[] bytes) {
        switch (ID_TYPE) {
            case LONG:
                return new LongId(bytes);
            case STRING:
                return new StringId(bytes);
            default:
                assert false;
                return null;
        }
    }

    /****************************** id defines ******************************/

    static class StringId implements Id {

        private String id;

        public StringId(String id) {
            this.id = id;
        }

        public StringId(byte[] bytes) {
            this.id = StringEncoding.decodeString(bytes);
        }

        @Override
        public Id prefixWith(HugeTypes type) {
            return new StringId(String.format("%x%s", type.code(), this.id));
        }

        @Override
        public String asString() {
            return this.id;
        }

        @Override
        public long asLong() {
            return Long.parseLong(this.id);
        }

        @Override
        public byte[] asBytes() {
            return StringEncoding.encodeString(this.id);
        }

        @Override
        public int compareTo(Id other) {
            return this.id.compareTo(((StringId) other).id);
        }

        @Override
        public String toString() {
            return this.asString();
        }
    }

    static class LongId implements Id {

        private long id;

        public LongId(long id) {
            this.id = id;
        }

        public LongId(byte[] bytes) {
            this.id = NumericUtil.bytesToLong(bytes);
        }

        @Override
        public Id prefixWith(HugeTypes type) {
            long t = type.code();
            this.id = (this.id & 0x00ffffffffffffffL) & (t << 56);
            return this;
        }

        @Override
        public String asString() {
            return String.valueOf(this.id);
        }

        @Override
        public long asLong() {
            return this.id;
        }

        @Override
        public byte[] asBytes() {
            return NumericUtil.longToBytes(this.id);
        }

        @Override
        public int compareTo(Id other) {
            long otherId = ((LongId) other).id;
            return Long.compare(this.id, otherId);
        }

        @Override
        public String toString() {
            return this.asString();
        }
    }

}
