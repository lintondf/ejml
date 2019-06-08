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

/** Hoisted from original Operation.java
 * 
 * @author Peter Abeles
 */
public class ArrayExtent {
	public int array[];
	public int length;

    public ArrayExtent() {
        array = new int[1];
    }
    
    public void setLength( int length ) {
        if( length > array.length ) {
            array = new int[ length ];
        }
        this.length = length;
    }

    public void extractArrayExtent( Variable var , int length ) {
        if( var.getType() == VariableType.INTEGER_SEQUENCE ) {
            IntegerSequence sequence = ((VariableIntegerSequence)var).sequence;
            sequence.initialize(length-1);
            setLength(sequence.length());
            int index = 0;
            while( sequence.hasNext() ) {
                array[index++] = sequence.next();
            }
        } else if( var.getType() == VariableType.SCALAR ) {
            setLength(1);
            array[0] = ((VariableInteger)var).value;
        } else {
            throw new RuntimeException("How did a bad variable get put here?!?!");
        }
    }
}
