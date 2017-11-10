/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.core;

import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import com.baidu.hugegraph.backend.BackendException;
import com.baidu.hugegraph.exception.NotFoundException;
import com.baidu.hugegraph.schema.EdgeLabel;
import com.baidu.hugegraph.schema.IndexLabel;
import com.baidu.hugegraph.schema.SchemaManager;
import com.baidu.hugegraph.schema.VertexLabel;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.type.HugeType;
import com.baidu.hugegraph.type.define.IndexType;

public class IndexLabelCoreTest extends SchemaCoreTest {

    @Test
    public void testAddIndexLabelOfVertex() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();
        schema.indexLabel("personByCity").onV("person").secondary()
              .by("city").create();
        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();

        VertexLabel person = schema.getVertexLabel("person");
        IndexLabel personByCity = schema.getIndexLabel("personByCity");
        IndexLabel personByAge = schema.getIndexLabel("personByAge");

        Assert.assertNotNull(personByCity);
        Assert.assertNotNull(personByAge);
        Assert.assertEquals(2, person.indexNames().size());
        Assert.assertTrue(person.indexNames().contains("personByCity"));
        Assert.assertTrue(person.indexNames().contains("personByAge"));
        Assert.assertEquals(HugeType.VERTEX_LABEL, personByCity.baseType());
        Assert.assertEquals(HugeType.VERTEX_LABEL, personByAge.baseType());
        Assert.assertEquals("person", personByCity.baseValue());
        Assert.assertEquals("person", personByAge.baseValue());
        Assert.assertEquals(IndexType.SECONDARY, personByCity.indexType());
        Assert.assertEquals(IndexType.SEARCH, personByAge.indexType());
    }

    @Test
    public void testAddIndexLabelWithIllegalName() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();

        // Empty string
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("").onV("person").by("name").create();
        });
        // One space
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel(" ").onV("person").by("name").create();
        });
        // Two spaces
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("  ").onV("person").by("name").create();
        });
        // Multi spaces
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("    ").onV("person").by("name").create();
        });
        // Start with '~'
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("~").onV("person").by("name").create();
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("~ ").onV("person").by("name").create();
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("~x").onV("person").by("name").create();
        });
    }

    @Test
    public void testAddIndexLabelOfEdge() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("author").properties("id", "name")
              .primaryKeys("id").create();
        schema.vertexLabel("book").properties("name")
              .primaryKeys("name").create();
        schema.edgeLabel("authored").singleTime()
              .link("author", "book")
              .properties("contribution")
              .create();

        schema.indexLabel("authoredByContri").onE("authored").secondary()
              .by("contribution").create();

        EdgeLabel authored = schema.getEdgeLabel("authored");
        IndexLabel authoredByContri = schema.getIndexLabel("authoredByContri");

        Assert.assertNotNull(authoredByContri);
        Assert.assertEquals(1, authored.indexNames().size());
        Assert.assertTrue(authored.indexNames().contains("authoredByContri"));
        Assert.assertEquals(HugeType.EDGE_LABEL, authoredByContri.baseType());
        Assert.assertEquals("authored", authoredByContri.baseValue());
        Assert.assertEquals(IndexType.SECONDARY, authoredByContri.indexType());
    }

    @Test
    public void testAddIndexLabelOfVertexWithVertexExist() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();

        graph().addVertex(T.label, "person", "name", "Baby",
                          "city", "Hongkong", "age", 3);

        Assert.assertThrows(BackendException.class, () -> {
            graph().traversal().V().hasLabel("person")
                   .has("city", "Hongkong").next();
        });

        schema.indexLabel("personByCity").onV("person").secondary()
              .by("city").create();

        Vertex vertex = graph().traversal().V().hasLabel("person")
                        .has("city", "Hongkong").next();
        Assert.assertNotNull(vertex);

        Assert.assertThrows(BackendException.class, () -> {
            graph().traversal().V().hasLabel("person")
                   .has("age", P.inside(2, 4)).next();
        });
        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();

        vertex = graph().traversal().V().hasLabel("person")
                 .has("age", P.inside(2, 4)).next();
        Assert.assertNotNull(vertex);
    }

    @Test
    public void testAddIndexLabelOfEdgeWithEdgeExist() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("author").properties("id", "name")
              .primaryKeys("id").create();
        schema.vertexLabel("book").properties("name")
              .primaryKeys("name").create();
        schema.edgeLabel("authored").singleTime().link("author", "book")
              .properties("contribution").create();

        Vertex james = graph().addVertex(T.label, "author", "id", 1,
                                         "name", "James Gosling");
        Vertex java1 = graph().addVertex(T.label, "book", "name", "java-1");

        james.addEdge("authored", java1,"contribution", "test");
        Assert.assertThrows(BackendException.class, () -> {
            graph().traversal().E().hasLabel("authored")
                   .has("contribution", "test").next();
        });

        schema.indexLabel("authoredByContri").onE("authored")
              .secondary().by("contribution").create();

        Edge edge = graph().traversal().E().hasLabel("authored")
                    .has("contribution", "test").next();
        Assert.assertNotNull(edge);
    }

    @Test
    public void testAddIndexLabelOnUndefinedSchemaLabel() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("authorByName").onV("undefined-vertex-label")
                  .by("name").secondary().create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("authoredByContri").onE("undefined-edge-label")
                  .by("contribution").secondary().create();
        });
    }

    @Test
    public void testAddIndexLabelByUndefinedProperty() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("author").properties("id", "name")
              .primaryKeys("id").create();
        schema.vertexLabel("book").properties("name")
              .primaryKeys("name").create();
        schema.edgeLabel("authored").singleTime().link("author", "book")
              .properties("contribution").create();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("authorByData").onV("author")
                  .by("undefined-property").secondary().create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("authoredByData").onE("authored")
                  .by("undefined-property").secondary().create();
        });
    }

    @Test
    public void testAddIndexLabelByNotBelongedProperty() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();

        schema.vertexLabel("author").properties("id", "name")
              .primaryKeys("id").create();
        schema.vertexLabel("book").properties("name")
              .primaryKeys("name").create();
        schema.edgeLabel("authored").singleTime().link("author", "book")
              .properties("contribution").create();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("bookById").onV("book")
                  .by("id").secondary().create();
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("authoredByName").onE("authored")
                  .by("name").secondary().create();
        });
    }

    @Test
    public void testAddIndexLabelWithSameFields() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();

        schema.indexLabel("personByCity").onV("person").secondary()
              .by("city").create();
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("personByCity1").onV("person").secondary()
                  .by("city").create();
        });

        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("personByAge1").onV("person").search()
                  .by("age").create();
        });

        schema.indexLabel("personByAgeAndCity").onV("person").secondary()
              .by("age", "city").create();
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("personByAgeAndCity1").onV("person").secondary()
                  .by("age", "city").create();
        });
    }

    @Test
    public void testAddIndexLabelWithSameFieldsBetweenSearchAndSecond() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();

        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();
        schema.indexLabel("personByAge2").onV("person").secondary()
              .by("age").create();
    }

    @Test
    public void testAddIndexLabelIsPrefixOfExisted() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();

        schema.indexLabel("personByAgeAndCity").onV("person").secondary()
              .by("age", "city").create();
        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            schema.indexLabel("personByAge1").onV("person").secondary()
                  .by("age").create();
        });
    }

    @Test
    public void testAddIndexLabelPrefixWithExistedSecondary() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();
        graph().addVertex(T.label, "person", "name", "Baby",
                          "city", "Hongkong", "age", 3);

        schema.indexLabel("personByCity").onV("person").secondary()
              .by("city").create();
        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();
        schema.indexLabel("personByAgeSecondary").onV("person").secondary()
              .by("age").create();

        List<Vertex> vertices;
        vertices = graph().traversal().V().has("age", 3).toList();
        Assert.assertEquals(1, vertices.size());
        vertices = graph().traversal().V().has("city", "Hongkong").toList();
        Assert.assertEquals(1, vertices.size());
        Assert.assertThrows(BackendException.class, () -> {
            graph().traversal().V().has("city", "Hongkong")
                   .has("age", "3").toList();
        });

        schema.indexLabel("personByCityAndAge").onV("person").secondary()
              .by("city", "age").create();
        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getIndexLabel("personByCity");
        });
        schema.getIndexLabel("personByAge");
        schema.getIndexLabel("personByAgeSecondary");

        vertices = graph().traversal().V().has("city", "Hongkong").toList();
        Assert.assertEquals(1, vertices.size());
        vertices = graph().traversal().V().has("age", 3).toList();
        Assert.assertEquals(1, vertices.size());
        vertices = graph().traversal().V().has("city", "Hongkong")
                          .has("age", 3).toList();
        Assert.assertEquals(1, vertices.size());
    }

    @Test
    public void testAddIndexLabelPrefixWithExistedSearch() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();
        graph().addVertex(T.label, "person", "name", "Baby",
                          "city", "Hongkong", "age", 3);

        schema.indexLabel("personByCity").onV("person").secondary()
              .by("city").create();
        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();
        schema.indexLabel("personByAgeSecondary").onV("person").secondary()
              .by("age").create();

        List<Vertex> vertices;
        vertices = graph().traversal().V().has("age", 3).toList();
        Assert.assertEquals(1, vertices.size());
        vertices = graph().traversal().V().has("city", "Hongkong").toList();
        Assert.assertEquals(1, vertices.size());
        Assert.assertThrows(BackendException.class, () -> {
            graph().traversal().V().has("city", "Hongkong")
                   .has("age", "3").toList();
        });

        schema.indexLabel("personByAgeAndCity").onV("person").secondary()
              .by("age", "city").create();
        schema.getIndexLabel("personByAge");
        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getIndexLabel("personByAgeSecondary");
        });

        vertices = graph().traversal().V().has("city", "Hongkong").toList();
        Assert.assertEquals(1, vertices.size());
        vertices = graph().traversal().V().has("age", 3).toList();
        Assert.assertEquals(1, vertices.size());
        vertices = graph().traversal().V().has("city", "Hongkong")
                          .has("age", 3).toList();
        Assert.assertEquals(1, vertices.size());
    }

    @Test
    public void testRemoveNotExistIndexLabel() {
        SchemaManager schema = graph().schema();
        schema.indexLabel("not-exist-il").remove();
    }

    @Test
    public void testRemoveIndexLabelOfVertex() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();
        schema.indexLabel("personByCity").onV("person").secondary()
              .by("city").create();
        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();
        VertexLabel person = schema.getVertexLabel("person");

        Assert.assertEquals(2, person.indexNames().size());
        Assert.assertTrue(person.indexNames().contains("personByCity"));
        Assert.assertTrue(person.indexNames().contains("personByAge"));

        graph().addVertex(T.label, "person", "name", "Baby",
                          "city", "Hongkong", "age", 3);
        Vertex vertex = graph().traversal().V().hasLabel("person")
                        .has("city", "Hongkong").next();
        Assert.assertNotNull(vertex);
        vertex = graph().traversal().V().hasLabel("person")
                 .has("age", P.inside(2, 4)).next();
        Assert.assertNotNull(vertex);

        schema.indexLabel("personByCity").remove();

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getIndexLabel("personByCity");
        });

        Assert.assertEquals(1, person.indexNames().size());
        Assert.assertTrue(!person.indexNames().contains("personByCity"));
        Assert.assertTrue(person.indexNames().contains("personByAge"));

        Assert.assertThrows(BackendException.class, () -> {
            graph().traversal().V().hasLabel("person")
                   .has("city", "Hongkong").next();
        });
        vertex = graph().traversal().V().hasLabel("person")
                 .has("age", P.inside(2, 4)).next();
        Assert.assertNotNull(vertex);

        schema.indexLabel("personByAge").remove();

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getIndexLabel("personByAge");
        });

        person = schema.getVertexLabel("person");
        Assert.assertEquals(0, person.indexNames().size());

        Assert.assertThrows(BackendException.class, () -> {
            graph().traversal().V().hasLabel("person")
                   .has("age", P.inside(2, 4)).next();
        });
    }

    @Test
    public void testRemoveIndexLabelOfEdge() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("author").properties("id", "name")
              .primaryKeys("id").create();
        schema.vertexLabel("book").properties("name")
              .primaryKeys("name").create();
        schema.edgeLabel("authored").singleTime()
              .link("author", "book")
              .properties("contribution")
              .create();

        Vertex james = graph().addVertex(T.label, "author", "id", 1,
                                         "name", "James Gosling");
        Vertex java1 = graph().addVertex(T.label, "book", "name", "java-1");

        schema.indexLabel("authoredByContri").onE("authored").secondary()
              .by("contribution").create();

        EdgeLabel authored = schema.getEdgeLabel("authored");

        Assert.assertEquals(1, authored.indexNames().size());
        Assert.assertTrue(authored.indexNames().contains("authoredByContri"));

        james.addEdge("authored", java1,"contribution", "test");

        Edge edge = graph().traversal().E().hasLabel("authored")
                    .has("contribution", "test").next();
        Assert.assertNotNull(edge);

        schema.indexLabel("authoredByContri").remove();

        Assert.assertThrows(NotFoundException.class, () -> {
            schema.getIndexLabel("authoredByContri");
        });

        Assert.assertEquals(0, authored.indexNames().size());

        Assert.assertThrows(BackendException.class, () -> {
            graph().traversal().E().hasLabel("authored")
                   .has("contribution", "test").next();
        });
    }

    @Test
    public void testRebuildIndexLabelOfVertex() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();
        schema.indexLabel("personByCity").onV("person").secondary()
              .by("city").create();
        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();
        VertexLabel person = schema.getVertexLabel("person");
        Assert.assertEquals(2, person.indexNames().size());
        Assert.assertTrue(person.indexNames().contains("personByCity"));
        Assert.assertTrue(person.indexNames().contains("personByAge"));

        graph().addVertex(T.label, "person", "name", "Baby",
                          "city", "Hongkong", "age", 3);
        Vertex vertex = graph().traversal().V().hasLabel("person")
                        .has("city", "Hongkong").next();
        Assert.assertNotNull(vertex);
        vertex = graph().traversal().V().hasLabel("person")
                 .has("age", P.inside(2, 4)).next();
        Assert.assertNotNull(vertex);

        schema.indexLabel("personByCity").rebuild();
        vertex = graph().traversal().V().hasLabel("person")
                 .has("city", "Hongkong").next();
        Assert.assertNotNull(vertex);

        schema.indexLabel("personByAge").rebuild();
        vertex = graph().traversal().V().hasLabel("person")
                 .has("age", P.inside(2, 4)).next();
        Assert.assertNotNull(vertex);
    }

    @Test
    public void testRebuildIndexLabelOfVertexLabel() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("person").properties("name", "age", "city")
              .primaryKeys("name").create();
        schema.indexLabel("personByCity").onV("person").secondary()
              .by("city").create();
        schema.indexLabel("personByAge").onV("person").search()
              .by("age").create();

        VertexLabel person = schema.getVertexLabel("person");
        Assert.assertEquals(2, person.indexNames().size());
        Assert.assertTrue(person.indexNames().contains("personByCity"));
        Assert.assertTrue(person.indexNames().contains("personByAge"));

        graph().addVertex(T.label, "person", "name", "Baby",
                          "city", "Hongkong", "age", 3);
        Vertex vertex = graph().traversal().V().hasLabel("person")
                        .has("city", "Hongkong").next();
        Assert.assertNotNull(vertex);
        vertex = graph().traversal().V().hasLabel("person")
                 .has("age", P.inside(2, 4)).next();
        Assert.assertNotNull(vertex);

        schema.vertexLabel("person").rebuildIndex();
        vertex = graph().traversal().V().hasLabel("person")
                 .has("city", "Hongkong").next();
        Assert.assertNotNull(vertex);
        vertex = graph().traversal().V().hasLabel("person")
                 .has("age", P.inside(2, 4)).next();
        Assert.assertNotNull(vertex);
    }

    @Test
    public void testRebuildIndexLabelOfEdgeLabel() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("author").properties("id", "name")
              .primaryKeys("id").create();
        schema.vertexLabel("book").properties("name")
              .primaryKeys("name").create();
        schema.edgeLabel("authored").singleTime()
              .link("author", "book")
              .properties("contribution")
              .create();

        Vertex james = graph().addVertex(T.label, "author", "id", 1,
                                         "name", "James Gosling");
        Vertex java1 = graph().addVertex(T.label, "book", "name", "java-1");

        schema.indexLabel("authoredByContri").onE("authored").secondary()
              .by("contribution").create();

        EdgeLabel authored = schema.getEdgeLabel("authored");

        Assert.assertEquals(1, authored.indexNames().size());
        Assert.assertTrue(authored.indexNames().contains("authoredByContri"));

        james.addEdge("authored", java1,"contribution", "test");

        Edge edge = graph().traversal().E().hasLabel("authored")
                    .has("contribution", "test").next();
        Assert.assertNotNull(edge);

        schema.indexLabel("authoredByContri").rebuild();
        Assert.assertEquals(1, authored.indexNames().size());
        Assert.assertTrue(authored.indexNames().contains("authoredByContri"));

        edge = graph().traversal().E().hasLabel("authored")
               .has("contribution", "test").next();
        Assert.assertNotNull(edge);
    }

    @Test
    public void testRebuildIndexLabelOfEdge() {
        super.initPropertyKeys();
        SchemaManager schema = graph().schema();
        schema.vertexLabel("author").properties("id", "name")
              .primaryKeys("id").create();
        schema.vertexLabel("book").properties("name")
              .primaryKeys("name").create();
        schema.edgeLabel("authored").singleTime()
              .link("author", "book")
              .properties("contribution")
              .create();

        Vertex james = graph().addVertex(T.label, "author", "id", 1,
                                         "name", "James Gosling");
        Vertex java1 = graph().addVertex(T.label, "book", "name", "java-1");

        schema.indexLabel("authoredByContri").onE("authored")
              .secondary().by("contribution").create();

        EdgeLabel authored = schema.getEdgeLabel("authored");

        Assert.assertEquals(1, authored.indexNames().size());
        Assert.assertTrue(authored.indexNames().contains("authoredByContri"));

        james.addEdge("authored", java1,"contribution", "test");

        Edge edge = graph().traversal().E().hasLabel("authored")
                    .has("contribution", "test").next();
        Assert.assertNotNull(edge);

        schema.edgeLabel("authored").rebuildIndex();
        Assert.assertEquals(1, authored.indexNames().size());
        Assert.assertTrue(authored.indexNames().contains("authoredByContri"));
        edge = graph().traversal().E().hasLabel("authored")
               .has("contribution", "test").next();
        Assert.assertNotNull(edge);
    }
}
