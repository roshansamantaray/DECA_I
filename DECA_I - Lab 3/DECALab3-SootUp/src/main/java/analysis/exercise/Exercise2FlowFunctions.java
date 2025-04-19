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
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;

import java.util.*;


public class Exercise2FlowFunctions extends TaintAnalysisFlowFunctions {

    private final VulnerabilityReporter reporter;

    public Exercise2FlowFunctions(VulnerabilityReporter reporter) {
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
            if (callSite instanceof JInvokeStmt) {
                AbstractInvokeExpr invokeExpr = callSite.getInvokeExpr();
                Set<Integer> integerSet = new HashSet<>();
                List<Immediate> methodArguments = invokeExpr.getArgs();
                int index = 0;
                for (Value argument : methodArguments) {
                    if (argument.equals(fact.getVariable())) {
                        integerSet.add(index);
                    }
                    index++;
                }

                //TODO: Implement interprocedural part of Exercise 2 here
                int paramIndex = 0;
                for (Local parameter : callee.getBody().getParameterLocals()) {
                    if (integerSet.contains(paramIndex)) {
                        out.add(new DataFlowFact(parameter));
                    }
                    paramIndex++;
                }

                if (fact.getFieldSignature() != null) {
                    out.add(new DataFlowFact(fact.getFieldSignature()));
                }
            }
            return out;
        };
    }

    public FlowFunction<DataFlowFact> getCallToReturnFlowFunction(final Stmt callSiteStmt, Stmt returnSite) {
        return val -> {

            Set<DataFlowFact> out = Sets.newHashSet();
            out.add(val);
            modelStringOperations(val, out, callSiteStmt);

            if (val == DataFlowFact.getZeroInstance()) {

                //TODO: Implement Exercise 1a) here
                Value leftOp;
                if (!(callSiteStmt instanceof JAssignStmt)) return out;
                leftOp = ((JAssignStmt) callSiteStmt).getLeftOp();

                if (callSiteStmt.toString().contains("getParameter(")) {
                    if (leftOp instanceof Local) {
                        out.add(new DataFlowFact((Local) leftOp));
                    } else {
                        if (leftOp instanceof JFieldRef) {
                            JFieldRef left = (JFieldRef) leftOp;
                            out.add(new DataFlowFact(left.getFieldSignature()));
                        }
                    }
                }
            }

            if (callSiteStmt.toString().contains("executeQuery")) {
                Value arg = callSiteStmt.getInvokeExpr().getArg(0);
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

    @Override
    public FlowFunction<DataFlowFact> getNormalFlowFunction(final Stmt curr, Stmt succ) {
        return fact -> {
            prettyPrint(curr, fact);
            Set<DataFlowFact> out = Sets.newHashSet();
            out.add(fact);

            //TODO: Implement Exercise 1b) here
            //TODO: Implement cases for field load and field store statement of Exercise 2) here
            if (curr instanceof JAssignStmt) {
                JAssignStmt jAssignStmt = (JAssignStmt) curr;
                DataFlowFact right = getRightDataFlowFact(jAssignStmt);
                if (out.contains(right)) {
                    DataFlowFact left = getLeftDataFlowFact(jAssignStmt);
                    out.add(left);
                }
            }
            return out;
        };
    }

    private static DataFlowFact getLeftDataFlowFact(JAssignStmt jAssignStmt) {
        Value leftOp = jAssignStmt.getLeftOp();
        if (leftOp instanceof Local) {
            return new DataFlowFact((Local) leftOp);
        } else if (leftOp instanceof JFieldRef) {
            return new DataFlowFact(((JFieldRef) leftOp).getFieldSignature());
        }
        return null;
    }

    private static DataFlowFact getRightDataFlowFact(JAssignStmt jAssignStmt) {
        DataFlowFact right = null;
        Value rightOp = jAssignStmt.getRightOp();
        if (rightOp instanceof Local) {
            right = new DataFlowFact((Local) rightOp);
        } else if (rightOp instanceof JFieldRef) {
            right = new DataFlowFact(((JFieldRef) rightOp).getFieldSignature());
        }
        return right;
    }

    @Override
    public FlowFunction<DataFlowFact> getReturnFlowFunction(Stmt callSite, SootMethod callee, Stmt exitStmt, Stmt retSite) {
        return fact -> {
            prettyPrint(callSite, fact);
            return Collections.emptySet();
        };
    }

}
