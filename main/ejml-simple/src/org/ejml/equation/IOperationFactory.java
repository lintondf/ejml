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

import java.util.List;

import org.ejml.equation.Operation.Info;

/** Interface derived from original Operation.java
 * 
 * @author D. F. Linton, Blue Lightning Development, LLC 2019.
 */
public interface IOperationFactory {

	/**
	 * Multiply two variables
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info multiply(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Divide A by B (A / B)
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info divide(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Returns the negative of the input variable
	 */
	Info neg(Variable A, ManagerTempVariables manager);

	/**
	 * Raise A to the B power
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info pow(Variable A, Variable B, ManagerTempVariables manager);

	/** 
	 * Arctangent of A / B
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info atan2(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Square root
	 * @param A
	 * @param manager
	 * @return
	 */
	Info sqrt(Variable A, ManagerTempVariables manager);

	/**
	 * Sine of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info sin(Variable A, ManagerTempVariables manager);

	/**
	 * Cosine of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info cos(Variable A, ManagerTempVariables manager);

	/** 
	 * Arctangent of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info atan(Variable A, ManagerTempVariables manager);

	/**
	 * e to the power of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info exp(Variable A, ManagerTempVariables manager);

	/**
	 * Natural log of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info log(Variable A, ManagerTempVariables manager);

	/**
	 * Add A and B
	 */
	Info add(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Subtract B from A (A - B)
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info subtract(Variable A, Variable B, ManagerTempVariables manager);
	
	/**
	 * Multiply each element of A by the corresponding element of B
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info elementMult(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Divide each element of A by the corresponding element of B
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info elementDivision(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Raise each element of A to the power of the corresponding element of B
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info elementPow(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Copy src to dst
	 * @param src
	 * @param dst
	 * @return
	 */
	Operation copy(Variable src, Variable dst);

	/**
	 * Copy src into the submatrix of dst defined by range
	 * @param src
	 * @param dst
	 * @param range
	 * @return
	 */
	Operation copy(Variable src, Variable dst, List<Variable> range);

	/**
	 * Transpose A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info transpose(Variable A, ManagerTempVariables manager);

	/**
	 * Matrix inverse
	 */
	Info inv(Variable A, ManagerTempVariables manager);

	/**
	 * Matrix pseudo-inverse
	 */
	Info pinv(Variable A, ManagerTempVariables manager);

	/**
	 * Put matrix A into reduced row echelon form (RREF)
	 * @param A
	 * @param manager
	 * @return
	 */
	Info rref(Variable A, ManagerTempVariables manager);

	/**
	 * Matrix determinant
	 */
	Info det(Variable A, ManagerTempVariables manager);

	/**
	 * Matrix trace
	 * @param A
	 * @param manager
	 * @return
	 */
	Info trace(Variable A, ManagerTempVariables manager);

	/**
	 * Frobenius matrix norm of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info normF(Variable A, ManagerTempVariables manager);

	/**
	 * P-norm of A
	 * @param A
	 * @param P
	 * @param manager
	 * @return
	 */
	Info normP(Variable A, Variable P, ManagerTempVariables manager);

	/**
	 * Return the maximum element of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info max(Variable A, ManagerTempVariables manager);

	/**
	 * Return the maximum value in matrix A rows or columns
	 * @param A
	 * @param P - 0: rows; 1: columns
	 * @param manager
	 * @return
	 */
	Info max_two(Variable A, Variable P, ManagerTempVariables manager);

	/**
	 * Return the minimum element of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info min(Variable A, ManagerTempVariables manager);

	/**
	 * Return the minimum value in matrix A rows or columns
	 * @param A
	 * @param P - 0: rows; 1: columns
	 * @param manager
	 * @return
	 */
	Info min_two(Variable A, Variable P, ManagerTempVariables manager);

	/**
	 * Element-wise absolute value of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info abs(Variable A, ManagerTempVariables manager);

	/**
	 * Returns an identity matrix
	 */
	Info eye(Variable A, ManagerTempVariables manager);

	/**
	 * Returns vector of the diagonal elements of A
	 * @param A
	 * @param manager
	 * @return
	 */
	Info diag(Variable A, ManagerTempVariables manager);

	/**
	 * Returns a matrix full of zeros
	 */
	Info zeros(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Returns a matrix full of ones
	 */
	Info ones(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Sets the seed for random number generator
	 */
	Info rng(Variable A, ManagerTempVariables manager);

	/**
	 * Uniformly random numbers
	 */
	Info rand(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Normal distrbution numbers
	 */
	Info randn(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Kronecker product
	 */
	Info kron(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * If input is two vectors then it returns the dot product as a double.
	 */
	Info dot(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Solve the system A, B
	 * @param A
	 * @param B
	 * @param manager
	 * @return
	 */
	Info solve(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Extract a submatrix of a matrix
	 * @param inputs
	 * @param manager
	 * @return
	 */
	Info extract(List<Variable> inputs, ManagerTempVariables manager);

	/**
	 * Element-wise sum of the a matrix
	 * @param A
	 * @param manager
	 * @return
	 */
	Info sum_one(Variable A, ManagerTempVariables manager);

	/**
	 * Row-wise or column-wise sum of a matrix
	 * @param A
	 * @param P - 0: rows; 1: cols
	 * @param manager
	 * @return
	 */
	Info sum_two(Variable A, Variable P, ManagerTempVariables manager);

	/**
	 * Extract a scalar value from a matrix
	 * @param inputs
	 * @param manager
	 * @return
	 */
	Info extractScalar(List<Variable> inputs, ManagerTempVariables manager);

	/**
	 * Construct a matrix
	 * @param m
	 * @return
	 */
	Info matrixConstructor(MatrixConstructor m);

}