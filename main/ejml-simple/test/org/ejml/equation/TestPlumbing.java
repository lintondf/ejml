package org.ejml.equation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
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
        System.out.println(sb.toString());
        
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
    	TokenList.Token start = new TokenList.Token(new VariableInteger(6));
    	TokenList.Token step = new TokenList.Token(new VariableInteger(6));
    	TokenList.Token end = new TokenList.Token(new VariableInteger(6));
    	IntegerSequence.For f = new IntegerSequence.For(start, step, end);
		Extents extents = new Extents();
		assert( ! extents.extractSimpleExtents( new VariableIntegerSequence(f), true, 0 ));
    }
}
