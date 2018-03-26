package com.baidu.hugegraph.unit.core;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.baidu.hugegraph.HugeException;
import com.baidu.hugegraph.backend.id.IdGenerator;
import com.baidu.hugegraph.backend.id.SplicingIdGenerator;
import com.baidu.hugegraph.backend.serializer.TextBackendEntry;
import com.baidu.hugegraph.backend.store.BackendAction;
import com.baidu.hugegraph.backend.store.BackendAction.Action;
import com.baidu.hugegraph.backend.store.BackendEntry;
import com.baidu.hugegraph.backend.store.BackendMutation;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.type.HugeType;
import com.baidu.hugegraph.unit.BaseUnitTest;

public class BackendMutationTest extends BaseUnitTest {

    @Before
    public void setup() {
        // pass
    }

    @After
    public void teardown() throws Exception {
        // pass
    }

    @Test
    public void testInsertAndInsertEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("2");
        BackendEntry entry3 = constructBackendEntry("1");

        mutation.add(entry1, Action.INSERT);
        mutation.add(entry2, Action.INSERT);
        mutation.add(entry3, Action.INSERT);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.INSERT,
                            get(mutation, "1").get(0).action());
        Assert.assertEquals(1, get(mutation, "2").size());
        Assert.assertEquals(Action.INSERT,
                            get(mutation, "2").get(0).action());
    }

    @Test
    public void testInsertAndDeleteEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("1");

        mutation.add(entry1, Action.INSERT);
        Assert.assertThrows(HugeException.class, () -> {
            mutation.add(entry2, Action.DELETE);
        });
    }

    @Test
    public void testInsertAndAppendEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.INSERT);
        Assert.assertThrows(HugeException.class, () -> {
            mutation.add(entry2, Action.APPEND);
        });
    }

    @Test
    public void testInsertAndEliminateEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.INSERT);
        Assert.assertThrows(HugeException.class, () -> {
            mutation.add(entry2, Action.ELIMINATE);
        });
    }

    @Test
    public void testDeleteAndInsertEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("2");
        BackendEntry entry3 = constructBackendEntry("1");

        mutation.add(entry1, Action.DELETE);
        mutation.add(entry2, Action.DELETE);
        mutation.add(entry3, Action.INSERT);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.INSERT,
                            get(mutation, "1").get(0).action());
        Assert.assertEquals(1, get(mutation, "2").size());
        Assert.assertEquals(Action.DELETE,
                            get(mutation, "2").get(0).action());
    }

    @Test
    public void testDeleteAndDeleteEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("1");

        mutation.add(entry1, Action.DELETE);
        mutation.add(entry2, Action.DELETE);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.DELETE,
                            get(mutation, "1").get(0).action());
    }

    @Test
    public void testDeleteAndAppendEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.DELETE);
        Assert.assertThrows(HugeException.class, () -> {
            mutation.add(entry2, Action.APPEND);
        });
    }

    @Test
    public void testDeleteAndEliminateEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.DELETE);
        Assert.assertThrows(HugeException.class, () -> {
            mutation.add(entry2, Action.ELIMINATE);
        });
    }

    @Test
    public void testAppendAndInsertEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.APPEND);
        mutation.add(entry2, Action.INSERT);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.INSERT,
                            get(mutation, "1").get(0).action());
    }

    @Test
    public void testAppendAndDeleteEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1");

        mutation.add(entry1, Action.APPEND);
        mutation.add(entry2, Action.DELETE);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.DELETE,
                            get(mutation, "1").get(0).action());
    }

    @Test
    public void testAppendAndAppendEntryWithSameId() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "city", "Wuhan");

        mutation.add(entry1, Action.APPEND);
        mutation.add(entry2, Action.APPEND);

        Assert.assertEquals(2, get(mutation, "1").size());
        Assert.assertEquals(Action.APPEND,
                            get(mutation, "1").get(0).action());
        Assert.assertEquals(Action.APPEND,
                            get(mutation, "1").get(1).action());
    }

    @Test
    public void testAppendAndAppendEntryWithSameEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.APPEND);
        mutation.add(entry2, Action.APPEND);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.APPEND,
                            get(mutation, "1").get(0).action());
    }

    @Test
    public void testAppendAndEliminateEntryWithSameId() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "city", "Wuhan");

        mutation.add(entry1, Action.APPEND);
        mutation.add(entry2, Action.ELIMINATE);

        Assert.assertEquals(2, get(mutation, "1").size());
        Assert.assertEquals(Action.APPEND,
                            get(mutation, "1").get(0).action());
        Assert.assertEquals(Action.ELIMINATE,
                            get(mutation, "1").get(1).action());
    }

    @Test
    public void testAppendAndEliminateEntryWithSameEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.APPEND);
        mutation.add(entry2, Action.ELIMINATE);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.ELIMINATE,
                            get(mutation, "1").get(0).action());
    }

    @Test
    public void testEliminateAndInsertEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.ELIMINATE);
        mutation.add(entry2, Action.INSERT);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.INSERT,
                            get(mutation, "1").get(0).action());
    }

    @Test
    public void testEliminateAndDeleteEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1");

        mutation.add(entry1, Action.ELIMINATE);
        mutation.add(entry2, Action.DELETE);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.DELETE,
                            get(mutation, "1").get(0).action());
    }

    @Test
    public void testEliminateAndAppendEntryWithSameId() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "city", "Wuhan");

        mutation.add(entry1, Action.ELIMINATE);
        mutation.add(entry2, Action.APPEND);

        Assert.assertEquals(2, get(mutation, "1").size());
        Assert.assertEquals(Action.ELIMINATE,
                            get(mutation, "1").get(0).action());
        Assert.assertEquals(Action.APPEND,
                            get(mutation, "1").get(1).action());
    }

    @Test
    public void testEliminateAndAppendEntryWithSameEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.ELIMINATE);
        mutation.add(entry2, Action.APPEND);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.APPEND,
                            get(mutation, "1").get(0).action());
    }

    @Test
    public void testEliminateAndEliminateEntryWithSameId() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "city", "Wuhan");

        mutation.add(entry1, Action.ELIMINATE);
        mutation.add(entry2, Action.ELIMINATE);

        Assert.assertEquals(2, get(mutation, "1").size());
        Assert.assertEquals(Action.ELIMINATE,
                            get(mutation, "1").get(0).action());
        Assert.assertEquals(Action.ELIMINATE,
                            get(mutation, "1").get(1).action());
    }

    @Test
    public void testEliminateAndEliminateEntryWithSameEntry() {
        BackendMutation mutation = new BackendMutation();
        BackendEntry entry1 = constructBackendEntry("1", "name", "marko");
        BackendEntry entry2 = constructBackendEntry("1", "name", "marko");

        mutation.add(entry1, Action.ELIMINATE);
        mutation.add(entry2, Action.ELIMINATE);

        Assert.assertEquals(1, get(mutation, "1").size());
        Assert.assertEquals(Action.ELIMINATE,
                            get(mutation, "1").get(0).action());
    }

    private static BackendEntry constructBackendEntry(String id,
                                                      String... columns) {
        assert (columns.length == 0 || columns.length == 2);
        TextBackendEntry entry = new TextBackendEntry(HugeType.VERTEX,
                                                      IdGenerator.of(id));
        if (columns.length == 2) {
            String subId = SplicingIdGenerator.concat(id, columns[0]);
            entry.subId(IdGenerator.of(subId));
        }
        for (int i = 0; i < columns.length; i = i + 2) {
            entry.column(columns[i], columns[i + 1]);
        }
        return entry;
    }

    private static List<BackendAction> get(BackendMutation mutation, String id) {
        return mutation.mutation(HugeType.VERTEX, IdGenerator.of(id));
    }
}
