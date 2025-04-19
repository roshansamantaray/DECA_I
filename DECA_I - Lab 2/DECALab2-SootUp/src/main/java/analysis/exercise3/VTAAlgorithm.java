package analysis.exercise3;

import analysis.CallGraph;
import analysis.CallGraphAlgorithm;
import analysis.exercise1.CHAAlgorithm;
import org.graphstream.algorithm.TarjanStronglyConnectedComponents;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.views.JavaView;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VTAAlgorithm extends CallGraphAlgorithm {

    private final Logger log = LoggerFactory.getLogger("VTA");

    @Nonnull
    @Override
    protected String getAlgorithm() {
        return "VTA";
    }

    @Override
    protected void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {
        // Your implementation goes here, also feel free to add methods as needed
        // To get your entry points we prepared getEntryPoints(view) in the superclass for you

        // TODO: implement
        // 1) form initial conservative graph
        CallGraph initialCG = new CHAAlgorithm().constructCallGraph(view);

        // 2) build type assignment graph
        TypeAssignmentGraph tAG = new TypeAssignmentGraph();
        buildTAG(view, tAG);

        // 3) add the TAG to the main CG
        buildFinalCG(view, cg, initialCG, tAG);

        // 4) collapse strongly connected graph
        tAG.annotateScc();
    }

    private void buildFinalCG(JavaView view, CallGraph cg, CallGraph initialCG, TypeAssignmentGraph tAG) {
        Stream<MethodSignature> entries = getEntryPoints(view);
        Iterator<MethodSignature> iterator2 = entries.iterator();
        while (iterator2.hasNext()) {
            MethodSignature entry = iterator2.next();
            if (entry.getDeclClassType().toString().contains("exercise3")) {
                if (view.getMethod(entry).get().hasBody()) {
                    Set<MethodSignature> outEdges = initialCG.edgesOutOf(entry);
                    List<ClassType> allTypes = tAG.getClassTypesAssociatedWithTaggedNodes();
                    cg.addNode(entry);

                    Iterator<MethodSignature> edgeIterator = outEdges.iterator();
                    while (edgeIterator.hasNext()) {
                        MethodSignature edge = edgeIterator.next();
                        for (ClassType type : allTypes) {
                            if (type.equals(edge.getDeclClassType())) {
                                cg.addNode(edge);
                                cg.addEdge(entry, edge);
                            }
                        }
                    }
                }
            }
        }
    }

    private void buildTAG(JavaView view, TypeAssignmentGraph tAG) {
        Stream<MethodSignature> entries = getEntryPoints(view);
        Iterator<MethodSignature> iterator = entries.iterator();
        while (iterator.hasNext()) {
            MethodSignature entry = iterator.next();
            if (entry.getDeclClassType().toString().contains("exercise3")) {
                addToTAG(entry, tAG, null, view);
            }
        }
    }

    private void addToTAG(MethodSignature entry, TypeAssignmentGraph tAG, Object o, JavaView view) {
        Optional.ofNullable(view.getMethod(entry).orElse(null)).filter(method -> method.hasBody()).ifPresent(method -> method.getBody().getStmts().stream().filter(stmt -> stmt instanceof JAssignStmt).map(stmt -> (JAssignStmt) stmt).forEach(assignStmt -> {
            Value leftOp = assignStmt.getLeftOp();
            tAG.addNode(leftOp);
            Value rightOp = assignStmt.getRightOp();

            if (assignStmt.containsInvokeExpr()) {
                MethodSignature methodSign = assignStmt.getInvokeExpr().getMethodSignature();
                addToTAG(methodSign, tAG, leftOp, view);
            } else {
                if (rightOp instanceof JNewExpr) {
                    ClassType type = ((JNewExpr) rightOp).getType();
                    tAG.tagNode(o != null ? (Value) o : leftOp, type);
                } else if (rightOp instanceof JCastExpr) {
                    tAG.tagNode(((JCastExpr) rightOp).getOp(), (ClassType) rightOp.getType());
                } else {
                    tAG.addEdge(leftOp, rightOp);
                }
            }
        }));
    }


    static class Pair<A, B> {
        final A first;
        final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair<?, ?> pair = (Pair<?, ?>) o;

            if (!Objects.equals(first, pair.first)) return false;
            return Objects.equals(second, pair.second);
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ')';
        }
    }

    /**
     * You can use this class to represent your type assignment graph. We do not use this data
     * structure in tests, so you are free to use something else. However, we use this data structure
     * in our solution and it instantly supports collapsing strong-connected components.
     */
    private class TypeAssignmentGraph {
        private final Graph graph;
        private final TarjanStronglyConnectedComponents tscc = new TarjanStronglyConnectedComponents();

        public TypeAssignmentGraph() {
            this.graph = new MultiGraph("tag");
        }

        private boolean containsNode(Value value) {
            return graph.getNode(createId(value)) != null;
        }

        private boolean containsEdge(Value source, Value target) {
            return graph.getEdge(createId(source) + "-" + createId(target)) != null;
        }

        private String createId(Value value) {
            if (value instanceof JFieldRef) return value.toString();
            return Integer.toHexString(System.identityHashCode(value));
        }

        public void addNode(Value value) {
            if (!containsNode(value)) {
                Node node = graph.addNode(createId(value));
                node.setAttribute("value", value);
                node.setAttribute("ui.label", value);
                node.setAttribute("tags", new HashSet<ClassType>());
            }
        }

        public void tagNode(Value value, ClassType classTag) {
            if (containsNode(value)) {
                getNodeTags(value).add(classTag);
            }
        }

        public Set<Pair<Value, Set<ClassType>>> getTaggedNodes() {
            return graph.getNodeSet().stream().map(n -> new Pair<Value, Set<ClassType>>(n.getAttribute("value"), n.getAttribute("tags"))).filter(p -> p.second.size() > 0).collect(Collectors.toSet());
        }

        public Set<ClassType> getNodeTags(Value val) {
            return graph.getNode(createId(val)).getAttribute("tags");
        }

        public void addEdge(Value source, Value target) {
            if (!containsEdge(source, target)) {
                Node sourceNode = graph.getNode(createId(source));
                Node targetNode = graph.getNode(createId(target));
                if (sourceNode == null || targetNode == null)
                    log.error("Could not find one of the nodes. Source: " + sourceNode + " - Target: " + targetNode);
                graph.addEdge(createId(source) + "-" + createId(target), sourceNode, targetNode, true);
            }
        }

        public List<ClassType> getClassTypesAssociatedWithTaggedNodes() {
            return getTaggedNodes().stream().flatMap(p -> p.second.stream()).collect(Collectors.toList());
        }

        public Set<Value> getTargetsFor(Value initialNode) {
            if (!containsNode(initialNode)) return Collections.emptySet();
            Node source = graph.getNode(createId(initialNode));
            Collection<org.graphstream.graph.Edge> edges = source.getLeavingEdgeSet();
            return edges.stream().map(e -> (Value) e.getTargetNode().getAttribute("value")).collect(Collectors.toSet());
        }

        /**
         * Use this method to start the SCC computation.
         */
        public void annotateScc() {
            tscc.init(graph);
            tscc.compute();
        }

        /**
         * Retrieve the index assigned by the SCC algorithm
         *
         * @param value
         * @return
         */
        public Object getSccIndex(Value value) {
            if (!containsNode(value)) return null;
            return graph.getNode(createId(value)).getAttribute(tscc.getSCCIndexAttribute());
        }

        /**
         * Use this method to inspect your type assignment graph
         */
        public void draw() {
            graph.display();
        }
    }
}
