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
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Exercise3FlowFunctions extends TaintAnalysisFlowFunctions {

    private final VulnerabilityReporter reporter;

    public Exercise3FlowFunctions(VulnerabilityReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public FlowFunction<DataFlowFact> getCallFlowFunction(Stmt callSite, SootMethod callee) {
        return fact -> {

            if (fact == DataFlowFact.getZeroInstance()) {
                return Collections.emptySet();
            }

            prettyPrint(callSite, fact);
            Set<DataFlowFact> out = Sets.newHashSet();

            //TODO: Implement Exercise 1c) here
            if (!(callSite.getInvokeExpr() instanceof JSpecialInvokeExpr)) return out;
            if (!callSite.containsInvokeExpr()) return out;

            List<Immediate> args = callSite.getInvokeExpr().getArgs();
            List<Local> locals = new ArrayList<>(callee.getBody().getParameterLocals());

            locals.stream().filter(local -> args.stream()
                            .anyMatch(arg -> fact.getVariable().equals(arg)))
                    .forEach(local -> out.add(new DataFlowFact(local, fact.getFieldSignature())));

            //TODO: Implement interprocedural part of Exercise 3 here.
            Value leftOp = null, rightOp = null;
            if (callSite instanceof JAssignStmt) {
                leftOp = ((JAssignStmt) callSite).getLeftOp();
                rightOp = ((JAssignStmt) callSite).getRightOp();
                if (fact.getVariable().equals(rightOp) && leftOp instanceof JInstanceFieldRef) {
                    out.add(new DataFlowFact(((JInstanceFieldRef) leftOp).getBase(), fact.getFieldSignature()));
                }
            }
            return out;
        };
    }

    public FlowFunction<DataFlowFact> getCallToReturnFlowFunction(final Stmt call, Stmt returnSite) {
        return val -> {

            Set<DataFlowFact> out = Sets.newHashSet();
            out.add(val);
            modelStringOperations(val, out, call);

            if (val == DataFlowFact.getZeroInstance()) {

                //TODO: Implement Exercise 1a) here
                Value leftOp = null, rightOp = null;
                if (!(call instanceof JAssignStmt)) return out;
                leftOp = ((JAssignStmt) call).getLeftOp();
                rightOp = ((JAssignStmt) call).getRightOp();

                System.out.println(rightOp);
                if (rightOp.toString().contains("getParameter(java.lang.String)")) {
                    out.add(new DataFlowFact((Local) leftOp));
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

    private void modelStringOperations(DataFlowFact fact, Set<DataFlowFact> out, Stmt callSiteStmt) {
        handleCallSite(fact, out, callSiteStmt);

        /*For any call x = var.toString(), if the base variable var is tainted, then x is tainted.*/
        if (callSiteStmt instanceof JAssignStmt && callSiteStmt.toString().contains("toString()")) {
            if (callSiteStmt.getInvokeExpr() instanceof AbstractInstanceInvokeExpr) {
                AbstractInstanceInvokeExpr instanceInvokeExpr = (AbstractInstanceInvokeExpr) callSiteStmt.getInvokeExpr();
                if (fact.getVariable().equals(instanceInvokeExpr.getBase())) {
                    Value leftOp = ((JAssignStmt) callSiteStmt).getLeftOp();
                    if (leftOp instanceof Local) {
                        out.add(new DataFlowFact((Local) leftOp));
                    }
                }
            }
        }
    }

    static void handleCallSite(DataFlowFact fact, Set<DataFlowFact> out, Stmt callSiteStmt) {
        if (callSiteStmt instanceof JAssignStmt && callSiteStmt.toString().contains("java.lang.StringBuilder append(") && callSiteStmt.getInvokeExpr() instanceof AbstractInstanceInvokeExpr) {
            Value arg0 = callSiteStmt.getInvokeExpr().getArg(0);
            Value base = ((AbstractInstanceInvokeExpr) callSiteStmt.getInvokeExpr()).getBase();
            /*Does the propagated value match the first parameter of the append call or the base variable*/
            if (fact.getVariable().equals(arg0) || fact.getVariable().equals(base)) {
                /*Yes, then taint the left side of the assignment*/
                Value leftOp = ((JAssignStmt) callSiteStmt).getLeftOp();
                if (leftOp instanceof Local) {
                    out.add(new DataFlowFact((Local) leftOp));
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
            if (!(curr instanceof JAssignStmt)) return out;
            if (fact == DataFlowFact.getZeroInstance()) return out;
            Value leftOp = null, rightOp = null;

            leftOp = ((JAssignStmt) curr).getLeftOp();
            rightOp = ((JAssignStmt) curr).getRightOp();

      
            if (curr.toString().contains("loaded")) {

            }
            if (leftOp instanceof Local && rightOp instanceof JInstanceFieldRef) {
                out.add(new DataFlowFact((Local) leftOp));
            }
            //TODO: Implement cases for field load and field store statement of Exercise 3) here
            if (rightOp instanceof Local && fact.getVariable().equals(rightOp)) {
                System.out.println(curr);
                    System.out.println();
                    if (leftOp instanceof Local) {
                        out.add(new DataFlowFact((Local) leftOp));
                    } else if (leftOp instanceof JInstanceFieldRef) {
                        JInstanceFieldRef left = (JInstanceFieldRef) leftOp;
                        out.add(new DataFlowFact(left.getBase(), left.getFieldSignature()));
                    }
                } else if (rightOp instanceof JInstanceFieldRef
                        && fact.getFieldSignature() != null
                        && fact.getFieldSignature().toString().equals(((JInstanceFieldRef) rightOp).getFieldSignature().toString())
                        && fact.getVariable() == (((JInstanceFieldRef) rightOp).getBase())
                ) {
                  
                    if (leftOp instanceof Local) {
                        out.add(new DataFlowFact((Local) leftOp));
                    } else if (leftOp instanceof JInstanceFieldRef) {
                        JInstanceFieldRef left = (JInstanceFieldRef) leftOp;
                        out.add(new DataFlowFact(left.getBase(), left.getFieldSignature()));
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
