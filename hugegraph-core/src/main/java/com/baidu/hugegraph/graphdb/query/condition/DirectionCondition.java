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

package com.baidu.hugegraph.graphdb.query.condition;

import com.baidu.hugegraph.core.HugeGraphEdge;
import com.baidu.hugegraph.core.HugeGraphVertexProperty;
import com.baidu.hugegraph.core.HugeGraphRelation;
import com.baidu.hugegraph.core.HugeGraphVertex;
import com.baidu.hugegraph.graphdb.relations.CacheEdge;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */

public class DirectionCondition<E extends HugeGraphRelation> extends Literal<E> {

    private final HugeGraphVertex baseVertex;
    private final Direction direction;

    public DirectionCondition(HugeGraphVertex vertex, Direction dir) {
        assert vertex != null && dir != null;
        this.baseVertex = vertex;
        this.direction = dir;
    }

    @Override
    public boolean evaluate(E element) {
        if (direction==Direction.BOTH) return true;
        if (element instanceof CacheEdge) {
            return direction==((CacheEdge)element).getVertexCentricDirection();
        } else if (element instanceof HugeGraphEdge) {
            return ((HugeGraphEdge)element).vertex(direction).equals(baseVertex);
        } else if (element instanceof HugeGraphVertexProperty) {
            return direction==Direction.OUT;
        }
        return false;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getType()).append(direction).append(baseVertex).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (other == null || !getClass().isInstance(other))
            return false;

        DirectionCondition oth = (DirectionCondition)other;
        return direction == oth.direction && baseVertex.equals(oth.baseVertex);
    }

    @Override
    public String toString() {
        return "dir["+getDirection()+"]";
    }
}
