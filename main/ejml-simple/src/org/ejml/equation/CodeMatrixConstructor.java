package org.ejml.equation;

import java.util.List;

import org.ejml.equation.MatrixConstructor.Item;

public class CodeMatrixConstructor {
	
	MatrixConstructor executor;

	public CodeMatrixConstructor(MatrixConstructor executor) {
		this.executor = executor;
	}
	
	public void construct( StringBuilder sb ) {
		List<Item> items = executor.getItems();
        // make sure the last item is and end row
        if( !items.get(items.size()-1).endRow )
        	executor.endRow();

        // have to initialize some variable types first to get the actual size
        for (int i = 0; i < items.size(); i++) {
        	if (items.get(i).matrix) {
        		throw new IllegalArgumentException("Construction from submatrices can not be precompiled.");
        	}
            items.get(i).initialize();
        }

        executor.setToRequiredSize(executor.getOutput().matrix);
        
        final String formatDecl = "DMatrixRMaj %s = new DMatrixRMaj(%d, %d);";
        final String formatSet = "%s.set(new double%s {{";
        final String postFix = "});";
        String brackets = "[]";
        if (executor.getOutput().matrix.numCols > 1) {
        	brackets = "[][]";
        }
        sb.append( String.format(formatDecl, executor.getOutput().getName(), executor.getOutput().matrix.numRows, executor.getOutput().matrix.numCols));
        sb.append( String.format(formatSet, executor.getOutput().getName(), brackets ));
        for (Item item : items) {
        	if (item.endRow) {
        		sb.deleteCharAt(sb.length()-1);
        		sb.append("},{");
        	} else {
        		if( item.variable.getType() == VariableType.SCALAR ){
        			sb.append(String.format("%s, ", item.variable.getOperand()));
                } else if( item.variable.getType() == VariableType.INTEGER_SEQUENCE ) {
                    IntegerSequence sequence = ((VariableIntegerSequence)item.variable).sequence;
                    //int col = numCols;
                    while( sequence.hasNext() ) {
                    	sb.append(String.format("%d,", sequence.next()));
                    }
                }        	
        	}
        }
        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
		sb.append(postFix);		
	}

}
