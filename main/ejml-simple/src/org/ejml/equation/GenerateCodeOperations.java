/**
 * 
 */
package org.ejml.equation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ejml.equation.Operation.Info;
import org.ejml.simple.SimpleMatrix;

/**
 * @author NOOK
 *
 */
public class GenerateCodeOperations {

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
	
	/**
	 * 
	 */
	public GenerateCodeOperations(List<Operation> operations) {    	
    	this.operations = operations;
    	lastOperationCopyR = operations.get(operations.size()-1).name().startsWith("copyR-");
	}

	protected void recordVariable( Variable variable ) {
		//System.out.println("Record: " + variable + " " + variable.isTemp());
		if (variable.isTemp()) {
			switch (variable.getType()) {
			case INTEGER_SEQUENCE:
				//throw new Exception("NIY"); // TODO
				break;
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
					//throw new Exception("NIY"); // TODO
					break;
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
	
	protected static class Usage {
		public Variable variable;
		public Deque<Integer> uses;
		
		public Usage(Variable variable) {
			this.variable = variable;
			this.uses = new ArrayDeque<>();
		}
		
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
	
	protected Usage findUsage( Variable variable) {
		Usage usage = new Usage(variable);
		for (int i = 0; i < operations.size(); i++) {
			CodeOperation codeOp = (CodeOperation) operations.get(i);
			if (codeOp.uses(variable)) {
				usage.uses.addLast(i);
			}
		}
		return usage;
	}
	
	
	protected void printUsage( Usage usage ) {
		System.out.printf("  %-30s : ", usage.variable.toString());
		System.out.print(usage.toString());
		System.out.println();
	}
	
	//matrix temps must not appear on both the lhs and rhs; TODO maybe codeOp dependent?
	protected void eliminateRedundantMatrixTemps(List<Usage> usages) {
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
	protected void eliminateRedundantScalarTemps(List<Usage> usages) {
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
	
	public Usage locateUsage( List<Usage> usages, Variable target) {
		for (Usage usage : usages) {
			if (usage.variable.equals(target))
				return usage;
		}
		return null;
	}
    
    private void mapVariableUsage() {
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
    
    private void eliminateFinalCopy() {
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
    

    protected String getJavaType( Variable variable ) {
		switch (variable.getType()) {
		case INTEGER_SEQUENCE:
			return ""; // TODO
		case SCALAR:
			VariableScalar scalar = (VariableScalar) variable;
			switch (scalar.getScalarType()) {
			case INTEGER:
				return "int";
			case DOUBLE:
				return "double";
			case COMPLEX:
				return ""; // TODO
			}
			break;
		case MATRIX:
			return "DMatrixRMaj";
		}
		return "";
    }
    
    

	final String declFormatWithValue = "%s%s%-10s\t%s = %s";
    final String declFormatInitialize = "%s%s%-10s\t%s = new %s(%s)";
    final String declFormatScalar = "%s%s%-10s %s = 0";
    final String declFormat = "%s%s%-10s\t%s";
    final String returnFormat = "%s%sreturn %s;\n";
    
    final Pattern reshapePattern = Pattern.compile("(\\w+)\\.reshape\\(\\s*(\\w+)\\.numRows\\,\\s*(\\w+)\\.numCols");
    
    public String getAssert( HashMap<String, String> constants, HashMap<String, String> lookups, Equation eq ) {
    	String ret = "";
    	if (assignmentTarget != null) {
	    	HashMap<String, Variable> variables = eq.getVariables();
	    	StringBuilder call = new StringBuilder();
	    	for (Variable variable : variables.values()) {
	    		if (variable.getName().equals("e")) {
	    		} else if (variable.getName().equals("pi")) {
	    		} else if (assignmentTarget != null && variable.equals(assignmentTarget)) {
	    			//assertTrue(new SimpleMatrix(A_coded).isIdentical(A, 1e-15));
	    			String var = variable.getName();
	    			String l = lookups.get(var);
	    			if (l != null) {
	    				var = l;
	    			} else if (constants.containsKey(variable.getName())) {
	    				var = constants.get(variable.getName());
	    			}
	    			
	    			ret = String.format("assertTrue(isIdentical(%s_coded, %s));", variable.getName(), var);
//	    			if (assignmentTarget.getType() != VariableType.MATRIX) {
//	    				System.out.println( assignmentTarget.type.toString() + " " + ret);
//	    				ret += " // " + assignmentTarget.getClass()
//	    			}
	    		}
	    	}
    	}
    	return ret;
    }
    
   
    public String getCallingSequence( HashMap<String, String> constants, Equation eq) {
    	HashMap<String, Variable> variables = eq.getVariables();
    	StringBuilder call = new StringBuilder();
    	boolean notFirst = false;
    	for (Variable variable : variables.values()) {
    		if (variable.getName().equals("e")) {
    		} else if (variable.getName().equals("pi")) {
    		} else if (assignmentTarget != null && variable.equals(assignmentTarget)) {
    			if (lastOperationCopyR) {
        			if (notFirst)
        				call.append(", ");
        			if (constants.containsKey(variable.getName())) {
        				call.append( constants.get(variable.getName()) );
        			} else {
    	    			call.append(variable.getName());
    	    			if (variable.getType() == VariableType.MATRIX)
    	    				call.append(".getDDRM()");
        			}
        			notFirst = true;
    			}
    		} else {
    			if (notFirst)
    				call.append(", ");
    			if (constants.containsKey(variable.getName())) {
    				call.append( constants.get(variable.getName()) );
    			} else {
	    			call.append(variable.getName());
	    			if (variable.getType() == VariableType.MATRIX)
	    				call.append(".getDDRM()");
    			}
    			notFirst = true;
    		}
    	}
    	return call.toString();
    }
    
    public String getReturnVariable( Equation eq ) {
    	String ret = "";
    	if (assignmentTarget != null) {
	    	HashMap<String, Variable> variables = eq.getVariables();
	    	StringBuilder call = new StringBuilder();
	    	for (Variable variable : variables.values()) {
	    		if (variable.getName().equals("e")) {
	    		} else if (variable.getName().equals("pi")) {
	    		} else if (assignmentTarget != null && variable.equals(assignmentTarget)) {
	    			ret = String.format("%s %s_coded = ", getJavaType(assignmentTarget), variable.getName());
	    		}
	    	}
    	}
    	return ret;
    }
    
    
    protected boolean isConstructed( Variable v ) {
    	if (v instanceof VariableMatrix) {
    		for (Operation operation : operations) {
    			CodeOperation codeOp = (CodeOperation) operation;
    			if (codeOp.constructor != null) {
    				if (codeOp.constructor.output != null && codeOp.constructor.output.getName().equals(v.getName()))
    					return true;
    			}
    		}
    	}
    	return false;
    }
   
    public void emitJavaTest( StringBuilder body, String prefix, String name, Equation eq, String equationText ) {
    	HashMap<String, Variable> variables = eq.getVariables();
    	StringBuilder header = new StringBuilder();
    	String returnType = "void";
    	if (assignmentTarget != null) {
    		returnType = getJavaType(assignmentTarget);
    	}
    	header.append(String.format("%sprotected %s %s(", prefix, returnType, name ) );
    	boolean notFirst = false;
    	
    	for (Variable variable : variables.values()) {
    		if (variable.getName().equals("e")) {
    		} else if (variable.getName().equals("pi")) {
    		} else if (assignmentTarget != null && variable.equals(assignmentTarget)) {
    			if (lastOperationCopyR) {
	    			if (notFirst)
	    				header.append(", ");
	    			String type = getJavaType(variable);
	   				header.append(String.format(declFormat, "", "", "final " + type, variable.getName()+"_in") );
	    			notFirst = true;
    			}
    		} else {
    			if (notFirst)
    				header.append(", ");
    			String type = getJavaType(variable);
   				header.append(String.format(declFormat, "", "", type, variable.getName()) );
    			notFirst = true;
    		}
    	}
    	
    	header.append(") {\n");
    	body.append(header.toString().replaceAll("\t", " "));
    	body.append(prefix);
    	body.append(prefix);
    	body.append("// ");
    	body.append( equationText);
    	body.append("\n");
    	for (Variable variable : variables.values()) {
			if (assignmentTarget != null && variable.equals(assignmentTarget)) {
				if (! isConstructed(variable)) {
					String type = getJavaType(assignmentTarget);
					if (type.equals("int") || type.equals("double")) {
						body.append(String.format(declFormatWithValue, prefix, prefix, type, variable.getName(), "0") );					
					} else {
						String constructParameters = "1,1";
						if (this.lastOperationCopyR) {
							constructParameters = variable.getName() + "_in";
						}
						body.append(String.format(declFormatInitialize, prefix, prefix, type, variable.getName(), type, constructParameters) );
					}
					body.append(";\n");
				}
			}
    	}    	
    	for (Usage usage : integerUsages) {
    		if (!usage.variable.isConstant()) {
	    		body.append(String.format(declFormatScalar, prefix, prefix, getJavaType(usage.variable), usage.variable.getOperand()) );
				body.append(";\n");
    		}
    	}
    	for (Usage usage : doubleUsages) {
    		if (!usage.variable.isConstant()) {
    			body.append(String.format(declFormatScalar, prefix, prefix, getJavaType(usage.variable), usage.variable.getOperand()) );
    			body.append(";\n");
    		}
    	}
    	for (Usage usage : matrixUsages) {
    		if (!usage.variable.isConstant()) {
				String type = getJavaType(usage.variable);
	    		body.append(String.format(declFormatInitialize, prefix, prefix, type, usage.variable.getName(), type, "1,1") );
				body.append(";\n");
    		}
    	}
    	body.append("\n");
    	HashMap<String, String> reshapes = new HashMap<>();
    	for (Operation operation : operations) {
    		CodeOperation codeOp = (CodeOperation) operation;
    		StringBuilder block = new StringBuilder();
    		EmitCodeOperation.emitJavaOperation( block, codeOp );
    		String[] lines = block.toString().split("\n");
    		for (String line : lines) {
    			// prune exact sequential reshapes
    			Matcher matcher = reshapePattern.matcher(line);
    			if (matcher.find()) {
    				if (reshapes.containsKey(matcher.group(1))) {
    					if (line.equals(reshapes.get(matcher.group(1))))
    						continue;
    				}
    				if (matcher.group(1).equals(matcher.group(2)) && matcher.group(1).equals(matcher.group(3))) {
    					continue; // skip self reshapes
    				}
    				
    				//TODO skip reshape if array is an input to the next operation
					reshapes.put(matcher.group(1), line);
    			}
    			body.append(prefix);
    			body.append(prefix);
    			body.append(line);
    			body.append("\n");
    		}
    	}    	
    	if (assignmentTarget != null) {
    		body.append("\n");
    		body.append( String.format(returnFormat, prefix, prefix, assignmentTarget.getName()) );
    	}
		body.append(prefix);
    	body.append("}");
    }

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
    
	public void optimize() {
		eliminateConstantExpressionTemps();
		mapVariableUsage();
//    	TODO (premature optimization, etc etc etc
		eliminateFinalCopy();
    	eliminateRedundantScalarTemps( integerUsages );
    	eliminateRedundantScalarTemps( doubleUsages );
    	eliminateRedundantMatrixTemps( matrixUsages );
		
    	if (false) {
	    	System.out.println("INPUTS:");
	    	for (Variable variable : inputs) {
	    		printUsage(new Usage(variable));
	    	}
	    	System.out.println("INTEGER TEMPS:");
	    	for (Usage usage : integerUsages) {
	    		printUsage(usage);
	    	}
	    	System.out.println("DOUBLE TEMPS:");
	    	for (Usage usage : doubleUsages) {
	    		printUsage(usage);
	    	}
	    	System.out.println("MATRIX TEMPS:");
	    	for (Usage usage : matrixUsages) {
	    		printUsage(usage);
	    	}
	    	System.out.println("TARGET:");
	    	printUsage(new Usage(assignmentTarget));
	    	
	    	for (Operation operation : operations) {
	    		CodeOperation codeOp = (CodeOperation) operation;
	    		System.out.println( codeOp );
	    	}    	
    	}
	}

	private String getConstantOperation( CodeOperation codeOp) {
		if (codeOp.range != null)
			return null;
		for (Variable in : codeOp.input) {
			if (!in.isConstant()) {
				return null;
			}
		}
		StringBuilder value = new StringBuilder();
		EmitCodeOperation.emitJavaOperation(value, codeOp);
		int i = value.indexOf(" = ");
		if (i < 0)
			return null;
		return value.substring(i+3).replace(";\n", "");
	}
	
	private void eliminateConstantExpressionTemps() {
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
    
}
