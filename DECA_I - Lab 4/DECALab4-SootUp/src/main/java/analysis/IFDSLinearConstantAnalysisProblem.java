package analysis;

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Identity;
import heros.solver.Pair;
import sootup.analysis.interprocedural.ifds.DefaultJimpleIFDSTabulationProblem;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;

import sootup.core.jimple.common.expr.AbstractFloatBinopExpr;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JMulExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.NullType;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.wala.util.collections.ArraySet;

public class IFDSLinearConstantAnalysisProblem extends DefaultJimpleIFDSTabulationProblem<Pair<Local, Integer>, InterproceduralCFG<Stmt, SootMethod>> {
    private final List<MethodSignature> entryPoints;

    protected final JavaView view;

    protected final static int LOWER_BOUND = -1000;

    protected final static int UPPER_BOUND = 1000;

    protected InterproceduralCFG<Stmt, SootMethod> icfg;

    public IFDSLinearConstantAnalysisProblem(List<MethodSignature> entryPoints, JavaView view, InterproceduralCFG<Stmt, SootMethod> icfg) {
        super(icfg);
        this.entryPoints = entryPoints;
        this.view = view;
        this.icfg = icfg;
    }

    @Override
    public Map<Stmt, Set<Pair<Local, Integer>>> initialSeeds() {
        for (MethodSignature methodSignature : entryPoints) {
            SootMethod m = view.getMethod(methodSignature).get();
            if (!m.hasBody()) {
                continue;
            }
            if (m.getName().equals("entryPoint")) {
                return DefaultSeeds.make(Collections.singleton(m.getBody().getStmtGraph().getStartingStmt()), zeroValue());
            }
        }
        throw new IllegalStateException("View does not contain entryPoint " + entryPoints);
    }


    // TODO: You have to implement the FlowFunctions interface.
    // Use Pair<Local, Integer> as type for the data-flow facts.
    final ArrayList<Integer> checked = new ArrayList<>();
    int index = -1;
    
