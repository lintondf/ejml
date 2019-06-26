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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ejml.equation.CompileCodeOperations.Usage;
import org.ejml.simple.SimpleMatrix;

/**
 * Generate TestCompiled.java from TestEquation and TestOperation
 * D. F. Linton, Blue Lightning Development, LLC. June 2019.
 * All rights contributed to EJML project.
 */

public class GenerateTestCoded {
	
	IEmitOperation coder = new EmitJavaOperation();
	
	private static class GeneratorCompileCodeOperations extends CompileCodeOperations {
	    
		public GeneratorCompileCodeOperations(IEmitOperation coder, Sequence sequence) {
			super(coder, sequence, null);
		}
		
	    String getJavaType( Variable variable ) {
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
//		    			if (assignmentTarget.getType() != VariableType.MATRIX) {
//		    				System.out.println( assignmentTarget.type.toString() + " " + ret);
//		    				ret += " // " + assignmentTarget.getClass()
//		    			}
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
	    
		final String declFormatWithValue = "%s%s%-10s\t%s = %s";
	    final String declFormatInitialize = "%s%s%-10s\t%s = new %s(%s)";
	    final String declFormatScalar = "%s%s%-10s %s = 0";
	    final String declFormat = "%s%s%-10s\t%s";
	    final String returnFormat = "%s%sreturn %s;\n";
	    
	    final Pattern reshapePattern = Pattern.compile("(\\w+)\\.reshape\\(\\s*(\\w+)\\.numRows\\,\\s*(\\w+)\\.numCols");
	    
	    protected boolean isConstructed( Variable v ) {
	    	if (v instanceof VariableMatrix) {
	    		for (Info info : infos) {
	    			if (info.constructor != null) {
	    				if (info.constructor.output != null && info.constructor.output.getName().equals(v.getName()))
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
	    			if (! variable.getName().endsWith("}")) {
		    			if (notFirst)
		    				header.append(", ");
		    			String type = getJavaType(variable);
		   				header.append(String.format(declFormat, "", "", type, variable.getName()) );
		    			notFirst = true;
	    			}
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
	    		if (!usage.variable.getName().endsWith("}")) {
		    		body.append(String.format(declFormatScalar, prefix, prefix, getJavaType(usage.variable), usage.variable.getOperand()) );
					body.append(";\n");
	    		}
	    	}
	    	for (Usage usage : doubleUsages) {
	    		if (!usage.variable.getName().endsWith("}")) {
	    			body.append(String.format(declFormatScalar, prefix, prefix, getJavaType(usage.variable), usage.variable.getOperand()) );
	    			body.append(";\n");
	    		}
	    	}
	    	for (Usage usage : matrixUsages) {
	    		if (!usage.variable.getName().endsWith("}")) {
					String type = getJavaType(usage.variable);
		    		body.append(String.format(declFormatInitialize, prefix, prefix, type, usage.variable.getName(), type, "1,1") );
					body.append(";\n");
	    		}
	    	}
	    	body.append("\n");
	    	HashMap<String, String> reshapes = new HashMap<>();
	    	for (Info info : infos) {
	    		StringBuilder block = new StringBuilder();
	    		coder.emitOperation( block, info );
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
	}


	private String unquote(String f) {
		f = f.trim();
		if (f.charAt(0) == '\"') {
			f = f.substring(1, f.length() - 1);
		}
		return f.trim();
	}

	private void writeCodedEquationMethod(ArrayList<String> body, String prefix, String testName,
			HashMap<String, String> lookups, GeneratorCompileCodeOperations generator, Equation eq, Sequence sequence,
			String equationText) {

		body.add("");
		String name = String.format("%s_Coded", testName);
		StringBuilder sb = new StringBuilder();
		generator.emitJavaTest(sb, prefix, name, eq, equationText);
		body.addAll(Arrays.asList(sb.toString().split("\n")));
		// body.add("}");
	}

	private void handleAliases(Equation eq, HashMap<String, String> names, HashMap<String, String> constants,
			ArrayList<String> integers, ArrayList<String> doubles, ArrayList<String> matrices,
			ArrayList<String> aliases) {
		for (String a : aliases) {
			String[] f = a.trim().split(",");
			for (int i = 0; i < f.length; i += 2) {
				String var = f[i].trim();
				String name = unquote(f[i + 1]);
				char c = var.charAt(0);
				if (c == '-' || c == '+' || Character.isDigit(c)) {
					if (var.contains(".")) {
						doubles.add(name);
					} else {
						integers.add(name);
					}
					names.put(name, name);
					constants.put(name, var);
				} else {
					names.put(var, name);
				}
			}
		}

		for (String n : integers) {
			if (names.containsKey(n)) {
				Integer x = new Integer(0);
				eq.alias(x, names.get(n));
			}
		}
		for (String n : doubles) {
			if (names.containsKey(n)) {
				Double x = new Double(0.0);
				eq.alias(x, names.get(n));
			}
		}
		for (String n : matrices) {
			if (names.containsKey(n)) {
				SimpleMatrix x = new SimpleMatrix(1, 1);
				eq.alias(x, names.get(n));
			}
		}
	}
	
//	public void declareTemporary( StringBuilder header, String indent, Variable variable ) {
//		if (variable.isConstant())
//			return;
//		switch (variable.getType()) {
//		case SCALAR:
//			VariableScalar scalar = (VariableScalar) variable;
//			if (scalar.getScalarType() == VariableScalar.Type.INTEGER) {
//				header.append( String.format("%s%-10s %s;\n", indent, "int", variable.getOperand() ));
//			} else {
//				header.append( String.format("%s%-10s %s;\n", indent, "double", variable.getOperand() ));    					
//			}
//			break;
//		case MATRIX:
//			header.append( String.format("%s%-10s %s = new DMatrixRMaj(1,1);\n", indent, "DMatrixRMaj", variable.getName() ));
//			break;
//		default:
//			System.out.println("Unhandled variable type encountered: " + variable);
//			break;
//		}
//	}
//
	protected void emitIndentedOperation(StringBuilder test, String indent, Info info) {
		StringBuilder sb = new StringBuilder();
		
		coder.emitOperation( sb, info );
		for (String line : sb.toString().split("\n")) {
			test.append(indent);
			test.append(line);
			test.append('\n');
		}
	}

	
	private boolean copyTest(PrintStream code, Iterator<String> it, String testName, String line) {
		ArrayList<String> body = new ArrayList<>();
		final Pattern compilePattern = Pattern.compile("(\\w+)\\.compile\\((\\\"[^\\\"]*\\\")\\)");
		final Pattern processPattern = Pattern.compile("(\\w+)\\.process\\((\\\"[^\\\"]*\\\")\\)");
		final Pattern assignmentPattern = Pattern.compile("(\\w+).*=");

		final Pattern declInt = Pattern.compile("int\\s+(\\w+)");
		final Pattern declDouble = Pattern.compile("double\\s+(\\w+)");
		final Pattern declSimpleMatrix = Pattern.compile("SimpleMatrix\\s+(\\w+)");
		final Pattern declDMatrixRMaj = Pattern.compile("DMatrixRMaj\\s+(\\w+)");
		final Pattern aliasPattern = Pattern.compile("\\.alias\\(([^}]*)\\)");
		final Pattern lookupPattern = Pattern.compile("eq\\.lookup(\\w+)\\(([^\\)]*)\\)");
		final Pattern lookupAssignPattern = Pattern.compile("(\\w+)\\s+(\\w+)\\s*=\\s*eq\\.lookup(\\w+)\\(([^\\)]*)\\)");
		
		//assertEquals(-2, eq.lookupInteger("A"));
		final Pattern checkPattern = Pattern.compile("\\s*assertEquals\\((.*),\\s*eq\\.lookup(\\w+)\\(\\\"(.*)\\\"\\)\\)");
		
		final String prefix = "    ";

		ArrayList<String> integers = new ArrayList<>();
		ArrayList<String> doubles = new ArrayList<>();
		ArrayList<String> matrices = new ArrayList<>();
		ArrayList<String> aliases = new ArrayList<>();
		
		HashMap<String, String> lookups = new HashMap<>();

		body.add(prefix+"@Test");
		body.add(line);
		int nCompile = 0;
		int nProcess = 0;
		int nAssign = 0;
		final String endParen = "    }";
		String equationVariable = null;
		String equationText = null;
		while (it.hasNext()) {
			line = it.next();
			if (line.equals(endParen)) {
				break;
			}
			body.add(line);
			Matcher matcher = compilePattern.matcher(line);
			if (matcher.find()) {
//				System.out.println(line);
				nCompile++;
				equationVariable = matcher.group(1);
				equationText = unquote(matcher.group(2));
			}
			matcher = processPattern.matcher(line);
			if (matcher.find()) {
//				System.out.println(line);
				nProcess++;
				equationVariable = matcher.group(1);
				equationText = unquote(matcher.group(2));
			}
			matcher = declInt.matcher(line);
			if (matcher.find()) {
				integers.add(matcher.group(1));
			}
			matcher = declDouble.matcher(line);
			if (matcher.find()) {
				doubles.add(matcher.group(1));
			}
			matcher = declSimpleMatrix.matcher(line);
			if (matcher.find()) {
				matrices.add(matcher.group(1));
			}
			matcher = declDMatrixRMaj.matcher(line);
			if (matcher.find()) {
				matrices.add(matcher.group(1));
			}
			matcher = aliasPattern.matcher(line);
			if (matcher.find()) {
				aliases.add(matcher.group(1));
			}
			
			matcher = lookupPattern.matcher(line);
			if (matcher.find()) {
				String alias = matcher.group(2);
				String type = matcher.group(1);
//				System.out.println(line);
				matcher = lookupAssignPattern.matcher(line);
				if (matcher.find()) {
//					for (int g = 1; g <= matcher.groupCount(); g++) System.out.printf("[%s]", matcher.group(g));
//					System.out.println();
					lookups.put(unquote(matcher.group(4)), matcher.group(2));
				} else {
					lookups.put(unquote(alias), String.format("eq.lookup%s(%s)", type, alias) );
				}
			}
		}
		Equation eq = new Equation();
		OperationCodeFactory factory = new OperationCodeFactory();
		ManagerFunctions mf = new ManagerFunctions(factory);
		eq.setManagerFunctions(mf);

		HashMap<String, String> names = new HashMap<>();
		HashMap<String, String> constants = new HashMap<>();
		
		handleAliases( eq, names, constants, integers, doubles, matrices, aliases );
		
		if (nCompile == 1 || nProcess == 1) {
			equationText = equationText.replace("\\\\", "\\"); // for reasons unknown backslashes are double escaped here
			Matcher matcher = assignmentPattern.matcher(equationText);
			if (matcher.find()) {
				nAssign++;

				try {
					Sequence sequence = eq.compile(equationText); //, true, true);
					GeneratorCompileCodeOperations generator = new GeneratorCompileCodeOperations(coder, sequence);
					generator.optimize();
					String target = matcher.group(1);
					body.add(String.format("%s%s// %s: %s -> %s", prefix, prefix, equationVariable, equationText, target));
					String ret = generator.getReturnVariable(eq);
					String callCompiled = String.format("%s%s%s%s_Coded(%s);", prefix, prefix, ret, testName,
							generator.getCallingSequence(constants, eq));
					body.add(callCompiled);
					body.add(prefix+prefix + generator.getAssert(constants, lookups, eq));
					body.add(endParen);
					writeCodedEquationMethod(body, prefix, testName, lookups, generator, eq, sequence, equationText);
					generator.releaseTemporaries(eq);
					body.forEach(code::println);
					code.println();
					code.println();
					return true;
				} catch (Exception x) {
					System.out.printf("In %s: %s\n", testName, x.getMessage() );
//					throw x;
					return false;
				}
			} else {
				System.out.printf("No assignment in %s\n", testName);
			}
		}
		if (nCompile == 0 && nProcess == 0) {
			System.out.printf("In %s: %d, %d, %d; no compile() or process()\n", testName, nCompile, nProcess, nAssign );			
		} else if (nCompile > 1 && nProcess == 0) {
			System.out.printf("In %s: %d, %d, %d; more than one compile()\n", testName, nCompile, nProcess, nAssign );
		} else if (nCompile == 0 && nProcess > 1) {
			body.add(endParen);
			body.forEach(code::println);
			code.println();
			code.println();
			
			StringBuilder test = new StringBuilder();
			StringBuilder header = new StringBuilder();
			
			String indent = prefix + prefix;
			for (String v : integers ) {
				if (constants.containsKey(v)) {
					header.append( String.format("%s%-10s %s = %s;\n", indent, "int", v, constants.get(v) ));					
				} else {
					header.append( String.format("%s%-10s %s;\n", indent, "int", v ));
				}
			}
			for (String v : doubles ) {
				if (constants.containsKey(v)) {
					header.append( String.format("%s%-10s %s = %s;\n", indent, "double", v, constants.get(v) ));					
				} else {
					header.append( String.format("%s%-10s %s;\n", indent, "double", v ));
				}
			}
			for (String v : matrices ) {
				header.append( String.format("%s%-10s %s = new DMatrixRMaj(1,1);\n", indent, "DMatrixRMaj", v ));
			}
			
			TreeSet<String> declaredTemps = new TreeSet<>();

			Iterator<String> bit = body.iterator();
			boolean ok = true;
			while (bit.hasNext()) {
				line = bit.next();
				Matcher matcher = processPattern.matcher(line);
				if (matcher.find()) {
					equationText = unquote(matcher.group(2));
					test.append( String.format("%s//%s\n", indent, equationText));
					Sequence sequence = eq.compile(equationText); //
					GeneratorCompileCodeOperations generator = new GeneratorCompileCodeOperations(coder, sequence);
					generator.optimize();
					for (Usage usage : generator.integerUsages) {
						Variable variable = usage.variable;
						if (! declaredTemps.contains(variable.getOperand())) {
							declaredTemps.add(variable.getOperand() );
							if (! variable.getName().endsWith("}"))
								coder.declare( header, indent, variable );
						}
					}
					for (Usage usage : generator.doubleUsages) {
						Variable variable = usage.variable;
						if (! declaredTemps.contains(variable.getOperand())) {
							declaredTemps.add(variable.getOperand() );
							if (! variable.getName().endsWith("}"))
								coder.declare( header, indent, variable );
						}
					}
					for (Usage usage : generator.matrixUsages) {
						Variable variable = usage.variable;
						if (! declaredTemps.contains(variable.getOperand())) {
							declaredTemps.add(variable.getOperand() );
							if (! variable.getName().endsWith("}"))
								coder.declare( header, indent, variable );
						}
					}
					for (Info info : sequence.getInfos()) {
			    		emitIndentedOperation( test, indent, info );
			    	}	
					generator.releaseTemporaries(eq);
					String check = bit.next().trim();
					while (bit.hasNext() && check.isEmpty())
						check = bit.next().trim();
					matcher = checkPattern.matcher(check);
					if (matcher.find()) {
						test.append( String.format("%sassertTrue(isIdentical(%s, %s));\n", indent, matcher.group(1), matcher.group(3) ) );
					} else {
						ok = false;
						break;
					}
				}
			}
			if (ok) {
				code.print( String.format("%s@Test\n%spublic void %s_Coded() {\n", prefix, prefix, testName) );
				code.print(header.toString());
				code.print(test.toString());
				code.print( String.format("%s}\n", prefix) );
				code.println();
				code.println();
			} else {
				System.out.printf("In %s: %d, %d, %d; more than one process but not alternating asserts\n", testName, nCompile, nProcess, nAssign );
			}
			return true;
		} else {
			System.out.printf("In %s: %d, %d, %d; strange brew\n", testName, nCompile, nProcess, nAssign );
		}
		return false;
	}

	public GenerateTestCoded() {
		HashSet<String> skips = new HashSet<>();
		skips.add("print");
		skips.add("compile_parentheses_extract_IndexMath");
		skips.add("compile_constructMatrix_commas");
		skips.add("print");
		skips.add("compile_output");
		// not compiling random number support
		skips.add("rand");
		skips.add("randn");
		skips.add("rng");
		// exception throw tests
		skips.add("assign_lazy_right");
		skips.add("multiply_matrix1x1_matrixNxM");
		skips.add("add_matrix1x1_matrixNxM");
		skips.add("subtract_matrix1x1_matrixNxM");
		skips.add("copy_double_matrix");
		// Construction from submatrices can not be precompiled
		skips.add("compile_constructMatrix_MatrixAndScalar");
		skips.add("compile_constructMatrix_Operations");
		skips.add("compile_constructMatrix_Inner");
		skips.add("compile_constructMatrix_ForSequence_Case0");
		skips.add("compile_constructMatrix_ParenSubMatrixAndComma");
		// pure parsing tests
		skips.add("handleParentheses");
		skips.add("parseOperations");
		skips.add("macro");
		skips.add("extractTokens");
		skips.add("extractTokens_elementWise");
		skips.add("extractTokens_integers");
		skips.add("extractTokens_doubles");
		skips.add("extractTokens_minus");
		skips.add("insertFunctionsAndVariables");
		skips.add("isTargetOp");
		skips.add("isLetter");
		skips.add("gracefullyHandleBadCode");
		skips.add("compile_parentheses");
		skips.add("compile_parentheses_extract");
		skips.add("compile_parentheses_extractSpecial");
		skips.add("lookupVariable");
		skips.add("createOp");
		skips.add("compile_function_one");
		skips.add("compile_function_N");
		// integer sequence variables not supported
		skips.add("compile_assign_IntSequence_Case0");
		skips.add("compile_assign_IntSequence_Case1");
		skips.add("compile_assign_IntSequence_Case2");
		skips.add("compile_assign_IntSequence_Case3");
		skips.add("compile_assign_IntSequence_Case4");
		skips.add("compile_assign_IntSequence_Case5");
		skips.add("compile_assign_IntSequence_Case6");
		skips.add("compile_assign_IntSequence_Case7");
		
		final Pattern method = Pattern.compile("\\s*public void (\\w+)\\(\\)");
		Path path = Paths.get("test/org/ejml/equation");
		String[] tests = { "TestEquation.java", "TestOperation.java" };
		try {
			Path in = path.resolve("TestCoded.java");
			Path out = Paths.get("TestCoded_output.java");
			List<String> template = Files.readAllLines(in);
			PrintStream code = new PrintStream(out.toFile()); // System.out;
			Iterator<String> it = template.iterator();
			while (it.hasNext()) {
				String line = it.next();
				if (line.trim().equals("@Test"))
					break;
				code.println(line);
			}
			for (String test : tests) {
				List<String> lines = Files.readAllLines(path.resolve(test));
				System.out.println(test + " " + lines.size());
				it = lines.iterator();
				int nTests = 0;
				int nCoded = 0;
				int nSkipped = 0;
				while (it.hasNext()) {
					String line = it.next();
					Matcher matcher = method.matcher(line);
					if (matcher.find()) {
						nTests++;
						if (skips.contains(matcher.group(1))) {
							nSkipped++;
							continue;
						}
//						if (!matcher.group(1).equals("compile_neg"))
//							continue;
						if (copyTest(code, it, matcher.group(1), line)) {
							nCoded++;
						}
					}
				}
				System.out.printf("%d tests; %d skipped; %d coded\n", nTests, nSkipped, nCoded);
			}
			code.println("}");
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new GenerateTestCoded();
	}
	
}
//