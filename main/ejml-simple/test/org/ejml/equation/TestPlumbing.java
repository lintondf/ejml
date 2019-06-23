package org.ejml.equation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.ejml.data.DMatrix2x2;
import org.ejml.data.DMatrixFixed;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.FMatrix2x2;
import org.ejml.data.FMatrixFixed;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.equation.IntegerSequence.Explicit;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

public class TestPlumbing {

    Random rand = new Random(234);
    
    @Test
    public void testCompileCodeOperations() {
    	Sequence seq = new Sequence();
    	Info info = new Info();
    	info.output = VariableInteger.factory(1);
    	info.range = Arrays.asList(new Variable[] {info.output} );
    	ManagerTempVariables tempManager;
    	IEmitOperation coder;
		tempManager = new ManagerTempVariables();
		coder = new EmitJavaOperation();
		CompileCodeOperations compiler = new CompileCodeOperations(coder, seq, tempManager );
		compiler.optimize();
		//System.out.println(compiler.toString());
		
        Equation eq = new Equation();
        int  i = 1, j = 2;
        double a = 3.0, b = 4.0;
        
        eq.alias(i,"i", j, "j", a, "a", b, "b");
        
        seq = eq.compile("b = (2+3*4)*i - (1-7*4)*j + (2.0+3.0*4.0)*a");
        compiler = new CompileCodeOperations(coder, seq, tempManager );
		compiler.optimize();
		String expected = "INPUT:     12 operations,  5 integer temps,  3 double temps,  0 matrix temps\n" + 
				"OPTIMIZATIONS:\n" + 
				"  removed     6 constant expressions\n" + 
				"  removed     2 integer temporaries\n" + 
				"  removed     1 double temporarie\n" + 
				"  removed final copy from temp\n" + 
				"OUTPUT:    12 operations,  5 integer temps,  3 double temps,  0 matrix temps\n" + 
				"INPUTS:\n" + 
				"  i : ScalarI                    : i : ScalarI: \n" + 
				"  j : ScalarI                    : j : ScalarI: \n" + 
				"  a : ScalarD                    : a : ScalarD: \n" + 
				"INTEGER TEMPS:\n" + 
				"  Integer{(2 + (3 * 4))} : ScalarI : Integer{(2 + (3 * 4))} : ScalarI: 0,0,3,3,4,\n" + 
				"  Integer{(1 - (7 * 4))} : ScalarI : Integer{(1 - (7 * 4))} : ScalarI: 1,\n" + 
				"  ti8 : ScalarI                  : ti8 : ScalarI: 1,3,\n" + 
				"DOUBLE TEMPS:\n" + 
				"  Double{(2.0 + (3.0 * 4.0))} : ScalarD : Double{(2.0 + (3.0 * 4.0))} : ScalarD: 2,2,4,\n" + 
				"MATRIX TEMPS:\n" + 
				"TARGET:\n" + 
				"  b : ScalarD                    : b : ScalarD: \n" + 
				"multiply-ii[Integer{(2 + (3 * 4))}:SCALAR,i:SCALAR]->Integer{(2 + (3 * 4))}:SCALAR\n" + 
				"multiply-ii[Integer{(1 - (7 * 4))}:SCALAR,j:SCALAR]->ti8:SCALAR\n" + 
				"multiply-ss[Double{(2.0 + (3.0 * 4.0))}:SCALAR,a:SCALAR]->Double{(2.0 + (3.0 * 4.0))}:SCALAR\n" + 
				"subtract-ii[Integer{(2 + (3 * 4))}:SCALAR,ti8:SCALAR]->Integer{(2 + (3 * 4))}:SCALAR\n" + 
				"add-ss[Integer{(2 + (3 * 4))}:SCALAR,Double{(2.0 + (3.0 * 4.0))}:SCALAR]->b:SCALAR\n";
		assertEquals(expected, compiler.toString());
    }

