package com.baidu.hugegraph.core;

import java.util.List;
import java.util.function.Consumer;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Assert;

import com.baidu.hugegraph.backend.id.Id;
import com.baidu.hugegraph.core.FakeObjects.FakeVertex;

public class Utils {

    public static boolean containsId(List<Vertex> vertexes, Id id) {
        for (Vertex v : vertexes) {
            if (v.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(List<Vertex> vertexes,
            FakeVertex fakeVertex) {
        for (Vertex v : vertexes) {
            if (fakeVertex.equalsVertex(v)) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    interface ThrowableRunnable {
        void run() throws Throwable;
    }

    public static void assertThrows(
            Class<? extends Throwable> throwable,
            ThrowableRunnable runnable) {
        assertThrows(throwable, runnable, t -> {});
    }

    public static void assertThrows(
            Class<? extends Throwable> throwable,
            ThrowableRunnable runnable,
            Consumer<Throwable> exceptionConsumer) {
        boolean fail = false;
        try {
            runnable.run();
            fail = true;
        } catch (Throwable t) {
            Assert.assertTrue(String.format(
                    "Bad exception type %s(expect %s)",
                    t.getClass(), throwable),
                    throwable.isInstance(t));
            exceptionConsumer.accept(t);
        }
        if (fail) {
            Assert.fail(String.format("No exception was thrown(expect %s)",
                    throwable));
        }
    }
}
