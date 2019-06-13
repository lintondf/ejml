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

public class GenerateEquationCoders {
	
	private static class GeneratorCompileCodeOperations extends CompileCodeOperations {
	    
		public GeneratorCompileCodeOperations(Sequence sequence) {
			super(sequence, null);
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

//	private static class Execution {
//		public String method;
//		public String name;
//		public String inputs;
//		public ArrayList<String> body;
//
//		public Execution(String methodDecl, String operationDecl) {
//			method = methodDecl.replace("public ", "").replace(" {", "");
//			int i = operationDecl.indexOf('"');
//			int j = operationDecl.lastIndexOf('"');
//			String[] nameParts = operationDecl.substring(i + 1, j).split("-");
//			name = nameParts[0];
//			if (nameParts.length != 2)
//				inputs = "";
//			else
//				inputs = nameParts[1];
//			body = new ArrayList<>();
//		}
//
//		protected boolean codeVariableMatrix(StringBuilder sb, String prefix, Map<String, String> renames,
//				String line) {
//			final Pattern pattern = Pattern
//					.compile("VariableMatrix\\s*(\\w+)\\s*=\\s*\\(VariableMatrix\\)\\s*(\\w+)\\;");
//			return codeRenamePattern(sb, prefix, renames, pattern, line);
//		}
//
//		protected boolean codeVariableInteger(StringBuilder sb, String prefix, Map<String, String> renames,
//				String line) {
//			final Pattern pattern = Pattern
//					.compile("VariableInteger\\s*(\\w+)\\s*=\\s*\\(VariableInteger\\)\\s*(\\w+)\\;");
//			return codeRenamePattern(sb, prefix, renames, pattern, line);
//		}
//
//		protected boolean codeIntVariableInteger(StringBuilder sb, String prefix, Map<String, String> renames,
//				String line) {
//			final Pattern pattern1 = Pattern.compile("int\\s*(\\w+)\\s*=\\s*\\(VariableInteger\\)\\s*(\\w+)\\;");
//			final Pattern pattern2 = Pattern
//					.compile("int\\s*(\\w+)\\s*=\\s*\\(\\(VariableInteger\\)\\s*(\\w+)\\)\\.value\\;");
//			Matcher matcher = pattern1.matcher(line);
//			if (matcher.find()) {
//				renames.put(matcher.group(1), matcher.group(2));
//				return true;
//			}
//			matcher = pattern2.matcher(line);
//			if (matcher.find()) {
//				renames.put(matcher.group(1), matcher.group(2));
//				return true;
//			}
//			sb.append("codeIntVariableInteger: ");
//			sb.append(line);
//			sb.append("\n");
//			return false;
//		}
//
//		protected boolean codeVariableScalar(StringBuilder sb, String prefix, Map<String, String> renames,
//				String line) {
//			final Pattern pattern = Pattern
//					.compile("VariableScalar\\s*(\\w+)\\s*=\\s*\\(VariableScalar\\)\\s*(\\w+)\\;");
//			return codeRenamePattern(sb, prefix, renames, pattern, line);
//		}
//
//		protected boolean codeDoubleVariableScalar(StringBuilder sb, String prefix, Map<String, String> renames,
//				String line) {
//			final Pattern pattern = Pattern
//					.compile("double\\s*(\\w+)\\s*=\\s*\\(\\(VariableScalar\\)\\s*(\\w+)\\)\\.getDouble\\(\\)\\;");
//			return codeRenamePattern(sb, prefix, renames, pattern, line);
//		}
//
//		protected boolean codeDMatrixRMaj(StringBuilder sb, String prefix, Map<String, String> renames, String line) {
//			final Pattern pattern = Pattern
//					.compile("DMatrixRMaj\\s*(\\w+)\\s*=\\s*\\(\\(VariableMatrix\\)\\s*(.+)\\).matrix\\;");
//			return codeRenamePattern(sb, prefix, renames, pattern, line);
//		}
//
//		protected boolean codeRenamePattern(StringBuilder sb, String prefix, Map<String, String> renames,
//				Pattern pattern, String line) {
//			Matcher matcher = pattern.matcher(line);
//			if (matcher.find()) {
//				renames.put(matcher.group(1), matcher.group(2));
//				return true;
//			}
//			sb.append("codeRenamePattern: ");
//			sb.append(pattern.pattern());
//			sb.append(" -> ");
//			sb.append(line);
//			sb.append("\n");
//			return false;
//		}
//
//		protected static void codeFormats(PrintStream code, String prefix) {
//			code.print(prefix);
//			code.print("final String formatReshape = \"%s.reshape( %s.numRows, %s.numCols );\";");
//			code.print("\n");
//			code.print(prefix);
//			code.print("final String formatCommonOps3 = \"CommonOps_DDRM.%s( %s, %s, %s );\";");
//			code.print("\n");
//			code.print(prefix);
//			code.print("final String formatCommonOps2 = \"CommonOps_DDRM.%s( %s, %s );\";");
//			code.print("\n");
//			code.print(prefix);
//			code.print("final String formatCommonOps1 = \"CommonOps_DDRM.%s( %s );\";");
//			code.print("\n");
//		}
//
//		protected boolean codeManagerResize(StringBuilder sb, String prefix, Map<String, String> renames, String line) {
//			final Pattern pattern = Pattern.compile(
//					"manager\\.resize\\(\\s*output,\\s*(\\w+)(\\.matrix)?\\.(\\w+),\\s*(\\w+)(\\.matrix)?\\.(\\w+)\\)\\;");
//			Matcher matcher = pattern.matcher(line);
//			if (matcher.find()) {
//				// groups: 1 - rows-source, 2 - null|.matrix, 3-numrows, 4-cols-source,
//				// 5-null|.matrix, 6-numcols
//				/*
//				 * from OperationExecuteFactory: manager.resize(output, mA.matrix.numRows,
//				 * mA.matrix.numCols); becomes in user code: <target>.reshape(
//				 * <rowSource>.runRows, <colSource>.numCols ) manager.resize(output, ->
//				 * map[output].reshape( mA.matrix -> map[mA] mB.matrix -> map[mB]
//				 */
//				sb.append(prefix);
//				String code = "sb.append( String.format(formatReshape, output.getName(), %s.getName(), %s.getName()) );";
//				sb.append(String.format(code, rename(renames, matcher.group(1)), rename(renames, matcher.group(4))));
//				sb.append("\n");
//				return true;
//			}
//			sb.append("codeManagerResize: ");
//			sb.append(pattern.pattern());
//			sb.append("\n");
//			return false;
//		}
//
//		protected boolean codeManagerReshape(StringBuilder sb, String prefix, Map<String, String> renames,
//				String line) {
//			// (\w+)(\.matrix)?\.reshape\(\s*([^,]*),\s*(.*)\);
//			final Pattern pattern = Pattern.compile("(\\w+)(\\.matrix)?\\.reshape\\(\\s*([^,]*),\\s*(.*)\\);");
//			Matcher matcher = pattern.matcher(line);
//			if (matcher.find()) {
//				// groups: 1-variable, 2 - null|.matrix, 3-row expression, 4-col expression
//				sb.append(prefix);
//				String code = "sb.append( String.format(formatReshape, %s.getName(), %s.getName(), %s.getName()) );";
//				final Pattern variablePattern = Pattern.compile("(\\w+)(\\.matrix)?(\\.numRows)?(\\.numCols)?");
//				final Pattern patternBinary = Pattern.compile("%s\\s*=\\s*%s\\s*([\\+\\-\\*\\/])\\s*%s;");
//				String a = matcher.group(1);
//				String b = matcher.group(3);
//				String c = matcher.group(4);
//				matcher = variablePattern.matcher((a));
//				if (!matcher.matches()) {
//					return false;
//				} else {
//					a = rename(renames, matcher.group(1));
//				}
//				matcher = variablePattern.matcher((b));
//				if (!matcher.matches()) {
//					return false;
//				} else {
//					b = rename(renames, matcher.group(1));
//				}
//				matcher = variablePattern.matcher((c));
//				if (!matcher.matches()) {
//					return false;
//				} else {
//					c = rename(renames, matcher.group(1));
//				}
//				sb.append(String.format(code, a, b, c));
//				sb.append("\n");
//				return true;
//			}
//			sb.append("codeManagerReshape: ");
//			sb.append(pattern.pattern());
//			sb.append("\n");
//			return false;
//		}
//
//		protected boolean codeCommonOps(StringBuilder sb, String prefix, Map<String, String> renames, String line) {
//			// CommonOps_DDRM\.(\w+)\(\s*(-?\w+)(\.[^,]+)?,\s*(-?\w+)(\.[^,]+)?,\s*(\w+)(\.matrix)?\s*\);
//			final Pattern pattern3Args = Pattern.compile(
//					"CommonOps_DDRM\\.(\\w+)\\(\\s*(-?\\w+)(\\.[^,]+)?,\\s*(-?\\w+)(\\.[^,]+)?,\\s*(\\w+)(\\.matrix)?\\s*\\);");
//			Matcher matcher = pattern3Args.matcher(line);
//			if (matcher.find()) {
//				sb.append(prefix);
//				sb.append("//" + line + "\n");
//				sb.append(prefix);
//				if (matcher.group(3) == null && matcher.group(5) == null) {
//					String code = "sb.append( String.format(formatCommonOps3, \"%s\", %s.getName(), %s.getName(), %s.getName()) );";
//					sb.append(String.format(code, matcher.group(1), rename(renames, matcher.group(2)),
//							rename(renames, matcher.group(4)), rename(renames, matcher.group(6))));
//				} else if (matcher.group(3).equals(".matrix") && matcher.group(5).equals(".matrix")) {
//					String code = "sb.append( String.format(formatCommonOps3, \"%s\", %s.getName(), %s.getName(), %s.getName()) );";
//					sb.append(String.format(code, matcher.group(1), rename(renames, matcher.group(2)),
//							rename(renames, matcher.group(4)), rename(renames, matcher.group(6))));
//				} else {
//					String code = "sb.append( String.format(formatCommonOps3, \"%s\", %s, %s, %s) );";
//					String a = rename(renames, matcher.group(2)) + ".getName()";
//					if (!matcher.group(3).equals(".matrix")) {
//						a += matcher.group(3);
//					}
//					String b = rename(renames, matcher.group(4)) + ".getName()";
//					if (!matcher.group(5).equals(".matrix")) {
//						b += matcher.group(5);
//					}
//					String o = rename(renames, matcher.group(6)) + ".getName()";
//					sb.append(String.format(code, matcher.group(1), a, b, o));
//				}
//				sb.append("\n");
//				return true;
//			}
//			final Pattern pattern2Args = Pattern
//					.compile("CommonOps_DDRM\\.(\\w+)\\(\\s*(-?\\w+)(\\.[^,]+)?,\\s*(-?\\w+)(\\.matrix)?\\s*\\);");
//			matcher = pattern2Args.matcher(line);
//			if (matcher.find()) {
//				String code = "sb.append( String.format(formatCommonOps2, \"%s\", %s, %s) );";
//				sb.append(prefix);
//				sb.append("//" + line + "\n");
//				sb.append(prefix);
//				String a = rename(renames, matcher.group(4));
//				if (matcher.group(5) == null) {
//				} else if (!matcher.group(5).equals(".matrix")) {
//					a += matcher.group(5);
//				} else {
//					a += ".getName()";
//				}
//				String o = rename(renames, matcher.group(2)) + ".getName()";
//				sb.append(String.format(code, matcher.group(1), o, a));
//				sb.append("\n");
//				return true;
//			}
//			final Pattern pattern1Arg = Pattern.compile("CommonOps_DDRM\\.(\\w+)\\(\\s*(-?\\w+)(\\.matrix)?\\);");
//			matcher = pattern1Arg.matcher(line);
//			if (matcher.find()) {
////    			System.out.println("cCO1: " + line);
////    			System.out.print("{");
////    			for (int g = 1; g <= matcher.groupCount(); g++) System.out.print(g+":"+matcher.group(g)+",");
////    			System.out.println("}");
//				String code = "sb.append( String.format(formatCommonOps1, \"%s\", %s) );";
//				sb.append(prefix);
//				sb.append("//" + line + "\n");
//				sb.append(prefix);
//				String o = rename(renames, matcher.group(2)) + ".getName()";
//				sb.append(String.format(code, matcher.group(1), o));
//				sb.append("\n");
//				return true;
//			}
//			sb.append("codeCommonOps: ");
//			sb.append(line);
//			sb.append("\n");
//			return false;
//		}
//
//		protected boolean codeOutputValue(StringBuilder sb, String prefix, Map<String, String> renames, String line) {
//			ArrayList<String> args = new ArrayList<>();
//			// (\w+)\.value
//			final Pattern patternNoCast = Pattern.compile("(\\w+)\\.value");
//			// \(\((\w+)\)\s*(\w+)\)\.value
//			final Pattern patternCastValue = Pattern.compile("\\(\\((\\w+)\\)\\s*(\\w+)\\)\\.value");
//			final Pattern patternCastMatrix = Pattern.compile("\\(\\((\\w+)\\)\\s*(\\w+)\\)\\.matrix");
//			// (\w+)\.getDouble\(\)
//			final Pattern patternGetDouble = Pattern.compile("(\\w+)\\.getDouble\\(\\)");
//			// \(\((\w+)\)\s*(\w+)\)\.getDouble\(\)
//			final Pattern patternCastGetDouble = Pattern.compile("\\(\\((\\w+)\\)\\s*(\\w+)\\)\\.getDouble\\(\\)");
//
//			final Pattern patternBinary = Pattern.compile("%s\\s*=\\s*%s\\s*([\\+\\-\\*\\/])\\s*%s;");
//			final Pattern patternUnary = Pattern.compile("%s\\s*=\\s*([\\+\\-]*)\\s*%s;");
//
//			final Pattern pattern1Arg = Pattern.compile("(\\w+)\\.(\\w+)\\(\\s*(-?\\w+)(\\.[^\\)\\s]+)?\\s*\\);");
//			final Pattern pattern2Args = Pattern
//					.compile("(\\w+)\\.(\\w+)\\(\\s*(-?\\w+)(\\.[^,]+)?,\\s*(-?\\w+)(\\.matrix)?\\s*\\);");
//			final Pattern pattern1Parameter = Pattern.compile("(\\w+)\\.(\\w+)\\(\\s*(%s)\\s*\\);");
//
//			boolean success = false;
//			Matcher matcher = patternNoCast.matcher(line);
//			while (matcher.find()) {
//				args.add(rename(renames, matcher.group(1)));
//				line = matcher.replaceFirst("%s");
//				success = true;
//				matcher = patternNoCast.matcher(line);
//			}
//			matcher = patternCastValue.matcher(line);
//			while (matcher.find()) {
//				args.add(rename(renames, matcher.group(2)));
//				line = matcher.replaceFirst("%s");
//				success = true;
//				matcher = patternCastValue.matcher(line);
//			}
//			matcher = patternCastMatrix.matcher(line);
//			while (matcher.find()) {
//				args.add(rename(renames, matcher.group(2)));
//				line = matcher.replaceFirst("%s");
//				success = true;
//				matcher = patternCastMatrix.matcher(line);
//			}
//			matcher = patternGetDouble.matcher(line);
//			while (matcher.find()) {
//				args.add(rename(renames, matcher.group(1)));
//				line = matcher.replaceFirst("%s");
//				success = true;
//				matcher = patternGetDouble.matcher(line);
//			}
//			matcher = patternCastGetDouble.matcher(line);
//			while (matcher.find()) {
//				args.add(rename(renames, matcher.group(2)));
//				line = matcher.replaceFirst("%s");
//				success = true;
//				matcher = patternCastGetDouble.matcher(line);
//			}
//
//			matcher = patternBinary.matcher(line);
//			Matcher unary = patternUnary.matcher(line);
//			Matcher oneArg = pattern1Arg.matcher(line);
//			Matcher twoArgs = pattern2Args.matcher(line);
//			Matcher oneParameter = pattern1Parameter.matcher(line);
//
//			if (matcher.find()) {
////    			System.out.printf("   ! \"%%s = %%s %s %%s;\" %% %s\n", matcher.group(1), args.toString() );
//				String code = "sb.append( String.format(";
//				String close = ") );";
//				sb.append(prefix);
//				sb.append("//" + line + "\n");
//				sb.append(prefix);
//				sb.append(code);
//				String format = String.format("\"%%s = %%s %s %%s;\"", matcher.group(1));
//				sb.append(format);
//				for (String arg : args) {
//					sb.append(", ");
//					sb.append(arg);
//					sb.append(".getName()");
//				}
//				sb.append(close);
//				sb.append("\n");
//			} else if (unary.find()) {
////    			System.out.printf("   ! \"%%s = %s%%s;\" %% %s\n", unary.group(1), args.toString() );    			
//				String code = "sb.append( String.format(";
//				String close = ") );";
//				sb.append(prefix);
//				sb.append("//" + line + "\n");
//				sb.append(prefix);
//				sb.append(code);
//				String format = String.format("\"%%s = %s%%s;\"", unary.group(1));
//				sb.append(format);
//				for (String arg : args) {
//					sb.append(", ");
//					sb.append(arg);
//					sb.append(".getName()");
//				}
//				sb.append(close);
//				sb.append("\n");
//			} else if (oneArg.find()) {
////        		System.out.println("1cOV: " + line );
////    			System.out.print(oneArg.groupCount() + "{");
////    			for (int g = 1; g <= oneArg.groupCount(); g++) System.out.printf("%d:%s,", g, oneArg.group(g));
////    			System.out.println("}");
//				args.add(rename(renames, oneArg.group(3)));
//
//				String code = "sb.append( String.format(";
//				String close = ") );";
//				sb.append(prefix);
//				sb.append("//" + line + "\n");
//				sb.append(prefix);
//				sb.append(code);
//				String format = String.format("\"%%s = %s.%s(%%s);\"", oneArg.group(1), oneArg.group(2));
//				sb.append(format);
//				for (String arg : args) {
//					sb.append(", ");
//					sb.append(arg);
//					sb.append(".getName()");
//				}
//				sb.append(close);
//				sb.append("\n");
//			} else if (oneParameter.find()) {
////        		System.out.println("1cOV: " + line );
////    			System.out.print(oneArg.groupCount() + "{");
////    			for (int g = 1; g <= oneArg.groupCount(); g++) System.out.printf("%d:%s,", g, oneArg.group(g));
////    			System.out.println("}");
//				String code = "sb.append( String.format(";
//				String close = ") );";
//				sb.append(prefix);
//				sb.append("//" + line + "\n");
//				sb.append(prefix);
//				sb.append(code);
//				String format = String.format("\"%%s = %s.%s(%%s);\"", oneParameter.group(1), oneParameter.group(2));
//				sb.append(format);
//				for (String arg : args) {
//					sb.append(", ");
//					sb.append(arg);
//					sb.append(".getName()");
//				}
//				sb.append(close);
//				sb.append("\n");
//			} else if (twoArgs.find()) {
//				args.add(rename(renames, twoArgs.group(3)));
//				args.add(rename(renames, twoArgs.group(5)));
//				String code = "sb.append( String.format(";
//				String close = ") );";
//				sb.append(prefix);
//				sb.append("//" + line + "\n");
//				sb.append(prefix);
//				sb.append(code);
//				String format = String.format("\"%%s = %s.%s(%%s, %%s);\"", twoArgs.group(1), twoArgs.group(2));
//				sb.append(format);
//				for (String arg : args) {
//					sb.append(", ");
//					sb.append(arg);
//					sb.append(".getName()");
//				}
//				sb.append(close);
//				sb.append("\n");
//			} else {
//				System.out.println("cOV: " + line);
//				System.out.println("  \"" + line + "\" % " + args.toString() + "");
//				sb.append("codeOutputValue1: ");
//				sb.append(line);
//				sb.append("\n");
//				return false;
//			}
//			if (!success) {
//				sb.append("codeOutputValue2: ");
//				sb.append(line);
//				sb.append("\n");
//			}
//			return success;
//		}
//
//		protected String rename(Map<String, String> renames, String name) {
//			String r = renames.get(name);
//			if (r != null)
//				return r;
//			return name;
//		}
//
//		final Pattern startsVariableMatrix = Pattern.compile("^(final\\s*)?VariableMatrix\\s+");
//		final Pattern startsVariableScalar = Pattern.compile("^(final\\s*)?VariableScalar\\s+");
//
//		// writes code that will write code
//		public StringBuilder toCode(String prefix) {
//			HashMap<String, String> renames = new HashMap<>();
//			renames.put("target", "output");
//			boolean success = true;
//			StringBuilder sb = new StringBuilder();
//			for (String line : body) {
//				if (line.startsWith("//"))
//					continue;
//				if (startsVariableMatrix.matcher(line).find()) {
//					success = codeVariableMatrix(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("DMatrixRMaj ")) {
//					success = codeDMatrixRMaj(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("VariableInteger ")) {
//					success = codeVariableInteger(sb, prefix, renames, line) && success;
//				} else if (startsVariableScalar.matcher(line).find()) {
//					success = codeVariableScalar(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("int ")) {
//					success = codeIntVariableInteger(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("double ")) {
//					success = codeDoubleVariableScalar(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("manager.resize(output")) {
//					success = codeManagerResize(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("output.matrix.reshape(")) {
//					success = codeManagerReshape(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("out.reshape(")) {
//					success = codeManagerReshape(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("CommonOps_DDRM.")) {
//					success = codeCommonOps(sb, prefix, renames, line) && success;
//				} else if (line.startsWith("output.value = ")) {
//					success = codeOutputValue(sb, prefix, renames, line) && success;
//				} else {
//					success = false;
//					System.out.printf("? %s\n", line);
//				}
//				// success = true; // TODO REMOVE THIS
//			}
//			if (success)
//				return sb;
//			sb.insert(0, prefix + "/*\n");
//			for (String line : body) {
//				sb.append(line);
//				sb.append("\n");
//			}
//			sb.append(prefix + "*/\n");
//			sb.append(prefix);
//			sb.append("//TODO MANUAL\n");
//			return sb;
//		}
//
//		public String toString() {
//			return String.format("%s-%s: %s", name, inputs, body.toString());
//		}
//	}
//	
//
//	final String declFormatWithValue = "%s%s%-10s\t%s = %s";
//    final String declFormatInitialize = "%s%s%-10s\t%s = new %s(%s)";
//    final String declFormatScalar = "%s%s%-10s %s = 0";
//    final String declFormat = "%s%s%-10s\t%s";
//    final String returnFormat = "%s%sreturn %s;\n";
//    
//    final Pattern reshapePattern = Pattern.compile("(\\w+)\\.reshape\\(\\s*(\\w+)\\.numRows\\,\\s*(\\w+)\\.numCols");
//    
//
//	private static HashMap<String, HashMap<String, Execution>> byInputs = new HashMap<>();
//
//	private static void addExecution(Execution execution) {
//		if (!byInputs.containsKey(execution.inputs)) {
//			byInputs.put(execution.inputs, new HashMap<String, Execution>());
//		}
//		HashMap<String, Execution> byName = byInputs.get(execution.inputs);
//		byName.put(execution.name, execution);
//	}
//
//	public static void mainGenerateEmitCodeOperation(String[] args) {
//		Path path = Paths.get("src/org/ejml/equation/OperationExecuteFactory.java");
//		Path outPath = Paths.get("src/org/ejml/equation/EmitCodeOperation.java");
//		String methodDecl = null;
//		try {
//			List<String> lines = Files.readAllLines(path);
////			System.out.println(lines.size());
//			Iterator<String> it = lines.iterator();
//			while (it.hasNext()) {
//				String line = it.next().trim();
//				if (line.startsWith("public Operation")) {
//					line = it.next().trim();
//					while (!line.startsWith("@Override")) {
//						line = it.next().trim();
//					}
//					continue;
//				}
//				if (line.startsWith("public Info")) {
////					System.out.println(line);
//					methodDecl = line;
//				}
//				if (line.contains("new Operation")) {
////					System.out.println("  " + line);
//					Execution execution = new Execution(methodDecl, line);
//					line = it.next().trim();
//					while (!line.startsWith("};")) {
//						if (line.isEmpty() || line.startsWith("@Override") || line.startsWith("public void")
//								|| line.startsWith("try") || line.equals("}")) {
//							line = it.next().trim();
//							continue;
//						}
//						if (line.startsWith("} catch")) {
//							while (!line.equals("}")) {
//								line = it.next().trim();
//							}
//							continue;
//						}
////						System.out.println("    " + line);
//						execution.body.add(line);
//						line = it.next().trim();
//					}
//					addExecution(execution);
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		final String PACKAGE_HEADER = "package org.ejml.equation;\n" + "\n" + "public class EmitCodeOperation {\n"
//				+ "\n";
//		final String PACKAGE_TRAILER_FORMAT = "	public static void emitJavaOperation(StringBuilder body, CodeOperation codeOp) {\n"
//				+ "%s" + "	}\n" + "}";
//		final String XOP_HEADER_FORMAT = "\tprotected String %sOp(String op, CodeOperation codeOp) {\n"
//				+ "\t\tVariable output = codeOp.output;\n" + "\t\tVariable A = codeOp.input.get(0);\n"
//				+ "\t\tVariable B = codeOp.input.get(1);\n" + "\t\tStringBuilder sb = new StringBuilder();\n";
//		final String XOP_SWITCH_FORMAT = "\t\tswitch (op) {\n";
//
//		final String XOP_CASE_FORMAT = "\t\tcase \"%s\": // %s\n" + "%s" + "\t\t\treturn sb.toString();\n";
//
//		final String XOP_TRAILER_FORMAT = "\t\t}\n" + "\t\treturn sb.toString();\n" + "\t}\n";
//		for (String input : byInputs.keySet()) {
//			System.out.println(input);
//		}
////		try {
////			PrintStream code = new PrintStream( outPath.toFile() );
////			code.print(PACKAGE_HEADER);
////			final String prefix = "\t";
////    		Execution.codeFormats( code, prefix );
////    		code.println();
////    		code.println();
////	    	for (String input : byInputs.keySet()) {
////	    		code.printf( XOP_HEADER_FORMAT, input );
////	    		code.print(XOP_SWITCH_FORMAT);
////	    		Map<String, Execution> byName = byInputs.get(input);
////	    		for (String name : byName.keySet()) {
////	    			Execution execution = byName.get(name);
////	    			code.printf(XOP_CASE_FORMAT, name, execution.method, execution.toCode("\t\t\t").toString());
////	    		}
////	    		code.println(XOP_TRAILER_FORMAT);
////	    		code.println();
////	    	}
////	    	code.printf(PACKAGE_TRAILER_FORMAT, "\n");
////	    	code.close();
////		} catch (Exception x) {
////			x.printStackTrace();
////		}
//	}

	private static String unquote(String f) {
		f = f.trim();
		if (f.charAt(0) == '\"') {
			f = f.substring(1, f.length() - 1);
		}
		return f.trim();
	}

	private static void writeCodedEquationMethod(ArrayList<String> body, String prefix, String testName,
			HashMap<String, String> lookups, GeneratorCompileCodeOperations generator, Equation eq, Sequence sequence,
			String equationText) {

		body.add("");
		String name = String.format("%s_Coded", testName);
		StringBuilder sb = new StringBuilder();
		generator.emitJavaTest(sb, prefix, name, eq, equationText);
		body.addAll(Arrays.asList(sb.toString().split("\n")));
		// body.add("}");
	}

	private static void handleAliases(Equation eq, HashMap<String, String> names, HashMap<String, String> constants,
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
	
	public static void declareTemporary( StringBuilder header, String indent, Variable variable ) {
		if (variable.isConstant())
			return;
		switch (variable.getType()) {
		case SCALAR:
			VariableScalar scalar = (VariableScalar) variable;
			if (scalar.getScalarType() == VariableScalar.Type.INTEGER) {
				header.append( String.format("%s%-10s %s;\n", indent, "int", variable.getOperand() ));
			} else {
				header.append( String.format("%s%-10s %s;\n", indent, "double", variable.getOperand() ));    					
			}
			break;
		case MATRIX:
			header.append( String.format("%s%-10s %s = new DMatrixRMaj(1,1);\n", indent, "DMatrixRMaj", variable.getName() ));
			break;
		default:
			System.err.println("Unhandled variable type encountered: " + variable);
			break;
		}
	}

	protected static void emitIndentedOperation(StringBuilder test, String indent, Info info) {
		StringBuilder sb = new StringBuilder();
		EmitJavaCodeOperation coder = new EmitJavaCodeOperation();
		coder.emitOperation( sb, info );
		for (String line : sb.toString().split("\n")) {
			test.append(indent);
			test.append(line);
			test.append('\n');
		}
	}

	
	private static boolean copyTest(PrintStream code, Iterator<String> it, String testName, String line) {
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
					GeneratorCompileCodeOperations generator = new GeneratorCompileCodeOperations(sequence);
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
					System.err.printf("In %s: %s\n", testName, x.getMessage() );
//					throw x;
					return false;
				}
			} else {
				System.err.printf("No assignment in %s\n", testName);
			}
		}
		if (nCompile == 0 && nProcess == 0) {
			System.err.printf("In %s: %d, %d, %d; no compile() or process()\n", testName, nCompile, nProcess, nAssign );			
		} else if (nCompile > 1 && nProcess == 0) {
			System.err.printf("In %s: %d, %d, %d; more than one compile()\n", testName, nCompile, nProcess, nAssign );
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
					GeneratorCompileCodeOperations generator = new GeneratorCompileCodeOperations(sequence);
					generator.optimize();
					for (Usage usage : generator.integerUsages) {
						Variable variable = usage.variable;
						if (! declaredTemps.contains(variable.getOperand())) {
							declaredTemps.add(variable.getOperand() );
							declareTemporary( header, indent, variable );
						}
					}
					for (Usage usage : generator.doubleUsages) {
						Variable variable = usage.variable;
						if (! declaredTemps.contains(variable.getOperand())) {
							declaredTemps.add(variable.getOperand() );
							declareTemporary( header, indent, variable );
						}
					}
					for (Usage usage : generator.matrixUsages) {
						Variable variable = usage.variable;
						if (! declaredTemps.contains(variable.getOperand())) {
							declaredTemps.add(variable.getOperand() );
							declareTemporary( header, indent, variable );
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
				System.err.printf("In %s: %d, %d, %d; more than one process but not alternating asserts\n", testName, nCompile, nProcess, nAssign );
			}
			return true;
		} else {
			System.err.printf("In %s: %d, %d, %d; strange brew\n", testName, nCompile, nProcess, nAssign );
		}
		return false;
	}

	public static void main(String[] args) {
		HashSet<String> skips = new HashSet<>();
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
		// integer sequence variables not compiled
		skips.add("compile_assign_IntSequence_Case0");
		skips.add("compile_assign_IntSequence_Case1");
		skips.add("compile_assign_IntSequence_Case2");
		skips.add("compile_assign_IntSequence_Case3");
		skips.add("compile_assign_IntSequence_Case4");
		skips.add("compile_assign_IntSequence_Case5");
		skips.add("compile_assign_IntSequence_Case6");

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
//						if (!matcher.group(1).equals("sqrt_int"))
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

}
