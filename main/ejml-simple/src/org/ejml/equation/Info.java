/**
 * 
 */
package org.ejml.equation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author lintondf
 *
 */
public class Info {

	public enum DimensionSources {
		LHS_ROWS,
		LHS_COLS,
		RHS_ROWS,
		RHS_COLS
	};
	
    public Operation op;
    public Variable output;
    public List<Variable> input;
    public MatrixConstructor constructor;
    public List<DimensionSources> dimensions;
	public List<Variable> range;
    
    public Info() {
    	input = new ArrayList<Variable>();
    	dimensions = new ArrayList<DimensionSources>();
    	range = null;
    }
    
    public void setOperation() {} 
    
    public void setOperation(final Info.Operation operation ) {
    	this.op = operation;
    }
    
    public Info( final Variable A) {
    	this();
    	input.add(A);
    }

    public Info(final Variable A, final Variable B) {
    	this();
    	input.add(A);
    	input.add(B);
    }
    public Info(final List<Variable> inputs) {
    	this();
    	input.addAll(inputs);
    }
    
    public Info(final MatrixConstructor m) {
    	this();
    	constructor = m;
    }
    
    public void addDimension( final DimensionSources source ) {
    	dimensions.add(source);
    }
    
    public Variable A() {
    	return input.get(0);
    }

    public Variable B() {
    	return input.get(1);
    }
    
    public VariableInteger outputInteger() {
    	return (VariableInteger) output;
    }

    public VariableDouble outputDouble() {
    	return (VariableDouble) output;
    }

    public VariableMatrix outputMatrix() {
    	return (VariableMatrix) output;
    }

    /**
     * Check if this operation uses the specified variable.
     * @param variable
     * @return
     */
    public boolean uses(Variable variable) {
    	if (input.contains(variable))
    		return true;
    	if (output != null && output.equals(variable))
    		return true;
    	return false;
    }
    
    /** 
     * Replace occurences of one variable with another.
     * 
     * Supports reuse of temporaries during optimization.
     * 
     * @param original
     * @param replacement
     * @return true if one or more occurences replaced
     */
    public boolean replace(Variable original, Variable replacement) {
    	boolean found = false;
    	for (int i = 0; i < input.size(); i++) {
    		if (input.get(i) == original) {
    			input.set(i,  replacement);
    			found = true;
    		}
    	}
    	if (output != null && output.equals(original)) {
    		output = replacement;
    		found = true;
    	}
    	if (constructor != null && constructor.output != null && constructor.output.equals(original)) {
    		constructor.output = (VariableMatrix) replacement;
    		found = true;
    	}
    	return found;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append( (this.op != null) ? this.op.name : "<No-Operation>" );
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
    	if (range != null) {
        	sb.append("<");
        	for (Variable var : range) {
        		sb.append(var.getName());
        		sb.append(":");
        		sb.append(var.getType().toString());
        		sb.append(",");
        	}
        	sb.deleteCharAt(sb.length()-1);
        	sb.append(">");    		
    	}
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

    public abstract class Operation {

        String name;

        protected Operation(String name) {
            this.name = name;
        }

        public abstract void process();

        public String name() {
            return name;
        }
        
    }

}
