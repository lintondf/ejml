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
    		assertEquals( block.toString(), "m.unsafe_set(1, 0, 3.0);\n");
		}
	}
}
