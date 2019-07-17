/*
 * Copyright (c) 2009-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.equation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ejml.equation.Info;
import org.ejml.equation.Info.Operation;;

/** Optimizes a compiled sequence
 * 
 * @author D. F. Linton, Blue Lightning Development, LLC 2019.
 */
public class CompileCodeOperations {
	
	public final static int REDUCE_CONSTANTS = 1;
	public final static int REDUCE_SCALARS   = 3;
	
	public final static int DEFAULT_OPTIONS = REDUCE_SCALARS;
	
	int options = DEFAULT_OPTIONS;

	List<Info> infos = new ArrayList<>();
	List<Operation> operations = new ArrayList<>();
	List<Variable> inputs = new ArrayList<>();
	List<Variable> doubleTemps = new ArrayList<>();
	List<Variable> integerTemps = new ArrayList<>();
	List<Variable> matrixTemps = new ArrayList<>();
	Variable assignmentTarget = null;
	
	List<Usage> doubleUsages = new ArrayList<>();
	List<Usage> integerUsages = new ArrayList<>();
	List<Usage> matrixUsages = new ArrayList<>();
	
	boolean lastOperationCopyR = false;
	IEmitOperation coder;
	ManagerTempVariables tempManager;
	
	static class Statistics {
		static class Counts {
			public int operations;
			public int integerTemps;
			public int doubleTemps;
			public int matrixTemps;
		}
		Counts input = new Counts();
		Counts output = new Counts();
		int constantExpressions;
		int integerTemps;
		int doubleTemps;
		int matrixTemps;	
		int finalCopy;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("INPUT:  %5d operations, %2d integer temps, %2d double temps, %2d matrix temps\n", input.operations, input.integerTemps, input.doubleTemps, input.matrixTemps));
			sb.append("OPTIMIZATIONS:\n");
			if (constantExpressions > 0) 
				sb.append(String.format("  removed %5d constant expressions\n", this.constantExpressions));
			if (integerTemps > 0) 
				sb.append(String.format("  removed %5d integer temporaries\n", this.integerTemps));
			if (doubleTemps > 0) 
				sb.append(String.format("  removed %5d double temporarie\n", this.doubleTemps));
			if (matrixTemps > 0) 
				sb.append(String.format("  removed %5d matrix temporaries\n", this.matrixTemps));
			if (finalCopy > 0) 
				sb.append("  removed final copy from temp\n");
			sb.append(String.format("OUTPUT: %5d operations, %2d integer temps, %2d double temps, %2d matrix temps\n", output.operations, output.integerTemps, output.doubleTemps, output.matrixTemps));
			return sb.toString();
		}
	}
	
	Statistics stats = new Statistics();
	
	/**
	 * Constructs the compiler
	 * @param coder - source code emitter
	 * @param sequence - sequence of Info/Operation
	 * @param tempManager 
	 */
	public CompileCodeOperations(IEmitOperation coder, Sequence sequence, ManagerTempVariables tempManager, int options) {
		this.coder = coder;
		this.infos = sequence.getInfos();
    	this.operations = sequence.getOperations();
    	this.tempManager = tempManager;
    	this.options = options;
    	stats.input.operations = this.infos.size();
    	
    	lastOperationCopyR = operations.size() > 0 && operations.get(operations.size()-1).name().startsWith("copyR-");
	}
	
	public CompileCodeOperations(IEmitOperation coder, Sequence sequence, ManagerTempVariables tempManager) {
		this(coder, sequence, tempManager, DEFAULT_OPTIONS);
	}


	/**
	 * Add temporaries to the by-type list and other variables to the general list of inputs
	 * @param variable
	 */
	void recordVariable( Variable variable ) {
		//System.out.println("Record: " + variable + " " + variable.isTemp());
		if (variable.isTemp()) {
			switch (variable.getType()) {
			case INTEGER_SEQUENCE:
				throw new RuntimeException("Compilation of integer sequence operations is not supported");
			case SCALAR:
				VariableScalar scalar = (VariableScalar) variable;
				switch (scalar.getScalarType()) {
				case INTEGER:
					if (! integerTemps.contains(variable))
						integerTemps.add(variable);
					break;
				case DOUBLE:
					if (! doubleTemps.contains(variable))
						doubleTemps.add(variable);
					break;
				case COMPLEX:
					throw new RuntimeException("Compilation of complex variable operations is not supported");
				}
				break;
			case MATRIX:
				if (! matrixTemps.contains(variable))
					matrixTemps.add(variable);
				break;
			}
		} else {
			if (! inputs.contains(variable))
				inputs.add(variable);
		}
	}
	
	public static class Usage {
		public Variable variable;
		public Deque<Integer> uses;
		
		public Usage(Variable variable) {
			this.variable = variable;
			this.uses = new ArrayDeque<>();
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (this.variable != null)
				sb.append(this.variable.toString());
			sb.append(": ");
			for (Integer use : uses) {
				sb.append(use.toString());
				sb.append(",");
			}
			return sb.toString();
		}
	}
	
	Usage findUsage( Variable variable) {
		Usage usage = new Usage(variable);
		for (int i = 0; i < infos.size(); i++) {
			Info info = infos.get(i);
			if (info.uses(variable)) {
				usage.uses.addLast(i);
			}
		}
		return usage;
	}
	
	/**
	 * Scan the operations list and record the usage spans of all variables.
	 * 
	 * The scan, data-structures, and reuse approach is a pale echo of the register
	 * allocation algorithm of Poletto and Sarkar (ACM Transactions V21N5, Sept 1999)
	 */
    void mapVariableUsage() {
    	integerTemps.clear();
    	integerUsages.clear();
    	doubleTemps.clear();
    	doubleUsages.clear();
    	matrixTemps.clear();
    	matrixUsages.clear();
    	for (Info info : infos) {
    		//System.out.println( codeOp );
    		for (Variable var : info.input) {
    			recordVariable(var);
    		}
    		if (info.output != null) {
    			if (info.output.isTemp()) {
    				recordVariable( info.output );    				
    			} else {
    				assignmentTarget = info.output;
    			}
    		}
    	}
    	for (Variable variable : integerTemps) {
    		Usage usage = findUsage(variable);
    		integerUsages.add(usage);
    	}
    	
    	for (Variable variable : doubleTemps) {
    		Usage usage = findUsage(variable);
    		doubleUsages.add(usage);
    	}

    	for (Variable variable : matrixTemps) {
    		Usage usage = findUsage(variable);
    		matrixUsages.add(usage);
    	}
    }
    
	
	//matrix temps must not appear on both the lhs and rhs; TODO operation semantics dependent?
	void eliminateRedundantMatrixTemps(List<Usage> usages) {
		if (! usages.isEmpty()) {
			for (int i = 0; i < usages.size(); i++) {
				Usage first = usages.get(0);
				for (int j = i+1; j < usages.size(); j++) {
					Usage next = usages.get(j);
					if (first.uses.getLast() < next.uses.getFirst()) { // can replace next with first
//						System.out.println("  Replacing " + next.variable.getName() + " with " + first.variable.getName());
						for (Integer k : next.uses) {
							infos.get(k).replace(next.variable, first.variable);
							first.uses.addLast(k);
						}
						usages.remove(j);
						matrixUsages.remove(next.variable);
						stats.matrixTemps++;
						if (tempManager != null) {
							tempManager.release((VariableMatrix) next.variable); 
						}
						j--;
					}
				}
			}
		}
	}
	
	
	// scalar temps can appear on both the lhs and rhs
	void eliminateRedundantScalarTemps(List<Usage> usages) {
		if (! usages.isEmpty()) {
			for (int i = 0; i < usages.size(); i++) {
				Usage first = usages.get(0);
				for (int j = i+1; j < usages.size(); j++) {
					Usage next = usages.get(j);
					if (first.uses.getLast() <= next.uses.getFirst()) { // can replace next with first
//						System.out.println("  Replacing " + next.variable.getName() + " with " + first.variable.getName());
						for (Integer k : next.uses) {
							infos.get(k).replace(next.variable, first.variable);
							first.uses.addLast(k);
						}
						if (next.variable instanceof VariableInteger) {
							integerTemps.remove(next.variable);
							stats.integerTemps++;
							if (tempManager != null) {
								tempManager.release((VariableInteger) next.variable);
							}
						} else {
							doubleTemps.remove(next.variable);
							stats.doubleTemps++;
							if (tempManager != null) {
								tempManager.release((VariableDouble) next.variable);
							}
						}
						usages.remove(j);
						j--;
					}
				}
			}
		}
	}
	
	/**
	 * Find the Usage object in the usages list for the target variable
	 * @param usages
	 * @param target
	 * @return null if none
	 */
	Usage locateUsage( List<Usage> usages, Variable target) {
		for (Usage usage : usages) {
			if (usage.variable.equals(target))
				return usage;
		}
		return null;
	}
    
	/**
	 * Eliminate a final (but not the only) copy operation in a sequence.
	 * 
	 * Changes the output of the penultimate operation to the copy target.
	 */
    void eliminateFinalCopy() {
    	if (assignmentTarget != null) {
    		int last = infos.size()-1;
    		if (last <= 0)
    			return;
    		Info info = infos.get(last);
			Variable fromVariable = info.input.get(0);
    		switch (info.op.name()) {
    		case "copy-mm":
    			if (fromVariable.isTemp()) {
    				Usage fromUsage = locateUsage(matrixUsages, fromVariable);
    				for (Integer k : fromUsage.uses) {
						infos.get(k).replace(fromVariable, assignmentTarget);  
    				}
    				operations.remove(info.op);
    				infos.remove(info);
					matrixUsages.remove(fromUsage);
					stats.finalCopy++;
    			}
    			break;
    		case "copy-ss":
    			if (fromVariable.isTemp()) {
    				Usage fromUsage = locateUsage(doubleUsages, fromVariable);
    				for (Integer k : fromUsage.uses) {
						infos.get(k).replace(fromVariable, assignmentTarget);  
    				}
    				operations.remove(info.op);
    				infos.remove(info);
    				doubleUsages.remove(fromUsage);
					stats.finalCopy++;
    			}
    			break;
    		case "copy-ii":
    			if (fromVariable.isTemp()) {
    				Usage fromUsage = locateUsage(integerUsages, fromVariable);
    				for (Integer k : fromUsage.uses) {
						infos.get(k).replace(fromVariable, assignmentTarget);  
    				}
    				operations.remove(info.op);
    				infos.remove(info);
    				integerUsages.remove(fromUsage);
					stats.finalCopy++;
    			}
    			break;
    		default:
    			break;
    		}
    	}
    }

    /**
     * Release all temporaries used at the end of compilation.
     * @param eq
     */
    public void releaseTemporaries(Equation eq) {
    	for (Variable v : integerTemps) {
    		eq.getTemporariesManager().release( (VariableInteger) v );
    	}
    	for (Variable v : doubleTemps) {
    		eq.getTemporariesManager().release( (VariableDouble) v );
    	}
    	for (Variable v : matrixTemps) {
    		eq.getTemporariesManager().release( (VariableMatrix) v );
    	}
    }
    
    boolean isIntegerConstant( Variable v ) {
    	return v.isConstant() && v instanceof VariableInteger;
    }
    
    /**
     * Get the java code for a constant operation.
     * 
     * If there is no associated submatrix range and every variable is a constant
     * compile the operation and return the code string
     * 
     * @param info
     * @return null if not a constant operation; java code otherwise
     */
    
    static class Reduction {
    	boolean isSimpleConstant;
    	String operand;
    	String value;
    }
    
    static int parseInt( String s ) {
    	if (s.startsWith("(")) {
    		s = s.substring(1, s.length()-1);
    	}
    	if (s.startsWith("-")) {
    		return -Integer.parseInt(s.substring(1));    		
    	} else {
    		return Integer.parseInt(s);
    	}
    }
    
    String getOperandWithParenthesis(Variable v, int operatorPrecedence) {
    	String operand = v.getOperand();
    	if (v.getPrecedence() < operatorPrecedence) {
    		operand = "(" + operand + ")";
    	}
    	return operand;
    }
    
    Reduction reduceTemporaryOperations( Info info) {
    	Reduction ret = new Reduction();
		if (info.op == null)
			return null;
		if (info.range != null)
			return null;
		for (Variable in : info.input) {
			if ((options & REDUCE_SCALARS) > 0) {
				if (! (in.getType() == VariableType.SCALAR)) {
					return null;
				}
			} else if ((options & REDUCE_CONSTANTS) > 0) {
				if (!in.isConstant()) {
					return null;
				}
			} else {
				return null;
			}
		}
		
		Integer thisPrecedence = precedence.get(info.op.name());
		Variable p0 = (info.input.size() > 0) ? info.input.get(0) : null;
		String p0Operand = (p0 != null) ? getOperandWithParenthesis(p0, thisPrecedence) : null;
		Variable p1 = (info.input.size() > 1) ? info.input.get(1) : null;
		String p1Operand = (p1 != null) ? getOperandWithParenthesis(p1, thisPrecedence) : null;
		ret.operand = info.op.name();
		ret.isSimpleConstant = false;
		switch (info.op.name()) {
		case "neg-i":
		case "neg-s":
			if (isIntegerConstant(p0))  {
				ret.isSimpleConstant = true;
				ret.value = Integer.toString( -parseInt(p0Operand));
				info.output.setPrecedence(Precedence.MAX_PRECEDENCE);
				return ret;
			}
			break;
		case "add-ii":
		case "add-ss":
			if (isIntegerConstant(p0) && isIntegerConstant(p1)) {
				ret.isSimpleConstant = true;
				ret.value = Integer.toString( parseInt(p0Operand) + parseInt(p1Operand) );
				info.output.setPrecedence(Precedence.MAX_PRECEDENCE);
				return ret;
			}
			break;
		case "subtract-ii":
		case "subtract-ss":
			if (isIntegerConstant(p0) && isIntegerConstant(p1)) {
				ret.isSimpleConstant = true;
				ret.value = Integer.toString( parseInt(p0Operand) - parseInt(p1Operand) );
				info.output.setPrecedence(Precedence.MAX_PRECEDENCE);
				return ret;
			}
			break;
		case "multiply-ii":
		case "multiply-ss":
			if (isIntegerConstant(p0) && isIntegerConstant(p1)) {
				ret.isSimpleConstant = true;
				ret.value = Integer.toString( parseInt(p0Operand) * parseInt(p1Operand) );
				info.output.setPrecedence(Precedence.MAX_PRECEDENCE);
				return ret;
			}
			break;
		case "divide-ii":
		case "divide-ss":
			if (isIntegerConstant(p0) && isIntegerConstant(p1)) {
				ret.isSimpleConstant = true;
				ret.value = Integer.toString( parseInt(p0Operand) / parseInt(p1Operand) );
				info.output.setPrecedence(Precedence.MAX_PRECEDENCE);
				return ret;
			}
			break;
		default:
		}
		StringBuilder value = new StringBuilder();
		ArrayList<String> originals = new ArrayList<>();
		if (p0 != null) {
			originals.add(p0.getOperand());
			p0.setOperand(p0Operand);
			if (p1 != null) {
				originals.add(p1.getOperand());
				p1.setOperand(p1Operand);
			}
		}
		coder.emitOperation(value, info );
		if (p0 != null) {
			p0.setOperand(originals.get(0));
			if (p1 != null) {
				p1.setOperand(originals.get(1));			
			}
		}		
		int i = value.indexOf(" = "); 
		if (i < 0)
			return null;
		ret.value = value.substring(i+3).replace(";\n", "");
		info.output.setPrecedence( thisPrecedence );
		return ret;
	}
	
	final Precedence precedence = new Precedence();
	
    
	void eliminateConstantExpressions() {
//		infos.forEach(System.out::println); //TODO
		Iterator<Info> it = infos.iterator();
    	while( it.hasNext()) {
    		Info info = it.next();
    		if (! it.hasNext())
    			return;  // do not eliminate constants in final step
			if (info.output.isTemp()) {
				Reduction reduction = reduceTemporaryOperations(info);
				if (reduction != null) {
					if (info.output.getType() == VariableType.SCALAR) {
						stats.constantExpressions++;
						VariableScalar scalar = (VariableScalar) info.output;
						if (scalar.getScalarType() == VariableScalar.Type.INTEGER) {
							stats.integerTemps++;
							scalar.setName( String.format("Integer{%s}", reduction.value));
							scalar.setPrecedence(info.output.getPrecedence());
							it.remove();
						} else {
							stats.doubleTemps++;
							scalar.setName( String.format("Double{%s}", reduction.value));
							scalar.setPrecedence(info.output.getPrecedence());
							it.remove();
						}
//						System.out.println("ELIMINATE: " + info.toString() + " -> " + scalar );
					} // output is scalar
				} // if constant
			} // if temp
    	} // while
	}
    
	public void optimize() {
		mapVariableUsage();
    	stats.input.integerTemps = integerTemps.size();
    	stats.input.doubleTemps = doubleTemps.size();
    	stats.input.matrixTemps = matrixTemps.size();		
		eliminateConstantExpressions();
		mapVariableUsage();
		eliminateFinalCopy();
    	eliminateRedundantScalarTemps( integerUsages );
    	eliminateRedundantScalarTemps( doubleUsages );
    	eliminateRedundantMatrixTemps( matrixUsages );
    	stats.output.integerTemps = integerTemps.size();
    	stats.output.doubleTemps = doubleTemps.size();
    	stats.output.matrixTemps = matrixTemps.size();
	}
	
	void printUsage( StringBuilder out, Usage usage ) {
		if (usage == null) return;
		if (usage.variable != null)
			out.append( String.format("  %-30s : ", usage.variable.toString()) );
		out.append( usage.toString() );
		out.append( '\n' );
	}
	
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append(stats.toString());
    	out.append("INPUTS:\n");
    	for (Variable variable : inputs) {
    		printUsage(out, new Usage(variable));
    	}
    	out.append("INTEGER TEMPS:\n");
    	for (Usage usage : integerUsages) {
    		printUsage(out, usage);
    	}
    	out.append("DOUBLE TEMPS:\n");
    	for (Usage usage : doubleUsages) {
    		printUsage(out, usage);
    	}
    	out.append("MATRIX TEMPS:\n");
    	for (Usage usage : matrixUsages) {
    		printUsage(out, usage);
    	}
    	out.append("TARGET:\n");
    	printUsage(out, new Usage(assignmentTarget));
    	
    	for (Info info : infos) {
    		out.append( info.toString() );
    		out.append( '\n' );
    	}    	
		return out.toString();
	}

}