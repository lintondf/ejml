/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
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

import org.junit.Test;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.equation.Info;

import static org.junit.Assert.assertEquals;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestSequence {

    int total;

    /**
     * Checks the order in which operations are run
     */
    @Test
    public void order() {
        Sequence s = new Sequence();
        /*
         * The inversion of the Info/Operation hierarchy made this more complex.
         * Any type derived from Info.Operation must have an Info parameter for its
         * super() invocation and static factory methods are not possible for such
         * subtypes.  The order2 test below shows an alternate implementation.
         */
        Info info = new Info();
        info.op = new Foo(info, "a",0);
        s.addOperation( info );
        info = new Info();
        info.setOperation(new Foo(info, "b",1));;
        s.addOperation( info );

        total = 0;
        s.perform();
        assertEquals(2,total);
    }

    public class Foo extends Info.Operation {

        int expected;

        protected Foo(Info info, String name, int expected ) {
            info.super(name);
            this.expected = expected;
        }

        @Override
        public void process() {
            assertEquals(expected, total);
            total++;
        }
        
    }
    
    @Test
    public void order2() {
        Sequence s = new Sequence();
        s.addOperation( new Foo2("a", 0) );
        s.addOperation( new Foo2("b", 1));;

        total = 0;
        s.perform();
        assertEquals(2,total);
    }

    public class Foo2 extends Info {
    	
    	public Foo2(String name, int expected) {
    		this.op = new Operation( name, expected );
    	}
    	
    	public class Operation extends Info.Operation {

            int expected;

			protected Operation(String name, int expected) {
	            super(name);
	            this.expected = expected;
			}
    		
	        @Override
	        public void process() {
	            assertEquals(expected, total);
	            total++;
	        }
    	}
    }

    @Test 
    public void testOutput() {
    	Sequence s = new Sequence();
    	Variable x = new VariableInteger(1);
    	s.output = x;
    	assert( s.getOutput() == x);
    }
    
    @Test
    public void testOptimize() {
    	Random rand = new Random(234);
    	Equation eq;
    	Sequence updateK, updateP;
    	ManagerTempVariables tempManager;
    	IEmitOperation coder;
    	
    	DMatrixRMaj K,P, H, R;
		final int N = 6;
		final int M = 3;
		P = RandomMatrices_DDRM.symmetricPosDef( N, rand );
		R = RandomMatrices_DDRM.symmetricPosDef( M, rand );
		H = RandomMatrices_DDRM.rectangle(M, N, rand);
		K = new DMatrixRMaj(N, N);
		eq = new Equation();
		tempManager = eq.getTemporariesManager();
		coder = new EmitJavaOperation();
		eq.alias(K, "K", P, "P", H, "H", R, "R");
		updateK = eq.compile("K = P*H'*inv( H*P*H' + R )");
		CompileCodeOperations compiler = new CompileCodeOperations(coder, updateK, tempManager );
		compiler.optimize();
		String actual = compiler.toString();

		String expected = "INPUT:      9 operations,  0 integer temps,  0 double temps,  8 matrix temps\n" + 
				"OPTIMIZATIONS:\n" + 
				"  removed     2 matrix temporaries\n" + 
				"  removed final copy from temp\n" + 
				"OUTPUT:     0 operations,  0 integer temps,  0 double temps,  8 matrix temps\n" + 
				"INPUTS:\n" + 
				"  H : VAR_MATRIX                 : H : VAR_MATRIX: \n" + 
				"  P : VAR_MATRIX                 : P : VAR_MATRIX: \n" + 
				"  R : VAR_MATRIX                 : R : VAR_MATRIX: \n" + 
				"INTEGER TEMPS:\n" + 
				"DOUBLE TEMPS:\n" + 
				"MATRIX TEMPS:\n" + 
				"  tm1 : VAR_MATRIX TEMP          : tm1 : VAR_MATRIX TEMP: 0,2,3,4,5,6,\n" + 
				"  tm2 : VAR_MATRIX TEMP          : tm2 : VAR_MATRIX TEMP: 1,2,\n" + 
				"  tm3 : VAR_MATRIX TEMP          : tm3 : VAR_MATRIX TEMP: 2,3,\n" + 
				"  tm5 : VAR_MATRIX TEMP          : tm5 : VAR_MATRIX TEMP: 4,7,\n" + 
				"  tm7 : VAR_MATRIX TEMP          : tm7 : VAR_MATRIX TEMP: 6,7,\n" + 
				"TARGET:\n" + 
				"  K : VAR_MATRIX                 : K : VAR_MATRIX: \n" + 
				"transpose-m[H:MATRIX<100>]=>tm1:MATRIX<100>\n" + 
				"multiply-mm[H:MATRIX<100>,P:MATRIX<100>]=>tm2:MATRIX<100>\n" + 
				"multiply-mm[tm2:MATRIX<100>,tm1:MATRIX<100>]=>tm3:MATRIX<100>\n" + 
				"add-mm[tm3:MATRIX<100>,R:MATRIX<100>]=>tm1:MATRIX<100>\n" + 
				"inv-m[tm1:MATRIX<100>]=>tm5:MATRIX<100>\n" + 
				"transpose-m[H:MATRIX<100>]=>tm1:MATRIX<100>\n" + 
				"multiply-mm[P:MATRIX<100>,tm1:MATRIX<100>]=>tm7:MATRIX<100>\n" + 
				"multiply-mm[tm7:MATRIX<100>,tm5:MATRIX<100>]=>K:MATRIX<100>\n";
		assertEquals(actual, expected);
		
		expected = "transpose-m[H:MATRIX<100>]=>tm1:MATRIX<100>\n" + 
				"multiply-mm[H:MATRIX<100>,P:MATRIX<100>]=>tm2:MATRIX<100>\n" + 
				"multiply-mm[tm2:MATRIX<100>,tm1:MATRIX<100>]=>tm3:MATRIX<100>\n" + 
				"add-mm[tm3:MATRIX<100>,R:MATRIX<100>]=>tm1:MATRIX<100>\n" + 
				"inv-m[tm1:MATRIX<100>]=>tm5:MATRIX<100>\n" + 
				"transpose-m[H:MATRIX<100>]=>tm1:MATRIX<100>\n" + 
				"multiply-mm[P:MATRIX<100>,tm1:MATRIX<100>]=>tm7:MATRIX<100>\n" + 
				"multiply-mm[tm7:MATRIX<100>,tm5:MATRIX<100>]=>K:MATRIX<100>\n";
    	actual = new CaptureSystemOut() {
			@Override
			public boolean run() {
				updateK.print();
				return true;
			}
    		
    	}.capture();
    	assertEquals( expected, actual );
    }
}
