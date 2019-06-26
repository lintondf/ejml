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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable for storing primitive scalar data types, e.g. int and double.
 *
 * @author Peter Abeles
 */
public abstract class VariableScalar extends Variable {

	static final Pattern expression = Pattern.compile("[Integer|Double]\\{(.*)\\}");
	static final Pattern letter = Pattern.compile("[a-zA-Z]");
	
    Type type;

    public VariableScalar(Type type, String name) {
        super(VariableType.SCALAR, name);
        this.type = type;
    }

    public abstract double getDouble();

    @Override
    public String toString() {
        switch( type ) {
            case INTEGER:
                return name + " : ScalarI";
            case DOUBLE:
                return name + " : ScalarD";
            case COMPLEX:
                return name + " : ScalarC";
            default:
                return name + " : ScalarUnknown";
        }
    }

    public Type getScalarType() {
        return type;
    }

    public enum Type {
        INTEGER,
        DOUBLE,
        COMPLEX
    }
    
    /**
     * Check if variable is a constant or constant expression.
     * 
     * Constant names are either Integer{*} or Double{*}
     * If there are any letters between the curly braces, then it is not a constant expression.
     */
    @Override
    public boolean isConstant() {
    	Matcher matcher = expression.matcher( this.getName() );
    	if (matcher.find()) {
    		matcher = letter.matcher(matcher.group(1));
    		return ! matcher.find();
    	}
    	return false;
    }

    @Override
	public String getOperand() {
		if (this.getName().endsWith("}")) {
			int i = this.getName().indexOf('{');
			int j = this.getName().indexOf('}');
			return this.getName().substring(i+1, j); // text as specified in equation
		} else if (this.getName().isEmpty()) {
			return Double.toString(this.getDouble());
		} else {
			return this.getName();  // name of scalar variable
		}
	}
    
    public static void main(String[] args) {
    	
    	String[] tests = {
    			"zot",
    			"Integer{1}",
    			"Integer{i + 2*4}",
    			"Integer{i + 2*4 + j}",
    			"Double{1}",
    			"Double{i + 2*4}",
    			"Double{i + 2*4 + j}",
    	};
    	
    	for (String test : tests) {
    		Matcher matcher = expression.matcher(test);
    		if (matcher.find()) {
    			Matcher m2 = letter.matcher(matcher.group(1));
    			System.out.printf("%s FOUND %s %b\n", test, matcher.group(1), m2.find());
    		} else {
    			System.out.printf("%s NOT FOUND\n", test);
    		}
    	}
    }
}
