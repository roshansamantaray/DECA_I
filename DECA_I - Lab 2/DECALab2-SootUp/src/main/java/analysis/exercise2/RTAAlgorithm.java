package analysis.exercise2;

import analysis.CallGraph;
import analysis.exercise1.CHAAlgorithm;
import sootup.core.IdentifierFactory;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.views.JavaView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Collectors;


public class RTAAlgorithm extends CHAAlgorithm {

    @Nonnull
    @Override
    protected String getAlgorithm() {
        return "RTA";
    }

    @Override
    protected void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {
        // Your implementation goes here, also feel free to add methods as needed
        // To get your entry points we prepared getEntryPoints(view) in the superclass for you

        // TODO: implement
        final IdentifierFactory identifierFactory = view.getIdentifierFactory();
        for (MethodSignature methodSignature : getEntryPoints(view).collect(Collectors.toList())) {
            buildRTA(methodSignature, cg, view, null);
            MethodSignature original = identifierFactory.parseMethodSignature(methodSignature.toString());

            getConnectingPoints(view, methodSignature).stream().filter(connectingMethods -> cg.edgesOutOf(original).stream().anyMatch(connected -> connectingMethods.getDeclClassType() == connected.getDeclClassType())).forEach(connectingMethods -> buildRTA(connectingMethods, cg, view, original));
        }
    }

    protected void buildRTA(MethodSignature methodSignature, CallGraph cg, JavaView view, @Nullable MethodSignature original) {
        final IdentifierFactory identifierFactory = view.getIdentifierFactory();
        if (original != null) {
            addNode(cg, methodSignature);
            addEdge(cg, original, methodSignature);
        }

        //  System.out.println(view.getMethod(methodSignature).get().getBody());
        if (!view.getMethod(methodSignature).isPresent() || !view.getMethod(methodSignature).get().hasBody()) return;

        addNode(cg, methodSignature);
        for (Stmt stmt : view.getMethod(methodSignature).get().getBody().getStmts()) {
            System.out.println(stmt);
            if (!stmt.containsInvokeExpr()) continue;
            MethodSignature connectingMethod = stmt.getInvokeExpr().getMethodSignature();
            addNode(cg, connectingMethod);
            addEdge(cg, methodSignature, connectingMethod);

            if (original != null) {
                addEdge(cg, original, connectingMethod);
            }

            try {
                buildRTA(connectingMethod, cg, view, methodSignature);
            } catch (Exception ignored) {
            }

        }
    }
}