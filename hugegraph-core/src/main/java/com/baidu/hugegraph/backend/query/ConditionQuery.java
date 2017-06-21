package com.baidu.hugegraph.backend.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.T;

import com.baidu.hugegraph.backend.BackendException;
import com.baidu.hugegraph.backend.id.SplicingIdGenerator;
import com.baidu.hugegraph.backend.query.Condition.Relation;
import com.baidu.hugegraph.backend.query.Condition.RelationType;
import com.baidu.hugegraph.structure.HugeElement;
import com.baidu.hugegraph.type.HugeType;
import com.baidu.hugegraph.type.define.HugeKeys;

public class ConditionQuery extends IdQuery {

    // conditions will be concated with `and` by default
    private Set<Condition> conditions;

    public ConditionQuery(HugeType resultType) {
        super(resultType);
        this.conditions = new LinkedHashSet<>();
    }

    public ConditionQuery query(Condition condition) {
        // query by id (HugeGraph-259)
        if (condition instanceof Relation) {
            Relation relation = (Relation) condition;
            if (relation.key().equals(HugeKeys.ID)
                    && relation.relation() == RelationType.EQ) {
                super.query(HugeElement.getIdValue(T.id, relation.value()));
                return this;
            }
        }

        this.conditions.add(condition);
        return this;
    }

    public ConditionQuery eq(HugeKeys key, Object value) {
        // filter value by key
        return this.query(Condition.eq(key, value));
    }

    public ConditionQuery gt(HugeKeys key, Object value) {
        return this.query(Condition.gt(key, value));
    }

    public ConditionQuery gte(HugeKeys key, Object value) {
        return this.query(Condition.gte(key, value));
    }

    public ConditionQuery lt(HugeKeys key, Object value) {
        return this.query(Condition.lt(key, value));
    }

    public ConditionQuery lte(HugeKeys key, Object value) {
        return this.query(Condition.lte(key, value));
    }

    public ConditionQuery neq(HugeKeys key, Object value) {
        return this.query(Condition.neq(key, value));
    }

    public ConditionQuery key(HugeKeys key, String value) {
        return this.query(Condition.containsKey(key, value));
    }

    public ConditionQuery scan(String start, String end) {
        return this.query(Condition.scan(start, end));
    }

    @Override
    public Set<Condition> conditions() {
        return Collections.unmodifiableSet(this.conditions);
    }

    public void resetConditions(Set<Condition> conditions) {
        this.conditions = conditions;
    }

    public void resetConditions() {
        this.conditions = new LinkedHashSet<>();
    }

    @Override
    public String toString() {
        return String.format("%s and %s",
                super.toString(),
                this.conditions.toString());
    }

    public boolean allSysprop() {
        for (Condition c : this.conditions) {
            if (!c.isSysprop()) {
                return false;
            }
        }
        return true;
    }

    public List<Condition.Relation> relations() {
        List<Condition.Relation> relations = new LinkedList<>();
        for (Condition c : this.conditions) {
            relations.addAll(c.relations());
        }
        return relations;
    }

    public Object condition(Object key) {
        for (Condition c : this.conditions) {
            if (c.isRelation()) {
                Condition.Relation r = (Condition.Relation) c;
                if (r.key().equals(key)) {
                    return r.value();
                }
            }
            // TODO: deal with other Condition
        }
        return null;
    }

    public void unsetCondition(Object key) {
        Iterator<Condition> iterator = this.conditions.iterator();
        while (iterator.hasNext()) {
            Condition c = iterator.next();
            if (c.isRelation()
                    && ((Condition.Relation) c).key().equals(key)) {
                iterator.remove();
            }
            // TODO: deal with other Condition
        }
    }

    public boolean containsCondition(HugeKeys key) {
        return this.condition(key) != null;
    }

    public boolean containsCondition(Condition.RelationType type) {
        for (Condition c : this.conditions) {
            if (c.isRelation()
                    && ((Condition.Relation) c).relation().equals(type)) {
                return true;
            }
            // TODO: deal with other Condition
        }
        return false;
    }

    public boolean containsScanCondition() {
        return this.containsCondition(Condition.RelationType.SCAN);
    }

    public List<Condition> userpropConditions() {
        List<Condition> conds = new LinkedList<>();
        for (Condition c : this.conditions) {
            if (!c.isSysprop()) {
                conds.add(c);
            }
        }
        return conds;
    }

    public void resetUserpropConditions() {
        Iterator<Condition> iterator = this.conditions.iterator();
        while (iterator.hasNext()) {
            Condition c = iterator.next();
            if (!c.isSysprop()) {
                iterator.remove();
            }
        }
    }

    public Set<String> userpropKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (Relation r : this.relations()) {
            if (!r.isSysprop()) {
                Condition.UserpropRelation ur = (Condition.UserpropRelation) r;
                keys.add(ur.key());
            }
        }
        return keys;
    }

    public List<Object> userpropValues(List<String> fields) {
        List<Object> values = new ArrayList<>(fields.size());
        for (String field : fields) {
            boolean got = false;
            for (Condition c : this.conditions) {
                if (!c.isRelation()) {
                    // TODO: deal with other Condition like AND/OR
                    throw new BackendException(
                            "Not support getting userprop from non relation");
                }
                Relation r = ((Relation) c);
                if (r.key().equals(field) && !c.isSysprop()) {
                    assert r.relation() == Condition.RelationType.EQ;
                    values.add(r.value());
                    got = true;
                }
            }
            if (!got) {
                throw new BackendException(
                        "No such userprop named '%s' in the query '%s'",
                        field, this);
            }
        }
        return values;
    }

    public String userpropValuesString(List<String> fields) {
        return SplicingIdGenerator.concatValues(this.userpropValues(fields));
    }

    // TODO: remove this method after changing fields type to List<String>
    // that's when HugeGraph-290 is done
    public String userpropValuesString(Set<String> fields) {
        return userpropValuesString(new ArrayList<>(fields));
    }

    public boolean hasSearchCondition() {
        // NOTE: we need to judge all the conditions, including the nested
        for (Condition.Relation r : this.relations()) {
            if (r.relation().isSearchType()) {
                return true;
            }
        }
        return false;
    }

    public boolean matchUserpropKeys(Set<String> keys) {
        Set<String> conditionKeys = userpropKeys();
        if (keys.size() == conditionKeys.size()
                && keys.containsAll(conditionKeys)) {
            return true;
        }

        return false;
    }
}