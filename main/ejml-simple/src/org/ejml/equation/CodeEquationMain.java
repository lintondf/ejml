package org.ejml.equation;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ejml.data.DMatrixRMaj;
import org.ejml.equation.CompileCodeOperations.Usage;

import com.google.googlejavaformat.java.Formatter;


public class CodeEquationMain {
	/*
        Equation eq = new Equation();

        eq.alias(1, "A",2, "B");

        eq.process("A=-B");
        assertEquals(-2, eq.lookupInteger("A"));

        eq.process("A=B--B");
        assertEquals(4, eq.lookupInteger("A"));
        eq.process("A=B+-B");
        assertEquals(0,eq.lookupInteger("A"));
        eq.process("A=B---5");
        assertEquals(2 - 5, eq.lookupInteger("A"));
        eq.process("A=B--5");
        assertEquals(2+5,eq.lookupInteger("A"));
 
	 */
	
	
	StringBuilder block;
	String currentMethod = null;
	List<String> integers;
	List<String> doubles;
	List<String> matrices; 
	
	String returnType;
	String returnStatement;
	List<String> parameterList;
	HashSet<String> parameters;
	
	public CodeEquationMain(StringBuilder block, String className ) {
		this.block = block;
		block.append(String.format("public class %s {\n", className) );
	}
	
	public void startMethod( String methodName) {
		this.currentMethod = methodName;
		integers = new ArrayList<>();
		doubles = new ArrayList<>();
		matrices = new ArrayList<>();
		returnType = null;
		returnStatement = null;
		parameterList = new ArrayList<>();
		parameters = new HashSet<>();
	}
	
	/**
	 * 
	 * @param declarations - zero or more comma-separated declarations
	 * 	   <declaration> := <name> | <annotation><name>
	 * 		<annotation> := ">" - input parameter
	 *                      "<" - return variable
	 */
	public void declareIntegerVariable(List<String> declarations) {
		declareVariables( integers, "int", declarations );
	}
	
	public void declareDoubleVariable(List<String> declarations) {
		declareVariables( doubles, "double", declarations );
	}
	
	public void declareMatrixVariable(List<String> declarations) {
		declareVariables( matrices, "DMatrixRMaj", declarations );
	}
	
	private void declareVariables(List<String> variables, String type, List<String> declarations) {
		for (String declaration : declarations) {
			boolean isParameter = false;
			boolean isReturn = false;
			if (declaration.startsWith(">")) {
				declaration = declaration.substring(1);
				isParameter = true;
			}
			if (declaration.startsWith("<")) {
				declaration = declaration.substring(1);
				isReturn = true;
			}
			if (isParameter) {
				parameterList.add( String.format("%s %s", type, declaration) );
				parameters.add(declaration);
			}
			if (isReturn) {
				returnType = type;
				returnStatement = "return " + declaration + ";";
			}
			variables.add(declaration);
		}
	}
	
	final Pattern reshapePattern = Pattern.compile("(\\w+)\\.reshape\\((.+),(.+)\\)");

