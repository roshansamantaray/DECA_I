package analysis.exercise;

import analysis.TaintAnalysisFlowFunctions;
import analysis.VulnerabilityReporter;
import analysis.fact.DataFlowFact;
import com.google.common.collect.Sets;
import heros.FlowFunction;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.List;

public class Exercise1FlowFunctions extends TaintAnalysisFlowFunctions {

    private final VulnerabilityReporter reporter;

    public Exercise1FlowFunctions(VulnerabilityReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public FlowFunction<DataFlowFact> getCallFlowFunction(Stmt callSite, SootMethod callee) {
        return fact -> {
            if (fact.equals(DataFlowFact.getZeroInstance()))
                return Collections.emptySet();
            prettyPrint(callSite, fact);
            Set<DataFlowFact> out = Sets.newHashSet();

            //TODO: Implement Exercise 1c) here
            if (!(callSite.getInvokeExpr() instanceof JSpecialInvokeExpr)) return out;
            if (!callSite.containsInvokeExpr()) return out;
            List<Immediate> args = callSite.getInvokeExpr().getArgs();
            List<Local> locals = new ArrayList<>(callee.getBody().getParameterLocals());
            locals.stream().filter(local -> args.stream()
                            .anyMatch(arg -> fact.getVariable().equals(arg)))
                    .forEach(local -> out.add(new DataFlowFact(local)));

            return out;
        };
    }

    public FlowFunction<DataFlowFact> getCallToReturnFlowFunction(final Stmt call, Stmt returnSite) {
        return val -> {

            Set<DataFlowFact> out = Sets.newHashSet();
            out.add(val);
            modelStringOperations(val, out, call);

            if (val.equals(DataFlowFact.getZeroInstance())) {

                //TODO: Implement Exercise 1a) here
                if (call instanceof JAssignStmt && call.toString().contains("getParameter(") && ((JAssignStmt) call).getLeftOp() instanceof Local) {
                    out.add(new DataFlowFact((Local) ((JAssignStmt) call).getLeftOp()));
                }
            }
            if (call.toString().contains("executeQuery")) {
                Value arg = call.getInvokeExpr().getArg(0);
                if (val.getVariable().equals(arg)) {
                    reporter.reportVulnerability();
                }
            }
            return out;
        };
    }

    private void modelStringOperations(DataFlowFact fact, Set<DataFlowFact> out,
                                       Stmt callSiteStmt) {
        Exercise3FlowFunctions.handleCallSite(fact, out, callSiteStmt);


        /*For any call x = var.toString(), if the base variable var is tainted, then x is tainted.*/
        if (callSiteStmt instanceof JAssignStmt && callSiteStmt.toString().contains("toString()")) {
            if (callSiteStmt.getInvokeExpr() instanceof AbstractInstanceInvokeExpr) {
                AbstractInstanceInvokeExpr AbstractInstanceInvokeExpr = (AbstractInstanceInvokeExpr) callSiteStmt.getInvokeExpr();
                if (fact.getVariable().equals(AbstractInstanceInvokeExpr.getBase())) {
                    Value leftOp = ((JAssignStmt) callSiteStmt).getLeftOp();
                    if (leftOp instanceof Local) {
                        out.add(new DataFlowFact((Local) leftOp));
                    }
                }
            }
        }
    }

    @Override
    public FlowFunction<DataFlowFact> getNormalFlowFunction(final Stmt curr, Stmt succ) {
        return fact -> {
            prettyPrint(curr, fact);
            Set<DataFlowFact> out = Sets.newHashSet();
            out.add(fact);

            //TODO: Implement Exercise 1b) here
            if (curr instanceof JAssignStmt) {
                JAssignStmt jAssignStmt = (JAssignStmt) curr;
                DataFlowFact rightVariable = (jAssignStmt.getRightOp() instanceof Local) ? new DataFlowFact((Local) jAssignStmt.getRightOp()) : null;
                if (rightVariable != null && out.contains(rightVariable) && jAssignStmt.getLeftOp() instanceof Local) {
                    out.add(new DataFlowFact((Local) jAssignStmt.getLeftOp()));
                }
            }
            return out;
        };
    }

    @Override
    public FlowFunction<DataFlowFact> getReturnFlowFunction(Stmt callSite, SootMethod callee, Stmt exitStmt, Stmt retSite) {
        return fact -> {
            prettyPrint(callSite, fact);
            return Collections.emptySet();
        };
    }
}
