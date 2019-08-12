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
 * Instance of a variable created at compile time.  This base class only specifies the type of variable which it is.
 *
 * @author Peter Abeles
 */
public class Variable {
    public VariableType type;
    public String       name;
    int                 precedence = Precedence.MAX_PRECEDENCE;
    
	/**
	 * If true then the matrix is dynamically resized to match the output of a function
	 */
	public boolean temp;

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getOperand() {
		return name;
	}

	public void setOperand(String operand) {
		this.name = operand;
	}
	
	
	public int getPrecedence() {
		return precedence;
	}
	
	public void setPrecedence( int p ) {
		this.precedence = p;
	}
	
	protected Variable(VariableType type, String name) {
        this.type = type;
        this.name = name;
    }

    public VariableType getType() {
        return type;
    }

    public String toString() {
        return name + " : VAR_"  + type + ((temp) ? " TEMP" : "");
    }

	public boolean isTemp() {
	    return temp;
	}

	public void setTemp(boolean temp) {
	    this.temp = temp;
	}

	public boolean isConstant() {
		return false;
	}

	public boolean isSimple() {
		return false;
	}
}
