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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized place to create new instances of operations and functions.  Must call
 * {@link #setManagerTemp} before any other functions.
 *
 * @author Peter Abeles
 */
public class ManagerFunctions {
	
	IOperationFactory factory;

    // List of functions which take in N inputs
    Map<String,Input1> input1 = new HashMap<>();
    Map<String,InputN> inputN = new HashMap<>();

    // Reference to temporary variable manager
    protected ManagerTempVariables managerTemp;

    public ManagerFunctions() {
    	factory = new OperationExecuteFactory();
        addBuiltIn();
    }

    public ManagerFunctions(IOperationFactory factory) {
    	this.factory = factory;
        addBuiltIn();
    }

    public IOperationFactory getFactory() {
		return factory;
	}

	/**
     * Returns true if the string matches the name of a function
     */
    public boolean isFunctionName( String s ) {
        if( input1.containsKey(s))
            return true;
        if( inputN.containsKey(s))
            return true;

        return false;
    }

    /**
     * Create a new instance of single input functions
     * @param name function name
     * @param var0 Input variable
     * @return Resulting operation
     */
    public Info create( String name , Variable var0 ) {
        Input1 func = input1.get(name);
        if( func == null )
            return null;
        return func.create(var0,managerTemp);
    }

    /**
     * Create a new instance of single input functions
     * @param name function name
     * @param vars Input variables
     * @return Resulting operation
     */
    public Info create( String name , List<Variable> vars ) {
        InputN func = inputN.get(name);
        if( func == null )
            return null;
        return func.create(vars,managerTemp);
    }

    /**
     * Create a new instance of a single input function from an operator character
     * @param op Which operation
     * @param input Input variable
     * @return Resulting operation
     */
    public Info create( char op , Variable input ) {
        switch( op ) {
            case '\'':
                return factory.transpose(input, managerTemp);

            default:
                throw new RuntimeException("Unknown operation " + op);
        }
    }

    /**
     * Create a new instance of a two input function from an operator character
     * @param op Which operation
     * @param left Input variable on left
     * @param right Input variable on right
     * @return Resulting operation
     */
    public Info create( Symbol op , Variable left , Variable right ) {
        switch( op ) {
            case PLUS:
                return factory.add(left, right, managerTemp);

            case MINUS:
                return factory.subtract(left, right, managerTemp);

            case TIMES:
                return factory.multiply(left, right, managerTemp);

            case RDIVIDE:
                return factory.divide(left, right, managerTemp);

            case LDIVIDE:
                return factory.divide(right, left, managerTemp);

            case POWER:
                return factory.pow(left, right, managerTemp);

            case ELEMENT_DIVIDE:
                return factory.elementDivision(left, right, managerTemp);

            case ELEMENT_TIMES:
                return factory.elementMult(left, right, managerTemp);

            case ELEMENT_POWER:
                return factory.elementPow(left, right, managerTemp);

            default:
                throw new RuntimeException("Unknown operation " + op);
        }
    }

    /**
     *
     * @param managerTemp
     */

    public void setManagerTemp(ManagerTempVariables managerTemp) {
        this.managerTemp = managerTemp;
    }

    /**
     * Adds a function, with a single input, to the list
     * @param name Name of function
     * @param function Function factory
     */
    public void add1(String name , Input1 function ) {
       input1.put(name, function);
    }

    /**
     * Adds a function, with a two inputs, to the list
     * @param name Name of function
     * @param function Function factory
     */
    public void addN(String name , InputN function ) {
        inputN.put(name,function);
    }

    /**
     * Adds built in functions
     */
    private void addBuiltIn( ) {
        input1.put("inv", factory::inv);
        input1.put("pinv", factory::pinv);
        input1.put("rref", factory::rref);
        input1.put("eye", factory::eye);
        input1.put("det", factory::det);
        input1.put("normF", factory::normF);
        input1.put("sum", factory::sum_one);
        input1.put("trace", factory::trace);
        input1.put("diag", factory::diag);
        input1.put("min", factory::min);
        input1.put("max", factory::max);
        input1.put("abs", factory::abs);
        input1.put("sin", factory::sin);
        input1.put("cos", factory::cos);
        input1.put("atan", factory::atan);
        input1.put("exp", factory::exp);
        input1.put("log", factory::log);
        input1.put("sqrt", factory::sqrt);
        input1.put("rng", factory::rng);

        inputN.put("normP", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.normP(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("max", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("One or two inputs expected");
            return factory.max_two(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("min", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("One or two inputs expected");
            return factory.min_two(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("sum", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("One or two inputs expected");
            return factory.sum_two(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("zeros", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.zeros(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("ones", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.ones(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("rand", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.rand(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("randn", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.randn(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("kron", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.kron(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("dot", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.dot(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("pow", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.pow(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("atan2", (inputs, manager) -> {
            if (inputs.size() != 2) throw new RuntimeException("Two inputs expected");
            return factory.atan2(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("solve", (inputs, manager) -> {
            if( inputs.size() != 2 ) throw new RuntimeException("Two inputs expected");
            return factory.solve(inputs.get(0), inputs.get(1), manager);
        });

        inputN.put("extract", factory::extract) ; //Operation::extract);
        inputN.put("extractScalar", (inputs, manager) -> {
            if( inputs.size() != 2 && inputs.size() != 3 ) throw new RuntimeException("Two or three inputs expected");
            return factory.extractScalar(inputs, manager);
        });
    }

    public ManagerTempVariables getManagerTemp() {
        return managerTemp;
    }

    /**
     * Creates new instances of functions from a single variable
     */
    public static interface Input1 {
        Info create(Variable A, ManagerTempVariables manager);
    }

    /**
     * Creates a new instance of functions from two variables
     */
    public static interface InputN {
        Info create(List<Variable> inputs, ManagerTempVariables manager);
    }
}
