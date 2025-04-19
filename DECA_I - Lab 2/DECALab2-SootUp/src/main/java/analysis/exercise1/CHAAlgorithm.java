package analysis.exercise1;

import analysis.CallGraph;
import analysis.CallGraphAlgorithm;
import sootup.core.IdentifierFactory;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootClassMember;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CHAAlgorithm extends CallGraphAlgorithm {

    @Nonnull
    @Override
    protected String getAlgorithm() {
        return "CHA";
    }

    @Override
    protected void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {

        // Your implementation goes here, also feel free to add methods as needed
        // To get your entry points we prepared getEntryPoints(view) in the superclass for you

        // TODO: implement
        for (MethodSignature methodSignature : getEntryPoints(view).collect(Collectors.toList())) {
            buildCHA(methodSignature, cg, view);
            getConnectingPoints(view, methodSignature).forEach(connectingMethods -> buildCHA(connectingMethods, cg, view));
        }
    }


    protected void buildCHA(MethodSignature methodSignature, CallGraph cg, JavaView view) {
        final IdentifierFactory identifierFactory = view.getIdentifierFactory();
//        System.out.println(methodSignature);
        if (!view.getMethod(methodSignature).isPresent() || !view.getMethod(methodSignature).get().hasBody()) return;

        addNode(cg, methodSignature);

        for (Stmt stmt : view.getMethod(methodSignature).get().getBody().getStmts()) {
            if (!stmt.containsInvokeExpr()) continue;


            MethodSignature connectingMethod = stmt.getInvokeExpr().getMethodSignature();
            List<MethodSignature> methods = new ArrayList<>();
            for (JavaSootClass classType : view.getClasses()) {
                MethodSignature method = identifierFactory.getMethodSignature(classType.getType(), stmt.getInvokeExpr().getMethodSignature().getSubSignature());

                methods.add(method);
                methods.add(stmt.getInvokeExpr().getMethodSignature());
            }


            for (MethodSignature method : methods) {
                addNode(cg, method);
                addEdge(cg, methodSignature, method);
            }

            buildCHA(connectingMethod, cg, view);


        }
    }

    protected void addNode(CallGraph cg, MethodSignature method) {
        if (!cg.hasNode(method)) {
            cg.addNode(method);
        }
    }

    protected void addEdge(CallGraph cg, MethodSignature from, MethodSignature to) {
        if (!cg.hasEdge(from, to)) {
            cg.addEdge(from, to);
        }
    }

    protected List<MethodSignature> getConnectingPoints(@Nonnull JavaView view, MethodSignature methodSignature) {

        String className = methodSignature.toString().substring(methodSignature.toString().indexOf('<') + 1, methodSignature.toString().indexOf(':'));
        String packageName = className.substring(0, className.lastIndexOf('.'));

        return view.getClasses().stream().flatMap(clas -> clas.getMethods().stream()).filter(method -> method.toString().contains(packageName) && method.hasBody()).map(SootClassMember::getSignature).collect(Collectors.toList());
    }
}