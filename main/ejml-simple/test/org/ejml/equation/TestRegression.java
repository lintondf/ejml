package org.ejml.equation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.ejml.data.DMatrix2x2;
import org.ejml.data.DMatrixFixed;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.FMatrix2x2;
import org.ejml.data.FMatrixFixed;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.equation.Info.Operation;
import org.ejml.equation.IntegerSequence.Explicit;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;




public class TestRegression {

    Random rand = new Random(234);

	public static class JavaTargetManagerFunctions extends ManagerFunctions {
		
		public String declare( Variable variable ) {
			if (variable.isConstant())
				return "";
			switch (variable.getType()) {
			case SCALAR:
				VariableScalar scalar = (VariableScalar) variable;
				if (scalar.getScalarType() == VariableScalar.Type.INTEGER) {
					return( "int ");
				} else {
					return( "double " );    					
				}
			case MATRIX:
				return( "DMatrixRMaj " );
			default:
				return "";
			}
		}
		
		
		public class GenericMethodFunction implements ManagerFunctions.InputN, ManagerFunctions.Coder {
			
			String name;
			String type;
			Variable output;
			
			public GenericMethodFunction(String name, String type ) {
				this.name = name;
				this.type = type;
			}

			@Override
			public String code(Info info) {
				StringBuilder sb = new StringBuilder();
				if (info.output.isTemp()) {
					sb.append( declare(info.output) );
				}
				if ( ! info.output.getOperand().equals("void")) {
					sb.append(info.output.getOperand());
					sb.append(" = ");
				}
				sb.append(name);
				sb.append("(");
				for (int i = 0; i < info.input.size(); i++) {
					Variable v = info.input.get(i);
					sb.append(v.getOperand());
					if (i < info.input.size()-1) 
						sb.append(", ");
				}
				sb.append(");\n");
				return sb.toString();
			}

			@Override
			public String opName() {
				return name;
			}

			@Override
			public Info create(List<Variable> inputs, ManagerTempVariables manager) {
				final Info info = new Info(inputs);
				switch (type) {
				case "int":
					output = manager.createInteger();
					break;
				case "float":
					output = manager.createDouble();
					break;
				case "array":
					output = manager.createMatrix();
					break;
				case "vector":
					output = manager.createMatrix();
					break;
				default:
					output = manager.createIntegerConstant(0, "void");
				}
				info.output = output;
				info.op = info.new Operation(opName()) {
					@Override
					public void process() {
					}
				};
				return info;
			}
			
		}
		

		
		public JavaTargetManagerFunctions( IOperationFactory factory ) {
			super(factory);
			new IntFunction().add();
			new NumRowsFunction().add();
			new AssertAlmostEqualFunction().add();
		}
		
		public abstract class CodedFunction1 implements ManagerFunctions.Input1, ManagerFunctions.Coder {
			public String name;
			
			CodedFunction1(String name) {
				this.name = name;
			}
	
			public void add() {
				add1( name, this, this);
			}
		}
		
		public abstract class CodedFunctionN implements ManagerFunctions.InputN, ManagerFunctions.Coder {
			public String name;
			
			CodedFunctionN(String name) {
				this.name = name;
			}
			
			public void add() {
				addN( name, this, this);
			}
		}
		
	    public class AssertAlmostEqualFunction extends CodedFunctionN {
	    	
	    	String opname;
	    	
	    	AssertAlmostEqualFunction() {
				super("assert_almost_equal");
			}
	    	
			@Override
			public Info create(List<Variable> inputs, ManagerTempVariables manager) {
		    	final Info info = new Info();
		    	info.input = inputs;
		    	if (info.input.size() == 2) {
		    		info.output = manager.createIntegerConstant(0, "void");
		    		info.op = info.new Operation(opName()) {
						@Override
						public void process() {
						}
		    		};
		    	} else {
		    		throw new RuntimeException("assert_almost_equal requires exactly two parameters");
		    	}
	    		return info;
			}

			@Override
			public String code(Info info) {
				Variable A = info.input.get(0);
				StringBuilder sb = new StringBuilder();
				sb.append( String.format("assert_almost_equal(%s, %s);\n",
						info.input.get(0).getOperand(),
						info.input.get(1).getOperand() ) );
				return sb.toString();
			}

			@Override
			public String opName() {
				return opname;
			}

			@Override
			public void add() {
				String[] opnames = {"assert_almost_equal-ss", "assert_almost_equal-mm"};
				for (String n : opnames) {
					opname = n;
					super.add();
				}
			}
	    }
	    
	    
	    public class IntFunction extends CodedFunction1 {
	    	IntFunction() {
				super("int");
			}

