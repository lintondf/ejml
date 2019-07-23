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
import org.ejml.equation.Info.Operation;
import org.ejml.equation.IntegerSequence.Explicit;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

public class TestRegression {

    Random rand = new Random(234);
    
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
}
