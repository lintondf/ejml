/**
 * 
 */
package org.ejml.equation;

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
    	for (Operation operation : operations) {
    		CodeOperation codeOp = (CodeOperation) operation;
    		System.out.println( codeOp );
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
    
    
    
//    protected String mmOp(String op, CodeOperation codeOp) {
//    	String target = codeOp.output.getName();
//    	String lhs = codeOp.input.get(0).getName();
//    	String rhs = codeOp.input.get(1).getName();
//    	switch (op) {
//    	case "add":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "add", lhs, rhs, target);
////    	case "copy":
////    	case "copyR":
//    	case "dot":
//        	return String.format("\t\t%s = %s(%s, %s);\n", target, "dot", lhs, rhs );    	
//    	case "elementDivision":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "elementDiv", lhs, rhs, target);
//    	case "elementMult":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "elementMult", lhs, rhs, target);
//    	case "elementPow":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "elementPower", lhs, rhs, target);
//    	case "kron":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "kron", lhs, rhs, target);
//    	case "multiply":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "mult", lhs, rhs, target);
//    	case "solve":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "solve", lhs, rhs, target);    	
//    	case "subtract":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "subtract", rhs, lhs, target);    	
//    	}
//    	return String.format("\t\t%s = %s %s %s; ERROR\n", target, lhs, op, rhs );
//    }
//    
//    protected String msOp(String op, CodeOperation codeOp) {
//    	String target = codeOp.output.getName();
//    	String lhs = codeOp.input.get(0).getName();
//    	VariableScalar rhs = (VariableScalar) codeOp.input.get(1);
//    	switch (op) {
//    	case "add":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "add", lhs, rhs.getOperand(), target);    	
//    	case "multiply":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "scale", rhs.getOperand(), lhs, target);    	
//    	case "subtract":   	
//    		return String.format("\t\t%s(%s, %s, %s);\n", "substract", lhs, rhs.getOperand(), target);    	
//    	case "elementPow":
//    		return String.format("\t\t%s(%s, %s, %s);\n", "elementPower", lhs, rhs.getOperand(), target);
//    	}
//    	return String.format("\t\t%s = %s %s %s;\n", target, lhs, op, rhs );
//    }
//    
//    protected String sOp(String op, CodeOperation codeOp) {
//    	String target = codeOp.output.getName();
//    	VariableScalar lhs = (VariableScalar) codeOp.input.get(0);
//    	return String.format("\t\t%s = Math.%s(%s);\n", target, op, lhs.getOperand() );    	
//    }
//    
//    protected String ssOp(String op, CodeOperation codeOp) {
//    	String target = codeOp.output.getName();
//    	VariableScalar lhs = (VariableScalar) codeOp.input.get(0);
//    	VariableScalar rhs = (VariableScalar) codeOp.input.get(1);
//    	switch (op) {
//    	case "add":
//    		return String.format("\t\t%s = %s %s %s;\n", target, lhs.getOperand(), "+", rhs.getOperand() );
//    	case "divide":
//    		return String.format("\t\t%s = %s %s %s;\n", target, lhs.getOperand(), "/", rhs.getOperand() );
//    	case "multiply":
//    		return String.format("\t\t%s = %s %s %s;\n", target, lhs.getOperand(), "*", rhs.getOperand() );
//    	case "subtract":
//    		return String.format("\t\t%s = %s %s %s;\n", target, lhs.getOperand(), "-", rhs.getOperand() );
//    	case "atan2":
//        	return String.format("\t\t%s = %s(%s, %s);\n", target, "atan2", lhs.getOperand(), rhs.getOperand() );    	
//    	case "copy":
//        	return String.format("\t\t%s = %s(%s);\n", target, lhs.getOperand() );    	
//    	case "elementPow":
//    	case "pow":
//        	return String.format("\t\t%s = %s(%s);\n", target, "pow", lhs.getOperand(), rhs.getOperand() );    	
//    	}
//    	return String.format("\t\t%s = %s %s %s; NIY\n", target, lhs, op, rhs );
//    }
    
    
	protected void emitJavaOperation(StringBuilder body, CodeOperation codeOp) {
//		if (! codeOp.dimensions.isEmpty()) {
//			emitResizeTarget( body, codeOp );
//		}
//		String[] fields = codeOp.name().split("-");
//		switch (fields[1]) {
//		case "mm":
//			body.append( mmOp( fields[0], codeOp ) );
//			break;
//		case "ms":
//			body.append( msOp( fields[0], codeOp ) );
//			break;
//		case "ss":
//			body.append( ssOp( fields[0], codeOp ) );
//			break;
//		case "s":
//			switch (fields[0]) {
//			case "det":
//			case "inv":
//			case "normF":
//			case "pinv":
//			case "rref":
//			case "trace":
//				break;
//			default: // java.lang.Math
//				body.append( sOp( fields[0], codeOp) );
//			}
//			break;
//		default:
//			break;
//		}
//		/*
//ii
//ma
//i
//s
//m
//sm
//sm1
//is		 
//		*/
	}

    private void emitResizeTarget(StringBuilder body, CodeOperation codeOp) {
    	String rows = "";
    	String cols = "";
    	switch (codeOp.dimensions.get(0)) {
    	case RHS_ROWS:
    		rows = String.format("%s.numRows", codeOp.input.get(1).getName());
    		break;
    	case LHS_ROWS:
    		rows = String.format("%s.numRows", codeOp.input.get(0).getName());
    		break;
    	}
    	if (codeOp.dimensions.size() > 1) {
	    	switch (codeOp.dimensions.get(1)) {    	
	    	case RHS_COLS:
	    		cols = String.format("%s.numCols", codeOp.input.get(1).getName());
	    		break;
	    	case LHS_COLS:
	    		cols = String.format("%s.numCols", codeOp.input.get(0).getName());
	    		break;
	    	}
    	}
    	body.append(String.format("\t\t%s.reshape(%s,%s);\n", codeOp.output.getName(), rows, cols ) );
	}


	final String declFormatWithValue = "\t\t%-10s\t%s = %s";
    final String declFormatInitialize = "\t\t%-10s\t%s = new %s(1,1)";
    final String declFormat = "\t\t%-10s\t%s";
    final String returnFormat = "\t\treturn %s;\n";
    
   
    public void emitJavaTest( PrintStream out, Equation eq, String equationText ) {
    	HashMap<String, Variable> variables = eq.getVariables();
    	StringBuilder header = new StringBuilder();
    	String returnType = "void";
    	if (assignmentTarget != null) {
    		returnType = getJavaType(assignmentTarget);
    	}
    	header.append(String.format("\tpublic %s test(", returnType ) );
    	StringBuilder body = new StringBuilder();
    	boolean notFirst = false;
    	for (Variable variable : variables.values()) {
    		if (variable.getName().equals("e")) {
    			body.append(String.format(declFormatWithValue, "double", "e", "Math.E") );
    			body.append(";\n");
    		} else if (variable.getName().equals("pi")) {
    			body.append(String.format(declFormatWithValue, "double", "pi", "Math.PI") );
    			body.append(";\n");
    		} else if (assignmentTarget != null && variable.equals(assignmentTarget)) {
    			String type = getJavaType(assignmentTarget);
    			body.append(String.format(declFormatInitialize, type, variable.getName(), type) );
    			body.append(";\n");
    		} else {
    			if (notFirst)
    				header.append(",");
    			String type = getJavaType(variable);
    			header.append(String.format(declFormat, type, variable.getName()) );
    			notFirst = true;
    		}
    	}
    	
    	header.append(") {\n");
    	System.out.println("INTEGER TEMPS:");
    	for (Usage usage : integerUsages) {
    		body.append(String.format(declFormat, getJavaType(usage.variable), usage.variable.getName()) );
			body.append(";\n");
    	}
    	System.out.println("DOUBLE TEMPS:");
    	for (Usage usage : doubleUsages) {
    		body.append(String.format(declFormat, getJavaType(usage.variable), usage.variable.getName()) );
			body.append(";\n");
    	}
    	System.out.println("MATRIX TEMPS:");
    	for (Usage usage : matrixUsages) {
			String type = getJavaType(usage.variable);
    		body.append(String.format(declFormatInitialize, type, usage.variable.getName(), type) );
			body.append(";\n");
    	}
    	body.append("\n");
    	for (Operation operation : operations) {
    		CodeOperation codeOp = (CodeOperation) operation;
    		emitJavaOperation( body, codeOp );
    	}    	
    	out.print("\t// ");
    	out.println( equationText);
    	out.print("\t");
    	out.println(header.toString().replaceAll("\t", " "));
    	out.print(body);
    	if (assignmentTarget != null) {
        	out.println();
    		out.print( String.format(returnFormat, assignmentTarget.getName()) );
    	}
    	out.println("\t}");
    }
    
    private static class Execution {
    	public String method;
    	public String name;
    	public String inputs;
    	public ArrayList<String> body;
    	
    	public Execution( String methodDecl, String operationDecl ) {
    		method = methodDecl.replace("public ", "").replace(" {", "");
    		int i = operationDecl.indexOf('"');
    		int j = operationDecl.lastIndexOf('"');
    		String[] nameParts = operationDecl.substring(i+1, j).split("-");
    		name = nameParts[0];
    		if (nameParts.length != 2)
    			inputs = "";
    		else
    			inputs = nameParts[1];
    		body = new ArrayList<>();
    	}
    	
    	protected boolean codeVariableMatrix( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		final Pattern pattern = Pattern.compile("VariableMatrix\\s*(\\w+)\\s*=\\s*\\(VariableMatrix\\)\\s*(\\w+)\\;");
    		return codeRenamePattern( sb, prefix, renames, pattern, line );
    	}
    	
    	protected boolean codeVariableInteger( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		final Pattern pattern = Pattern.compile("VariableInteger\\s*(\\w+)\\s*=\\s*\\(VariableInteger\\)\\s*(\\w+)\\;");
    		return codeRenamePattern( sb, prefix, renames, pattern, line );
    	}
    	
    	protected boolean codeIntVariableInteger( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		final Pattern pattern = Pattern.compile("int\\s*(\\w+)\\s*=\\s*\\(VariableInteger\\)\\s*(\\w+)\\;");
    		return codeRenamePattern( sb, prefix, renames, pattern, line );
    	}
    	
    	protected boolean codeVariableScalar( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		final Pattern pattern = Pattern.compile("VariableScalar\\s*(\\w+)\\s*=\\s*\\(VariableScalar\\)\\s*(\\w+)\\;");
    		return codeRenamePattern( sb, prefix, renames, pattern, line );
    	}
    	
    	protected boolean codeDoubleVariableScalar( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		final Pattern pattern = Pattern.compile("double\\s*(\\w+)\\s*=\\s*\\(\\(VariableScalar\\)\\s*(\\w+)\\)\\.getDouble\\(\\)\\;");
    		return codeRenamePattern( sb, prefix, renames, pattern, line );
    	}
    	
    	protected boolean codeDMatrixRMaj( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		final Pattern pattern = Pattern.compile("DMatrixRMaj\\s*(\\w+)\\s*=\\s*\\(\\(VariableMatrix\\)\\s*(.+)\\).matrix\\;");
    		return codeRenamePattern( sb, prefix, renames, pattern, line );
    	}
    	
    	protected boolean codeRenamePattern( StringBuilder sb, String prefix, Map<String, String> renames, Pattern pattern, String line ) {
    		Matcher matcher = pattern.matcher(line);
    		if (matcher.find()) {
    			renames.put( matcher.group(1), matcher.group(2) );
    			return true;
    		}
    		return false;
    	}
    	
    	protected static void codeFormats( PrintStream code, String prefix ) {
			code.print(prefix);
			code.print("final String formatReshape = \"%s.reshape( %s.numRows, %s.numCols );\";");
			code.print("\n");    		
			code.print(prefix);
			code.print("final String formatCommonOps3 = \"CommonOps_DDRM.%s( %s, %s, %s );\";");
			code.print("\n");
			code.print(prefix);
			code.print("final String formatCommonOps2 = \"CommonOps_DDRM.%s( %s, %s );\";");
			code.print("\n");
			code.print(prefix);
			code.print("final String formatCommonOps1 = \"CommonOps_DDRM.%s( %s );\";");
			code.print("\n");
    	}
    	
    	protected boolean codeManagerResize( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		final Pattern pattern = Pattern.compile("manager\\.resize\\(\\s*output,\\s*(\\w+)(\\.matrix)?\\.(\\w+),\\s*(\\w+)(\\.matrix)?\\.(\\w+)\\)\\;");
    		Matcher matcher = pattern.matcher(line);
    		if (matcher.find()) {
    			// groups: 1 - rows-source, 2 - null|.matrix, 3-numrows, 4-cols-source, 5-null|.matrix, 6-numcols
    			/* from OperationExecuteFactory:
    			 	manager.resize(output, mA.matrix.numRows, mA.matrix.numCols);
    			   becomes in user code:
					<target>.reshape( <rowSource>.runRows, <colSource>.numCols )
			manager.resize(output, -> map[output].reshape(
			mA.matrix -> map[mA]
			mB.matrix -> map[mB]
    			 */
    			sb.append(prefix);
    			String code = "sb.append( String.format(formatReshape, output.getName(), %s.getName(), %s.getName()) );"; 
    			sb.append(String.format(code, rename(renames, matcher.group(1)), rename(renames, matcher.group(4))));
    			sb.append("\n");
    			return true;
    		}
    		return false;    		
    	}
    	
    	protected boolean codeManagerReshape( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		//(\w+)(\.matrix)?\.reshape\(\s*([^,]*),\s*(.*)\);
    		final Pattern pattern = Pattern.compile("(\\w+)(\\.matrix)?\\.reshape\\(\\s*([^,]*),\\s*(.*)\\);");
    		Matcher matcher = pattern.matcher(line);
    		if (matcher.find()) {
    			//groups: 1-variable, 2 - null|.matrix, 3-row expression, 4-col expression
    			sb.append(prefix);
    			String code = "sb.append( String.format(formatReshape, %s.getName(), %s.getName(), %s.getName()) );";
    			String a = rename(renames, matcher.group(1));
        		final Pattern variable = Pattern.compile("\\w+");
        		final Pattern patternBinary = Pattern.compile("%s\\s*=\\s*%s\\s*([\\+\\-\\*\\/])\\s*%s;");
        		if (!variable.matcher(a).matches()) {
        			return false;
        		}
        		String b = rename(renames, matcher.group(3));
        		if (!variable.matcher(b).matches()) {
        			return false;
        		}
        		String c = rename(renames, matcher.group(4));
        		if (!variable.matcher(c).matches()) {
        			return false;
        		}
    			sb.append(String.format(code, a, b, c));
    			sb.append("\n");
    			return true;
    		}
    		return false;
    	}
    	
    	
    	protected boolean codeCommonOps( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		//CommonOps_DDRM\.(\w+)\(\s*(-?\w+)(\.[^,]+)?,\s*(-?\w+)(\.[^,]+)?,\s*(\w+)(\.matrix)?\s*\);
    		final Pattern pattern3Args = Pattern.compile("CommonOps_DDRM\\.(\\w+)\\(\\s*(-?\\w+)(\\.[^,]+)?,\\s*(-?\\w+)(\\.[^,]+)?,\\s*(\\w+)(\\.matrix)?\\s*\\);");
    		Matcher matcher = pattern3Args.matcher(line);
    		if (matcher.find()) {
    			sb.append(prefix);
    			sb.append("//" + line + "\n");
    			sb.append(prefix);
    			if (matcher.group(3) == null && matcher.group(5) == null) {
        			String code = "sb.append( String.format(formatCommonOps3, \"%s\", %s.getName(), %s.getName(), %s.getName()) );"; 
        			sb.append(String.format(code, matcher.group(1), rename(renames, matcher.group(2)), rename(renames, matcher.group(4)), rename(renames, matcher.group(6))));
    			} else if (matcher.group(3).equals(".matrix") && matcher.group(5).equals(".matrix")) {
    				String code = "sb.append( String.format(formatCommonOps3, \"%s\", %s.getName(), %s.getName(), %s.getName()) );"; 
    				sb.append(String.format(code, matcher.group(1), rename(renames, matcher.group(2)), rename(renames, matcher.group(4)), rename(renames, matcher.group(6))));
    			} else {
    				String code = "sb.append( String.format(formatCommonOps3, \"%s\", %s, %s, %s) );"; 
    				String a = rename(renames, matcher.group(2)) + ".getName()";
    				if (!matcher.group(3).equals(".matrix")) {
    					a += matcher.group(3);
    				}
    				String b = rename(renames, matcher.group(4)) + ".getName()";
    				if (!matcher.group(5).equals(".matrix")) {
    					b += matcher.group(5);
    				}
    				String o = rename(renames, matcher.group(6)) + ".getName()";
    				sb.append(String.format(code, matcher.group(1), a, b, o));
    			}
    			sb.append("\n");
    			return true;
    		}
    		final Pattern pattern2Args = Pattern.compile("CommonOps_DDRM\\.(\\w+)\\(\\s*(-?\\w+)(\\.[^,]+)?,\\s*(-?\\w+)(\\.matrix)?\\s*\\);");
    		matcher = pattern2Args.matcher(line);
    		if (matcher.find()) {
    			String code = "sb.append( String.format(formatCommonOps2, \"%s\", %s, %s) );";
    			sb.append(prefix);
    			sb.append("//" + line + "\n");
    			sb.append(prefix);
   				String a = rename(renames, matcher.group(4));
   				if (matcher.group(5) == null) {
   				} else if (!matcher.group(5).equals(".matrix")) {
					a += matcher.group(5);
				} else {
   					a += ".getName()";					
				}
				String o = rename(renames, matcher.group(2)) + ".getName()";
    			sb.append(String.format(code, matcher.group(1), o, a));
    			sb.append("\n");
    			return true;
    		}
    		final Pattern pattern1Arg  = Pattern.compile("CommonOps_DDRM\\.(\\w+)\\(\\s*(-?\\w+)(\\.matrix)?\\);");
    		matcher = pattern1Arg.matcher(line);
    		if (matcher.find()) {
//    			System.out.println("cCO1: " + line);
//    			System.out.print("{");
//    			for (int g = 1; g <= matcher.groupCount(); g++) System.out.print(g+":"+matcher.group(g)+",");
//    			System.out.println("}");
    			String code = "sb.append( String.format(formatCommonOps1, \"%s\", %s) );";
    			sb.append(prefix);
    			sb.append("//" + line + "\n");
    			sb.append(prefix);
    			String o = rename(renames, matcher.group(2)) + ".getName()";
    			sb.append(String.format(code, matcher.group(1), o ));
    			sb.append("\n");
    			return true;
    		}
    		System.out.println("?cCO: "+line);
    		return false;
    	}
    	
    	protected boolean codeOutputValue( StringBuilder sb, String prefix, Map<String, String> renames, String line ) {
    		ArrayList<String> args = new ArrayList<>();
    		//(\w+)\.value
    		final Pattern patternNoCast = Pattern.compile("(\\w+)\\.value");
    		//	\(\((\w+)\)\s*(\w+)\)\.value
    		final Pattern patternCastValue = Pattern.compile("\\(\\((\\w+)\\)\\s*(\\w+)\\)\\.value");
    		final Pattern patternCastMatrix = Pattern.compile("\\(\\((\\w+)\\)\\s*(\\w+)\\)\\.matrix");
    		//(\w+)\.getDouble\(\)
    		final Pattern patternGetDouble = Pattern.compile("(\\w+)\\.getDouble\\(\\)");
    		//	\(\((\w+)\)\s*(\w+)\)\.getDouble\(\)
    		final Pattern patternCastGetDouble = Pattern.compile("\\(\\((\\w+)\\)\\s*(\\w+)\\)\\.getDouble\\(\\)");
    		
    		final Pattern patternBinary = Pattern.compile("%s\\s*=\\s*%s\\s*([\\+\\-\\*\\/])\\s*%s;");
    		final Pattern patternUnary = Pattern.compile("%s\\s*=\\s*([\\+\\-]*)\\s*%s;");

    		final Pattern pattern1Arg = Pattern.compile("(\\w+)\\.(\\w+)\\(\\s*(-?\\w+)(\\.[^\\)\\s]+)?\\s*\\);");
    		final Pattern pattern2Args = Pattern.compile("(\\w+)\\.(\\w+)\\(\\s*(-?\\w+)(\\.[^,]+)?,\\s*(-?\\w+)(\\.matrix)?\\s*\\);");
    		final Pattern pattern1Parameter = Pattern.compile("(\\w+)\\.(\\w+)\\(\\s*(%s)\\s*\\);");

    		boolean success = false;
    		Matcher matcher = patternNoCast.matcher(line);
    		while (matcher.find()) {
    			args.add(rename(renames, matcher.group(1)));
    			line = matcher.replaceFirst("%s");
    			success = true;
    			matcher = patternNoCast.matcher(line);
    		}
    		matcher = patternCastValue.matcher(line);
    		while (matcher.find()) {
    			args.add(rename(renames, matcher.group(2)));
    			line = matcher.replaceFirst("%s");
    			success = true;
    			matcher = patternCastValue.matcher(line);
    		}
    		matcher = patternCastMatrix.matcher(line);
    		while (matcher.find()) {
    			args.add(rename(renames, matcher.group(2)));
    			line = matcher.replaceFirst("%s");
    			success = true;
    			matcher = patternCastMatrix.matcher(line);
    		}
    		matcher = patternGetDouble.matcher(line);
    		while (matcher.find()) {
    			args.add(rename(renames, matcher.group(1)));
    			line = matcher.replaceFirst("%s");
    			success = true;
    			matcher = patternGetDouble.matcher(line);
    		}
    		matcher = patternCastGetDouble.matcher(line);
    		while (matcher.find()) {
    			args.add(rename(renames, matcher.group(2)));
    			line = matcher.replaceFirst("%s");
    			success = true;
    			matcher = patternCastGetDouble.matcher(line);
    		}

    		matcher = patternBinary.matcher(line);
    		Matcher unary = patternUnary.matcher(line);
    		Matcher oneArg = pattern1Arg.matcher(line);
    		Matcher twoArgs = pattern2Args.matcher(line);
    		Matcher oneParameter = pattern1Parameter.matcher(line);

    		if (matcher.find()) {
//    			System.out.printf("   ! \"%%s = %%s %s %%s;\" %% %s\n", matcher.group(1), args.toString() );
    			String code = "sb.append( String.format(";
    			String close = ") );";
    			sb.append(prefix);
    			sb.append("//" + line + "\n");
    			sb.append(prefix);
    			sb.append(code);
    			String format = String.format("\"%%s = %%s %s %%s;\"", matcher.group(1));
    			sb.append(format);
    			for (String arg : args) {
    				sb.append(", ");
    				sb.append(arg);
    				sb.append(".getName()");
    			}
    			sb.append(close);
    			sb.append("\n");
    		} else if (unary.find()) {
//    			System.out.printf("   ! \"%%s = %s%%s;\" %% %s\n", unary.group(1), args.toString() );    			
    			String code = "sb.append( String.format(";
    			String close = ") );";
    			sb.append(prefix);
    			sb.append("//" + line + "\n");
    			sb.append(prefix);
    			sb.append(code);
    			String format = String.format("\"%%s = %s%%s;\"", unary.group(1));
    			sb.append(format);
    			for (String arg : args) {
    				sb.append(", ");
    				sb.append(arg);
    				sb.append(".getName()");
    			}
    			sb.append(close);
    			sb.append("\n");
    		} else if (oneArg.find()) {
//        		System.out.println("1cOV: " + line );
//    			System.out.print(oneArg.groupCount() + "{");
//    			for (int g = 1; g <= oneArg.groupCount(); g++) System.out.printf("%d:%s,", g, oneArg.group(g));
//    			System.out.println("}");
    			args.add(rename(renames, oneArg.group(3)));
    			
    			String code = "sb.append( String.format(";
    			String close = ") );";
    			sb.append(prefix);
    			sb.append("//" + line + "\n");
    			sb.append(prefix);
    			sb.append(code);
    			String format = String.format("\"%%s = %s.%s(%%s);\"", oneArg.group(1), oneArg.group(2));
    			sb.append(format);
    			for (String arg : args) {
    				sb.append(", ");
    				sb.append(arg);
    				sb.append(".getName()");
    			}
    			sb.append(close);
    			sb.append("\n");
    		} else if (oneParameter.find()) {
//        		System.out.println("1cOV: " + line );
//    			System.out.print(oneArg.groupCount() + "{");
//    			for (int g = 1; g <= oneArg.groupCount(); g++) System.out.printf("%d:%s,", g, oneArg.group(g));
//    			System.out.println("}");
    			String code = "sb.append( String.format(";
    			String close = ") );";
    			sb.append(prefix);
    			sb.append("//" + line + "\n");
    			sb.append(prefix);
    			sb.append(code);
    			String format = String.format("\"%%s = %s.%s(%%s);\"", oneParameter.group(1), oneParameter.group(2));
    			sb.append(format);
    			for (String arg : args) {
    				sb.append(", ");
    				sb.append(arg);
    				sb.append(".getName()");
    			}
    			sb.append(close);
    			sb.append("\n");
    		} else if (twoArgs.find()) {
    			args.add(rename(renames, twoArgs.group(3)));
    			args.add(rename(renames, twoArgs.group(5)));
    			String code = "sb.append( String.format(";
    			String close = ") );";
    			sb.append(prefix);
    			sb.append("//" + line + "\n");
    			sb.append(prefix);
    			sb.append(code);
    			String format = String.format("\"%%s = %s.%s(%%s, %%s);\"", twoArgs.group(1), twoArgs.group(2));
    			sb.append(format);
    			for (String arg : args) {
    				sb.append(", ");
    				sb.append(arg);
    				sb.append(".getName()");
    			}
    			sb.append(close);
    			sb.append("\n");
    		} else {
        		System.out.println("cOV: " + line );
    			System.out.println( "  \"" + line + "\" % " + args.toString() + "");
    			return false;
    		}
    		return success;
    	}
    	
    	protected String rename(Map<String, String> renames, String name) {
    		String r = renames.get(name);
    		if (r != null)
    			return r;
    		return name;
    	}

    	final Pattern startsVariableMatrix = Pattern.compile("^(final\\s*)?VariableMatrix\\s+");
    	final Pattern startsVariableScalar = Pattern.compile("^(final\\s*)?VariableScalar\\s+");
    	
    	// writes code that will write code
    	public StringBuilder toCode(String prefix) {
    		HashMap<String, String> renames = new HashMap<>();
    		renames.put("target", "output");
    		boolean success = true;
    		StringBuilder sb = new StringBuilder();
    		for (String line : body) {
    			if (startsVariableMatrix.matcher(line).find()) {
    			//if (line.startsWith("VariableMatrix ")) {
    				success = success && codeVariableMatrix( sb, prefix, renames, line );
    			} else if (line.startsWith("DMatrixRMaj ")) {
    				success = success && codeDMatrixRMaj( sb, prefix, renames, line );
    			} else if (line.startsWith("VariableInteger ")) {
    				success = success && codeVariableInteger( sb, prefix, renames, line );
    			} else if (startsVariableScalar.matcher(line).find()) {
    				success = success && codeVariableScalar( sb, prefix, renames, line );
    			} else if (line.startsWith("int ")) {
    				success = success && codeIntVariableInteger( sb, prefix, renames, line );
    			} else if (line.startsWith("double ")) {
    				success = success && codeDoubleVariableScalar( sb, prefix, renames, line );
    			} else if (line.startsWith("manager.resize(output")) {
    				success = success && codeManagerResize( sb, prefix, renames, line );
    			} else if (line.startsWith("output.matrix.reshape(")) {
    				success = success && codeManagerReshape( sb, prefix, renames, line );
    			} else if (line.startsWith("out.reshape(")) {
    				success = success && codeManagerReshape( sb, prefix, renames, line );
    			} else if (line.startsWith("CommonOps_DDRM.")) {
    				success = success && codeCommonOps( sb, prefix, renames, line );
    			} else if (line.startsWith("output.value = ")) {
    				success = success && codeOutputValue( sb, prefix, renames, line );
    			} else {
    				sb.append("?");
    				sb.append(line);
    				sb.append("\n") ;   	
    				success = false;
    			}
    			//success = true;  // TODO REMOVE THIS
    		}
    		if (success)
    			return sb;
    		sb.insert(0, prefix + "/*\n");
    		for (String line : body) {
    			sb.append(line);
				sb.append("\n") ;   	
    		}
    		sb.append(prefix + "*/\n");
    		sb.append(prefix);
    		sb.append("//TODO MANUAL\n");
    		return sb;
    	}
    	
    	public String toString() {
    		return String.format("%s-%s: %s", name, inputs, body.toString() );
    	}
    }
    
    private static HashMap<String, HashMap<String, Execution>> byInputs = new HashMap<>();
    
    private static void addExecution( Execution execution ) {
    	if (! byInputs.containsKey(execution.inputs)) {
    		byInputs.put( execution.inputs, new HashMap<String, Execution>() );
    	}
    	HashMap<String, Execution> byName = byInputs.get(execution.inputs);
    	byName.put( execution.name, execution);
    }

    public static void main(String[] args) {
		Path path = Paths.get("src/org/ejml/equation/OperationExecuteFactory.java");
		
		String methodDecl = null;
		try {
			List<String> lines = Files.readAllLines(path);
//			System.out.println(lines.size());
			Iterator<String> it = lines.iterator();
			while (it.hasNext()) { 
				String line = it.next().trim();
				if (line.startsWith("public Operation")) {
					line = it.next().trim();
					while (!line.startsWith("@Override")) {
						line = it.next().trim();						
					}
					continue;
				}
				if (line.startsWith("public Info")) {
//					System.out.println(line);
					methodDecl = line;
				}
				if (line.contains("new Operation")) {
//					System.out.println("  " + line);
					Execution execution = new Execution( methodDecl, line );
					line = it.next().trim();
					while (! line.startsWith("};")) {
						if (line.isEmpty() ||
							line.startsWith("@Override") ||
							line.startsWith("public void") ||
							line.startsWith("try") ||
							line.equals("}") ) {
							line = it.next().trim();
							continue;
						}
						if (line.startsWith("} catch")) {
							while (!line.equals("}")) {
								line = it.next().trim();
							}
							continue;
						}
//						System.out.println("    " + line);
						execution.body.add(line);
						line = it.next().trim();
					}
					addExecution(execution);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		final String XOP_HEADER_FORMAT = "\tprotected String %sOp(String op, CodeOperation codeOp) {\n" + 
				"\t\tVariable output = codeOp.output;\n" + 
				"\t\tVariable A = codeOp.input.get(0);\n" + 
				"\t\tVariable B = codeOp.input.get(1);\n" + 
				"\t\tStringBuilder sb = new StringBuilder();\n";
		final String XOP_SWITCH_FORMAT = "\t\tswitch (op) {\n";
		
		final String XOP_CASE_FORMAT = "\t\tcase \"%s\": // %s\n" +
				"%s" +
				"\t\t\treturn sb.toString();\n";
		
		final String XOP_TRAILER_FORMAT = "\t\t}\n" + 
				"\t\treturn sb.toString();\n" + 
				"\t}\n";
		try {
			PrintStream code = new PrintStream( new File("code.java") );
    		Execution.codeFormats( code, "\t" );
    		code.println();
    		code.println();
	    	for (String input : byInputs.keySet()) {
	    		code.printf( XOP_HEADER_FORMAT, input );
	    		code.print(XOP_SWITCH_FORMAT);
	    		Map<String, Execution> byName = byInputs.get(input);
	    		for (String name : byName.keySet()) {
	    			Execution execution = byName.get(name);
	    			code.printf(XOP_CASE_FORMAT, name, execution.method, execution.toCode("\t\t\t").toString());
	    		}
	    		code.println(XOP_TRAILER_FORMAT);
	    		code.println();
	    	}
	    	code.close();
		} catch (Exception x) {
			x.printStackTrace();
		}
    }
    /*
	add-mm: [
	VariableMatrix mA = (VariableMatrix)A;
		map A -> mA
	VariableMatrix mB = (VariableMatrix)B;
		map B -> mB
	manager.resize(output, mA.matrix.numRows, mA.matrix.numCols);
		output.reshape( mA.runRows, mA.numCols )
			manager.resize(output, -> output.reshape(
			mA.matrix -> map[mA]
			mB.matrix -> map[mB]
	CommonOps_DDRM.add(mA.matrix, mB.matrix, output.matrix);
		add(mA, mB, output);
			CommonOps_DDRM. -> ""
			mA.matrix -> map[mA]
			mB.matrix -> map[mB]
			output.matrix -> output
	]
     */
}
