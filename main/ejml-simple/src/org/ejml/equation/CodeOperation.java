package org.ejml.equation;

import java.util.ArrayList;
import java.util.List;

import org.ejml.equation.Operation.Info;

public class CodeOperation extends Operation {

	public enum DimensionSources {
		LHS_ROWS,
		LHS_COLS,
		RHS_ROWS,
		RHS_COLS
	};
	
	
	public static class CodeInfo extends Info {
	    public List<Variable> input;
	    public MatrixConstructor constructor;
	    public List<DimensionSources> dimensions;
	    
	    public CodeInfo() {
	    	input = new ArrayList<Variable>();
	    	dimensions = new ArrayList<DimensionSources>();
	    }
	    
	    public CodeInfo( final Variable A) {
	    	this();
	    	input.add(A);
	    }

	    public CodeInfo(final Variable A, final Variable B) {
	    	this();
	    	input.add(A);
	    	input.add(B);
	    }
	    public CodeInfo(final List<Variable> inputs) {
	    	this();
	    	input.addAll(inputs);
	    }
	    
	    public CodeInfo(final MatrixConstructor m) {
	    	this();
	    	constructor = m;
	    }
	    
	    public void addDimension( final DimensionSources source ) {
	    	dimensions.add(source);
	    }
	    
	    public String toString() {
	    	return "CodeInfo";
	    }
	}
	
	
    public List<Variable> input;
    public Variable       output;
    public MatrixConstructor constructor;
    public List<DimensionSources> dimensions;

    protected CodeOperation(String name, CodeInfo info) {
        super(name);
        input = info.input;
        output = info.output;
        constructor = info.constructor;
        dimensions = info.dimensions;
    }

    public void process() {}
    
    public boolean uses(Variable variable) {
    	if (input.contains(variable))
    		return true;
    	if (output != null && output.equals(variable))
    		return true;
    	return false;
    }
    
    public boolean replace(Variable original, Variable replacement) {
    	boolean found = false;
    	for (int i = 0; i < input.size(); i++) {
    		if (input.get(i) == original) {
    			input.set(i,  replacement);
    			found = true;
    		}
    	}
//    	if (input.contains(original)) {
//    		input.remove(original);
//    		input.add(replacement);
//    		found = true;
//    	}
    	if (output != null && output.equals(original)) {
    		output = replacement;
    		found = true;
    	}
    	return found;
    }
    
    
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(this.name );
    	if (constructor != null) {
    		sb.append("(");
    		sb.append(this.constructor.toString());
    		sb.append(") ");
    	}
    	sb.append("[");
    	for (Variable var : input) {
    		sb.append(var.getName());
    		sb.append(":");
    		sb.append(var.getType().toString());
    		sb.append(",");
    	}
    	sb.deleteCharAt(sb.length()-1);
    	sb.append("]");
    	if (output != null) {
    		sb.append("->");
    		sb.append(output.getName());
    		sb.append(":");
    		sb.append(output.getType().toString());
    		if (! dimensions.isEmpty()) {
    	    	sb.append("[");
    	    	for (DimensionSources source : this.dimensions) {
    	    		sb.append(source.toString());
    	    		sb.append(",");
    	    	}
    	    	sb.deleteCharAt(sb.length()-1);
    	    	sb.append("]");    			
    		}
    	}
    	return sb.toString();
    }

}