	public void finishMethod(List<String> equations) {
		if (returnType == null)
			returnType = "void";
		String p = parameterList.toString();
		block.append( String.format("public %s %s(%s) {\n", returnType, currentMethod, p.substring(1, p.length()-1)) );
		
		Equation eq = new Equation();
		OperationCodeFactory factory = new OperationCodeFactory();
		ManagerFunctions mf = new ManagerFunctions(factory);
		eq.setManagerFunctions(mf);
		
		StringBuilder body = new StringBuilder();
		StringBuilder header = new StringBuilder();
		
		for (String v : integers ) {
			eq.alias(new Integer(0), v);
			if (! parameters.contains(v)) {
				header.append( String.format("%-10s %s;\n", "int", v ));
			}
		}
		for (String v : doubles ) {
			eq.alias(new Double(0.0), v);
			if (! parameters.contains(v)) {
				header.append( String.format("%-10s %s;\n", "double", v ));
			}
		}
		for (String v : matrices ) {
			eq.alias(new DMatrixRMaj(1,1), v);
		}

		TreeSet<String> declaredTemps = new TreeSet<>();
		for (String equationText : equations) {
			body.append(String.format("// %s\n",  equationText));
			Sequence sequence = eq.compile(equationText);//, true, true);//); //
			List<Operation> operations = sequence.getOperations();
			CompileCodeOperations generator = new CompileCodeOperations(operations);
			generator.optimize();
			for (Operation operation : operations) {
	    		CodeOperation codeOp = (CodeOperation) operation;
	    		EmitJavaCodeOperation.emitOperation( body, codeOp );
	    	}	
			for (Usage usage : generator.integerUsages) {
				Variable variable = usage.variable;
				if (! declaredTemps.contains(variable.getOperand())) {
					declaredTemps.add(variable.getOperand() );
					GenerateEquationCoders.declareTemporary( header, "", variable );
				}
			}
			for (Usage usage : generator.doubleUsages) {
				Variable variable = usage.variable;
				if (! declaredTemps.contains(variable.getOperand())) {
					declaredTemps.add(variable.getOperand() );
					GenerateEquationCoders.declareTemporary( header, "", variable );
				}
			}

			// replace first reshape with declaration of matrix variables and temporaries
			List<String> codeLines = Arrays.asList(body.toString().split("\n"));
			for (String v : matrices ) {
				if (! parameters.contains(v)) {
					for (int i = 0; i < codeLines.size(); i++) {
						Matcher matcher = reshapePattern.matcher( codeLines.get(i) );
						if (matcher.find()) {
							if (matcher.group(1).equals(v)) {
								System.out.printf("%s in %s\n", v, matcher.group(0));
								String decl = String.format("DMatrixRMaj %s = new DMatrixRMaj(%s,%s);", v, matcher.group(2), matcher.group(3) );
								codeLines.set(i, decl);
								break;
							}
						}
					}
				}
			}
			
			for (Usage usage : generator.matrixUsages) {
				Variable variable = usage.variable;
				if (! declaredTemps.contains(variable.getOperand())) {
					declaredTemps.add(variable.getOperand() );
					//GenerateEquationCoders.declareTemporary( header, "", variable );
					for (int i = 0; i < codeLines.size(); i++) {
						Matcher matcher = reshapePattern.matcher( codeLines.get(i) );
						if (matcher.find()) {
							if (matcher.group(1).equals(variable.getName())) {
								System.out.printf("%s in %s\n", variable.getName(), matcher.group(0));
								String decl = String.format("DMatrixRMaj %s = new DMatrixRMaj(%s,%s);", variable.getName(), matcher.group(2), matcher.group(3) );
								codeLines.set(i, decl);
								break;
							}
						}
					}
				}
			}
			body = new StringBuilder();
			body.append((String.join("\n", codeLines)));
			generator.releaseTemporaries(eq);
		}
    	block.append(header);
    	block.append(body);
    	if (returnStatement != null) {
    		block.append( returnStatement );
    	}
		block.append("}\n");		
	}
	
	public void finishClass() {
		block.append("}\n");
	}
	
	public static void main(String[] args) {
		List<String> integers = new ArrayList<>();
		List<String> doubles = new ArrayList<>();
		List<String> matrices = new ArrayList<>();
		
		matrices.add("<K");
		matrices.add(">P");
		matrices.add(">H");
		matrices.add(">R");
		
		List<String> equations = new ArrayList<>();
		equations.add("K = P*H'*inv( H*P*H' + R )");
		
		StringBuilder block = new StringBuilder();
		CodeEquationMain main = new CodeEquationMain( block, "code" );
		main.startMethod("test");
		main.declareIntegerVariable(integers);
		main.declareDoubleVariable(doubles);
		main.declareMatrixVariable(matrices);
		main.finishMethod(equations);
		main.finishClass();
		try {
			String pretty = new Formatter().formatSource(block.toString());
			PrintStream code = System.out; // new PrintWriter("code.java");
			code.println(pretty);
			code.close();
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

}