    @Override
    protected FlowFunctions<Stmt, Pair<Local, Integer>, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Stmt, Pair<Local, Integer>, SootMethod>() {
            final Set<Pair<Local, Integer>> pairsSet = new ArraySet<>();
            @Override
            public FlowFunction<Pair<Local, Integer>> getNormalFlowFunction(Stmt curr, Stmt next) {
                // TODO: Implement this flow function factory to obtain an intra-procedural data-flow analysis.
            if (!(curr instanceof JAssignStmt)) return Identity.v();
                //    System.out.println(curr);
                return localIntegerPair -> {
                    Value leftOp = null, rightOp = null;
                    leftOp = ((JAssignStmt) curr).getLeftOp();
                    rightOp = ((JAssignStmt) curr).getRightOp();

                    if (leftOp instanceof Local) {
                        //    System.out.println(leftOp);
                        if (rightOp instanceof IntConstant) {
                            int result = ((IntConstant) rightOp).getValue();

                            addPair(pairsSet, (Local) leftOp, result);

                        } else if (rightOp instanceof AbstractFloatBinopExpr) {
                            AbstractFloatBinopExpr expr = (AbstractFloatBinopExpr) rightOp;
                            calculateAndAddPair(expr, (Local) leftOp, pairsSet);
                        }
                    }
                    return pairsSet;
                };
                // return Identity.v();
            }

            @Override
            public FlowFunction<Pair<Local, Integer>> getCallFlowFunction(Stmt callsite, SootMethod dest) {
                // TODO: Implement this flow function factory to map the actual into the formal arguments.
                // Caution, actual parameters may be integer literals as well.
            if (!(callsite instanceof JAssignStmt)) return Identity.v();
                return localIntegerPair -> {
                    Value leftOp = null, rightOp = null;
                    leftOp = ((JAssignStmt) callsite).getLeftOp();
                    rightOp = ((JAssignStmt) callsite).getRightOp();

                    if (rightOp instanceof JVirtualInvokeExpr) {
                        List<Immediate> arguments = ((JVirtualInvokeExpr) rightOp).getArgs();
                        
                        for (Immediate arg : arguments) {
                            Local parameterLocal = dest.getBody().getParameterLocal(arguments.indexOf(arg));
                            int result = 0;

                            if (arg instanceof Local) {
                                int val = getValueFromExpression((Local) arg, pairsSet);

                                addPair(pairsSet, parameterLocal, val);
                            } else if (arg instanceof IntConstant) {
                                result = ((IntConstant) arg).getValue();
                                addPair(pairsSet, (Local) leftOp, result);

                            }
                        }
                    }

                    return pairsSet;
                };
                //    return Identity.v();
            }

            @Override
            public FlowFunction<Pair<Local, Integer>> getReturnFlowFunction(Stmt callsite, SootMethod callee, Stmt exit, Stmt retsite) {
                // TODO: Map the return value back into the caller's context if applicable.
                // Since Java has pass-by-value semantics for primitive data types, you do not have to map the formals
                // back to the actuals at the exit of the callee.
            if (!(callsite instanceof JAssignStmt)) return Identity.v();
                return localIntegerPair -> {
                    if (exit instanceof JReturnStmt) {
                        Value exitValue = ((JReturnStmt) exit).getOp();

                        if (exitValue instanceof Local) {
                            Local local = (Local) ((JAssignStmt) callsite).getLeftOp();
                            Value local2 = (Value) ((JAssignStmt) callsite).getRightOp();
                            
                        // Handle arithmetic operations
                        if (local2 instanceof JAddExpr) {
                            Value op1 = ((JAddExpr) local2).getOp1();
                            Value op2 = ((JAddExpr) local2).getOp2();
                            if (op1 instanceof Local && op2 instanceof IntConstant) {
                                Integer val1 = getValueFromExpression(op1, pairsSet);
                                int val2 = ((IntConstant) op2).getValue();
                                addPair(pairsSet, local, val1 + val2);
                            }
                        }
                        // Handle constants
                        else if (local2 instanceof IntConstant) {
                            int value = ((IntConstant) local2).getValue();
                            addPair(pairsSet, local, value);
                        }
                    }
                }
                    return pairsSet;
                };
                //    return Identity.v();
            }

            @Override
            public FlowFunction<Pair<Local, Integer>> getCallToReturnFlowFunction(Stmt callsite, Stmt retsite) {
                // TODO: getCallToReturnFlowFunction can be left to return id in many analysis; this time as well?
                return Identity.v();
            }
        };
    }


    @Override
    protected Pair<Local, Integer> createZeroValue() {
        return new Pair<>(new Local("<<zero>>", NullType.getInstance()), Integer.MIN_VALUE);
    }

    protected void calculateAndAddPair(AbstractFloatBinopExpr expr, Local left, Set<Pair<Local, Integer>> pairsSet) {
        int result = 0;
        Value leftExpr = expr.getOp1();
        Value rightExpr = expr.getOp2();
        Integer leftValue = getValueFromExpression(leftExpr, pairsSet);
        Integer rightValue = getValueFromExpression(rightExpr, pairsSet);
   
        if (expr instanceof JMulExpr) {
            result = leftValue * rightValue;
        } else if (expr instanceof JAddExpr) {
            result = leftValue + rightValue;
        } else if (expr instanceof JSubExpr) {
            result = leftValue - rightValue;
        }
        addPair(pairsSet, left, result);
    }

    private void addPair(Set<Pair<Local, Integer>> pairsSet, Local key, Integer value) {
        if (value > UPPER_BOUND || value < LOWER_BOUND) return;
        if (pairsSet.contains(new Pair<>(key, value))) return;
    //    if(key.getName().equals("$i")&& value.equals(200))return;
        try {
            pairsSet.add(new Pair<>(key, value));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private Integer getValueFromExpression(Value exp, Set<Pair<Local, Integer>> pairsSet) {
        if (exp instanceof Local) {
            ArrayList<Integer> arrayList = pairsSet.stream()
                    .filter(pair -> pair != null && pair.getO1() != null && pair.getO1().equals(exp))
                    .map(Pair::getO2)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new));
      
            if (arrayList.isEmpty()) return 0;

            return arrayList.get(arrayList.size() - 1);
        } else if (exp instanceof IntConstant) {
            return ((IntConstant) exp).getValue();
        }

        return 0;
    }

    boolean containsLocal(Set<Pair<Local, Integer>> pairsSet, Local targetLocal) {
        for (Pair<Local, Integer> pair : pairsSet) {
            if (pair.getO1().equals(targetLocal)) {
                return true;
            }
        }
        return false;
    }
}
