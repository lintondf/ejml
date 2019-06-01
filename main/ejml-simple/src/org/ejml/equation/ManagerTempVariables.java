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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Manages the creation and recycling of temporary variables used to store intermediate results.  The user
 * cannot directly access these variables
 *
 * @author Peter Abeles
 */
// TODO add function to purge temporary variables.  basically resize and redeclare their array to size 1
public class ManagerTempVariables {
	
	private long sequence = 1;
	
	private Deque<VariableMatrix> matrixRecycle = new ArrayDeque<>();
	private Deque<VariableInteger> integerRecycle = new ArrayDeque<>();
	private Deque<VariableDouble> doubleRecycle = new ArrayDeque<>();
	
	public ManagerTempVariables() {
	}
	
	public void release( VariableMatrix variable) {
		matrixRecycle.addLast(variable);
	}
	
	public void release( VariableInteger variable) {
		integerRecycle.addLast(variable);
	}
	
	public void release( VariableDouble variable) {
		doubleRecycle.addLast(variable);
	}
	
    /**
     * Set random seed to a constant value by default for repeatable results.
     */
    Random rand = new Random(0xDEADBEEF);

    public Random getRandom() {
        return rand;
    }

    public VariableMatrix createMatrix() {
    	if (! matrixRecycle.isEmpty())
    		return matrixRecycle.removeFirst();
    	VariableMatrix ret = VariableMatrix.createTemp();
    	String name = String.format("tm%d", sequence++ );
    	ret.setName(name);
        return ret;
    }

    public VariableDouble createDouble() {
    	if (! doubleRecycle.isEmpty())
    		return doubleRecycle.removeFirst();
    	VariableDouble ret = new VariableDouble(0, String.format("td%d", sequence++));
    	ret.setTemp(true);
    	return ret;
    }

    public VariableDouble createDoubleConstant( double value, String representation ) {
        return new VariableDouble(value, String.format("DOUBLE{%s}", representation));
    }

    public VariableInteger createInteger() {
    	if (! integerRecycle.isEmpty())
    		return integerRecycle.removeFirst();
    	VariableInteger ret = new VariableInteger(0, String.format("ti%d", sequence++));
    	ret.setTemp(true);
    	return ret;
    }

    public VariableInteger createIntegerConstant( int value, String representation ) {
        return new VariableInteger(value, String.format("INTEGER{%s}", representation));
    }

    public VariableIntegerSequence createIntegerSequence( IntegerSequence sequence ) {
        return new VariableIntegerSequence(sequence, String.format("SEQUENCE{%s}", sequence.toString())) ;
    }
    
    /**
     * If the variable is a local temporary variable it will be resized so that the operation can complete.  If not
     * temporary then it will not be reshaped
     * @param mat Variable containing the matrix
     * @param numRows Desired number of rows
     * @param numCols Desired number of columns
     */
    public void resize( VariableMatrix mat , int numRows , int numCols ) {
        if( mat.isTemp() ) {
            mat.matrix.reshape(numRows,numCols);
        }
    }

    
}