			@Override
			public String opName() {
				return "$int-s";
			} 

			@Override
			public Info create(Variable A, ManagerTempVariables manager) {
		    	final Info info = new Info(A);
		    	if( A instanceof VariableScalar ) {
		    		info.output = manager.createInteger();
		    		info.op = info.new Operation(opName()) {
						@Override
						public void process() {
							info.outputInteger().value = (int) ((VariableScalar) info.A()).getDouble();
						}
		    		};
		    	} else {
		    		throw new RuntimeException("int() only takes one scalar parameter");
		    	}
	    		return info;
			}

			@Override
			public String code(Info info) {
				Variable A = info.input.get(0);
				StringBuilder sb = new StringBuilder();
				sb.append(String.format("%s = (int) %s;\n", info.output.getOperand(), info.A().getOperand()));
				return sb.toString();
			}
	    }
	    
	    public class NumRowsFunction extends CodedFunction1 {
	    	NumRowsFunction() {
				super("numRows");
			}

			@Override
			public String opName() {
				return "$numRows-m";
			} 

			@Override
			public Info create(Variable A, ManagerTempVariables manager) {
		    	final Info info = new Info(A);
		    	if( A instanceof VariableMatrix ) {
		    		info.output = manager.createInteger();
		    		info.op = info.new Operation(opName()) {
						@Override
						public void process() {
		                    CommonOps_DDRM.setIdentity(info.outputMatrix().matrix);
		                    info.outputInteger().value = ((VariableMatrix) info.A()).matrix.getNumRows();
						}
		    		};
		    	} else {
		    		throw new RuntimeException("numRows only takes one matrix parameter");
		    	}
	    		return info;
			}

			@Override
			public String code(Info info) {
				Variable A = info.input.get(0);
				StringBuilder sb = new StringBuilder();
				sb.append( String.format("%s = %s.getNumRows();\n", info.output.getOperand(), A.getOperand()) );
				return sb.toString();
			}
	    }
	    
	}	

    @Test
    public void testMatrixTempReuse() {
		Equation eq = new Equation();
		OperationCodeFactory factory = new OperationCodeFactory();
		JavaTargetManagerFunctions mf = new JavaTargetManagerFunctions(factory);
		JavaTargetManagerFunctions.GenericMethodFunction gmf = mf.new GenericMethodFunction("getVRF", "array");
		mf.addN( gmf.opName(), gmf, gmf );
		eq.setManagerFunctions(mf);
		
		IEmitOperation coder = new EmitJavaOperation(eq.getFunctions());		
		TreeSet<String> declaredTemps = new TreeSet<>();
		HashSet<String> parameters = new HashSet<>();
		List<String> matrices = new ArrayList<>();
		GenerateEquationCode codeGenerator = new GenerateEquationCode(eq, coder, parameters, matrices, declaredTemps);
		int N = 0;
		double a = 1.1;
		DMatrixRMaj A = new DMatrixRMaj(3,3);
		eq.alias(a, "a", N, "N", A, "A");
		codeGenerator.generate("A = solve(getVRF(1), getVRF(2))", true);
		String expected = "[// A = solve(getVRF(1), getVRF(2)), DMatrixRMaj tm1 = getVRF(1);, DMatrixRMaj tm2 = getVRF(2);, A.reshape( tm1.numRows, tm2.numCols );, LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(tm1.numRows, tm1.numCols);, boolean ok = solver.setA(tm1);, solver.solve(tm2, A);]";
		assertEquals( expected, codeGenerator.getCode().toString());
    }
    
    @Test
    public void testScalarDeclarations() {
		Equation eq = new Equation();
		OperationCodeFactory factory = new OperationCodeFactory();
		ManagerFunctions mf = new JavaTargetManagerFunctions(factory);
		eq.setManagerFunctions(mf);
		
		IEmitOperation coder = new EmitJavaOperation(eq.getFunctions());		
		TreeSet<String> declaredTemps = new TreeSet<>();
		HashSet<String> parameters = new HashSet<>();
		List<String> matrices = new ArrayList<>();
		GenerateEquationCode codeGenerator = new GenerateEquationCode(eq, coder, parameters, matrices, declaredTemps);
		int N = 0;
		double a = 1.1;
		DMatrixRMaj A = new DMatrixRMaj(3,3);
		eq.alias(a, "a", N, "N", A, "A");
		codeGenerator.generate("N = int(A(numRows(A)-1,1))");
		assertTrue(codeGenerator.getHeader().length() == 0);
		assertEquals("[// N = int(A(numRows(A)-1,1)), int        ti1 = A.getNumRows();, double     td3 = A.get(ti1 - 1, 1);, N = (int) td3;]",
					codeGenerator.getCode().toString());
    }
    
