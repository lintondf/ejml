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

import org.ejml.equation.Operation.Info;

import java.util.Arrays;
import java.util.List;

public class OperationExecuteFactory implements IOperationFactory {

	/**
	 * 
	 */
	public OperationExecuteFactory() {
	}


    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#multiply(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info multiply(final Variable A, final Variable B, ManagerTempVariables manager) {

        Info ret = new Info();

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("multiply-mm") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    VariableMatrix mB = (VariableMatrix)B;

                    manager.resize(output,mA.matrix.numRows,mB.matrix.numCols);
                    try {
                        CommonOps_DDRM.mult(mA.matrix, mB.matrix, output.matrix);
                    } catch( MatrixDimensionException e ) {
                        // provide a more informative message if special case
                        checkThrow1x1AgainstNxM(mA.matrix,mB.matrix,"multiply");
                        throw e;
                    }
                }
            };
        } else if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new Operation("multiply-ii") {
                @Override
                public void process() {
                    VariableInteger mA = (VariableInteger)A;
                    VariableInteger mB = (VariableInteger)B;

                    output.value = mA.value*mB.value;
                }
            };
        } else if( A instanceof VariableScalar && B instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("multiply-ss") {
                @Override
                public void process() {
                    VariableScalar mA = (VariableScalar)A;
                    VariableScalar mB = (VariableScalar)B;

                    output.value = mA.getDouble()*mB.getDouble();
                }
            };
        } else {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            final VariableMatrix m;
            final VariableScalar s;

            if( A instanceof VariableMatrix ) {
                m = (VariableMatrix)A;
                s = (VariableScalar)B;
            } else {
                m = (VariableMatrix)B;
                s = (VariableScalar)A;
            }

            ret.op = new Operation("multiply-ms") {
                @Override
                public void process() {
                    output.matrix.reshape(m.matrix.numRows,m.matrix.numCols);
                    CommonOps_DDRM.scale(s.getDouble(),m.matrix,output.matrix);
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#divide(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info divide(final Variable A, final Variable B, ManagerTempVariables manager) {

        Info ret = new Info();

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            return solve(B,A,manager);
        } else if( A instanceof VariableMatrix && B instanceof VariableScalar ) {
            final VariableMatrix output = manager.createMatrix();
            final VariableMatrix m = (VariableMatrix)A;
            final VariableScalar s = (VariableScalar)B;
            ret.output = output;
            ret.op = new Operation("divide-ms") {
                @Override
                public void process() {
                    output.matrix.reshape(m.matrix.numRows,m.matrix.numCols);
                    CommonOps_DDRM.divide(m.matrix,s.getDouble(),output.matrix);
                }
            };
        } else if( A instanceof VariableScalar && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            final VariableMatrix m = (VariableMatrix)B;
            final VariableScalar s = (VariableScalar)A;
            ret.output = output;
            ret.op = new Operation("divide-sm") {
                @Override
                public void process() {
                    output.matrix.reshape(m.matrix.numRows,m.matrix.numCols);
                    CommonOps_DDRM.divide(s.getDouble(), m.matrix, output.matrix);
                }
            };
        } else if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new Operation("divide-ii") {
                @Override
                public void process() {
                    VariableInteger mA = (VariableInteger)A;
                    VariableInteger mB = (VariableInteger)B;

                    output.value = mA.value/mB.value;
                }
            };
        } else {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("divide-ss") {
                @Override
                public void process() {
                    VariableScalar mA = (VariableScalar)A;
                    VariableScalar mB = (VariableScalar)B;

                    output.value = mA.getDouble()/mB.getDouble();
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#neg(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info neg(final Variable A, ManagerTempVariables manager) {
        Info ret = new Info();

        if( A instanceof VariableInteger  ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new Operation("neg-i") {
                @Override
                public void process() {
                    output.value = -((VariableInteger)A).value;
                }
            };
        } else if( A instanceof VariableScalar  ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("neg-s") {
                @Override
                public void process() {
                    output.value = -((VariableScalar)A).getDouble();
                }
            };
        } else if( A instanceof VariableMatrix  ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("neg-m") {
                @Override
                public void process() {
                    DMatrixRMaj a = ((VariableMatrix)A).matrix;
                    output.matrix.reshape(a.numRows, a.numCols);
                    CommonOps_DDRM.changeSign(a, output.matrix);
                }
            };
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
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar && B instanceof VariableScalar ) {

            ret.op = new Operation("pow-ss") {
                @Override
                public void process() {
                    double a = ((VariableScalar)A).getDouble();
                    double b = ((VariableScalar)B).getDouble();

                    output.value = Math.pow(a, b);
                }
            };
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
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar && B instanceof VariableScalar ) {

            ret.op = new Operation("atan2-ss") {
                @Override
                public void process() {
                    double a = ((VariableScalar)A).getDouble();
                    double b = ((VariableScalar)B).getDouble();

                    output.value = Math.atan2(a, b);
                }
            };
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
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar  ) {

            ret.op = new Operation("sqrt-s") {
                @Override
                public void process() {
                    double a = ((VariableScalar)A).getDouble();

                    output.value = Math.sqrt(a);
                }
            };
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
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar  ) {

            ret.op = new Operation("sin-s") {
                @Override
                public void process() {
                    output.value = Math.sin(((VariableScalar) A).getDouble());
                }
            };
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
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar  ) {

            ret.op = new Operation("cos-s") {
                @Override
                public void process() {
                    output.value = Math.cos(((VariableScalar) A).getDouble());
                }
            };
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
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableScalar  ) {

            ret.op = new Operation("atan-s") {
                @Override
                public void process() {
                    output.value = Math.atan(((VariableScalar) A).getDouble());
                }
            };
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
        final Info ret = new Info();


        if( A instanceof VariableScalar  ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("exp-s") {
                @Override
                public void process() {
                    output.value = Math.exp(((VariableScalar) A).getDouble());
                }
            };
        } else if( A instanceof VariableMatrix  ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("exp-m") {
                @Override
                public void process() {
                    DMatrixRMaj a = ((VariableMatrix)A).matrix;
                    DMatrixRMaj out = ((VariableMatrix)ret.output).matrix;
                    out.reshape(a.numRows,a.numCols);
                    CommonOps_DDRM.elementExp(a, out);
                }
            };
        } else {
            throw new RuntimeException("Only scalars are supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#log(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info log(final Variable A, ManagerTempVariables manager) {
        final Info ret = new Info();

        if( A instanceof VariableScalar  ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("log-s") {
                @Override
                public void process() {
                    output.value = Math.log(((VariableScalar) A).getDouble());
                }
            };
        } else if( A instanceof VariableMatrix  ) {
            final Variable output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("log-m") {
                @Override
                public void process() {
                    DMatrixRMaj a = ((VariableMatrix)A).matrix;
                    DMatrixRMaj out = ((VariableMatrix)output).matrix;
                    out.reshape(a.numRows,a.numCols);
                    CommonOps_DDRM.elementLog(a, out);
                }
            };
        } else {
            throw new RuntimeException("Only scalars are supported");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#add(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info add(final Variable A, final Variable B, ManagerTempVariables manager) {
        Info ret = new Info();

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("add-mm") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    VariableMatrix mB = (VariableMatrix)B;

                    manager.resize(output, mA.matrix.numRows, mA.matrix.numCols);
                    try {
                        CommonOps_DDRM.add(mA.matrix, mB.matrix, output.matrix);
                    } catch( MatrixDimensionException e ) {
                        checkThrow1x1AgainstNxM(mA.matrix,mB.matrix,"add");
                    }
                }
            };
        } else if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new Operation("add-ii") {
                @Override
                public void process() {
                    VariableInteger mA = (VariableInteger)A;
                    VariableInteger mB = (VariableInteger)B;

                    output.value = mA.value + mB.value;
                }
            };
        } else if( A instanceof VariableScalar && B instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("add-ss") {
                @Override
                public void process() {
                    VariableScalar mA = (VariableScalar)A;
                    VariableScalar mB = (VariableScalar)B;

                    output.value = mA.getDouble() + mB.getDouble();
                }
            };
        } else {

            if( A instanceof VariableMatrix ) {
                final VariableMatrix output = manager.createMatrix();
                ret.output = output;
                final VariableMatrix m = (VariableMatrix)A;
                final VariableScalar s = (VariableScalar)B;
                
                ret.op = new Operation("add-ms") {
                    @Override
                    public void process() {
                        output.matrix.reshape(m.matrix.numRows,m.matrix.numCols);
                        CommonOps_DDRM.add(m.matrix, s.getDouble(), output.matrix);
                    }
                };
                
            } else {
                final VariableMatrix output = manager.createMatrix();
                ret.output = output;
                final VariableMatrix m = (VariableMatrix)B;
                final VariableScalar s = (VariableScalar)A;
                
                
                ret.op = new Operation("add-sm") {
                    @Override
                    public void process() {
                        output.matrix.reshape(m.matrix.numRows,m.matrix.numCols);
                        CommonOps_DDRM.add(m.matrix, s.getDouble(), output.matrix);
                    }
                };
               
            }

        }

        return ret;
    }

    private void checkThrow1x1AgainstNxM( Matrix A , Matrix B , String operation ) {
        if((A.getNumCols() == 1&&A.getNumRows()==1) || (B.getNumCols() == 1&&B.getNumRows() == 1)) {
            throw new MatrixDimensionException("Trying to "+operation+" a 1x1 matrix to every element in a " +
                    "MxN matrix? Turn the 1x1 matrix into a scalar by accessing its element. This is " +
                    "stricter than matlab to catch more accidental math errors.");
        }
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#subtract(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info subtract(final Variable A, final Variable B, ManagerTempVariables manager) {
        Info ret = new Info();

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("subtract-mm") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    VariableMatrix mB = (VariableMatrix)B;

                    manager.resize(output, mA.matrix.numRows, mA.matrix.numCols);
                    try {
                        CommonOps_DDRM.subtract(mA.matrix, mB.matrix, output.matrix);
                    } catch( MatrixDimensionException e ) {
                        checkThrow1x1AgainstNxM(mA.matrix,mB.matrix,"subtract");
                    }
                }
            };
        } else if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new Operation("subtract-ii") {
                @Override
                public void process() {
                    VariableInteger mA = (VariableInteger)A;
                    VariableInteger mB = (VariableInteger)B;

                    output.value = mA.value - mB.value;
                }
            };
        } else if( A instanceof VariableScalar && B instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("subtract-ss") {
                @Override
                public void process() {
                    VariableScalar mA = (VariableScalar)A;
                    VariableScalar mB = (VariableScalar)B;

                    output.value = mA.getDouble() - mB.getDouble();
                }
            };
        } else {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;

            if( A instanceof VariableMatrix ) {
                ret.op = new Operation("subtract-ms") {
                    @Override
                    public void process() {
                        DMatrixRMaj m = ((VariableMatrix)A).matrix;
                        double v = ((VariableScalar)B).getDouble();
                        output.matrix.reshape(m.numRows, m.numCols);
                        CommonOps_DDRM.subtract(m, v, output.matrix);
                    }
                };
            } else {
                ret.op = new Operation("subtract-sm") {
                    @Override
                    public void process() {
                        DMatrixRMaj m = ((VariableMatrix)B).matrix;
                        double v = ((VariableScalar)A).getDouble();
                        output.matrix.reshape(m.numRows, m.numCols);
                        CommonOps_DDRM.subtract(v, m, output.matrix);
                    }
                };
            }
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#elementMult(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info elementMult( final Variable A , final Variable B , ManagerTempVariables manager ) {
        Info ret = new Info();

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("elementMult-mm") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    VariableMatrix mB = (VariableMatrix)B;

                    manager.resize(output, mA.matrix.numRows, mA.matrix.numCols);
                    CommonOps_DDRM.elementMult(mA.matrix, mB.matrix, output.matrix);
                }
            };
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
        Info ret = new Info();

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("elementDivision-mm") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    VariableMatrix mB = (VariableMatrix)B;

                    manager.resize(output, mA.matrix.numRows, mA.matrix.numCols);
                    CommonOps_DDRM.elementDiv(mA.matrix, mB.matrix, output.matrix);
                }
            };
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
        Info ret = new Info();


        if( A instanceof VariableScalar && B instanceof VariableScalar ) {

            final VariableDouble output = manager.createDouble();
            ret.output = output;

            ret.op = new Operation("elementPow-ss") {
                @Override
                public void process() {
                    double a = ((VariableScalar) A).getDouble();
                    double b = ((VariableScalar) B).getDouble();

                    output.value = Math.pow(a, b);
                }
            };
        } else if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {

            final VariableMatrix output = manager.createMatrix();
            ret.output = output;

            ret.op = new Operation("elementPow-mm") {
                @Override
                public void process() {
                    DMatrixRMaj a = ((VariableMatrix) A).matrix;
                    DMatrixRMaj b = ((VariableMatrix) B).matrix;

                    manager.resize(output, a.numRows, a.numCols);
                    CommonOps_DDRM.elementPower(a, b, output.matrix);
                }
            };
        } else if( A instanceof VariableMatrix && B instanceof VariableScalar ) {

            final VariableMatrix output = manager.createMatrix();
            ret.output = output;

            ret.op = new Operation("elementPow-ms") {
                @Override
                public void process() {
                    DMatrixRMaj a = ((VariableMatrix) A).matrix;
                    double b = ((VariableScalar) B).getDouble();

                    manager.resize(output, a.numRows, a.numCols);
                    CommonOps_DDRM.elementPower(a, b, output.matrix);
                }
            };
        } else if( A instanceof VariableScalar && B instanceof VariableMatrix ) {

            final VariableMatrix output = manager.createMatrix();
            ret.output = output;

            ret.op = new Operation("elementPow-sm") {
                @Override
                public void process() {
                    double a = ((VariableScalar) A).getDouble();
                    DMatrixRMaj b = ((VariableMatrix) B).matrix;

                    manager.resize(output, b.numRows, b.numCols);
                    CommonOps_DDRM.elementPower(a, b, output.matrix);
                }
            };
        } else {
            throw new RuntimeException("Unsupport element-wise power input types");
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#copy(org.ejml.equation.Variable, org.ejml.equation.Variable)
	 */
    @Override
	public Operation copy( final Variable src , final Variable dst ) {

        if( src instanceof VariableMatrix  ) {
            if( dst instanceof VariableMatrix ) {
                return new Operation("copy-mm") {
                    @Override
                    public void process() {
                        DMatrixRMaj d = ((VariableMatrix) dst).matrix;
                        DMatrixRMaj s = ((VariableMatrix) src).matrix;
                        d.reshape(s.numRows, s.numCols);
                        d.set(((VariableMatrix) src).matrix);
                    }
                };
            } else if( dst instanceof VariableDouble ) {
                return new Operation("copy-sm1") {
                    @Override
                    public void process() {
                        DMatrixRMaj s = ((VariableMatrix) src).matrix;
                        if( s.numRows != 1 || s.numCols != 1 ) {
                            throw new RuntimeException("Attempting to assign a non 1x1 matrix to a double");
                        }
                        ((VariableDouble) dst).value = s.unsafe_get(0,0);

                    }
                };
            }
        }
        if( src instanceof VariableInteger && dst instanceof VariableInteger ) {
            return new Operation("copy-ii") {
                @Override
                public void process() {
                    ((VariableInteger)dst).value = ((VariableInteger)src).value;
                }
            };
        }
        if( src instanceof VariableScalar && dst instanceof VariableDouble ) {
            return new Operation("copy-ss") {
                @Override
                public void process() {
                    ((VariableDouble)dst).value = ((VariableScalar)src).getDouble();
                }
            };
        }

        if( src instanceof VariableIntegerSequence ) {
            if( dst instanceof VariableIntegerSequence ) {
                return new Operation("copy-is-is") {
                    @Override
                    public void process() {
                        ((VariableIntegerSequence)dst).sequence = ((VariableIntegerSequence)src).sequence;
                    }
                };
            }
        }

        throw new RuntimeException("Unsupported copy types; src = "+src.getClass().getSimpleName()+" dst = "+dst.getClass().getSimpleName());
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#copy(org.ejml.equation.Variable, org.ejml.equation.Variable, java.util.List)
	 */
    @Override
	public Operation copy( final Variable src , final Variable dst , final List<Variable> range ) {
        if( src instanceof VariableMatrix && dst instanceof VariableMatrix ) {
            return new Operation("copyR-mm") {
            	Extents extents = new Extents();
                ArrayExtent rowExtent = new ArrayExtent();
                ArrayExtent colExtent = new ArrayExtent();

                @Override
                public void process() {

                    DMatrixRMaj msrc = ((VariableMatrix) src).matrix;
                    DMatrixRMaj mdst = ((VariableMatrix) dst).matrix;

                    if( range.size() == 1 ) {
                        if( !MatrixFeatures_DDRM.isVector(msrc) ) {
                            throw new ParseError("Source must be a vector for copy into elements");
                        }
                        if( extents.extractSimpleExtents(range.get(0),false,mdst.getNumElements())) {
                            int length = extents.col1-extents.col0+1;
                            if( msrc.getNumElements() != length )
                                throw new IllegalArgumentException("Source vector not the right length.");
                            if( extents.col1+1 > mdst.getNumElements() )
                                throw new IllegalArgumentException("Requested range is outside of dst length");
                            System.arraycopy(msrc.data,0,mdst.data,extents.col0,length);
                        } else {
                        	colExtent.extractArrayExtent(range.get(0),mdst.getNumElements());
                            if( colExtent.length > msrc.getNumElements() )
                                throw new IllegalArgumentException("src doesn't have enough elements");
                            for (int i = 0; i < colExtent.length; i++) {
                                mdst.data[colExtent.array[i]] = msrc.data[i];
                            }
                        }
                    } else if( range.size() == 2 ) {
                        if(extents.extractSimpleExtents(range.get(0),true,mdst.getNumRows()) &&
                        		extents.extractSimpleExtents(range.get(1),false,mdst.getNumCols()) ) {

                            int numRows = extents.row1 - extents.row0 + 1;
                            int numCols = extents.col1 - extents.col0 + 1;

                            CommonOps_DDRM.extract(msrc, 0, numRows, 0, numCols, mdst, extents.row0, extents.col0);
                        } else {
                        	rowExtent.extractArrayExtent(range.get(0),mdst.numRows);
                        	colExtent.extractArrayExtent(range.get(1),mdst.numCols);

                            CommonOps_DDRM.insert(msrc, mdst, rowExtent.array, rowExtent.length,
                                    colExtent.array, colExtent.length);
                        }
                    } else {
                        throw new RuntimeException("Unexpected number of ranges.  Should have been caught earlier");
                    }
                }
            };
        } else if( src instanceof VariableScalar && dst instanceof VariableMatrix ) {
            return new Operation("copyR-sm") {
            	Extents extents = new Extents();
            	ArrayExtent rowExtent = new ArrayExtent();
            	ArrayExtent colExtent = new ArrayExtent();

                @Override
                public void process() {

                    double msrc = ((VariableScalar)src).getDouble();
                    DMatrixRMaj mdst = ((VariableMatrix)dst).matrix;

                    if( range.size() == 1 ) {
                        if(extents.extractSimpleExtents(range.get(0),false,mdst.getNumElements())) {
                            Arrays.fill(mdst.data,extents.col0,extents.col1+1,msrc);
                        } else {
                        	colExtent.extractArrayExtent(range.get(0),mdst.getNumElements());
                            for (int i = 0; i < colExtent.length; i++) {
                                mdst.data[colExtent.array[i]] = msrc;
                            }
                        }
                    } else if( range.size() == 2 ) {
                        if(extents.extractSimpleExtents(range.get(0),true,mdst.getNumRows()) &&
                        		extents.extractSimpleExtents(range.get(1),false,mdst.getNumCols()) ) {

                            extents.row1 += 1;
                            extents.col1 += 1;

                            for (int i = extents.row0; i < extents.row1; i++) {
                                int index = i*mdst.numCols + extents.col0;
                                for (int j = extents.col0; j < extents.col1; j++) {
                                    mdst.data[index++] = msrc;
                                }
                            }
                        } else {
                        	rowExtent.extractArrayExtent(range.get(0),mdst.numRows);
                        	colExtent.extractArrayExtent(range.get(1),mdst.numCols);

                            for (int i = 0; i < rowExtent.length; i++) {
                                for (int j = 0; j < colExtent.length; j++) {
                                    mdst.unsafe_set(rowExtent.array[i],colExtent.array[j],msrc);
                                }
                            }
                        }
                    } else {
                        throw new RuntimeException("Unexpected number of ranges.  Should have been caught earlier");
                    }
                }
            };
        } else {
            throw new RuntimeException("Both variables must be of type VariableMatrix");
        }
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#transpose(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info transpose( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();

        if( A instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("transpose-m") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    output.matrix.reshape(mA.matrix.numCols, mA.matrix.numRows);
                    CommonOps_DDRM.transpose(mA.matrix, output.matrix);
                }
            };
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
        Info ret = new Info();

        if( A instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("inv-m") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    output.matrix.reshape(mA.matrix.numRows, mA.matrix.numCols);
                    if( !CommonOps_DDRM.invert(mA.matrix,output.matrix) )
                        throw new RuntimeException("Inverse failed!");
                }
            };
        } else {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("inv-s") {
                @Override
                public void process() {
                    VariableScalar mA = (VariableScalar)A;
                    output.value = 1.0/mA.getDouble();
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#pinv(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info pinv( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();

        if( A instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("pinv-m") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    output.matrix.reshape(mA.matrix.numCols, mA.matrix.numRows);
                    CommonOps_DDRM.pinv(mA.matrix, output.matrix);
                }
            };
        } else {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("pinv-s") {
                @Override
                public void process() {
                    VariableScalar mA = (VariableScalar)A;
                    output.value = 1.0/mA.getDouble();
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#rref(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info rref( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();

        if( A instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("rref-m") {
                @Override
                public void process() {
                    DMatrixRMaj a = ((VariableMatrix)A).matrix;
                    output.matrix.reshape(a.numRows,a.numCols);
                    CommonOps_DDRM.rref(a, -1, output.matrix);
                }
            };
        } else {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("rref-s") {
                @Override
                public void process() {
                    double a = ((VariableScalar)A).getDouble();
                    output.value = a == 0 ? 0 : 1;
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#det(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info det( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();

        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableMatrix ) {
            ret.op = new Operation("det-m") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    output.value = CommonOps_DDRM.det(mA.matrix);
                }
            };
        } else {
            ret.op = new Operation("det-s") {
                @Override
                public void process() {
                    VariableScalar mA = (VariableScalar)A;
                    output.value = mA.getDouble();
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#trace(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info trace( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableMatrix ) {
            ret.op = new Operation("trace-m") {
                @Override
                public void process() {
                    VariableMatrix mA = (VariableMatrix)A;
                    output.value = CommonOps_DDRM.trace(mA.matrix);
                }
            };
        } else {
            ret.op = new Operation("trace-s") {
                @Override
                public void process() {
                    VariableScalar mA = (VariableScalar)A;
                    output.value = mA.getDouble();
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#normF(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info normF( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableMatrix ) {
            ret.op = new Operation("normF-m") {
                @Override
                public void process() {
                    output.value = NormOps_DDRM.normF(((VariableMatrix) A).matrix);
                }
            };
        } else {
            ret.op = new Operation("normF-s") {
                @Override
                public void process() {
                    output.value = Math.abs(((VariableScalar) A).getDouble());
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#normP(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info normP( final Variable A , final Variable P , ManagerTempVariables manager) {
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( !(A instanceof VariableMatrix) || !(P instanceof VariableScalar) )
            throw new RuntimeException("normP(A,p) A should be a matrix and p a scalar");

        final double valueP = ((VariableScalar)P).getDouble();
        final VariableMatrix varA = (VariableMatrix)A;

        ret.op = new Operation("normP") {
            @Override
            public void process() {
                output.value = NormOps_DDRM.normP(varA.matrix,valueP);
            }
        };

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#max(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info max( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();

        if( A instanceof VariableMatrix ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("max-m") {
                @Override
                public void process() {
                    output.value = CommonOps_DDRM.elementMax(((VariableMatrix) A).matrix);
                }
            };
        } else if( A instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new Operation("max-i") {
                @Override
                public void process() {
                    output.value = ((VariableInteger)A).value;
                }
            };
        } else if( A instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("max-s") {
                @Override
                public void process() {
                    output.value = ((VariableDouble)A).getDouble();
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#max_two(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info max_two( final Variable A , final Variable P , ManagerTempVariables manager) {
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( !(A instanceof VariableMatrix) || !(P instanceof VariableScalar) )
            throw new RuntimeException("max(A,d) A = matrix and d = scalar");

        final double valueP = ((VariableScalar)P).getDouble();
        final VariableMatrix varA = (VariableMatrix)A;

        if( valueP == 0 ) {
            ret.op = new Operation("max_rows") {
                @Override
                public void process() {
                    output.matrix.reshape(varA.matrix.numRows,1);
                    CommonOps_DDRM.maxRows(varA.matrix, output.matrix);
                }
            };
        } else if( valueP == 1 ){
            ret.op = new Operation("max_cols") {
                @Override
                public void process() {
                    output.matrix.reshape(1,varA.matrix.numCols);
                    CommonOps_DDRM.maxCols(varA.matrix, output.matrix);
                }
            };
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
        Info ret = new Info();

        if( A instanceof VariableMatrix ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("min-m") {
                @Override
                public void process() {
                    output.value = CommonOps_DDRM.elementMin(((VariableMatrix) A).matrix);
                }
            };
        } else if( A instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new Operation("min-i") {
                @Override
                public void process() {
                    output.value = ((VariableInteger)A).value;
                }
            };
        } else if( A instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("min-s") {
                @Override
                public void process() {
                    output.value = ((VariableDouble)A).getDouble();
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#min_two(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info min_two( final Variable A , final Variable P , ManagerTempVariables manager) {
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( !(A instanceof VariableMatrix) || !(P instanceof VariableScalar) )
            throw new RuntimeException("min(A,d) A = matrix and d = scalar");

        final double valueP = ((VariableScalar)P).getDouble();
        final VariableMatrix varA = (VariableMatrix)A;

        if( valueP == 0 ) {
            ret.op = new Operation("min_rows") {
                @Override
                public void process() {
                    output.matrix.reshape(varA.matrix.numRows,1);
                    CommonOps_DDRM.minRows(varA.matrix, output.matrix);
                }
            };
        } else if( valueP == 1 ){
            ret.op = new Operation("min_cols") {
                @Override
                public void process() {
                    output.matrix.reshape(1,varA.matrix.numCols);
                    CommonOps_DDRM.minCols(varA.matrix, output.matrix);
                }
            };
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
        Info ret = new Info();

        if( A instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("abs-m") {
                @Override
                public void process() {
                    DMatrixRMaj a = ((VariableMatrix)A).matrix;
                    output.matrix.reshape(a.numRows,a.numCols);
                    CommonOps_DDRM.abs(a, output.matrix);
//                    int N = a.getNumElements();
//                    for (int i = 0; i < N; i++) {
//                        output.matrix.data[i] = Math.abs(a.data[i]);
//                    }
                }
            };
        } else if( A instanceof VariableInteger ) {
            final VariableInteger output = manager.createInteger();
            ret.output = output;
            ret.op = new Operation("abs-i") {
                @Override
                public void process() {
                    output.value = Math.abs(((VariableInteger)A).value);
                }
            };
        } else if( A instanceof VariableScalar ) {
            final VariableDouble output = manager.createDouble();
            ret.output = output;
            ret.op = new Operation("abs-s") {
                @Override
                public void process() {
                    output.value = Math.abs(((VariableDouble) A).getDouble());
                }
            };
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#eye(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info eye( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableMatrix ) {
            ret.op = new Operation("eye-m") {
                @Override
                public void process() {
                    DMatrixRMaj mA = ((VariableMatrix)A).matrix;
                    output.matrix.reshape(mA.numRows,mA.numCols);
                    CommonOps_DDRM.setIdentity(output.matrix);
                }
            };
        } else if( A instanceof VariableInteger ) {
            ret.op = new Operation("eye-i") {
                @Override
                public void process() {
                    int N = ((VariableInteger)A).value;
                    output.matrix.reshape(N,N);
                    CommonOps_DDRM.setIdentity(output.matrix);
                }
            };
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
        Info ret = new Info();

        if( A instanceof VariableMatrix ) {
            final VariableMatrix output = manager.createMatrix();
            ret.output = output;
            ret.op = new Operation("diag-m") {
                @Override
                public void process() {
                    DMatrixRMaj mA = ((VariableMatrix)A).matrix;

                    if(MatrixFeatures_DDRM.isVector(mA)) {
                        int N = mA.getNumElements();
                        output.matrix.reshape(N,N);
                        CommonOps_DDRM.diag(output.matrix,N,mA.data);
                    } else {
                        int N = Math.min(mA.numCols,mA.numRows);
                        output.matrix.reshape(N,1);
                        for (int i = 0; i < N; i++) {
                            output.matrix.data[i] = mA.unsafe_get(i,i);
                        }
                    }
                }
            };
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
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            ret.op = new Operation("zeros-ii") {
                @Override
                public void process() {
                    int numRows = ((VariableInteger)A).value;
                    int numCols = ((VariableInteger)B).value;
                    output.matrix.reshape(numRows,numCols);
                    CommonOps_DDRM.fill(output.matrix, 0);
                    //not sure if this is necessary.  Can its value every be modified?
                }
            };
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
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            ret.op = new Operation("ones-ii") {
                @Override
                public void process() {
                    int numRows = ((VariableInteger)A).value;
                    int numCols = ((VariableInteger)B).value;
                    output.matrix.reshape(numRows,numCols);
                    CommonOps_DDRM.fill(output.matrix, 1);
                }
            };
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

        Info ret = new Info();

        if( A instanceof VariableInteger ) {
            ret.op = new Operation("rng") {
                @Override
                public void process() {
                    int seed = ((VariableInteger)A).value;
                    manager.getRandom().setSeed(seed);
                }
            };
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
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            ret.op = new Operation("rand-ii") {
                @Override
                public void process() {
                    int numRows = ((VariableInteger)A).value;
                    int numCols = ((VariableInteger)B).value;
                    output.matrix.reshape(numRows,numCols);
                    RandomMatrices_DDRM.fillUniform(output.matrix, 0,1,manager.getRandom());
                }
            };
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
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableInteger && B instanceof VariableInteger ) {
            ret.op = new Operation("randn-ii") {
                @Override
                public void process() {
                    int numRows = ((VariableInteger)A).value;
                    int numCols = ((VariableInteger)B).value;
                    output.matrix.reshape(numRows,numCols);
                    RandomMatrices_DDRM.fillGaussian(output.matrix, 0,1,manager.getRandom());
                }
            };
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
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            ret.op = new Operation("kron-mm") {
                @Override
                public void process() {
                    DMatrixRMaj mA = ((VariableMatrix)A).matrix;
                    DMatrixRMaj mB = ((VariableMatrix)B).matrix;
                    output.matrix.reshape(mA.numRows * mB.numRows, mA.numCols * mB.numCols);
                    CommonOps_DDRM.kron(mA, mB, output.matrix);
                }
            };
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
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            ret.op = new Operation("dot-mm") {
                @Override
                public void process() {
                    DMatrixRMaj a = ((VariableMatrix)A).matrix;
                    DMatrixRMaj b = ((VariableMatrix)B).matrix;

                    if( !MatrixFeatures_DDRM.isVector(a) || !MatrixFeatures_DDRM.isVector(b))
                        throw new RuntimeException("Both inputs to dot() must be vectors");

                    output.value = VectorVectorMult_DDRM.innerProd(a,b);
                }
            };
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
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( A instanceof VariableMatrix && B instanceof VariableMatrix ) {
            ret.op = new Operation("solve-mm") {
                LinearSolverDense<DMatrixRMaj> solver;
                @Override
                public void process() {

                    DMatrixRMaj a = ((VariableMatrix)A).matrix;
                    DMatrixRMaj b = ((VariableMatrix)B).matrix;

                    if( solver == null ) {
                        solver = LinearSolverFactory_DDRM.leastSquares(a.numRows,a.numCols);
                    }

                    if( !solver.setA(a))
                        throw new RuntimeException("Solver failed!");

                    output.matrix.reshape(a.numCols,b.numCols);
                    solver.solve(b, output.matrix);
                }
            };
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
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if(  !(inputs.get(0) instanceof VariableMatrix))
            throw new RuntimeException("First parameter must be a matrix.");


        for (int i = 1; i < inputs.size(); i++) {
            if( !(inputs.get(i) instanceof VariableInteger) &&
                    (inputs.get(i).getType() != VariableType.INTEGER_SEQUENCE))
                throw new RuntimeException("Parameters must be integers, integer list, or array range");
        }

        ret.op = new Operation("extract") {

        	Extents extents = new Extents();

        	ArrayExtent rowExtent = new ArrayExtent();
        	ArrayExtent colExtent = new ArrayExtent();

            @Override
            public void process() {

                DMatrixRMaj A = ((VariableMatrix)inputs.get(0)).matrix;

                if( inputs.size() == 2  ) {
                    if( extents.extractSimpleExtents(inputs.get(1), false, A.getNumElements()) ) {
                        extents.col1 += 1;
                        output.matrix.reshape(1,extents.col1-extents.col0);
                        System.arraycopy(A.data,extents.col0,output.matrix.data,0,extents.col1-extents.col0);
                    } else {
                    	colExtent.extractArrayExtent(inputs.get(1),A.getNumElements());
                        output.matrix.reshape(1, colExtent.length);
                        CommonOps_DDRM.extract(A,
                                colExtent.array, colExtent.length, output.matrix);
                    }
                } else if( extents.extractSimpleExtents(inputs.get(1), true, A.numRows) &&
                		extents.extractSimpleExtents(inputs.get(2), false, A.numCols)) {
                    extents.row1 += 1;
                    extents.col1 += 1;
                    output.matrix.reshape(extents.row1-extents.row0,extents.col1-extents.col0);
                    CommonOps_DDRM.extract(A,extents.row0,extents.row1,extents.col0,extents.col1,output.matrix,0,0);
                } else {
                	rowExtent.extractArrayExtent(inputs.get(1),A.numRows);
                	colExtent.extractArrayExtent(inputs.get(2),A.numCols);

                    output.matrix.reshape(rowExtent.length, colExtent.length);
                    CommonOps_DDRM.extract(A,
                            rowExtent.array,rowExtent.length,
                            colExtent.array,colExtent.length,output.matrix);
                }
            }
        };

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#sum_one(org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info sum_one( final Variable A , ManagerTempVariables manager) {
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if( !(A instanceof VariableMatrix)  )
            throw new RuntimeException("sum(A) A = matrix");

        final VariableMatrix varA = (VariableMatrix)A;

        ret.op = new Operation("sum_all") {
            @Override
            public void process() {
                output.value = CommonOps_DDRM.elementSum(varA.matrix);
            }
        };

        return ret;
    }

    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#sum_two(org.ejml.equation.Variable, org.ejml.equation.Variable, org.ejml.equation.ManagerTempVariables)
	 */
    @Override
	public Info sum_two( final Variable A , final Variable P , ManagerTempVariables manager) {
        Info ret = new Info();
        final VariableMatrix output = manager.createMatrix();
        ret.output = output;

        if( !(A instanceof VariableMatrix) || !(P instanceof VariableScalar) )
            throw new RuntimeException("sum(A,p) A = matrix and p = scalar");

        final double valueP = ((VariableScalar)P).getDouble();
        final VariableMatrix varA = (VariableMatrix)A;

        if( valueP == 0 ) {
            ret.op = new Operation("sum_rows") {
                @Override
                public void process() {
                    output.matrix.reshape(varA.matrix.numRows,1);
                    CommonOps_DDRM.sumRows(varA.matrix, output.matrix);
                }
            };
        } else if( valueP == 1 ){
            ret.op = new Operation("sum_cols") {
                @Override
                public void process() {
                    output.matrix.reshape(1,varA.matrix.numCols);
                    CommonOps_DDRM.sumCols(varA.matrix, output.matrix);
                }
            };
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
        Info ret = new Info();
        final VariableDouble output = manager.createDouble();
        ret.output = output;

        if(  !(inputs.get(0) instanceof VariableMatrix))
            throw new RuntimeException("First parameter must be a matrix.");

        for (int i = 1; i < inputs.size(); i++) {
            if( !(inputs.get(i) instanceof VariableInteger) )
                throw new RuntimeException("Parameters must be integers for extract scalar");
        }

        ret.op = new Operation("extractScalar") {

            @Override
            public void process() {

                DMatrixRMaj A = ((VariableMatrix)inputs.get(0)).matrix;

                if( inputs.size() == 2 ) {
                    int index = ((VariableInteger)inputs.get(1)).value;

                    output.value = A.get(index);
                } else {
                    int row = ((VariableInteger) inputs.get(1)).value;
                    int col = ((VariableInteger) inputs.get(2)).value;

                    output.value = A.get(row, col);
                }
            }
        };

        return ret;
    }


    /* (non-Javadoc)
	 * @see org.ejml.equation.IOperationFactory#matrixConstructor(org.ejml.equation.MatrixConstructor)
	 */
    @Override
	public Info matrixConstructor( final MatrixConstructor m ) {
        Info ret = new Info();
        ret.output = m.getOutput();

        ret.op = new Operation("matrixConstructor") {

            @Override
            public void process() {
                m.construct();
            }
        };

        return ret;
    }

}
