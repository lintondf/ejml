package org.ejml.equation;

import java.util.List;

import org.ejml.equation.Operation.Info;

public interface IOperationFactory {

	Info multiply(Variable A, Variable B, ManagerTempVariables manager);

	Info divide(Variable A, Variable B, ManagerTempVariables manager);

	/**
	 * Returns the negative of the input variable
	 */
	Info neg(Variable A, ManagerTempVariables manager);

	Info pow(Variable A, Variable B, ManagerTempVariables manager);

	Info atan2(Variable A, Variable B, ManagerTempVariables manager);

	Info sqrt(Variable A, ManagerTempVariables manager);

	Info sin(Variable A, ManagerTempVariables manager);

	Info cos(Variable A, ManagerTempVariables manager);

	Info atan(Variable A, ManagerTempVariables manager);

	Info exp(Variable A, ManagerTempVariables manager);

	Info log(Variable A, ManagerTempVariables manager);

	Info add(Variable A, Variable B, ManagerTempVariables manager);

	Info subtract(Variable A, Variable B, ManagerTempVariables manager);

	Info elementMult(Variable A, Variable B, ManagerTempVariables manager);

	Info elementDivision(Variable A, Variable B, ManagerTempVariables manager);

	Info elementPow(Variable A, Variable B, ManagerTempVariables manager);

	Operation copy(Variable src, Variable dst);

	Operation copy(Variable src, Variable dst, List<Variable> range);

	Info transpose(Variable A, ManagerTempVariables manager);

	/**
	 * Matrix inverse
	 */
	Info inv(Variable A, ManagerTempVariables manager);

	/**
	 * Matrix pseudo-inverse
	 */
	Info pinv(Variable A, ManagerTempVariables manager);

	Info rref(Variable A, ManagerTempVariables manager);

	/**
	 * Matrix determinant
	 */
	Info det(Variable A, ManagerTempVariables manager);

	Info trace(Variable A, ManagerTempVariables manager);

	Info normF(Variable A, ManagerTempVariables manager);

	Info normP(Variable A, Variable P, ManagerTempVariables manager);

	Info max(Variable A, ManagerTempVariables manager);

	Info max_two(Variable A, Variable P, ManagerTempVariables manager);

	Info min(Variable A, ManagerTempVariables manager);

	Info min_two(Variable A, Variable P, ManagerTempVariables manager);

	Info abs(Variable A, ManagerTempVariables manager);

	/**
	 * Returns an identity matrix
	 */
	Info eye(Variable A, ManagerTempVariables manager);

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
	 * If input is two vectors then it returns the dot product as a double.
	 */
	Info solve(Variable A, Variable B, ManagerTempVariables manager);

	Info extract(List<Variable> inputs, ManagerTempVariables manager);

	Info sum_one(Variable A, ManagerTempVariables manager);

	Info sum_two(Variable A, Variable P, ManagerTempVariables manager);

	Info extractScalar(List<Variable> inputs, ManagerTempVariables manager);

	Info matrixConstructor(MatrixConstructor m);

}