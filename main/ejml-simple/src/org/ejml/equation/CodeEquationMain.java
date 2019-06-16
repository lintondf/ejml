package org.ejml.equation;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
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
import org.ejml.equation.Info;
import org.ejml.equation.Info.Operation;

import com.google.googlejavaformat.java.Formatter;


public class CodeEquationMain {
		
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
	public void declareIntegerVariables(List<String> declarations) {
		declareVariables( integers, "int", declarations );
	}
	
	public void declareDoubleVariables(List<String> declarations) {
		declareVariables( doubles, "double", declarations );
	}
	
	public void declareMatrixVariables(List<String> declarations) {
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
	final Pattern reshapeReferencePattern = Pattern.compile("(\\w+)\\.num(\\w+)");
	
	private static class ReshapeSources {
		public ReshapeSources( String target) {
			this.target = target;
		}
		public String target;
		public String rowVar;
		public String rowWhich;
		public String colVar;
		public String colWhich;
		
		public String toString() {
			return String.format("%s %s.%s, %s.%s", target, rowVar, rowWhich, colVar, colWhich);
		}
	}
	
	private HashMap<String, ReshapeSources> reshapeSourceMap = new HashMap<>();
	
	private String generateReshape( Matcher reshapeMatcher ) {
		String var = reshapeMatcher.group(1);
		String rows = reshapeMatcher.group(2);
		String cols = reshapeMatcher.group(3);
		
		ReshapeSources reshapeSources = new ReshapeSources(var);
		Matcher sourceMatcher = reshapeReferencePattern.matcher(rows);
		if (sourceMatcher.find()) {
			reshapeSources.rowVar = sourceMatcher.group(1);
			reshapeSources.rowWhich = "num" + sourceMatcher.group(2);
		}
		sourceMatcher = reshapeReferencePattern.matcher(cols);
		if (sourceMatcher.find()) {
			reshapeSources.colVar = sourceMatcher.group(1);
			reshapeSources.colWhich = "num" + sourceMatcher.group(2);
		}
		if (reshapeSources.rowVar != null && reshapeSources.colVar != null) {
			ReshapeSources prior = reshapeSourceMap.get(reshapeSources.rowVar);
			if (prior != null) {
				reshapeSources.rowVar = prior.rowVar;
				reshapeSources.rowWhich = prior.rowWhich;
				rows = String.format("%s.%s", reshapeSources.rowVar, reshapeSources.rowWhich );
			}
			prior = reshapeSourceMap.get(reshapeSources.colVar);
			if (prior != null) {
				reshapeSources.colVar = prior.colVar;
				reshapeSources.colWhich = prior.colWhich;
				cols = String.format("%s.%s", reshapeSources.colVar, reshapeSources.colWhich );
			}
			reshapeSourceMap.put(var, reshapeSources);
		}
		String decl = String.format("DMatrixRMaj %s = new DMatrixRMaj(%s,%s);", var, rows, cols );
		return decl;
	}

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
		EmitJavaCodeOperation coder = new EmitJavaCodeOperation();
		
		for (String equationText : equations) {
			body.append(String.format("// %s\n",  equationText));
			Sequence sequence = eq.compile(equationText);//, true, true);//); //
			CompileCodeOperations generator = new CompileCodeOperations(sequence, eq.getTemporariesManager());
			generator.optimize();
			for (Info info : sequence.getInfos()) {
	    		coder.emitOperation( body, info );
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
			for (Usage usage : generator.matrixUsages) {
				Variable variable = usage.variable;
				if (! declaredTemps.contains(variable.getOperand())) {
					declaredTemps.add(variable.getOperand() );
					//GenerateEquationCoders.declareTemporary( header, "", variable );
					for (int i = 0; i < codeLines.size(); i++) {
						Matcher matcher = reshapePattern.matcher( codeLines.get(i) );
						if (matcher.find()) {
							if (matcher.group(1).equals(variable.getName())) {
								//System.out.printf("%s in %s\n", variable.getName(), matcher.group(0));
								String decl = generateReshape( matcher );
								codeLines.set(i, decl);
								break;
							}
						}
					}
				}
			}
			for (String v : matrices ) {
				if (! parameters.contains(v)) {
					for (int i = 0; i < codeLines.size(); i++) {
						Matcher matcher = reshapePattern.matcher( codeLines.get(i) );
						if (matcher.find()) {
							if (matcher.group(1).equals(v)) {
								//System.out.printf("%s in %s [%s, %s]\n", v, matcher.group(0), matcher.group(2), matcher.group(3));
								String decl = generateReshape( matcher );
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
	
	public static void interactive( BufferedReader in, PrintWriter out ) {
		final String instructions =
				"Generates a java class source file from compiled EJML equations\n" +
				"First, input a class name.  The resulting source will be written to <name>.java.\n" +
				"Next, define one or more methods, each of which can implement one or more EJML equations.\n" +
				"For each method, input declarations of all integer, double, and matrix variables used.\n" +
				"Variable names for each type are entered separately in a comma-separated list.\n" +
				"Press <Enter> alone if no variables of the type are used.\n" +
				"Variables that are input parameters to a method are preceeded by a '>'\n" +
				"A return variable may be indicated by a preceeding '<'.\n" +
				"Input parameters that are returned are preceeded by '<>'.\n" +
				"\n"; 
		out.print(instructions); out.flush();
		
		try {
			out.print("Class name: "); out.flush();
			String className = in.readLine();
			String path = String.format("%s.java", className);
			StringBuilder block = new StringBuilder();
			CodeEquationMain main = new CodeEquationMain( block, className );

			PrintStream code = new PrintStream(path);
			while (true) {
				out.print(" Method name: "); out.flush();
				String methodName = in.readLine();
				if (methodName == null || methodName.isEmpty())
					break;
				main.startMethod(methodName);
				List<String> integers = new ArrayList<>();
				List<String> doubles = new ArrayList<>();
				List<String> matrices = new ArrayList<>();
				out.print("  Integer variable names: "); out.flush();
				String integerNames = in.readLine();
				if (integerNames != null && ! integerNames.isEmpty()) {
					String[] names = integerNames.split(",");
					for (String name : names) {
						integers.add( name.trim() );
					}
				}
				out.print("  Double variable names: "); out.flush();
				String doubleNames = in.readLine();
				if (doubleNames != null && ! doubleNames.isEmpty()) {
					String[] names = doubleNames.split(",");
					for (String name : names) {
						doubles.add( name.trim() );
					}
				}
				out.print("  Matrix variable names: "); out.flush();
				String matrixNames = in.readLine();
				if (matrixNames != null && ! matrixNames.isEmpty()) {
					String[] names = matrixNames.split(",");
					for (String name : names) {
						matrices.add( name.trim() );
					}
				}
				out.print("  Enter the equations to compile for this method.  One per line.  Enter alone when done.\n"); out.flush();
				List<String> equations = new ArrayList<>();
				while (true) {
					out.print("  Equation: "); out.flush();
					String equation = in.readLine();
					if (equation == null || equation.isEmpty())
						break;
					equations.add( equation.trim() );
				}
				
				main.declareIntegerVariables(integers);
				main.declareDoubleVariables(doubles);
				main.declareMatrixVariables(matrices);
				main.finishMethod(equations);
			} // while adding methods
			main.finishClass();
			String pretty = new Formatter().formatSource(block.toString());
			code.println(pretty);
			code.close();
		} catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Console console = System.console();
		interactive( new BufferedReader(console.reader()), console.writer() );
	}

}
