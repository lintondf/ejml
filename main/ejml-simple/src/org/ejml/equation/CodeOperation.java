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

import java.util.ArrayList;
import java.util.List;

import org.ejml.equation.Operation.Info;

/** Extends Operation to support equation compilation to java source
 * 
 * @author D. F. Linton, Blue Lightning Development, LLC 2019.
 */
public class CodeOperation extends Operation {

	public enum DimensionSources {
		LHS_ROWS,
		LHS_COLS,
		RHS_ROWS,
		RHS_COLS
	};
	
	/**
	 * Support class used during CodeOperation build process in OperationCodeFactory.
	 * This class simplified the generation of the compilation factory from the 
	 * execution factory at the cost of some additional copying of variables and lists. 
	 */
	public static class CodeInfo extends Info {
	    public List<Variable> input;
	    public MatrixConstructor constructor;
	    public List<DimensionSources> dimensions;
		public List<Variable> range;
	    
	    public CodeInfo() {
	    	input = new ArrayList<Variable>();
	    	dimensions = new ArrayList<DimensionSources>();
	    	range = null;
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
	    
	}
	
	// input variables for this operation
    public List<Variable> input;
    // output variable for this operation
    public Variable       output;
    // matrix constructor for this operation
    public MatrixConstructor constructor;
    // sources of output dimensions for reshaping 
    public List<DimensionSources> dimensions;
    // sequences defining ranges for this operation
    public List<Variable> range;

    /**
     * Construct from a populated CodeInfo object
     * @param name
     * @param info
     */
    protected CodeOperation(String name, CodeInfo info) {
        super(name);
        input = info.input;
        output = info.output;
        constructor = info.constructor;
        dimensions = info.dimensions;
        range = info.range;
    }

    /**
     * Should never be called
     */
    public void process() {
    	throw new RuntimeException("CodeOperation does not support process() invocations.");
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

}