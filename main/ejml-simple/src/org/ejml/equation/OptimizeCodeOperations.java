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
public class OptimizeCodeOperations {

	List<Operation> operations = new ArrayList<>();
	List<Variable> inputs = new ArrayList<>();
	List<Variable> doubleTemps = new ArrayList<>();
	List<Variable> integerTemps = new ArrayList<>();
	List<Variable> matrixTemps = new ArrayList<>();
	Variable assignmentTarget = null;
	
	List<Usage> doubleUsages = new ArrayList<>();
	List<Usage> integerUsages = new ArrayList<>();
	List<Usage> matrixUsages = new ArrayList<>();
	

	
	/**
	 * 
	 */
	public OptimizeCodeOperations(List<Operation> operations) {    	
    	this.operations = operations;
	}

	protected void recordVariable( Variable variable ) {
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
	
	protected void eliminateRedundantTemps(List<Usage> usages) {
		if (! usages.isEmpty()) {
			for (int i = 0; i < usages.size(); i++) {
				Usage first = usages.get(0);
				for (int j = i+1; j < usages.size(); j++) {
					Usage next = usages.get(j);
					if (first.uses.getLast() <= next.uses.getFirst()) { // can replace next with first
//						System.out.println("Replacing " + next.variable.getName() + " with " + first.variable.getName());
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
    
    public void mapVariableUsage() {
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
    	eliminateRedundantTemps( integerUsages );
    	
    	for (Variable variable : doubleTemps) {
    		Usage usage = findUsage(variable);
    		doubleUsages.add(usage);
    	}
    	eliminateRedundantTemps( doubleUsages );

    	for (Variable variable : matrixTemps) {
    		Usage usage = findUsage(variable);
    		matrixUsages.add(usage);
    	}
    	eliminateRedundantTemps( matrixUsages );

    	if (assignmentTarget != null) {
    		int last = operations.size()-1;
    		CodeOperation codeOp = (CodeOperation) operations.get(last);
    		if (codeOp.name().equals("copy-mm")) {
    			Variable fromVariable = codeOp.input.get(0);
    			if (fromVariable.isTemp()) {
    				Usage fromUsage = locateUsage(matrixUsages, fromVariable);
    				for (Integer k : fromUsage.uses) {
						((CodeOperation) operations.get(k)).replace(fromVariable, assignmentTarget);  
    				}
    				operations.remove(codeOp);
					matrixUsages.remove(fromUsage);
    			}
    		}
    	}

//    	System.out.println("INPUTS:");
//    	for (Variable variable : inputs) {
//    		printUsage(new Usage(variable));
//    	}
//    	System.out.println("INTEGER TEMPS:");
//    	for (Usage usage : integerUsages) {
//    		printUsage(usage);
//    	}
//    	System.out.println("DOUBLE TEMPS:");
//    	for (Usage usage : doubleUsages) {
//    		printUsage(usage);
//    	}
//    	System.out.println("MATRIX TEMPS:");
//    	for (Usage usage : matrixUsages) {
//    		printUsage(usage);
//    	}
//    	System.out.println("TARGET:");
//    	printUsage(new Usage(assignmentTarget));
//    	
//    	for (Operation operation : operations) {
//    		CodeOperation codeOp = (CodeOperation) operation;
//    		System.out.println( codeOp );
//    	}    	
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
    final String declFormatInitialize = "%s%s%-10s\t%s = new %s(1,1)";
    final String declFormat = "%s%s%-10s\t%s";
    final String returnFormat = "%s%sreturn %s;\n";
    
    final Pattern reshapePattern = Pattern.compile("(\\w+)\\.reshape\\(\\s*(\\w+)\\.numRows\\,\\s*(\\w+)\\.numCols");
    
    public String getAssert( Equation eq ) {
    	String ret = "";
    	if (assignmentTarget != null) {
	    	HashMap<String, Variable> variables = eq.getVariables();
	    	StringBuilder call = new StringBuilder();
	    	for (Variable variable : variables.values()) {
	    		if (variable.getName().equals("e")) {
	    		} else if (variable.getName().equals("pi")) {
	    		} else if (assignmentTarget != null && variable.equals(assignmentTarget)) {
	    			//assertTrue(new SimpleMatrix(A_coded).isIdentical(A, 1e-15));
	    			ret = String.format("assertTrue(new SimpleMatrix(%s_coded).isIdentical(%s.getDDRM(),1e-15));", variable.getName(), variable.getName());
	    		}
	    	}
    	}
    	return ret;
    }
    
   
    public String getCallingSequence( Equation eq) {
    	HashMap<String, Variable> variables = eq.getVariables();
    	StringBuilder call = new StringBuilder();
    	boolean notFirst = false;
    	for (Variable variable : variables.values()) {
    		if (variable.getName().equals("e")) {
    		} else if (variable.getName().equals("pi")) {
    		} else if (assignmentTarget != null && variable.equals(assignmentTarget)) {
    		} else {
    			if (notFirst)
    				call.append(", ");
    			call.append(variable.getName());
    			call.append(".getDDRM()");
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
//    			body.append(String.format(declFormatWithValue, "double", "e", "Math.E") );
//    			body.append(";\n");
    		} else if (variable.getName().equals("pi")) {
//    			body.append(String.format(declFormatWithValue, "double", "pi", "Math.PI") );
//    			body.append(";\n");
    		} else if (assignmentTarget != null && variable.equals(assignmentTarget)) {
//    			String type = getJavaType(assignmentTarget);
//    			body.append(String.format(declFormatInitialize, type, variable.getName(), type) );
//    			body.append(";\n");
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
				String type = getJavaType(assignmentTarget);
				body.append(String.format(declFormatInitialize, prefix, prefix, type, variable.getName(), type) );
				body.append(";\n");
			}
    	}    	
    	for (Usage usage : integerUsages) {
    		body.append(String.format(declFormat, prefix, prefix, getJavaType(usage.variable), usage.variable.getName()) );
			body.append(";\n");
    	}
    	for (Usage usage : doubleUsages) {
    		body.append(String.format(declFormat, prefix, prefix, getJavaType(usage.variable), usage.variable.getName()) );
			body.append(";\n");
    	}
    	for (Usage usage : matrixUsages) {
			String type = getJavaType(usage.variable);
    		body.append(String.format(declFormatInitialize, prefix, prefix, type, usage.variable.getName(), type) );
			body.append(";\n");
    	}
    	body.append("\n");
    	HashMap<String, String> reshapes = new HashMap<>();
    	for (Operation operation : operations) {
    		CodeOperation codeOp = (CodeOperation) operation;
    		StringBuilder block = new StringBuilder();
    		EmitCodeOperation.emitJavaOperation( block, codeOp );
    		String[] lines = block.toString().split(";");
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
    			body.append(";\n");
    		}
    	}    	
    	if (assignmentTarget != null) {
    		body.append("\n");
    		body.append( String.format(returnFormat, prefix, prefix, assignmentTarget.getName()) );
    	}
		body.append(prefix);
    	body.append("}");
    }
    
}
