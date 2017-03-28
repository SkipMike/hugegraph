// Copyright 2017 HugeGraph Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.baidu.hugegraph.graphdb.query;

import com.baidu.hugegraph.core.attribute.Cmp;
import com.baidu.hugegraph.core.attribute.Contain;
import org.apache.tinkerpop.gremlin.process.traversal.Compare;
import org.apache.tinkerpop.gremlin.process.traversal.Contains;

import java.util.function.BiPredicate;

/**
 * A special kind of {@link BiPredicate} which marks all the predicates that are natively supported by
 * HugeGraph and known to the query optimizer. Contains some custom methods that HugeGraph needs for
 * query answering and evaluation.
 * </p>
 * This class contains a subclass used to convert Tinkerpop's {@link BiPredicate} implementations to the corresponding HugeGraph predicates.
 *
 */
public interface HugeGraphPredicate extends BiPredicate<Object, Object> {

    /**
     * Whether the given condition is a valid condition for this predicate.
     * </p>
     * For instance, the {@link Cmp#GREATER_THAN} would require that the condition is comparable and not null.
     *
     * @param condition
     * @return
     */
    public boolean isValidCondition(Object condition);

    /**
     * Whether the given class is a valid data type for a value to which this predicate may be applied.
     * </p>
     * For instance, the {@link Cmp#GREATER_THAN} can only be applied to {@link Comparable} values.
     *
     * @param clazz
     * @return
     */
    public boolean isValidValueType(Class<?> clazz);

    /**
     * Whether this predicate has a predicate that is semantically its negation.
     * For instance, {@link Cmp#EQUAL} and {@link Cmp#NOT_EQUAL} are negatives of each other.
     *
     * @return
     */
    public boolean hasNegation();

    /**
     * Returns the negation of this predicate if it exists, otherwise an exception is thrown. Check {@link #hasNegation()} first.
     * @return
     */
    public HugeGraphPredicate negate();

    /**
     * Returns true if this predicate is in query normal form.
     * @return
     */
    public boolean isQNF();


    @Override
    public boolean test(Object value, Object condition);


    public static class Converter {

        /**
         * Convert Tinkerpop's comparison operators to HugeGraph's
         *
         * @param p Any predicate
         * @return A Predicate equivalent to the given predicate
         * @throws IllegalArgumentException if the given Predicate is unknown
         */
        public static final HugeGraphPredicate convertInternal(BiPredicate p) {
            if (p instanceof HugeGraphPredicate) {
                return (HugeGraphPredicate)p;
            } else if (p instanceof Compare) {
                Compare comp = (Compare)p;
                switch(comp) {
                    case eq: return Cmp.EQUAL;
                    case neq: return Cmp.NOT_EQUAL;
                    case gt: return Cmp.GREATER_THAN;
                    case gte: return Cmp.GREATER_THAN_EQUAL;
                    case lt: return Cmp.LESS_THAN;
                    case lte: return Cmp.LESS_THAN_EQUAL;
                    default: throw new IllegalArgumentException("Unexpected comparator: " + comp);
                }
            } else if (p instanceof Contains) {
                Contains con = (Contains)p;
                switch (con) {
                    case within: return Contain.IN;
                    case without: return Contain.NOT_IN;
                    default: throw new IllegalArgumentException("Unexpected container: " + con);

                }
            } else return null;
        }

        public static final HugeGraphPredicate convert(BiPredicate p) {
            HugeGraphPredicate hugegraphPred = convertInternal(p);
            if (hugegraphPred==null) throw new IllegalArgumentException("HugeGraph does not support the given predicate: " + p);
            return hugegraphPred;
        }

        public static final boolean supports(BiPredicate p) {
            return convertInternal(p)!=null;
        }
    }

}