    /** 
     * Coverage improvements and simple function tests.
     */
    @Test
    public void testEquationPlumbing() {
        Equation eq = new Equation();

        SimpleMatrix A = new SimpleMatrix(5, 6);
        SimpleMatrix B = SimpleMatrix.random_DDRM(5, 6, -1, 1, rand);
        SimpleMatrix C = SimpleMatrix.random_DDRM(5, 4, -1, 1, rand);
        SimpleMatrix D = SimpleMatrix.random_DDRM(4, 6, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(C, "C");
        eq.alias(D, "D");

        Equation out = eq.process("A=B+C*D-B", false);
        assertEquals(out, eq);
        
        ManagerFunctions mf = eq.getFunctions();
        assert( mf != null );
        
        SimpleMatrix Z = SimpleMatrix.random_DDRM(1, 1, -1, 1, rand);
        eq.alias(Z, "Z");
        double a = eq.lookupDouble("Z");
        assertEquals( a, Z.get(0,0), 1e-14 );
        
        FMatrixRMaj W = eq.lookupFDRM("Z");
        assertEquals( W.get(0,0), Z.get(0,0), 1e-6 );
        
        try {
        	eq.alias(Z, "W ");
        	fail("Should not be allowed");
        } catch (Exception x) {
        }
        try {
        	eq.alias(Z, "pow");
        	fail("Should not be allowed");
        } catch (Exception x) {
        }
    	String text = "a = b*c";
    	eq  = new Equation(1, "a", 2, "b", 3, "c");
    	eq.compile(text);
        assertEquals( eq.getText(), text);
        assertTrue( eq.getManagerFunctions() != null);
        eq.setSeed(1);
        double d1 = eq.getTemporariesManager().rand.nextDouble();
        eq.setSeed(2);
        double d2 = eq.getTemporariesManager().rand.nextDouble();
        eq.setSeed();
        double dn = eq.getTemporariesManager().rand.nextDouble();
        assertTrue( d1 != d2 && d2 != dn && d1 != dn);
        eq.alias(1, "i");
        eq.alias(2, "i");
        eq.alias(1.0, "x");
        eq.alias(2.0, "x");
        FMatrixRMaj  fM = new FMatrixRMaj(1,1);
        eq.alias(fM, "fM");
        DMatrixSparseCSC sM = new DMatrixSparseCSC(1,1);
        eq.alias(sM, "sM");
        DMatrixFixed dF = new DMatrix2x2();
        eq.alias(dF, "dF");
        FMatrixFixed fF = new FMatrix2x2();
        eq.alias(fF, "fF");
        TokenList.Token b = new TokenList.Token(new VariableInteger(6));

        eq = new Equation();
        eq.compile("a=3 2 1 7:3:25 30 40");
        eq.compile("a=1 2 3");
      }
    
    @Test
    public void testVariableIntegerPlumbing() {
    	VariableInteger i = VariableInteger.factory("314159");
    	assertEquals( i.getValue(), 314159);
    }

    @Test
    public void testVariableScalarPlumbing() {
    	VariableScalar s = new VariableDouble(3.14);
    	assertEquals(s.toString(), " : ScalarD");
    	s = new VariableInteger(0);
    	assertEquals(s.toString(), " : ScalarI");
    	s = new VariableScalar(VariableScalar.Type.COMPLEX, "Complex") {
			@Override
			public double getDouble() {
				return 0;
			}
    	};
    	assertEquals(s.toString(), "Complex : ScalarC");
    }
    
    @Test
    public void testInfoPlumbing() {
    	Info info = new Info();
    	assertEquals(info.toString(), "<No-Operation>]");
    	info = new Info( new VariableInteger(1));
    	assertEquals(info.toString(), "<No-Operation>[:SCALAR]");
    	info.output = new VariableInteger(1);
    	info.output.setName("outvar");
    	info.range = Arrays.asList( new Variable[] {new VariableInteger(1), new VariableInteger(2)} );
    	info.dimensions = Arrays.asList( new Info.DimensionSources[] {
    			Info.DimensionSources.LHS_ROWS,
    			Info.DimensionSources.LHS_COLS,
    			Info.DimensionSources.RHS_ROWS,
    			Info.DimensionSources.RHS_COLS} );
    	assertEquals(info.toString(), "<No-Operation>[:SCALAR]<:SCALAR,:SCALAR>->outvar:SCALAR[LHS_ROWS,LHS_COLS,RHS_ROWS,RHS_COLS]");
    	info = new Info( new VariableInteger(1), new VariableDouble(3.0)) ;
    	assertEquals(info.toString(), "<No-Operation>[:SCALAR,:SCALAR]");
    	info = new Info( new MatrixConstructor(new ManagerTempVariables() ) );
    	assertEquals(info.toString(), "<No-Operation>(CONSTRUCT(tm1 : VAR_MATRIX TEMP)) ]");
    	info.setOperation();  // for coverage of no-op override target
    }

    @Test
    public void testFunctionPlumbing() {
    	Function f = new Function("reimannZeta");
    	assertEquals(f.toString(), "Function{name='reimannZeta'}");
    }
    
    @Test
    public void testMatrixConstructorPlumbing() {
        DMatrixRMaj A = RandomMatrices_DDRM.rectangle(10,8,rand);

        DMatrixRMaj B = CommonOps_DDRM.extract(A,0,5,0,3);
        DMatrixRMaj C = CommonOps_DDRM.extract(A,0,5,3,8);
        DMatrixRMaj D = CommonOps_DDRM.extract(A,5,10,0,8);

        MatrixConstructor alg = new MatrixConstructor(new ManagerTempVariables());

        alg.addToRow(new VariableMatrix(B,"B"));
        alg.addToRow(new VariableMatrix(C,"C"));
        alg.endRow();
        alg.addToRow(new VariableMatrix(D,"D"));

        CodeMatrixConstructor cMC = new CodeMatrixConstructor(alg);
        StringBuilder sb = new StringBuilder();
    	try {
    		cMC.construct(sb);
    		fail("Should have thrown");
    	} catch (Exception x) {}
    	
        ManagerTempVariables mgr = new ManagerTempVariables();
        alg = new MatrixConstructor(mgr);
        alg.addToRow( mgr.createInteger() );
        alg.addToRow( mgr.createInteger() );
        cMC = new CodeMatrixConstructor(alg);
        sb = new StringBuilder();
        cMC.construct(sb);
        assertEquals(sb.toString(), "DMatrixRMaj tm1 = new DMatrixRMaj(new double[][] {{ti2, ti3,}});");
    }
    
    @Test
    public void testManagerTempVariablesPlumbing() {
    	ManagerTempVariables mgr = new ManagerTempVariables();
    	VariableInteger i = mgr.createInteger();
    	VariableDouble  d = mgr.createDouble();
    	VariableMatrix  m = mgr.createMatrix();
    	mgr.release(i);
    	mgr.release(d);
    	mgr.release(m);
    	VariableInteger i2 = mgr.createInteger();
    	VariableDouble  d2 = mgr.createDouble();
    	VariableMatrix  m2 = mgr.createMatrix();
    	assertEquals(i.getName(), i2.getName());
    	assertEquals(d.getName(), d2.getName());
    	assertEquals(m.getName(), m2.getName());
    	assertEquals(mgr.toString(), "4 issued; released/unused: 0, 0, 0");
    }
    
    private class Zeta1 implements ManagerFunctions.Input1 {
		@Override
		public Info create(Variable A, ManagerTempVariables manager) {
			Info info = new Info();
			info.setOperation( info.new Operation("zeta1") {
				@Override
				public void process() {
				}
			});
			return info;
		}
    }
    
    private class ZetaN implements ManagerFunctions.InputN {
		@Override
		public Info create(List<Variable> inputs, ManagerTempVariables manager) {
			Info info = new Info();
			info.setOperation( info.new Operation("zetan") {
				@Override
				public void process() {
				}
			});
			return info;
		}
    }
    
	protected VariableIntegerSequence variablesToExplicit( int[] vars ) {
		TokenList list = new TokenList();
		for (int var : vars) {
			list.add(VariableInteger.factory(var));
		}
		return new VariableIntegerSequence(new IntegerSequence.Explicit(list.getFirst(), list.getLast()));
	}
    
    
    @Test
    public void testOperationCodeFactoryPlumbing() {
    	Info info = new Info();
    	OperationCodeFactory.CodeOperation op = new OperationCodeFactory.CodeOperation("test", info);
    	op.process();  // coverage
    	
    	OperationCodeFactory factory = new OperationCodeFactory();
    	ManagerTempVariables mgr = new ManagerTempVariables();
    	VariableInteger i = VariableInteger.factory(3);
    	VariableMatrix  m = VariableMatrix.createTemp();
    	VariableIntegerSequence s = variablesToExplicit( new int[] {1,2});
    	
    	info = factory.copy(s, s);
    	assertEquals(info.toString(), "copy-is-is[:INTEGER_SEQUENCE]->:INTEGER_SEQUENCE");
    	
    	try {
    		info = factory.neg(s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.pow(m, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.atan2(m, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.sqrt(m, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.sin(m, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.cos(m, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.atan(m, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.exp(s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.log(s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.elementMult(m, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.elementDivision(m, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.elementPow(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.copy(m, i );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.copy(m, i, new ArrayList<Variable>() );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.transpose(i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.normP(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.max_two(i, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.max_two(m, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.min_two(i, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.min_two(m, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.eye(s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.diag(i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.zeros(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.ones(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.rng(m, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.rand(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.randn(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.kron(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.dot(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.solve(m, s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
		List<Variable> l = new ArrayList<>();
		l.add(i);
    	try {
    		info = factory.extract(l, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	l.clear();
    	l.add(m);
    	l.add(m);
    	try {
    		info = factory.extract(l, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.sum_one(s, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.sum_two(s, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	try {
    		info = factory.sum_two(m, i, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	l.clear();
		l.add(i);
    	try {
    		info = factory.extractScalar(l, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    	l.clear();
    	l.add(m);
    	l.add(m);
    	try {
    		info = factory.extractScalar(l, mgr );
    		fail("should have thrown");
    	} catch (Exception x) {}
    }
    
    @Test
    public void testEmitJavaOperationPlumbing() {
    	EmitJavaOperation coder = new EmitJavaOperation();
    	ManagerFunctions mgr = new ManagerFunctions();
    	
    	StringBuilder body = new StringBuilder();
    	Info info = new Info();
    	info.output = VariableInteger.factory(1);
    	info.input = new ArrayList<>();
    	info.input.add( VariableInteger.factory("-1"));
    	info.input.add( VariableInteger.factory("1"));
    	String[] ops = {"zeta-i", "zeta-ii", "zeta-mm", "zeta-m", "zeta-ms", "zeta", "zeta-sm", "zeta-s", "zeta-ss"};
    	for (String op : ops) {
    		OperationCodeFactory.CodeOperation operation = new OperationCodeFactory.CodeOperation(op, info);
    		info.op = operation;
        	try {
        		coder.emitOperation(body, info );
        		fail("should have thrown");
        	} catch (Exception x) {
        	}
    	}
		OperationCodeFactory.CodeOperation operation = new OperationCodeFactory.CodeOperation("neg-s", info);
		info.op = operation;
		coder.emitOperation(body, info);
		assertEquals( body.toString(), "1 = -(-1);\n");
		body = new StringBuilder();
		
		operation = new OperationCodeFactory.CodeOperation("copy-i", info);
		info.op = operation;
    	try {
    		coder.emitOperation(body, info );
    		fail("should have thrown");
    	} catch (Exception x) {
    	}
    	
    	coder.declare(body, "", VariableMatrix.createTemp());
    	assertEquals(body.toString(), "DMatrixRMaj tm = new DMatrixRMaj(1,1);\n");
    	try {
    		VariableIntegerSequence s = variablesToExplicit( new int[] {1,2});
    		coder.declare(body, "", s);
    	} catch (Exception x) {
    	}
    	assertEquals(coder.getZero().getOperand(), "0");
    	assertEquals(coder.getOne().getOperand(), "1");
    }
    
    
    @Test
    public void testManagerFunctionsPlumbing() {
    	ManagerFunctions mgr = new ManagerFunctions();
    	VariableInteger i = VariableInteger.factory(1);
    	List<Variable> l = new ArrayList<>();
    	Info v = mgr.create("zeta", i);
    	assertTrue( v == null);
    	v = mgr.create("zeta", l);
    	assertTrue( v == null);
    	mgr.add1("zeta1", new Zeta1());
    	mgr.addN("zetan", new ZetaN());
    	v = mgr.create("zeta1", i);
    	assertTrue(v != null);
    	assertEquals(v.op.name, "zeta1");
    	v = mgr.create("zetan", l);
    	assertTrue(v != null);
    	assertEquals(v.op.name, "zetan");
    }
    
    @Test
    public void testThrows() {
    	try {
    		ArrayExtent ae = new ArrayExtent();
    		ae.extractArrayExtent( new VariableDouble(3.14), 0 );
    		fail("Should have thrown");
    	} catch (Exception x) {}
    	try {
    		Extents ae = new Extents();
    		ae.extractSimpleExtents( new VariableDouble(3.14), true, 0 );
    		fail("Should have thrown");
    	} catch (Exception x) {}
    }
}
