package com.baidu.hugegraph.backend.store;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.backend.id.Id;
import com.baidu.hugegraph.util.E;

public class BackendMutation {

    private Map<Id, List<MutateItem>> updates;

    public BackendMutation() {
        this.updates = new LinkedHashMap<>();
    }

    public void add(BackendEntry entry, MutateAction mutateAction) {
        Id id = entry.id();
        List<MutateItem> items = this.updates.get(id);
        // If there is no entity of this id, add it
        if (items == null) {
            items = new LinkedList<>();
            items.add(MutateItem.of(entry, mutateAction));
            updates.put(id, items);
            return;
        }

        /*
         * TODO: Should do some optimize when seperate edges from vertex
         * The Optimized scenes include but are not limited to：
         * 1、If you want to delete an entry, the other mutations previously
         * can be ignored
         * 2、As similar to the item No. one, If you want to insert an entry,
         * the other mutations previously also can be ignored.
         * 3、To be added
         */

        items.add(MutateItem.of(entry, mutateAction));
    }

    /**
     * Reset all items in mutations.
     */
    private void reset() {
        this.updates = new LinkedHashMap<>();
    }

    /**
     * Reset all items in mutations of this id.
     * @param id
     */
    private void reset(Id id) {
        this.updates.replace(id, new LinkedList<>());
    }

    public Map<Id, List<MutateItem>> mutation() {
        return this.updates;
    }

    /**
     * Whether this mutation is empty
     * @return boolean
     */
    public boolean isEmpty() {
        return this.updates.isEmpty();
    }

    /**
     * Merges another mutation into this mutation. Ensures that all additions
     * and deletions are added to this mutation. Does not remove duplicates
     * if such exist - this needs to be ensured by the caller.
     *
     * @param mutation
     */
    public void merge(BackendMutation mutation) {
        E.checkNotNull(mutation, "mutation");
        for (List<MutateItem> items : mutation.mutation().values()) {
            for (MutateItem item : items) {
                this.add(item.entry(), item.type());
            }
        }
    }

    @Override
    public String toString() {
        return String.format("BackendMutation{mutations=%s}", this.updates);
    }
}
