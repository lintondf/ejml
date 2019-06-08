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

/** Compiles a list of CodeOperation objects with optimizations
 * 
 * @author D. F. Linton, Blue Lightning Development, LLC 2019.
 */
public class CompileCodeOperations {

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
	EmitJavaCodeOperation coder;
	
	/**
	 * Constructs the compiler
	 * @param operations - List of CodeOperation objects 
	 */
	public CompileCodeOperations(List<Operation> operations) {    	
		coder = new EmitJavaCodeOperation();
    	this.operations = operations;
    	lastOperationCopyR = operations.get(operations.size()-1).name().startsWith("copyR-");
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
		for (int i = 0; i < operations.size(); i++) {
			CodeOperation codeOp = (CodeOperation) operations.get(i);
			if (codeOp.uses(variable)) {
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
    	for (Operation operation : operations) {
    		CodeOperation codeOp = (CodeOperation) operation;
    		//System.out.println( codeOp );
    		for (Variable var : codeOp.input) {
    			recordVariable(var);
    		}
    		if (codeOp.output != null) {
    			if (codeOp.output.isTemp()) {
    				recordVariable( codeOp.output );    				
    			} else {
    				assignmentTarget = codeOp.output;
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
    
	
	//matrix temps must not appear on both the lhs and rhs; TODO maybe codeOp dependent?
	void eliminateRedundantMatrixTemps(List<Usage> usages) {
		if (! usages.isEmpty()) {
			for (int i = 0; i < usages.size(); i++) {
				Usage first = usages.get(0);
				for (int j = i+1; j < usages.size(); j++) {
					Usage next = usages.get(j);
					if (first.uses.getLast() < next.uses.getFirst()) { // can replace next with first
//						System.out.println("  Replacing " + next.variable.getName() + " with " + first.variable.getName());
						for (Integer k : next.uses) {
							((CodeOperation) operations.get(k)).replace(next.variable, first.variable);
							first.uses.addLast(k);
						}
						usages.remove(j);
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
							((CodeOperation) operations.get(k)).replace(next.variable, first.variable);
							first.uses.addLast(k);
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
    		int last = operations.size()-1;
    		if (last <= 0)
    			return;
    		CodeOperation codeOp = (CodeOperation) operations.get(last);
			Variable fromVariable = codeOp.input.get(0);
    		switch (codeOp.name()) {
    		case "copy-mm":
    			if (fromVariable.isTemp()) {
    				Usage fromUsage = locateUsage(matrixUsages, fromVariable);
    				for (Integer k : fromUsage.uses) {
						((CodeOperation) operations.get(k)).replace(fromVariable, assignmentTarget);  
    				}
    				operations.remove(codeOp);
					matrixUsages.remove(fromUsage);
    			}
    			break;
    		case "copy-ss":
    			if (fromVariable.isTemp()) {
    				Usage fromUsage = locateUsage(doubleUsages, fromVariable);
    				for (Integer k : fromUsage.uses) {
						((CodeOperation) operations.get(k)).replace(fromVariable, assignmentTarget);  
    				}
    				operations.remove(codeOp);
    				doubleUsages.remove(fromUsage);
    			}
    			break;
    		case "copy-ii":
    			if (fromVariable.isTemp()) {
    				Usage fromUsage = locateUsage(integerUsages, fromVariable);
    				for (Integer k : fromUsage.uses) {
						((CodeOperation) operations.get(k)).replace(fromVariable, assignmentTarget);  
    				}
    				operations.remove(codeOp);
    				integerUsages.remove(fromUsage);
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
    
    /**
     * Get the java code for a constant operation.
     * 
     * If there is no associated submatrix range and every variable is a constant
     * compile the operation and return the code string
     * 
     * @param codeOp
     * @return null if not a constant operation; java code otherwise
     */
	String getConstantOperation( CodeOperation codeOp) {
		if (codeOp.range != null)
			return null;
		for (Variable in : codeOp.input) {
			if (!in.isConstant()) {
				return null;
			}
		}
		StringBuilder value = new StringBuilder();
		
		coder.emitOperation(value, codeOp);
		int i = value.indexOf(" = ");
		if (i < 0)
			return null;
		return value.substring(i+3).replace(";\n", "");
	}
	
	/**
	 * If a temporary exists solely to hold a constant expression eliminate it.
	 * 
	 * Convert the output temp to a scalar constant holding the constant expression.
	 */
	void eliminateConstantExpressionTemps() {
		Iterator<Operation> it = operations.iterator();
    	while( it.hasNext()) {
    		CodeOperation codeOp = (CodeOperation) it.next();
    		if (! it.hasNext())
    			return;  // do not eliminate constants in final step
			if (codeOp.output.isTemp()) {
				String value = getConstantOperation(codeOp);
				if (value != null) {
					if (codeOp.output.getType() == VariableType.SCALAR) {
						VariableScalar scalar = (VariableScalar) codeOp.output;
						if (scalar.getScalarType() == VariableScalar.Type.INTEGER) {
							scalar.setName( String.format("Integer{(%s)}", value));
							it.remove();
						} else {
							scalar.setName( String.format("Double{(%s)}", value));
							it.remove();
						}
					} // output is scalar
				} // if constant
			} // if temp
    	} // while
	}
    
	public void optimize() {
		eliminateConstantExpressionTemps();
		mapVariableUsage();
		eliminateFinalCopy();
    	eliminateRedundantScalarTemps( integerUsages );
    	eliminateRedundantScalarTemps( doubleUsages );
    	eliminateRedundantMatrixTemps( matrixUsages );
	}
	
	void printUsage( StringBuilder out, Usage usage ) {
		out.append( String.format("  %-30s : ", usage.variable.toString()) );
		out.append( usage.toString() );
		out.append( '\n' );
	}
	
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
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
    	
    	for (Operation operation : operations) {
    		CodeOperation codeOp = (CodeOperation) operation;
    		out.append( codeOp );
    		out.append( '\n' );
    	}    	
		return out.toString();
	}

}
