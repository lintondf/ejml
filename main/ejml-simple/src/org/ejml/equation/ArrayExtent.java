package org.ejml.equation;

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
