package com.baidu.hugegraph.example;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph.HugeFactory;
import com.baidu.hugegraph.HugeGraph;
import com.baidu.hugegraph.schema.SchemaManager;

/**
 * Created by jishilei on 2017/4/2.
 */
public class Example2 {
    private static final Logger logger = LoggerFactory.getLogger(Example2.class);

    public static void main(String[] args) {

        logger.info("Example2 start!");
        String confFile = Example2.class.getClassLoader().getResource("hugegraph.properties").getPath();
        HugeGraph graph = HugeFactory.open(confFile);
        graph.clearBackend();
        graph.initBackend();
        Example2.load(graph);
        traversal(graph);
        System.exit(0);
    }

    public static void traversal(final HugeGraph graph) {

        // query all
        GraphTraversal<Vertex, Vertex> vertexs = graph.traversal().V();
        System.out.println(">>>> query all vertices: size=" + vertexs.toList().size());

        GraphTraversal<Edge, Edge> edges = graph.traversal().E();
        System.out.println(">>>> query all edges: size=" + edges.toList().size());

//        // query vertex by id
//        vertex = graph.traversal().V("author\u00021");
//        GraphTraversal<Vertex, Edge> edgesOfVertex = vertex.outE("created");
//        System.out.println(">>>> query edges of vertex: " + edgesOfVertex.toList());
//
//        vertex = graph.traversal().V("author\u00021");
//        GraphTraversal<Vertex, Vertex> verticesOfVertex = vertex.out("created");
//        System.out.println(">>>> query vertices of vertex: " + verticesOfVertex.toList());
//
//        // query edge by condition
//        ConditionQuery q = new ConditionQuery(HugeTypes.VERTEX);
//        q.query(IdGeneratorFactory.generator().generate("author\u00021"));
//        q.eq(HugeKeys.PROPERTY_KEY, "age");
//
//        Iterator<Vertex> vertices = graph.vertices(q);
//        System.out.println(">>>> queryVertices(): " + vertices.hasNext());
//        while (vertices.hasNext()) {
//            System.out.println(">>>> " + vertices.next().toString());
//        }
//
//        // query edge by id
//        String id = "author\u00021\u0001OUT\u0001authored\u0001\u0001book\u0002java-2";
//        GraphTraversal<Edge, Edge> edges = graph.traversal().E(id);
//        System.out.println(">>>> query edge: " + edges.toList());
//
//        // query edge by condition
//        q = new ConditionQuery(HugeTypes.EDGE);
//        q.eq(HugeKeys.SOURCE_VERTEX, "author\u00021");
//        q.eq(HugeKeys.DIRECTION, Direction.OUT.name());
//        q.eq(HugeKeys.LABEL, "authored");
//        q.eq(HugeKeys.SORT_VALUES, "");
//        q.eq(HugeKeys.TARGET_VERTEX, "book\u0002java-1");
//        q.eq(HugeKeys.PROPERTY_KEY, "contribution");
//
//        Iterator<Edge> edges2 = graph.edges(q);
//        System.out.println(">>>> queryEdges(): " + edges2.hasNext());
//        while (edges2.hasNext()) {
//            System.out.println(">>>> " + edges2.next().toString());
//        }

        // query by vertex label
//        vertex = graph.traversal().V().hasLabel("book");
        //System.out.println(">>>> query all books: size=" + vertex.toList().size());

    }

    public static void showSchema(final HugeGraph graph) {
        /************************* schemaManager operating *************************/
        SchemaManager schemaManager = graph.schema();

        logger.info("===============  show schema  ================");

        schemaManager.desc();
    }

    public static void load(final HugeGraph graph) {
        SchemaManager schema = graph.schema();

        /**
         * Note:
         * Use schema.makePropertyKey interface to create propertyKey.
         * Use schema.propertyKey interface to query propertyKey.
         */
        schema.makePropertyKey("name").asText().create();
        schema.makePropertyKey("age").asInt().create();
        schema.makePropertyKey("lang").asText().create();
        schema.makePropertyKey("date").asText().create();

        schema.makePropertyKey("boolean").asBoolean().create();
        schema.makePropertyKey("byte").asByte().create();
        schema.makePropertyKey("blob").asBlob().create();
        schema.makePropertyKey("double").asDouble().create();
        schema.makePropertyKey("float").asFloat().create();
        schema.makePropertyKey("long").asLong().create();

        schema.makeVertexLabel("person").properties("name", "age").primaryKeys("name").create();
        schema.makeVertexLabel("software").properties("name", "lang").primaryKeys("name").create();
        schema.makeVertexLabel("person").index("personByName").by("name").secondary().create();

        schema.vertexLabel("software").index("softwareByName").by("name").search().create();
        schema.vertexLabel("software").index("softwareByLang").by("lang").search().create();

        schema.makeEdgeLabel("knows").properties("date").create();
        schema.makeEdgeLabel("created").properties("date").create();
        schema.edgeLabel("created").index("createdByDate").by("date").secondary().create();

        schema.desc();

        graph.tx().open();

        Vertex marko = graph.addVertex(T.label, "person", "name", "marko", "age", 29);
        Vertex vadas = graph.addVertex(T.label, "person", "name", "vadas", "age", 27);
        Vertex lop = graph.addVertex(T.label, "software", "name", "lop", "lang", "java");
        Vertex josh = graph.addVertex(T.label, "person", "name", "josh", "age", 32);
        Vertex ripple = graph.addVertex(T.label, "software", "name", "ripple", "lang", "java");
        Vertex peter = graph.addVertex(T.label, "person", "name", "peter", "age", 35);

        marko.addEdge("knows", vadas, "date", "20160110");
        marko.addEdge("knows", josh, "date", "20130220");
        marko.addEdge("created", lop, "date", "20171210");
        josh.addEdge("created", ripple, "date", "20171210");
        josh.addEdge("created", lop, "date", "20091111");
        peter.addEdge("created", lop, "date", "20170324");

        graph.tx().commit();
    }
}
