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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.ejml.MatrixDimensionException;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.Matrix;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.dense.row.mult.VectorVectorMult_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.simple.SimpleMatrix;

//import org.ejml.equation.Operation.Info;
import org.ejml.equation.Info;
import org.ejml.equation.Info.Operation;
import org.ejml.equation.Info.DimensionSources;


/** Implement IOperationFactory for compilation of EJML equations to procedural java
 * 
 * @author D. F. Linton, Blue Lightning Development, LLC 2019.
 */
public class OperationCodeFactory implements IOperationFactory {
	
	protected class CodeOperation extends Operation {

		protected CodeOperation(String name, Info info) {
			info.super(name);
		}

		@Override
		public void process() {
		}
		
	}

	/**
	 * 
	 */
	public OperationCodeFactory() {
	}

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#multiply(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info multiply(final Variable A, final Variable B, ManagerTempVariables manager) {

        Info ret = new Info(A, B);

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            VariableMatrix mA = (VariableMatrix)A;
            VariableMatrix mB = (VariableMatrix)B;
            ret.addDimension(DimensionSources.LHS_ROWS);
            ret.addDimension(DimensionSources.RHS_COLS);
            ret.output = output;
            ret.op = new CodeOperation("multiply-mm", ret);
        } else if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new CodeOperation("multiply-ii", ret);
        } else if( A instanceof VariableScalar && B instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("multiply-ss", ret);
        } else {
            final Variable output = manager.createMatrix();
            ret.output = output;
            final Variable m;
            final VariableScalar s;

            if( A instanceof VariableMatrix ) {
                ret.addDimension(DimensionSources.LHS_ROWS);
                ret.addDimension(DimensionSources.LHS_COLS);
                m = (Variable)A;
                s = (VariableScalar)B;
            } else {
                ret.addDimension(DimensionSources.RHS_ROWS);
                ret.addDimension(DimensionSources.RHS_COLS);
                m = (Variable)B;
                s = (VariableScalar)A;
            }
            ret.input.clear();
            ret.input.add(m);
            ret.input.add(s);

            ret.op = new CodeOperation("multiply-ms", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#divide(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info divide(final Variable A, final Variable B, ManagerTempVariables manager) {

        Info ret = new Info(A, B);

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            return solve(B,A,manager);
        } else if( A instanceof VariableMatrix && B instanceof VariableScalar ) {
            final Variable output = manager.createMatrix();
            final Variable m = (Variable)A;
            final VariableScalar s = (VariableScalar)B;
            ret.output = output;
            ret.op = new CodeOperation("divide-ms", ret);
        } else if( A instanceof VariableScalar && B instanceof VariableMatrix ) {
            final Variable output = manager.createMatrix();
            final Variable m = (Variable)B;
            final VariableScalar s = (VariableScalar)A;
            ret.output = output;
            ret.input.clear();
            ret.input.add(m);
            ret.input.add(s);
            ret.op = new CodeOperation("divide-sm", ret);
        } else if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new CodeOperation("divide-ii", ret);
        } else {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("divide-ss", ret);
        }
        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#neg(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info neg(final Variable A, ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableInteger  ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new CodeOperation("neg-i", ret);
        } else if( A instanceof VariableScalar  ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("neg-s", ret);
        } else if( A instanceof VariableMatrix  ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("neg-m", ret);
        } else {
            throw new RuntimeException("Unsupported variable "+A);
        }
        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#pow(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info pow(final Variable A, final Variable B, ManagerTempVariables manager) {
        Info ret = new Info(A, B);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar && B instanceof VariableScalar ) {

            ret.op = new CodeOperation("pow-ss", ret);
        } else {
            throw new RuntimeException("Only scalar to scalar power supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#atan2(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info atan2(final Variable A, final Variable B, ManagerTempVariables manager) {
        Info ret = new Info(A, B);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar && B instanceof VariableScalar ) {

            ret.op = new CodeOperation("atan2-ss", ret);
        } else {
            throw new RuntimeException("Only scalar to scalar atan2 supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#sqrt(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info sqrt(final Variable A, ManagerTempVariables manager) {
        Info ret = new Info(A);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar  ) {

            ret.op = new CodeOperation("sqrt-s", ret);
        } else {
            throw new RuntimeException("Only scalars are supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#sin(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info sin(final Variable A, ManagerTempVariables manager) {
        Info ret = new Info(A);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar  ) {

            ret.op = new CodeOperation("sin-s", ret);
        } else {
            throw new RuntimeException("Only scalars are supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#cos(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info cos(final Variable A, ManagerTempVariables manager) {
        Info ret = new Info(A);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar  ) {

            ret.op = new CodeOperation("cos-s", ret);
        } else {
            throw new RuntimeException("Only scalars are supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#atan(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info atan(final Variable A, ManagerTempVariables manager) {
        Info ret = new Info(A);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar  ) {

            ret.op = new CodeOperation("atan-s", ret);
        } else {
            throw new RuntimeException("Only scalars are supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#exp(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info exp(final Variable A, ManagerTempVariables manager) {
        final Info ret = new Info(A);


        if( A instanceof VariableScalar  ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("exp-s", ret);
        } else if( A instanceof VariableMatrix  ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("exp-m", ret);
        } else {
            throw new RuntimeException("Only scalars or matrices are supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#log(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info log(final Variable A, ManagerTempVariables manager) {
        final Info ret = new Info(A);

        if( A instanceof VariableScalar  ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("log-s", ret);
        } else if( A instanceof VariableMatrix  ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("log-m", ret);
        } else {
            throw new RuntimeException("Only scalars or matrices are supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#add(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info add(final Variable A, final Variable B, ManagerTempVariables manager) {
        Info ret = new Info(A,B);

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.addDimension(DimensionSources.LHS_ROWS);
            ret.addDimension(DimensionSources.RHS_COLS);
            ret.output = output;
            ret.op = new CodeOperation("add-mm", ret);
        } else if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new CodeOperation("add-ii", ret);
        } else if( A instanceof VariableScalar && B instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("add-ss", ret);
        } else {
            final Variable output = manager.createMatrix();
            ret.output = output;

            if( A instanceof VariableMatrix ) {
                ret.op = new CodeOperation("add-ms", ret);
            } else {
                ret.op = new CodeOperation("add-sm", ret);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#subtract(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info subtract(final Variable A, final Variable B, ManagerTempVariables manager) {
        Info ret = new Info(A,B);

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            VariableMatrix mA = (VariableMatrix)A;
            Variable mB = (Variable)B;

            ret.addDimension(DimensionSources.LHS_ROWS);
            ret.addDimension(DimensionSources.LHS_COLS);
            ret.output = output;
            ret.op = new CodeOperation("subtract-mm", ret);
        } else if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new CodeOperation("subtract-ii", ret);
        } else if( A instanceof VariableScalar && B instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("subtract-ss", ret);
        } else {
            final Variable output = manager.createMatrix();
            ret.output = output;

            if( A instanceof VariableMatrix ) {
                ret.op = new CodeOperation("subtract-ms", ret);
            } else {
                ret.op = new CodeOperation("subtract-sm", ret);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#elementMult(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info elementMult( final Variable A , final Variable B , ManagerTempVariables manager ) {
        Info ret = new Info(A,B);

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            VariableMatrix mA = (VariableMatrix)A;
            Variable mB = (Variable)B;

            ret.addDimension(DimensionSources.LHS_ROWS);
            ret.addDimension(DimensionSources.LHS_COLS);
            ret.output = output;
            ret.op = new CodeOperation("elementMult-mm", ret);
        } else {
            throw new RuntimeException("Both inputs must be matrices for element wise multiplication");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#elementDivision(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info elementDivision( final Variable A , final Variable B , ManagerTempVariables manager ) {
        Info ret = new Info(A,B);

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            VariableMatrix mA = (VariableMatrix)A;
            Variable mB = (Variable)B;

            ret.addDimension(DimensionSources.LHS_ROWS);
            ret.addDimension(DimensionSources.LHS_COLS);
            ret.output = output;
            ret.op = new CodeOperation("elementDivision-mm", ret);
        } else {
            throw new RuntimeException("Both inputs must be matrices for element wise multiplication");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#elementPow(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager) {
        Info ret = new Info(A,B);


        if( A instanceof VariableScalar && B instanceof VariableScalar ) {

            final VariableDouble output = manager.createDouble();
            ret.output = output;

            ret.op = new CodeOperation("elementPow-ss", ret);
        } else if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {

            final VariableMatrix output = manager.createMatrix();
            DMatrixRMaj a = ((VariableMatrix) A).matrix;
            DMatrixRMaj b = ((VariableMatrix) B).matrix;

            ret.addDimension(DimensionSources.LHS_ROWS);
            ret.addDimension(DimensionSources.LHS_COLS);
            ret.output = output;

            ret.op = new CodeOperation("elementPow-mm", ret);
        } else if( A instanceof VariableMatrix && B instanceof VariableScalar ) {

            final VariableMatrix output = manager.createMatrix();
            DMatrixRMaj a = ((VariableMatrix) A).matrix;

            ret.addDimension(DimensionSources.LHS_ROWS);
            ret.addDimension(DimensionSources.LHS_COLS);
            ret.output = output;

            ret.op = new CodeOperation("elementPow-ms", ret);
        } else if( A instanceof VariableScalar && B instanceof VariableMatrix ) {

            final VariableMatrix output = manager.createMatrix();
            DMatrixRMaj b = ((VariableMatrix) B).matrix;

            ret.addDimension(DimensionSources.RHS_ROWS);
            ret.addDimension(DimensionSources.RHS_COLS);
            ret.output = output;

            ret.op = new CodeOperation("elementPow-sm", ret);
        } else {
            throw new RuntimeException("Unsupport element-wise power input types");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#copy(org.ejml.equation.Variable, org.ejml.equation.Variable)
	 */
    @Override
	public Info copy( final Variable src , final Variable dst ) {
    	Info ret = new Info(src);
    	ret.output = dst;

        if( src instanceof VariableMatrix  ) {
            if( dst instanceof VariableMatrix ) {
            	ret.op = new CodeOperation("copy-mm", ret);
                return ret;
            } else if( dst instanceof VariableDouble ) {
            	ret.op = new CodeOperation("copy-sm1", ret);
                return ret;
            }
        }
        if( src instanceof VariableInteger && dst instanceof VariableInteger ) {
        	ret.op = new CodeOperation("copy-ii", ret);
            return ret;
        }
        if( src instanceof VariableScalar && dst instanceof VariableDouble ) {
        	ret.op = new CodeOperation("copy-ss", ret);
            return ret;
        }

        if( src instanceof VariableIntegerSequence ) {
            if( dst instanceof VariableIntegerSequence ) {
            	ret.op = new CodeOperation("copy-is-is", ret);
                return ret;
            }
        }

        throw new RuntimeException("Unsupported copy types; src = "+src.getClass().getSimpleName()+" dst = "+dst.getClass().getSimpleName());
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#copy(org.ejml.equation.Variable, org.ejml.equation.Variable, java.util.List)
	 */
    @Override
	public Info copy( final Variable src , final Variable dst , final List<Variable> range ) {
    	Info ret = new Info(src);
    	ret.range = range;
    	ret.output = dst;

    	if( src instanceof VariableMatrix && dst instanceof VariableMatrix ) {
    		ret.op = new CodeOperation("copyR-mm", ret);
            return ret;
        } else if( src instanceof VariableScalar && dst instanceof VariableMatrix ) {
        	ret.op = new CodeOperation("copyR-sm", ret);
            return ret;
        } else {
            throw new RuntimeException("Both variables must be of type VariableMatrix");
        }
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#transpose(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info transpose( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableMatrix ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("transpose-m", ret);
        } else {
            throw new RuntimeException("Transpose only makes sense for a matrix");
        }
        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#inv(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info inv( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableMatrix ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("inv-m", ret);
        } else {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("inv-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#pinv(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info pinv( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableMatrix ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("pinv-m", ret);
        } else {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("pinv-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#rref(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info rref( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableMatrix ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("rref-m", ret);
        } else {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("rref-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#det(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info det( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableMatrix ) {
            ret.op = new CodeOperation("det-m", ret);
        } else {
            ret.op = new CodeOperation("det-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#trace(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info trace( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableMatrix ) {
            ret.op = new CodeOperation("trace-m", ret);
        } else {
            ret.op = new CodeOperation("trace-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#normF(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info normF( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableMatrix ) {
            ret.op = new CodeOperation("normF-m", ret);
        } else {
            ret.op = new CodeOperation("normF-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#normP(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info normP( final Variable A , final Variable P , ManagerTempVariables manager) {
        Info ret = new Info(A,P);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( !(A instanceof VariableMatrix) || !(P instanceof VariableScalar) )
            throw new RuntimeException("normP(A,p) A should be a matrix and p a scalar");

        final double valueP = ((VariableScalar)P).getDouble();
        final Variable varA = (Variable)A;

        ret.op = new CodeOperation("normP", ret);

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#max(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info max( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableMatrix ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("max-m", ret);
        } else if( A instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new CodeOperation("max-i", ret);
        } else if( A instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("max-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#max_two(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info max_two( final Variable A , final Variable P , ManagerTempVariables manager) {
        Info ret = new Info(A,P);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( !(A instanceof VariableMatrix) || !(P instanceof VariableScalar) )
            throw new RuntimeException("max(A,d) A = matrix and d = scalar");

        final double valueP = ((VariableScalar)P).getDouble();
        final Variable varA = (Variable)A;

        if( valueP == 0 ) {
            ret.op = new CodeOperation("max_rows", ret);
        } else if( valueP == 1 ){
            ret.op = new CodeOperation("max_cols", ret);
        } else {
            throw new RuntimeException("max(A,d) expected d to be 0 for rows or 1 for columns");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#min(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info min( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableMatrix ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("min-m", ret);
        } else if( A instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new CodeOperation("min-i", ret);
        } else if( A instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("min-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#min_two(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info min_two( final Variable A , final Variable P , ManagerTempVariables manager) {
        Info ret = new Info(A);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( !(A instanceof VariableMatrix) || !(P instanceof VariableScalar) )
            throw new RuntimeException("min(A,d) A = matrix and d = scalar");

        final double valueP = ((VariableScalar)P).getDouble();
        final Variable varA = (Variable)A;

        if( valueP == 0 ) {
            ret.op = new CodeOperation("min_rows", ret);
        } else if( valueP == 1 ){
            ret.op = new CodeOperation("min_cols", ret);
        } else {
            throw new RuntimeException("min(A,d) expected d to be 0 for rows or 1 for columns");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#abs(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info abs( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableMatrix ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("abs-m", ret);
        } else if( A instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new CodeOperation("abs-i", ret);
        } else if( A instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new CodeOperation("abs-s", ret);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#eye(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info eye( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableMatrix ) {
            ret.op = new CodeOperation("eye-m", ret);
        } else if( A instanceof VariableInteger ) {
            ret.op = new CodeOperation("eye-i", ret);
        } else {
            throw new RuntimeException("Unsupported variable type "+A);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#diag(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info diag( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);

        if( A instanceof VariableMatrix ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new CodeOperation("diag-m", ret);
        } else {
            throw new RuntimeException("diag requires a matrix as input");
        }
        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#zeros(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info zeros( final Variable A , final Variable B , ManagerTempVariables manager) {
        Info ret = new Info(A,B);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            ret.op = new CodeOperation("zeros-ii", ret);
        } else {
            throw new RuntimeException("Expected two integers got "+A+" "+B);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#ones(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info ones( final Variable A , final Variable B , ManagerTempVariables manager) {
        Info ret = new Info(A,B);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            ret.op = new CodeOperation("ones-ii", ret);
        } else {
            throw new RuntimeException("Expected two integers got "+A+" "+B);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#rng(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info rng( final Variable A , ManagerTempVariables manager) {

        Info ret = new Info(A);

        if( A instanceof VariableInteger ) {
            ret.op = new CodeOperation("rng", ret);
        } else {
            throw new RuntimeException("Expected one integer");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#rand(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info rand( final Variable A , final Variable B , ManagerTempVariables manager) {
        Info ret = new Info(A,B);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            ret.op = new CodeOperation("rand-ii", ret);
        } else {
            throw new RuntimeException("Expected two integers got "+A+" "+B);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#randn(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info randn( final Variable A , final Variable B , ManagerTempVariables manager) {
        Info ret = new Info(A,B);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            ret.op = new CodeOperation("randn-ii", ret);
        } else {
            throw new RuntimeException("Expected two integers got "+A+" "+B);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#kron(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info kron( final Variable A , final Variable B, ManagerTempVariables manager) {
        Info ret = new Info(A,B);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            ret.op = new CodeOperation("kron-mm", ret);
        } else {
            throw new RuntimeException("Both inputs must be matrices ");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#dot(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info dot( final Variable A , final Variable B , ManagerTempVariables manager) {
        Info ret = new Info(A,B);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            ret.op = new CodeOperation("dot-mm", ret);
        } else {
            throw new RuntimeException("Expected two matrices got "+A+" "+B);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#solve(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info solve( final Variable A , final Variable B , ManagerTempVariables manager) {
        Info ret = new Info(A,B);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            ret.op = new CodeOperation("solve-mm", ret);
        } else {
            throw new RuntimeException("Expected two matrices got "+A+" "+B);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#extract(java.util.List, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info extract( final List<Variable> inputs, ManagerTempVariables manager) {
        Info ret = new Info( inputs );
        final Variable output = manager.createMatrix();
        ret.output = output;

        if(  !(inputs.get(0) instanceof VariableMatrix))
            throw new RuntimeException("First parameter must be a matrix.");


        for (int i = 1; i < inputs.size(); i++) {
            if( !(inputs.get(i) instanceof VariableInteger) &&
                    (inputs.get(i).getType() != VariableType.INTEGER_SEQUENCE))
                throw new RuntimeException("Parameters must be integers, integer list, or array range");
        }

        ret.op = new CodeOperation("extract", ret);

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#sum_one(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info sum_one( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info(A);
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( !(A instanceof VariableMatrix)  )
            throw new RuntimeException("sum(A) A = matrix");

        final Variable varA = (Variable)A;

        ret.op = new CodeOperation("sum_all", ret);

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#sum_two(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info sum_two( final Variable A , final Variable P , ManagerTempVariables manager) {
        Info ret = new Info(A,P);
        final Variable output = manager.createMatrix();
        ret.output = output;

        if( !(A instanceof VariableMatrix) || !(P instanceof VariableScalar) )
            throw new RuntimeException("sum(A,p) A = matrix and p = scalar");

        final double valueP = ((VariableScalar)P).getDouble();
        final Variable varA = (Variable)A;

        if( valueP == 0 ) {
            ret.op = new CodeOperation("sum_rows", ret);
        } else if( valueP == 1 ){
            ret.op = new CodeOperation("sum_cols", ret);
        } else {
            throw new RuntimeException("sum(A,d) expected d to be 0 for rows or 1 for columns");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#extractScalar(java.util.List, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info extractScalar( final List<Variable> inputs, ManagerTempVariables manager) {
        Info ret = new Info( inputs );
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if(  !(inputs.get(0) instanceof VariableMatrix))
            throw new RuntimeException("First parameter must be a matrix.");

        for (int i = 1; i < inputs.size(); i++) {
            if( !(inputs.get(i) instanceof VariableInteger) )
                throw new RuntimeException("Parameters must be integers for extract scalar");
        }

        ret.op = new CodeOperation("extractScalar", ret);

        return ret;
    }

//    /**
//     * See if a simple sequence can be used to extract the array.  A simple extent is a continuous block from
//     * a min to max index
//     *
//     * @return true if it is a simple range or false if not
//     */
//    private boolean extractSimpleExtents(Variable var, Extents extents, boolean row, int length) {
//        int lower;
//        int upper;
//        if( var.getType() == VariableType.INTEGER_SEQUENCE ) {
//            IntegerSequence sequence = ((VariableIntegerSequence)var).sequence;
//            if( sequence.getType() == IntegerSequence.Type.FOR ) {
//                IntegerSequence.For seqFor = (IntegerSequence.For)sequence;
//                seqFor.initialize(length);
//                if( seqFor.getStep() == 1 ) {
//                    lower = seqFor.getStart();
//                    upper = seqFor.getEnd();
//                } else {
//                    return false;
//                }
//            } else {
//                return false;
//            }
//        } else if( var.getType() == VariableType.SCALAR ) {
//            lower = upper = ((VariableInteger)var).value;
//        } else {
//            throw new RuntimeException("How did a bad variable get put here?!?!");
//        }
//        if( row ) {
//            extents.row0 = lower;
//            extents.row1 = upper;
//        } else {
//            extents.col0 = lower;
//            extents.col1 = upper;
//        }
//        return true;
//    }
//
//    private void extractArrayExtent( Variable var , int length , ArrayExtent extent ) {
//        if( var.getType() == VariableType.INTEGER_SEQUENCE ) {
//            IntegerSequence sequence = ((VariableIntegerSequence)var).sequence;
//            sequence.initialize(length-1);
//            extent.setLength(sequence.length());
//            int index = 0;
//            while( sequence.hasNext() ) {
//                extent.array[index++] = sequence.next();
//            }
//        } else if( var.getType() == VariableType.SCALAR ) {
//            extent.setLength(1);
//            extent.array[0] = ((VariableInteger)var).value;
//        } else {
//            throw new RuntimeException("How did a bad variable get put here?!?!");
//        }
//    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#matrixConstructor(org.ejml.equation.MatrixConstructor)
	 */
    @Override
	public Info matrixConstructor( final MatrixConstructor m ) {
        Info ret = new Info(m);
        ret.output = m.getOutput();

        ret.op = new CodeOperation("matrixConstructor", ret);

        return ret;
    }
    
}
