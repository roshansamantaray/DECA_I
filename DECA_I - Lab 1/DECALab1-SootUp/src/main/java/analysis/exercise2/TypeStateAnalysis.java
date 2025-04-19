package analysis.exercise2;

import analysis.FileStateFact;
import analysis.ForwardAnalysis;
import analysis.VulnerabilityReporter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaSootMethod;

import javax.annotation.Nonnull;

public class TypeStateAnalysis extends ForwardAnalysis<Set<FileStateFact>> {

	public TypeStateAnalysis(@Nonnull JavaSootMethod method, @Nonnull VulnerabilityReporter reporter) {
		super(method, reporter);
	    // System.out.println(method.getBody());
	}

	@Override
	protected void flowThrough(@Nonnull Set<FileStateFact> in, @Nonnull Stmt stmt, @Nonnull Set<FileStateFact> out) {
		copy(in, out);
		// TODO: Implement your flow function here.
		// MethodSignature currentMethodSig = method.getSignature();
		// this.reporter.reportVulnerability(currentMethodSig, stmt);

		if (stmt instanceof JInvokeStmt) {
			JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
			AbstractInvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

			if (invokeExpr instanceof JSpecialInvokeExpr)
			{
				String name = ((JSpecialInvokeExpr) invokeExpr).getBase().getName();
				for (FileStateFact fsf : out)
				{
					if (fsf.containsAlias(name) && invokeExpr.getMethodSignature().toString().contains("<target.exercise2.File: void <init>()>"))
						{
							fsf.updateState(FileStateFact.FileState.Init);
						}

				}
			}

			if (invokeExpr instanceof JVirtualInvokeExpr)
			{
				String name = ((JVirtualInvokeExpr) invokeExpr).getBase().getName();
				for (FileStateFact fsf : out)
				{
					if (fsf.containsAlias(name))
					{
						if (invokeExpr.getMethodSignature().toString().contains("<target.exercise2.File: void open()>"))
						{
							if (fsf.getState() == FileStateFact.FileState.Init)
							{
								fsf.updateState(FileStateFact.FileState.Open);
							}
							else if (fsf.getState() == FileStateFact.FileState.Close) {
								fsf.updateState(FileStateFact.FileState.Open);
							}
						}
						if (invokeExpr.getMethodSignature().toString().contains("<target.exercise2.File: void close()>"))
						{
							if (fsf.getState() == FileStateFact.FileState.Init) {
								fsf.updateState(FileStateFact.FileState.Close);
							}
							else if (fsf.getState() == FileStateFact.FileState.Open) {
								fsf.updateState(FileStateFact.FileState.Close);
							}
//							else if (fsf.getState() == FileStateFact.FileState.Close) {
//								this.reporter.reportVulnerability(method.getSignature(), stmt);
//							}
						}
					}
				}
			}
		}

		if (stmt instanceof JReturnVoidStmt)
		{
			for (FileStateFact fsf: out)
			{
				if (fsf.getState() == FileStateFact.FileState.Open) {
					this.reporter.reportVulnerability(method.getSignature(), stmt);
				}
//				if (fsf.getState() == FileStateFact.FileState.Init) {
//					this.reporter.reportVulnerability(method.getSignature(), stmt);
//				}
			}
		}
		//prettyPrint(in, stmt, out);
	}

	@Nonnull
	@Override
	protected Set<FileStateFact> newInitialFlow() {
		// TODO: Implement your initialization here.
		// The following line may be just a place holder, check for yourself if
		// it needs some adjustments.
		// Initialize the hashset here - flowThrough must have something to parse
		Set<FileStateFact> fileStateFactSet = new HashSet<>();
		List<Stmt> statements = this.method.getBody().getStmts();
		Set<Set<Value>> mainSet = new HashSet<>();

		for (Stmt stmt : statements)
		{
			Set<Value> alias = new HashSet<>();
			if (stmt instanceof JAssignStmt)
			{
				JAssignStmt aStmt = (JAssignStmt) stmt;
				Value right = aStmt.getRightOp();
				if (right.toString().contains("new target.exercise2.File"))
				{
					alias.add(aStmt.getLeftOp());
					mainSet.add(alias);
				}

				for (Set<Value> innerSet : mainSet)
				{
					if (innerSet.contains(right))
					{
						innerSet.add(aStmt.getLeftOp());
					}
					if (right instanceof JStaticFieldRef)
					{
						boolean found =  innerSet.stream().anyMatch(v -> v.getType().equals(right.getType()));
						if (found)
						{
							innerSet.add(aStmt.getLeftOp());
						}
					}
				}
			}
		}

		for (Set<Value> set : mainSet)
		{
			boolean exists = false;
			for (FileStateFact fsf : fileStateFactSet)
			{
				if (fsf.getAliases().equals(set)) {
					exists = true;
					break;
				}

			}
			if (!exists)
				fileStateFactSet.add(new FileStateFact(set, FileStateFact.FileState.Init));
		}
		return fileStateFactSet;
	}

	@Override
	protected void copy(@Nonnull Set<FileStateFact> source, @Nonnull Set<FileStateFact> dest) {
		// TODO: Implement the copy function here.
		for (FileStateFact fsfSource : source) {
			boolean found = false;
			for (FileStateFact fsfDest : dest) {
				if (fsfDest.getAliases().equals(fsfSource.getAliases())) {
					fsfDest.updateState(fsfSource.getState());
					found = true;
					break;
				}
			}
			if (!found) {
				dest.add(new FileStateFact(fsfSource));
			}
		}
		//System.out.println("source: " + source  + " ---- dest: " + dest);
	}

	@Override
	protected void merge(@Nonnull Set<FileStateFact> in1, @Nonnull Set<FileStateFact> in2, @Nonnull Set<FileStateFact> out) {
		// TODO: Implement the merge function here.
		out.clear();
		for (FileStateFact fsf : in1) {
			if (!out.contains(fsf)) {
				out.add(fsf);
			}
		}
		for (FileStateFact fsf : in2) {
			if (!out.contains(fsf)) {
				out.add(fsf);
			}
		}
		//System.out.println("in1: " + in1 + " --- in2: " + in2 + " --- out: " + out);
	}

}