    @Test
    public void testVectorAssignment() {
    	Sequence seq = new Sequence();
    	Info info = new Info();
    	info.output = VariableInteger.factory(1);
    	info.range = Arrays.asList(new Variable[] {info.output} );
    	ManagerTempVariables tempManager;
    	IEmitOperation coder;
		tempManager = new ManagerTempVariables();
		coder = new EmitJavaOperation( new ManagerFunctions());
		CompileCodeOperations compiler = new CompileCodeOperations(coder, seq, tempManager );
		compiler.optimize();
		//System.out.println(compiler.toString());
		
        Equation eq = new Equation();
        DMatrixRMaj Z = new DMatrixRMaj(3,1, 5.0);
        DMatrixRMaj V = new DMatrixRMaj(3,1, 2.0);

        int  m = 1;
        
        eq.alias(m, "m", Z, "Z", V, "V");
        
        seq = eq.compile("Z(0:m) = V(0:m)");
        compiler = new CompileCodeOperations(coder, seq, tempManager );
		compiler.optimize();
    	
		for (Info inf : seq.getInfos()) {
    		StringBuilder block = new StringBuilder();
    		coder.emitOperation( block, inf );
    		System.out.println( block.toString() );
		}
    }
    
	@Test
	public void testSingleIndexSet() {
    	Sequence seq = new Sequence();
    	Info info = new Info();
    	info.output = VariableInteger.factory(1);
    	info.range = Arrays.asList(new Variable[] {info.output} );
    	ManagerTempVariables tempManager;
    	IEmitOperation coder;
		tempManager = new ManagerTempVariables();
		coder = new EmitJavaOperation( new ManagerFunctions());
		CompileCodeOperations compiler = new CompileCodeOperations(coder, seq, tempManager );
		compiler.optimize();
		//System.out.println(compiler.toString());
		
        Equation eq = new Equation();
        DMatrixRMaj m = new DMatrixRMaj(3,1, 5.0);

        int  i = 0;
        
        eq.alias(i,"i", m, "m");
        
        seq = eq.compile("m(i+1, 0) = 3.0");
        compiler = new CompileCodeOperations(coder, seq, tempManager );
		compiler.optimize();
		
		for (Info inf : seq.getInfos()) {
    		StringBuilder block = new StringBuilder();
    		coder.emitOperation( block, inf );
    		assertEquals( "m.unsafe_set(i + 1, 0, 3.0);\n", block.toString() );
		}
	}
	
	static class IntFunction implements ManagerFunctions.Input1, ManagerFunctions.Coder {

		@Override
		public String code(Info info) {
			Variable A = info.input.get(0);
			StringBuilder sb = new StringBuilder();
			if (info.output.isTemp()) {
				sb.append( String.format("int %s;\n", 
					info.output.getOperand()));
			} else {
				 sb.append( String.format("%s = (int) %s;", info.output.getOperand(), A.getOperand() ));
			}
			return sb.toString();
		}

		@Override
		public String opName() {
			return "int-s";
		}

		@Override
		public Info create(Variable A, ManagerTempVariables manager) {
	    	final Info info = new Info(A);
	    	if( A instanceof VariableScalar ) {
	    		info.output = manager.createInteger();
	    		info.op = info.new Operation(opName()) {
					@Override
					public void process() {
						VariableScalar s = ((VariableScalar) info.input.get(0));
	                    info.outputInteger().value = (int) s.getDouble();
					}
	    		};
	    	} else {
	    		throw new RuntimeException("int only takes one scalar parameter");
	    	}
    		return info;
		}
		
	}
	
