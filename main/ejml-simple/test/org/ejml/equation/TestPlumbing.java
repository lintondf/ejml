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
    	System.out.println(mgr.toString());
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
