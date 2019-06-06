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

/**
 * Variable for storing primitive scalar data types, e.g. int and double.
 *
 * @author Peter Abeles
 */
public abstract class VariableScalar extends Variable {

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
    
    @Override
    public boolean isConstant() {
    	return this.getName().endsWith("}");
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
}