    @Test
    public void testOverOptimizeIntegerTemps() {
    	Equation eq;
    	ManagerTempVariables tempManager;
    	IEmitOperation coder;
    	
    	DMatrixRMaj V;
		V = new DMatrixRMaj(3,3);
		eq = new Equation();
		tempManager = eq.getTemporariesManager();
		IntFunction f = new IntFunction();
		eq.getManagerFunctions().add1("int", f, f);
		coder = new EmitJavaOperation(eq.getManagerFunctions());
		eq.alias(V, "V");
		Sequence seq = eq.compile("out = V(int(min(V))+1, int(max(V))+1)");
		CompileCodeOperations compiler = new CompileCodeOperations(coder, seq, tempManager );
		compiler.optimize();
		String actual = compiler.toString();
		String expected = "INPUT:      8 operations,  4 integer temps,  3 double temps,  0 matrix temps\n" + 
				"OPTIMIZATIONS:\n" + 
				"  removed     2 constant expressions\n" + 
				"  removed     2 integer temporaries\n" + 
				"  removed final copy from temp\n" + 
				"OUTPUT:     0 operations,  4 integer temps,  3 double temps,  0 matrix temps\n" + 
				"INPUTS:\n" + 
				"  V : VAR_MATRIX                 : V : VAR_MATRIX: \n" + 
				"  Integer{1} : ScalarI           : Integer{1} : ScalarI: \n" + 
				"  Integer{1} : ScalarI           : Integer{1} : ScalarI: \n" + 
				"INTEGER TEMPS:\n" + 
				"  ti2 : ScalarI                  : ti2 : ScalarI: 1,\n" + 
				"  ti4 : ScalarI                  : ti4 : ScalarI: 3,\n" + 
				"  Integer{ti2 + 1} : ScalarI     : Integer{ti2 + 1} : ScalarI: 4,\n" + 
				"  Integer{ti4 + 1} : ScalarI     : Integer{ti4 + 1} : ScalarI: 4,\n" + 
				"DOUBLE TEMPS:\n" + 
				"  td1 : ScalarD                  : td1 : ScalarD: 0,1,\n" + 
				"  td3 : ScalarD                  : td3 : ScalarD: 2,3,\n" + 
				"MATRIX TEMPS:\n" + 
				"TARGET:\n" + 
				"  out : ScalarD                  : out : ScalarD: \n" + 
				"min-m[V:MATRIX<100>]=>td1:SCALAR<100>\n" + 
				"int-s[td1:SCALAR<100>]=>ti2:SCALAR<100>\n" + 
				"max-m[V:MATRIX<100>]=>td3:SCALAR<100>\n" + 
				"int-s[td3:SCALAR<100>]=>ti4:SCALAR<100>\n" + 
				"extractScalar[V:MATRIX<100>,Integer{ti2 + 1}:SCALAR<10>,Integer{ti4 + 1}:SCALAR<10>]=>out:SCALAR<100>\n";
		assertEquals(expected, actual);
    }	
    
    @Test
    public void testSimplifyWithVariables() {
    	Equation eq;
    	ManagerTempVariables tempManager;
    	IEmitOperation coder;
    	int i1 = 1;
    	int i2 = 3;

		eq = new Equation();
		tempManager = eq.getTemporariesManager();
		coder = new EmitJavaOperation(eq.getManagerFunctions());
		String expr = "out = ((3+2)*i2 - i1) + (i2 - 5*i1)";
		//eq.alias(i1, "i1", i2, "i2");
		eq.autoDeclare(expr);
		Sequence seq = eq.compile(expr);
		CompileCodeOperations compiler = new CompileCodeOperations(coder, seq, tempManager );
		compiler.optimize();
    	assertEquals( seq.getInfos().size(), 1 );
    	StringBuilder block = new StringBuilder();
    	coder.emitOperation( block, seq.getInfos().get(0) );
    	assertEquals("5 * i2 - i1 + i2 - 5 * i1", block.substring(6, block.length()-2));
    }
    
    @Test
    public void testIntMatrixElement() {
    	Equation eq;
    	ManagerTempVariables tempManager;
    	IEmitOperation coder;
    	int N = 0;
    	int i = 0;
    	int j = 0;
    	DMatrixRMaj V;
		V = new DMatrixRMaj(3,3);
		V.unsafe_set(0,  0, 5.0);
    	
		eq = new Equation();
		tempManager = eq.getTemporariesManager();
		IntFunction f = new IntFunction();
		eq.getManagerFunctions().add1("int", f, f);
		coder = new EmitJavaOperation(eq.getManagerFunctions());
		String expr = "N = int(V(0,0))";
    	eq.alias(N, "N", i, "i", j, "j", V, "V");
		Sequence seq = eq.compile(expr);
		CompileCodeOperations compiler = new CompileCodeOperations(coder, seq, tempManager );
		assertEquals( seq.infos.size(), 3);
		compiler.optimize();
		assertEquals( seq.infos.size(), 2);
    }
}
