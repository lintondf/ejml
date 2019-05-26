package org.ejml.equation;


public class Extents {
	public int row0,row1;
	public int col0,col1;

    /**
     * See if a simple sequence can be used to extract the array.  A simple extent is a continuous block from
     * a min to max index
     *
     * @return true if it is a simple range or false if not
     */
    public boolean extractSimpleExtents(Variable var, boolean row, int length) {
        int lower;
        int upper;
        if( var.getType() == VariableType.INTEGER_SEQUENCE ) {
            IntegerSequence sequence = ((VariableIntegerSequence)var).sequence;
            if( sequence.getType() == IntegerSequence.Type.FOR ) {
                IntegerSequence.For seqFor = (IntegerSequence.For)sequence;
                seqFor.initialize(length);
                if( seqFor.getStep() == 1 ) {
                    lower = seqFor.getStart();
                    upper = seqFor.getEnd();
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if( var.getType() == VariableType.SCALAR ) {
            lower = upper = ((VariableInteger)var).value;
        } else {
            throw new RuntimeException("How did a bad variable get put here?!?!");
        }
        if( row ) {
            row0 = lower;
            row1 = upper;
        } else {
            col0 = lower;
            col1 = upper;
        }
        return true;
    }

}
