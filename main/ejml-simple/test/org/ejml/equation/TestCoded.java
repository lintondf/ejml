package org.ejml.equation;

import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.dense.row.mult.VectorVectorMult_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.ejml.equation.TokenList.Type;
import static java.lang.Math.exp;
import static org.ejml.dense.row.CommonOps_DDRM.*;
//import static org.ejml.dense.row.CommonOps_DDRM.mult;
//import static org.ejml.dense.row.CommonOps_DDRM.scale;
//import static org.ejml.dense.row.CommonOps_DDRM.subtract;
import static org.junit.Assert.*;

public class TestCoded {
	
	Random rand = new Random(234);
	
	protected boolean isIdentical( double a, double b) {
		return Math.abs(a - b) < UtilEjml.TEST_F64;
	}

	protected boolean isIdentical( double a, Variable B) {
		double b = ((VariableScalar) B).getDouble();
		return Math.abs(a - b) < UtilEjml.TEST_F64;
	}

	protected boolean isIdentical( int a, int b) {
		return a == b;
	}
	
	protected boolean isIdentical( SimpleMatrix a, SimpleMatrix b) {
		return a.isIdentical(b,1e-15);
	}
	
	protected boolean isIdentical( SimpleMatrix a, DMatrixRMaj b) {
		return isIdentical( a, new SimpleMatrix(b) );
	}
	
	protected boolean isIdentical( DMatrixRMaj a, SimpleMatrix b) {
		return isIdentical( b, new SimpleMatrix(a) );
	}
	
	protected boolean isIdentical( DMatrixRMaj a, DMatrixRMaj b) {
		return isIdentical( new SimpleMatrix(b), new SimpleMatrix(a) );
	}
	
    private void checkSubMatrixArraysExtract(SimpleMatrix src, DMatrixRMaj dst, int[] rows, int[] cols) {
        assertTrue(dst.numRows == rows.length && dst.numCols == cols.length);
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < cols.length; j++) {
                assertEquals(src.get(rows[i],cols[j]), dst.get(i,j), UtilEjml.TEST_F64);
            }
        }
    }

    private void checkSubMatrixArraysInsert(SimpleMatrix src, SimpleMatrix dst, int[] rows, int[] cols) {
        assertTrue(src.numRows() == rows.length && src.numCols() == cols.length);
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < cols.length; j++) {
                assertEquals(src.get(i,j), dst.get(rows[i],cols[j]), UtilEjml.TEST_F64);
            }
        }
    }

    private void checkSubMatrixArraysInsert(double src, SimpleMatrix dst, int[] rows, int[] cols) {
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < cols.length; j++) {
                assertEquals(src, dst.get(rows[i],cols[j]), UtilEjml.TEST_F64);
            }
        }
    }
    


    @Test
    public void compile_basic() {
        Equation eq = new Equation();

        SimpleMatrix A = new SimpleMatrix(5, 6);
        SimpleMatrix B = SimpleMatrix.random_DDRM(5, 6, -1, 1, rand);
        SimpleMatrix C = SimpleMatrix.random_DDRM(5, 4, -1, 1, rand);
        SimpleMatrix D = SimpleMatrix.random_DDRM(4, 6, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(C, "C");
        eq.alias(D, "D");

        Sequence sequence = eq.compile("A=B+C*D-B");
        SimpleMatrix expected = C.mult(D);
        sequence.perform();
        assertTrue(expected.isIdentical(A,1e-15));
        // eq: A=B+C*D-B -> A
        DMatrixRMaj A_coded = compile_basic_Coded(B.getDDRM(), C.getDDRM(), D.getDDRM());
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj compile_basic_Coded(DMatrixRMaj B, DMatrixRMaj C, DMatrixRMaj D) {
        // A=B+C*D-B
        DMatrixRMaj	A = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm1 = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm2 = new DMatrixRMaj(1,1);

        tm1.reshape( C.numRows, D.numCols );
        CommonOps_DDRM.mult( C, D, tm1 );
        tm2.reshape( B.numRows, B.numCols );
        CommonOps_DDRM.add( B, tm1, tm2 );
        A.reshape( tm2.numRows, tm2.numCols );
        CommonOps_DDRM.subtract( tm2, B, A );

        return A;
    }


    @Test
    public void compile_assign_submatrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(2, 5, -1, 1, rand);

        SimpleMatrix A_orig = A.copy();

        eq.alias(A, "A");
        eq.alias(B, "B");

        Sequence sequence = eq.compile("A(2:3,0:4)=B");
        sequence.perform();

        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                if( x < 5 && y >= 2 && y <= 3 ) {
                    assertTrue(A.get(y,x) == B.get(y-2,x));
                } else {
                    assertTrue(x+" "+y,A.get(y,x) == A_orig.get(y,x));
                }
            }
        }
        // eq: A(2:3,0:4)=B -> A
        DMatrixRMaj A_coded = compile_assign_submatrix_Coded(A.getDDRM(), B.getDDRM());
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj compile_assign_submatrix_Coded(final DMatrixRMaj A_in, DMatrixRMaj B) {
        // A(2:3,0:4)=B
        DMatrixRMaj	A = new DMatrixRMaj(A_in);

        CommonOps_DDRM.insert( B, A, 2, 0 );

        return A;
    }


    @Test
    public void compile_assign_submatrix_special() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(4, 5, -1, 1, rand);

        SimpleMatrix A_orig = A.copy();

        eq.alias(A, "A");
        eq.alias(B, "B");

        eq.process("A(2:,:)=B");

        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 5; x++) {
                if( y >= 2 ) {
                    assertTrue(A.get(y,x) == B.get(y-2,x));
                } else {
                    assertTrue(x+" "+y,A.get(y,x) == A_orig.get(y,x));
                }
            }
        }
        // eq: A(2:,:)=B -> A
        DMatrixRMaj A_coded = compile_assign_submatrix_special_Coded(A.getDDRM(), B.getDDRM());
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj compile_assign_submatrix_special_Coded(final DMatrixRMaj A_in, DMatrixRMaj B) {
        // A(2:,:)=B
        DMatrixRMaj	A = new DMatrixRMaj(A_in);

        CommonOps_DDRM.insert( B, A, 2, 0 );

        return A;
    }


    @Test
    public void compile_assign_submatrix_scalar() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);

        eq.alias(A, "A");

        // single element
        eq.process("A(1,2)=0.5");

        assertEquals(A.get(1, 2), 0.5, UtilEjml.TEST_F64);

        // multiple elements
        eq.process("A(1:2,2:4)=0.5");

        for (int i = 1; i <= 2; i++) {
            for (int j = 2; j <= 4; j++) {
                assertEquals(A.get(i, j), 0.5, UtilEjml.TEST_F64);
            }
        }
    }


    @Test
    public void compile_assign_submatrix_IndexMath() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);

        eq.alias(A, "A");

        // single element
//        eq.process("A(1+2,2-1)=0.5");
//
//        assertEquals(A.get(3, 1), 0.5, UtilEjml.TEST_F64);

        // multiple elements
        eq.process("A((1-1):2,2:3)=0.5");

        for (int i = 0; i <= 2; i++) {
            for (int j = 2; j <= 3; j++) {
                assertEquals(A.get(i, j), 0.5, UtilEjml.TEST_F64);
            }
        }
    }


    @Test
    public void assign_lazy() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        eq.alias(A, "A");
        eq.process("B=A");

        DMatrixRMaj B = eq.lookupDDRM("B");
        assertTrue(A.getMatrix() != B);
        assertTrue(MatrixFeatures_DDRM.isEquals((DMatrixRMaj)A.getMatrix(), B));
        // eq: B=A -> B
        DMatrixRMaj B_coded = assign_lazy_Coded(A.getDDRM());
        assertTrue(isIdentical(B_coded, B));
    }

    protected DMatrixRMaj assign_lazy_Coded(DMatrixRMaj A) {
        // B=A
        DMatrixRMaj	B = new DMatrixRMaj(1,1);

        B.reshape( A.numRows, A.numCols );
        B.set( A );

        return B;
    }


    @Test
    public void assign_resize_lazy() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(2, 3, -1, 1, rand);
        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.process("B=A");

        assertTrue(A.isIdentical(B, UtilEjml.TEST_F64));
        // eq: B=A -> B
        DMatrixRMaj B_coded = assign_resize_lazy_Coded(A.getDDRM());
        assertTrue(isIdentical(B_coded, B));
    }

    protected DMatrixRMaj assign_resize_lazy_Coded(DMatrixRMaj A) {
        // B=A
        DMatrixRMaj	B = new DMatrixRMaj(1,1);

        B.reshape( A.numRows, A.numCols );
        B.set( A );

        return B;
    }


    @Test
    public void compile_parentheses_extractScalar() {
        Equation eq = new Equation();

        SimpleMatrix B = SimpleMatrix.random_DDRM(8, 8, -1, 1, rand);

        eq.alias(B, "B");

        eq.process("A=B(1,2)");
        Variable v = eq.lookupVariable("A");
        assertTrue(v instanceof VariableDouble);
        assertEquals(eq.lookupDouble("A"), B.get(1, 2), UtilEjml.TEST_F64);
        // eq: A=B(1,2) -> A
        double A_coded = compile_parentheses_extractScalar_Coded(B.getDDRM());
        assertTrue(isIdentical(A_coded, eq.lookupDouble("A")));
    }

    protected double compile_parentheses_extractScalar_Coded(DMatrixRMaj B) {
        // A=B(1,2)
        double    	A = 0;

        A = B.get(1, 2);

        return A;
    }


    @Test
    public void compile_neg() {
        Equation eq = new Equation();

        eq.alias(1, "A",2, "B");

        eq.process("A=-B");
        assertEquals(-2, eq.lookupInteger("A"));

        eq.process("A=B--B");
        assertEquals(4, eq.lookupInteger("A"));
        eq.process("A=B+-B");
        assertEquals(0,eq.lookupInteger("A"));
        eq.process("A=B---5");
        assertEquals(2 - 5, eq.lookupInteger("A"));
        eq.process("A=B--5");
        assertEquals(2+5,eq.lookupInteger("A"));
    }


    @Test
    public void compile_neg_Coded() {
        int        A = 1;
        int        B = 2;
        int        ti1;
        //A=-B
        A = -B;
        assertTrue(isIdentical(-2, A));
        //A=B--B
        ti1 = -B;
        A = B - ti1;
        assertTrue(isIdentical(4, A));
        //A=B+-B
        ti1 = -B;
        A = B + ti1;
        assertTrue(isIdentical(0, A));
        //A=B---5
        A = B - (-(-5));
        assertTrue(isIdentical(2 - 5, A));
        //A=B--5
        A = B - -5;
        assertTrue(isIdentical(2+5, A));
    }


    @Test
    public void compile_constructMatrix_scalars() {
        Equation eq = new Equation();

        SimpleMatrix expected = new SimpleMatrix(new double[][]{{0,1,2,3},{4,5,6,7},{8,1,1,1}});
        SimpleMatrix A = new SimpleMatrix(3,4);

        eq.alias(A, "A");
        Sequence sequence = eq.compile("A=[0 1 2 3; 4 5 6 7;8 1 1 1]");
        sequence.perform();
        assertTrue(A.isIdentical(expected, UtilEjml.TEST_F64));
        // eq: A=[0 1 2 3; 4 5 6 7;8 1 1 1] -> A
        DMatrixRMaj A_coded = compile_constructMatrix_scalars_Coded();
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj compile_constructMatrix_scalars_Coded() {
        // A=[0 1 2 3; 4 5 6 7;8 1 1 1]

        DMatrixRMaj A = new DMatrixRMaj(new double[][] {{0,1,2,3},{4,5,6,7},{8,1,1,1}});

        return A;
    }


    @Test
    public void compile_constructMatrix_doubles() {
        Equation eq = new Equation();

        eq.process("A=[1 2 3 4.5 6 7.7 8.8 9]");
        DMatrixRMaj found = eq.lookupDDRM("A");

        double[] expected = new double[]{1,2,3,4.5,6,7.7,8.8,9};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(found.get(i),expected[i],UtilEjml.TEST_F64);
        }
        // eq: A=[1 2 3 4.5 6 7.7 8.8 9] -> A
        DMatrixRMaj A_coded = compile_constructMatrix_doubles_Coded();
        assertTrue(isIdentical(A_coded, found));
    }

    protected DMatrixRMaj compile_constructMatrix_doubles_Coded() {
        // A=[1 2 3 4.5 6 7.7 8.8 9]

        DMatrixRMaj A = new DMatrixRMaj(new double[][] {{1,2,3,4.5, 6, 7.7, 8.8, 9,}});

        return A;
    }


    @Test
    public void compile_constructMatrix_for() {
        Equation eq = new Equation();

        eq.process("A=[ 2:2:10 12 14 ]");
        DMatrixRMaj found = eq.lookupDDRM("A");

        assertEquals(7,found.getNumCols());
        assertEquals(1,found.getNumRows());

        for (int i = 0; i < 7; i++) {
            assertEquals(found.get(i),2+2*i,UtilEjml.TEST_F64);
        }
        // eq: A=[ 2:2:10 12 14 ] -> A
        DMatrixRMaj A_coded = compile_constructMatrix_for_Coded();
        assertTrue(isIdentical(A_coded, found));
    }

    protected DMatrixRMaj compile_constructMatrix_for_Coded() {
        // A=[ 2:2:10 12 14 ]

        DMatrixRMaj A = new DMatrixRMaj(new double[][] {{2,4,6,8,10,12,14}});

        return A;
    }


    @Test
    public void compile_constructMatrix_ForSequence_Case1() {
        Equation eq = new Equation();

        eq.process("found=[1:4 5:1:8]");
        SimpleMatrix found = SimpleMatrix.wrap(eq.lookupDDRM("found"));
        assertEquals(1,found.numRows());
        assertEquals(8, found.numCols());

        for (int x = 0; x < 8; x++) {
            assertEquals(x+1,found.get(0,x),UtilEjml.TEST_F64);
        }
        // eq: found=[1:4 5:1:8] -> found
        DMatrixRMaj found_coded = compile_constructMatrix_ForSequence_Case1_Coded();
        assertTrue(isIdentical(found_coded, eq.lookupDDRM("found")));
    }

    protected DMatrixRMaj compile_constructMatrix_ForSequence_Case1_Coded() {
        // found=[1:4 5:1:8]

        DMatrixRMaj found = new DMatrixRMaj(new double[][] {{1,2,3,4,5,6,7,8}});

        return found;
    }


    @Test
    public void compile_constructMatrix_ForSequence_Case2() {
        Equation eq = new Equation();

        eq.process("found=[1 2 3 4 5:1:8]");
        SimpleMatrix found = SimpleMatrix.wrap(eq.lookupDDRM("found"));
        assertEquals(1,found.numRows());
        assertEquals(8,found.numCols());

        for (int x = 0; x < 8; x++) {
            assertEquals(x+1,found.get(0,x),UtilEjml.TEST_F64);
        }
        // eq: found=[1 2 3 4 5:1:8] -> found
        DMatrixRMaj found_coded = compile_constructMatrix_ForSequence_Case2_Coded();
        assertTrue(isIdentical(found_coded, eq.lookupDDRM("found")));
    }

    protected DMatrixRMaj compile_constructMatrix_ForSequence_Case2_Coded() {
        // found=[1 2 3 4 5:1:8]

        DMatrixRMaj found = new DMatrixRMaj(new double[][] {{1,2,3,4,5,6,7,8}});

        return found;
    }


    @Test
    public void compile_transpose() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix C = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix R = new SimpleMatrix(6, 6);

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(C, "C");
        eq.alias(R, "R");

        Sequence sequence = eq.compile("R=A'*(B'+C)'+inv(B)'");
        SimpleMatrix expected = A.transpose().mult(B.transpose().plus(C).transpose()).plus(B.invert().transpose());
        sequence.perform();
        assertTrue(expected.isIdentical(R, 1e-15));
        // eq: R=A'*(B'+C)'+inv(B)' -> R
        DMatrixRMaj R_coded = compile_transpose_Coded(A.getDDRM(), B.getDDRM(), C.getDDRM());
        assertTrue(isIdentical(R_coded, R));
    }

    protected DMatrixRMaj compile_transpose_Coded(DMatrixRMaj A, DMatrixRMaj B, DMatrixRMaj C) {
        // R=A'*(B'+C)'+inv(B)'
        DMatrixRMaj	R = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm1 = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm2 = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm4 = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm5 = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm6 = new DMatrixRMaj(1,1);

        tm1.reshape( B.numCols, B.numRows );
        CommonOps_DDRM.transpose( B, tm1 );
        tm2.reshape( tm1.numRows, tm1.numCols );
        CommonOps_DDRM.add( tm1, C, tm2 );
        tm1.reshape( B.numRows, B.numCols );
        boolean ok = CommonOps_DDRM.invert(B, tm1);
        tm4.reshape( A.numCols, A.numRows );
        CommonOps_DDRM.transpose( A, tm4 );
        tm5.reshape( tm2.numCols, tm2.numRows );
        CommonOps_DDRM.transpose( tm2, tm5 );
        tm6.reshape( tm1.numCols, tm1.numRows );
        CommonOps_DDRM.transpose( tm1, tm6 );
        tm1.reshape( tm4.numRows, tm5.numCols );
        CommonOps_DDRM.mult( tm4, tm5, tm1 );
        R.reshape( tm1.numRows, tm1.numCols );
        CommonOps_DDRM.add( tm1, tm6, R );

        return R;
    }


    @Test
    public void compile_elementWise() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix C = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix R = new SimpleMatrix(6, 6);

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(C, "C");
        eq.alias(R, "R");

        Sequence sequence = eq.compile("R=A.*(B./C)");
        SimpleMatrix expected = A.elementMult(B.elementDiv(C));
        sequence.perform();
        assertTrue(expected.isIdentical(R, 1e-15));
        // eq: R=A.*(B./C) -> R
        DMatrixRMaj R_coded = compile_elementWise_Coded(A.getDDRM(), B.getDDRM(), C.getDDRM());
        assertTrue(isIdentical(R_coded, R));
    }

    protected DMatrixRMaj compile_elementWise_Coded(DMatrixRMaj A, DMatrixRMaj B, DMatrixRMaj C) {
        // R=A.*(B./C)
        DMatrixRMaj	R = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm1 = new DMatrixRMaj(1,1);

        tm1.reshape( B.numRows, B.numCols );
        CommonOps_DDRM.elementDiv( B, C, tm1 );
        R.reshape( A.numRows, A.numCols );
        CommonOps_DDRM.elementMult( A, tm1, R );

        return R;
    }


    @Test
    public void compile_double_1() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        double C = 2.5;
        double D = 1.7;

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(D, "D");
        eq.alias(0.0, "E");

        VariableDouble E = eq.lookupVariable("E");

        Sequence sequence = eq.compile("A=2.5*B");
        SimpleMatrix expected = B.scale(C);
        sequence.perform();
        assertTrue(expected.isIdentical(A, 1e-15));
        // eq: A=2.5*B -> A
        DMatrixRMaj A_coded = compile_double_1_Coded(B.getDDRM(), D, 0.0);
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj compile_double_1_Coded(DMatrixRMaj B, double     D, double     E) {
        // A=2.5*B
        DMatrixRMaj	A = new DMatrixRMaj(1,1);

        A.reshape( B.numRows, B.numCols );
        CommonOps_DDRM.scale( 2.5, B, A );

        return A;
    }


    @Test
    public void compile_double_2() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        double C = 2.5;
        double D = 1.7;

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(D, "D");
        eq.alias(0.0, "E");

        VariableDouble E = eq.lookupVariable("E");
        SimpleMatrix expected = B.scale(C);

        Sequence sequence = eq.compile("A=B*2.5");
        sequence.perform();
        assertTrue(expected.isIdentical(A, 1e-15));
        // eq: A=B*2.5 -> A
        DMatrixRMaj A_coded = compile_double_2_Coded(B.getDDRM(), D, 0.0);
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj compile_double_2_Coded(DMatrixRMaj B, double     D, double     E) {
        // A=B*2.5
        DMatrixRMaj	A = new DMatrixRMaj(1,1);

        A.reshape( B.numRows, B.numCols );
        CommonOps_DDRM.scale( 2.5, B, A );

        return A;
    }


    @Test
    public void compile_double_3() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        double C = 2.5;
        double D = 1.7;

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(D, "D");
        eq.alias(0.0, "E");

        VariableDouble E = eq.lookupVariable("E");

        Sequence sequence = eq.compile("E=2.5*D");
        sequence.perform();
        assertEquals(C * D, E.value, UtilEjml.TEST_F64);
        // eq: E=2.5*D -> E
        double E_coded = compile_double_3_Coded(A.getDDRM(), B.getDDRM(), D);
        assertTrue(isIdentical(E_coded, E));
    }

    protected double compile_double_3_Coded(DMatrixRMaj A, DMatrixRMaj B, double     D) {
        // E=2.5*D
        double    	E = 0;

        E = 2.5 * D;

        return E;
    }


    @Test
    public void compile_double_4() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        double C = 2.5;
        double D = 1.7;

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(D, "D");
        eq.alias(0.0, "E");

        VariableDouble E = eq.lookupVariable("E");

        // try exponential formats
        Sequence sequence = eq.compile("E=2.001e-6*1e3");
        sequence.perform();
        assertEquals(2.001e-6*1e3, E.value, UtilEjml.TEST_F64);
        // eq: E=2.001e-6*1e3 -> E
        double E_coded = compile_double_4_Coded(A.getDDRM(), B.getDDRM(), D);
        assertTrue(isIdentical(E_coded, E));
    }

    protected double compile_double_4_Coded(DMatrixRMaj A, DMatrixRMaj B, double     D) {
        // E=2.001e-6*1e3
        double    	E = 0;

        E = (2.001e-6 * 1e3);

        return E;
    }


    @Test
    public void divide_matrix_scalar() {
        Equation eq = new Equation();

        SimpleMatrix x = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);

        eq.alias(2.5, "A");
        eq.alias(b, "b");
        eq.alias(x, "x");

        eq.process("x=b/A");

        assertTrue(b.divide(2.5).isIdentical(x, UtilEjml.TEST_F64));
        // eq: x=b/A -> x
        DMatrixRMaj x_coded = divide_matrix_scalar_Coded(2.5, b.getDDRM());
        assertTrue(isIdentical(x_coded, x));
    }

    protected DMatrixRMaj divide_matrix_scalar_Coded(double     A, DMatrixRMaj b) {
        // x=b/A
        DMatrixRMaj	x = new DMatrixRMaj(1,1);

        x.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.divide( b, A, x );

        return x;
    }


    @Test
    public void divide_scalar_matrix() {
        Equation eq = new Equation();

        SimpleMatrix x = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);

        eq.alias(2.5, "A");
        eq.alias(b, "b");
        eq.alias(x, "x");

        eq.process("x=A/b");

        DMatrixRMaj tmp = new DMatrixRMaj(5,3);
        CommonOps_DDRM.divide(2.5, (DMatrixRMaj)b.getMatrix(), tmp);

        assertTrue(MatrixFeatures_DDRM.isIdentical(tmp, (DMatrixRMaj)x.getMatrix(), UtilEjml.TEST_F64));
        // eq: x=A/b -> x
        DMatrixRMaj x_coded = divide_scalar_matrix_Coded(2.5, b.getDDRM());
        assertTrue(isIdentical(x_coded, x));
    }

    protected DMatrixRMaj divide_scalar_matrix_Coded(double     A, DMatrixRMaj b) {
        // x=A/b
        DMatrixRMaj	x = new DMatrixRMaj(1,1);

        x.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.divide( A, b, x );

        return x;
    }


    @Test
    public void divide_int_int() {
        Equation eq = new Equation();

        eq.alias(4, "A");
        eq.alias(13, "b");
        eq.alias(13 / 4, "x");

        eq.process("x=b/A");

        int found = eq.lookupInteger("x");

        assertEquals(13 / 4, found, UtilEjml.TEST_F64);
        // eq: x=b/A -> x
        int x_coded = divide_int_int_Coded(4, 13);
        assertTrue(isIdentical(x_coded, found));
    }

    protected int divide_int_int_Coded(int        A, int        b) {
        // x=b/A
        int       	x = 0;

        x = b / A;

        return x;
    }


    @Test
    public void divide_scalar_scalar() {
        Equation eq = new Equation();

        eq.alias(5, "A");
        eq.alias(4.2, "b");
        eq.alias(-1.0, "x");

        eq.process("x=b/A");

        double found = eq.lookupDouble("x");

        assertEquals(4.2 / 5.0, found, UtilEjml.TEST_F64);
        // eq: x=b/A -> x
        double x_coded = divide_scalar_scalar_Coded(5, 4.2);
        assertTrue(isIdentical(x_coded, found));
    }

    protected double divide_scalar_scalar_Coded(int        A, double     b) {
        // x=b/A
        double    	x = 0;

        x = b / A;

        return x;
    }


    @Test
    public void divide_matrix_matrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix x = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(6, 3, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(b, "b");
        eq.alias(x, "x");

        eq.process("x=b/A");

        assertTrue(A.solve(b).isIdentical(x, UtilEjml.TEST_F64));
        // eq: x=b/A -> x
        DMatrixRMaj x_coded = divide_matrix_matrix_Coded(A.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(x_coded, x));
    }

    protected DMatrixRMaj divide_matrix_matrix_Coded(DMatrixRMaj A, DMatrixRMaj b) {
        // x=b/A
        DMatrixRMaj	x = new DMatrixRMaj(1,1);

        x.reshape( A.numRows, b.numCols );
        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(A.numRows, A.numCols);
        boolean ok = solver.setA(A);
        solver.solve(b, x);

        return x;
    }


    @Test
    public void ldivide_matrix_matrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix x = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(6, 3, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(b, "b");
        eq.alias(x, "x");

        eq.process("x=A\\b");//hello

        assertTrue(A.solve(b).isIdentical(x, UtilEjml.TEST_F64));
        // eq: x=A\b -> x
        DMatrixRMaj x_coded = ldivide_matrix_matrix_Coded(A.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(x_coded, x));
    }

    protected DMatrixRMaj ldivide_matrix_matrix_Coded(DMatrixRMaj A, DMatrixRMaj b) {
        // x=A\b
        DMatrixRMaj	x = new DMatrixRMaj(1,1);

        x.reshape( A.numRows, b.numCols );
        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(A.numRows, A.numCols);
        boolean ok = solver.setA(A);
        solver.solve(b, x);

        return x;
    }


    @Test
    public void multiply_matrix_scalar1() {
        Equation eq = new Equation();

        SimpleMatrix x = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);

        eq.alias(2.5, "A");
        eq.alias(b, "b");
        eq.alias(x, "x");

        eq.process("x=b*A");
        assertTrue(b.scale(2.5).isIdentical(x, UtilEjml.TEST_F64));
        // eq: x=b*A -> x
        DMatrixRMaj x_coded = multiply_matrix_scalar1_Coded(2.5, b.getDDRM());
        assertTrue(isIdentical(x_coded, x));
    }

    protected DMatrixRMaj multiply_matrix_scalar1_Coded(double     A, DMatrixRMaj b) {
        // x=b*A
        DMatrixRMaj	x = new DMatrixRMaj(1,1);

        x.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.scale( A, b, x );

        return x;
    }


    @Test
    public void multiply_matrix_scalar2() {
        Equation eq = new Equation();

        SimpleMatrix x = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);

        eq.alias(2.5, "A");
        eq.alias(b, "b");
        eq.alias(x, "x");

        eq.process("x=A*b");
        assertTrue(b.scale(2.5).isIdentical(x, UtilEjml.TEST_F64));
        // eq: x=A*b -> x
        DMatrixRMaj x_coded = multiply_matrix_scalar2_Coded(2.5, b.getDDRM());
        assertTrue(isIdentical(x_coded, x));
    }

    protected DMatrixRMaj multiply_matrix_scalar2_Coded(double     A, DMatrixRMaj b) {
        // x=A*b
        DMatrixRMaj	x = new DMatrixRMaj(1,1);

        x.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.scale( A, b, x );

        return x;
    }


    @Test
    public void multiply_int_int() {
        Equation eq = new Equation();

        eq.alias(4, "A");
        eq.alias(13, "b");
        eq.alias(-1, "x");

        eq.process("x=b*A");

        int found = eq.lookupInteger("x");

        assertEquals(13 * 4, found, UtilEjml.TEST_F64);
        // eq: x=b*A -> x
        int x_coded = multiply_int_int_Coded(4, 13);
        assertTrue(isIdentical(x_coded, found));
    }

    protected int multiply_int_int_Coded(int        A, int        b) {
        // x=b*A
        int       	x = 0;

        x = b * A;

        return x;
    }


    @Test
    public void multiply_scalar_scalar() {
        Equation eq = new Equation();

        eq.alias(5, "A");
        eq.alias(4.2, "b");
        eq.alias(-1.0, "x");

        eq.process("x=b*A");

        double found = eq.lookupDouble("x");

        assertEquals(4.2 * 5.0, found, UtilEjml.TEST_F64);
        // eq: x=b*A -> x
        double x_coded = multiply_scalar_scalar_Coded(5, 4.2);
        assertTrue(isIdentical(x_coded, found));
    }

    protected double multiply_scalar_scalar_Coded(int        A, double     b) {
        // x=b*A
        double    	x = 0;

        x = b * A;

        return x;
    }


    @Test
    public void multiply_matrix_matrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix x = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(6, 3, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(b, "b");
        eq.alias(x, "x");

        eq.process("b=A*x");

        assertTrue(A.mult(x).isIdentical(b, UtilEjml.TEST_F64));
        // eq: b=A*x -> b
        DMatrixRMaj b_coded = multiply_matrix_matrix_Coded(A.getDDRM(), x.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj multiply_matrix_matrix_Coded(DMatrixRMaj A, DMatrixRMaj x) {
        // b=A*x
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( A.numRows, x.numCols );
        CommonOps_DDRM.mult( A, x, b );

        return b;
    }


    @Test
    public void elementMult_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix c = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);

        eq.alias(a, "a", b, "b", c, "c");

        eq.process("c=a.*b");

        assertTrue(a.elementMult(b).isIdentical(c, UtilEjml.TEST_F64));
        // eq: c=a.*b -> c
        DMatrixRMaj c_coded = elementMult_matrix_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(c_coded, c));
    }

    protected DMatrixRMaj elementMult_matrix_Coded(DMatrixRMaj a, DMatrixRMaj b) {
        // c=a.*b
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( a.numRows, a.numCols );
        CommonOps_DDRM.elementMult( a, b, c );

        return c;
    }


    @Test
    public void elementDivide_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix c = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);

        eq.alias(a,"a",b,"b",c,"c");

        eq.process("c=a./b");

        assertTrue(a.elementDiv(b).isIdentical(c, UtilEjml.TEST_F64));
        // eq: c=a./b -> c
        DMatrixRMaj c_coded = elementDivide_matrix_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(c_coded, c));
    }

    protected DMatrixRMaj elementDivide_matrix_Coded(DMatrixRMaj a, DMatrixRMaj b) {
        // c=a./b
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( a.numRows, a.numCols );
        CommonOps_DDRM.elementDiv( a, b, c );

        return c;
    }


    @Test
    public void elementPower_mm() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(6, 5, 0, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(6, 5, 0, 1, rand);
        SimpleMatrix c = SimpleMatrix.random_DDRM(6, 5, 0, 1, rand);

        eq.alias(a,"a",b,"b",c,"c");

        eq.process("c=a.^b");

        assertTrue(a.elementPower(b).isIdentical(c, UtilEjml.TEST_F64));
        // eq: c=a.^b -> c
        DMatrixRMaj c_coded = elementPower_mm_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(c_coded, c));
    }

    protected DMatrixRMaj elementPower_mm_Coded(DMatrixRMaj a, DMatrixRMaj b) {
        // c=a.^b
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( a.numRows, a.numCols );
        CommonOps_DDRM.elementPower( a, b, c );

        return c;
    }


    @Test
    public void elementPower_ms() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(6, 5, 0, 1, rand);
        double b = 1.1;
        SimpleMatrix c = SimpleMatrix.random_DDRM(6, 5, 0, 1, rand);

        eq.alias(a,"a",b,"b",c,"c");

        eq.process("c=a.^b");

        assertTrue(a.elementPower(b).isIdentical(c, UtilEjml.TEST_F64));
        // eq: c=a.^b -> c
        DMatrixRMaj c_coded = elementPower_ms_Coded(a.getDDRM(), b);
        assertTrue(isIdentical(c_coded, c));
    }

    protected DMatrixRMaj elementPower_ms_Coded(DMatrixRMaj a, double     b) {
        // c=a.^b
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( a.numRows, a.numCols );
        CommonOps_DDRM.elementPower( a, b, c );

        return c;
    }


    @Test
    public void elementPower_sm() {
        Equation eq = new Equation();

        double a = 1.1;
        SimpleMatrix b = SimpleMatrix.random_DDRM(6, 5, 0, 1, rand);
        SimpleMatrix c = SimpleMatrix.random_DDRM(6, 5, 0, 1, rand);

        eq.alias(a,"a",b,"b",c,"c");

        eq.process("c=a.^b");

        SimpleMatrix expected = new SimpleMatrix(6,5);
        CommonOps_DDRM.elementPower(a, (DMatrixRMaj)b.getMatrix(), (DMatrixRMaj)expected.getMatrix());
        assertTrue(expected.isIdentical(c, UtilEjml.TEST_F64));
        // eq: c=a.^b -> c
        DMatrixRMaj c_coded = elementPower_sm_Coded(a, b.getDDRM());
        assertTrue(isIdentical(c_coded, c));
    }

    protected DMatrixRMaj elementPower_sm_Coded(double     a, DMatrixRMaj b) {
        // c=a.^b
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.elementPower( a, b, c );

        return c;
    }


    @Test
    public void elementPower_ss() {
        Equation eq = new Equation();

        double a = 1.1;
        double b = 0.7;

        eq.alias(a,"a",b,"b");

        eq.process("c=a.^b");

        double found = eq.lookupDouble("c");

        assertEquals(Math.pow(a, b), found, UtilEjml.TEST_F64);
        // eq: c=a.^b -> c
        double c_coded = elementPower_ss_Coded(a, b);
        assertTrue(isIdentical(c_coded, found));
    }

    protected double elementPower_ss_Coded(double     a, double     b) {
        // c=a.^b
        double    	c = 0;

        c = Math.pow(a, b);

        return c;
    }


    @Test
    public void kron_matrix_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(2, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 2, -1, 1, rand);
        SimpleMatrix c = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);

        eq.alias(a,"a",b,"b",c,"c");

        eq.process("c=kron(a,b)");

        assertTrue(a.kron(b).isIdentical(c, UtilEjml.TEST_F64));
        // eq: c=kron(a,b) -> c
        DMatrixRMaj c_coded = kron_matrix_matrix_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(c_coded, c));
    }

    protected DMatrixRMaj kron_matrix_matrix_Coded(DMatrixRMaj a, DMatrixRMaj b) {
        // c=kron(a,b)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( a.numRows * b.numRows, a.numCols * b.numCols );
        CommonOps_DDRM.kron( a, b, c );

        return c;
    }


    @Test
    public void power_double_double() {
        Equation eq = new Equation();

        eq.alias(1.1,"a");
        eq.process("a=2.3^4.2");

        assertEquals(Math.pow(2.3, 4.2), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=2.3^4.2 -> a
        double a_coded = power_double_double_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double power_double_double_Coded() {
        // a=2.3^4.2
        double    	a = 0;

        a = (Math.pow(2.3, 4.2));

        return a;
    }


    @Test
    public void power_int_int() {
        Equation eq = new Equation();

        eq.alias(1.1,"a");
        eq.process("a=2^4");

        assertEquals(Math.pow(2, 4), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=2^4 -> a
        double a_coded = power_int_int_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double power_int_int_Coded() {
        // a=2^4
        double    	a = 0;

        a = (Math.pow(2, 4));

        return a;
    }


    @Test
    public void sqrt_int() {
        Equation eq = new Equation();

        eq.process("a=sqrt(5)");

        assertEquals(Math.sqrt(5),eq.lookupDouble("a"),UtilEjml.TEST_F64);
        // eq: a=sqrt(5) -> a
        double a_coded = sqrt_int_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double sqrt_int_Coded() {
        // a=sqrt(5)
        double    	a = 0;

        a = (Math.sqrt(5));

        return a;
    }


    @Test
    public void sqrt_double() {
        Equation eq = new Equation();

        eq.process("a=sqrt(5.7)");

        assertEquals(Math.sqrt(5.7), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=sqrt(5.7) -> a
        double a_coded = sqrt_double_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double sqrt_double_Coded() {
        // a=sqrt(5.7)
        double    	a = 0;

        a = (Math.sqrt(5.7));

        return a;
    }


    @Test
    public void atan2_scalar() {
        Equation eq = new Equation();

        eq.alias(1.1,"a");
        eq.process("a=atan2(1.1,0.5)");

        assertEquals(Math.atan2(1.1, 0.5), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=atan2(1.1,0.5) -> a
        double a_coded = atan2_scalar_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double atan2_scalar_Coded() {
        // a=atan2(1.1,0.5)
        double    	a = 0;

        a = (Math.atan2(1.1, 0.5));

        return a;
    }


    @Test
    public void neg_int() {
        Equation eq = new Equation();

        eq.alias(2,"a");
        eq.alias(3,"b");
        eq.process("a=-b");

        assertEquals(-3, eq.lookupInteger("a"));
        // eq: a=-b -> a
        int a_coded = neg_int_Coded(3);
        assertTrue(isIdentical(a_coded, eq.lookupInteger("a")));
    }

    protected int neg_int_Coded(int        b) {
        // a=-b
        int       	a = 0;

        a = -b;

        return a;
    }


    @Test
    public void neg_scalar() {
        Equation eq = new Equation();

        eq.alias(2.1,"a");
        eq.alias(3.1,"b");
        eq.process("a=-b");

        assertEquals(-3.1, eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=-b -> a
        double a_coded = neg_scalar_Coded(3.1);
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double neg_scalar_Coded(double     b) {
        // a=-b
        double    	a = 0;

        a = -b;

        return a;
    }


    @Test
    public void neg_matrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(1, 1, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(B, "B");

        eq.process("A=-B");

        for (int i = 0; i < A.getNumElements(); i++) {
            assertEquals(-A.get(i),B.get(i),UtilEjml.TEST_F64);
        }
        // eq: A=-B -> A
        DMatrixRMaj A_coded = neg_matrix_Coded(B.getDDRM());
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj neg_matrix_Coded(DMatrixRMaj B) {
        // A=-B
        DMatrixRMaj	A = new DMatrixRMaj(1,1);

        A.reshape( B.numRows, B.numCols );
        CommonOps_DDRM.changeSign( B, A );

        return A;
    }


    @Test
    public void sin() {
        Equation eq = new Equation();

        eq.alias(1.1,"a");
        eq.process("a=sin(2.1)");

        assertEquals(Math.sin(2.1), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=sin(2.1) -> a
        double a_coded = sin_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double sin_Coded() {
        // a=sin(2.1)
        double    	a = 0;

        a = (Math.sin(2.1));

        return a;
    }


    @Test
    public void cos() {
        Equation eq = new Equation();

        eq.alias(1.1,"a");
        eq.process("a=cos(2.1)");

        assertEquals(Math.cos(2.1), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=cos(2.1) -> a
        double a_coded = cos_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double cos_Coded() {
        // a=cos(2.1)
        double    	a = 0;

        a = (Math.cos(2.1));

        return a;
    }


    @Test
    public void atan() {
        Equation eq = new Equation();

        eq.alias(1.1,"a");
        eq.process("a=atan(2.1)");

        assertEquals(Math.atan(2.1), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=atan(2.1) -> a
        double a_coded = atan_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double atan_Coded() {
        // a=atan(2.1)
        double    	a = 0;

        a = (Math.atan(2.1));

        return a;
    }


    @Test
    public void exp_s() {
        Equation eq = new Equation();

        eq.alias(1.1,"a");
        eq.process("a=exp(2.1)");

        assertEquals(Math.exp(2.1), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=exp(2.1) -> a
        double a_coded = exp_s_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double exp_s_Coded() {
        // a=exp(2.1)
        double    	a = 0;

        a = (Math.exp(2.1));

        return a;
    }


    @Test
    public void exp_m() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,0,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,0,1,rand);

        eq.alias(a,"a",b,"b");
        eq.process("b=exp(a)");

        SimpleMatrix expected = a.elementExp();

        assertTrue(expected.isIdentical(b, UtilEjml.TEST_F64));
        // eq: b=exp(a) -> b
        DMatrixRMaj b_coded = exp_m_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj exp_m_Coded(DMatrixRMaj a) {
        // b=exp(a)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, a.numCols );
        CommonOps_DDRM.elementExp( a, b );

        return b;
    }


    @Test
    public void log_s() {
        Equation eq = new Equation();

        eq.alias(1.1,"a");
        eq.process("a=log(2.1)");

        assertEquals(Math.log(2.1), eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=log(2.1) -> a
        double a_coded = log_s_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double log_s_Coded() {
        // a=log(2.1)
        double    	a = 0;

        a = (Math.log(2.1));

        return a;
    }


    @Test
    public void log_m() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,0,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,0,1,rand);

        eq.alias(a,"a",b,"b");
        eq.process("b=log(a)");

        SimpleMatrix expected = a.elementLog();

        assertTrue(expected.isIdentical(b, UtilEjml.TEST_F64));
        // eq: b=log(a) -> b
        DMatrixRMaj b_coded = log_m_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj log_m_Coded(DMatrixRMaj a) {
        // b=log(a)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, a.numCols );
        CommonOps_DDRM.elementLog( a, b );

        return b;
    }


    @Test
    public void add_int_int() {
        Equation eq = new Equation();

        eq.alias(1,"a");
        eq.process("a=2 + 3");

        assertEquals(5, eq.lookupInteger("a"), UtilEjml.TEST_F64);
        // eq: a=2 + 3 -> a
        int a_coded = add_int_int_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupInteger("a")));
    }

    protected int add_int_int_Coded() {
        // a=2 + 3
        int       	a = 0;

        a = (2 + 3);

        return a;
    }


    @Test
    public void add_scalar_scalar() {
        Equation eq = new Equation();

        eq.alias(1.2,"a");
        eq.process("a= 2.3 + 3");

        assertEquals(5.3, eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a= 2.3 + 3 -> a
        double a_coded = add_scalar_scalar_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double add_scalar_scalar_Coded() {
        // a= 2.3 + 3
        double    	a = 0;

        a = (2.3 + 3);

        return a;
    }


    @Test
    public void add_matrix_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix c = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a",b,"b",c,"c");
        eq.process("a=b+c");

        assertTrue(b.plus(c).isIdentical(a, UtilEjml.TEST_F64));
        // eq: a=b+c -> a
        DMatrixRMaj a_coded = add_matrix_matrix_Coded(b.getDDRM(), c.getDDRM());
        assertTrue(isIdentical(a_coded, a));
    }

    protected DMatrixRMaj add_matrix_matrix_Coded(DMatrixRMaj b, DMatrixRMaj c) {
        // a=b+c
        DMatrixRMaj	a = new DMatrixRMaj(1,1);

        a.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.add( b, c, a );

        return a;
    }


    @Test
    public void add_matrix_scalar_1() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a",b,"b");

        eq.process("a=b+2.2");
        assertTrue(b.plus(2.2).isIdentical(a, UtilEjml.TEST_F64));
        // eq: a=b+2.2 -> a
        DMatrixRMaj a_coded = add_matrix_scalar_1_Coded(b.getDDRM());
        assertTrue(isIdentical(a_coded, a));
    }

    protected DMatrixRMaj add_matrix_scalar_1_Coded(DMatrixRMaj b) {
        // a=b+2.2
        DMatrixRMaj	a = new DMatrixRMaj(1,1);

        a.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.add( b, 2.2, a );

        return a;
    }


    @Test
    public void add_matrix_scalar_2() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a",b,"b");

        eq.process("a=2.2+b");
        assertTrue(b.plus(2.2).isIdentical(a, UtilEjml.TEST_F64));
        // eq: a=2.2+b -> a
        DMatrixRMaj a_coded = add_matrix_scalar_2_Coded(b.getDDRM());
        assertTrue(isIdentical(a_coded, a));
    }

    protected DMatrixRMaj add_matrix_scalar_2_Coded(DMatrixRMaj b) {
        // a=2.2+b
        DMatrixRMaj	a = new DMatrixRMaj(1,1);

        a.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.add( b, 2.2, a );

        return a;
    }


    @Test
    public void subtract_int_int() {
        Equation eq = new Equation();

        eq.alias(1, "a");
        eq.process("a=2 - 3");

        assertEquals(-1, eq.lookupInteger("a"), UtilEjml.TEST_F64);
        // eq: a=2 - 3 -> a
        int a_coded = subtract_int_int_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupInteger("a")));
    }

    protected int subtract_int_int_Coded() {
        // a=2 - 3
        int       	a = 0;

        a = (2 - 3);

        return a;
    }


    @Test
    public void subtract_scalar_scalar() {
        Equation eq = new Equation();

        eq.alias(1.2, "a");
        eq.process("a= 2.3 - 3");

        assertEquals(2.3 - 3.0, eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a= 2.3 - 3 -> a
        double a_coded = subtract_scalar_scalar_Coded();
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double subtract_scalar_scalar_Coded() {
        // a= 2.3 - 3
        double    	a = 0;

        a = (2.3 - 3);

        return a;
    }


    @Test
    public void subtract_matrix_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix c = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a, "a", b, "b", c, "c");
        eq.process("a=b-c");

        assertTrue(b.minus(c).isIdentical(a, UtilEjml.TEST_F64));
        // eq: a=b-c -> a
        DMatrixRMaj a_coded = subtract_matrix_matrix_Coded(b.getDDRM(), c.getDDRM());
        assertTrue(isIdentical(a_coded, a));
    }

    protected DMatrixRMaj subtract_matrix_matrix_Coded(DMatrixRMaj b, DMatrixRMaj c) {
        // a=b-c
        DMatrixRMaj	a = new DMatrixRMaj(1,1);

        a.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.subtract( b, c, a );

        return a;
    }


    @Test
    public void subtract_matrix_scalar_1() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a",b,"b");

        eq.process("a=b-2.2");
        assertTrue(b.plus(-2.2).isIdentical(a, UtilEjml.TEST_F64));
        // eq: a=b-2.2 -> a
        DMatrixRMaj a_coded = subtract_matrix_scalar_1_Coded(b.getDDRM());
        assertTrue(isIdentical(a_coded, a));
    }

    protected DMatrixRMaj subtract_matrix_scalar_1_Coded(DMatrixRMaj b) {
        // a=b-2.2
        DMatrixRMaj	a = new DMatrixRMaj(1,1);

        a.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.subtract( b, 2.2, a );

        return a;
    }


    @Test
    public void subtract_matrix_scalar_2() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a",b,"b");

        eq.process("a=2.2-b");

        DMatrixRMaj expected = new DMatrixRMaj(3,4);
        CommonOps_DDRM.subtract(2.2, (DMatrixRMaj)b.getMatrix(), expected);
        assertTrue(SimpleMatrix.wrap(expected).isIdentical(a, UtilEjml.TEST_F64));
        // eq: a=2.2-b -> a
        DMatrixRMaj a_coded = subtract_matrix_scalar_2_Coded(b.getDDRM());
        assertTrue(isIdentical(a_coded, a));
    }

    protected DMatrixRMaj subtract_matrix_scalar_2_Coded(DMatrixRMaj b) {
        // a=2.2-b
        DMatrixRMaj	a = new DMatrixRMaj(1,1);

        a.reshape( b.numRows, b.numCols );
        CommonOps_DDRM.subtract( 2.2, b, a );

        return a;
    }


    @Test
    public void copy_matrix_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a",b,"b");
        eq.process("b=a");

        assertTrue(a.isIdentical(b, UtilEjml.TEST_F64));
        // eq: b=a -> b
        DMatrixRMaj b_coded = copy_matrix_matrix_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_matrix_matrix_Coded(DMatrixRMaj a) {
        // b=a
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, a.numCols );
        b.set( a );

        return b;
    }


    @Test
    public void copy_int_int() {
        Equation eq = new Equation();

        eq.alias(2,"a");
        eq.alias(3, "b");

        eq.process("a=b");

        assertEquals(3, eq.lookupInteger("a"));
        // eq: a=b -> a
        int a_coded = copy_int_int_Coded(3);
        assertTrue(isIdentical(a_coded, eq.lookupInteger("a")));
    }

    protected int copy_int_int_Coded(int        b) {
        // a=b
        int       	a = 0;

        a = b;

        return a;
    }


    @Test
    public void copy_double_scalar_1() {
        Equation eq = new Equation();

        // int to double
        eq.alias(2.2,"a");
        eq.alias(3,"b");

        eq.process("a=b");
        assertEquals(3, eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=b -> a
        double a_coded = copy_double_scalar_1_Coded(3);
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double copy_double_scalar_1_Coded(int        b) {
        // a=b
        double    	a = 0;

        a = b;

        return a;
    }


    @Test
    public void copy_double_scalar_2() {
        Equation eq = new Equation();

        // int to double
        eq.alias(2.2,"a");
        eq.alias(3,"b");

        // double to double
        eq.alias(3.5, "c");
        eq.process("a=c");
        assertEquals(3.5, eq.lookupDouble("a"), UtilEjml.TEST_F64);
        // eq: a=c -> a
        double a_coded = copy_double_scalar_2_Coded(3, 3.5);
        assertTrue(isIdentical(a_coded, eq.lookupDouble("a")));
    }

    protected double copy_double_scalar_2_Coded(int        b, double     c) {
        // a=c
        double    	a = 0;

        a = c;

        return a;
    }


    @Test
    public void copy_submatrix_matrix_case0() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(2,3,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a",b,"b");
        eq.process("b(1:2,1:3)=a");

        assertTrue(a.isIdentical(b.extractMatrix(1, 3, 1, 4), UtilEjml.TEST_F64));
        // eq: b(1:2,1:3)=a -> b
        DMatrixRMaj b_coded = copy_submatrix_matrix_case0_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_matrix_case0_Coded(DMatrixRMaj a, final DMatrixRMaj b_in) {
        // b(1:2,1:3)=a
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( a, b, 1, 1 );

        return b;
    }


    @Test
    public void copy_submatrix_matrix_case1() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(2,3,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a, "a", b, "b");
        eq.process("b(0 1,3 2 0)=a");

        int rows[] = new int[]{0,1};
        int cols[] = new int[]{3,2,0};

        checkSubMatrixArraysInsert(a, b, rows, cols);
        // eq: b(0 1,3 2 0)=a -> b
        DMatrixRMaj b_coded = copy_submatrix_matrix_case1_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_matrix_case1_Coded(DMatrixRMaj a, final DMatrixRMaj b_in) {
        // b(0 1,3 2 0)=a
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( a, b, new int[] {0,1}, 2, new int[] {3,2,0}, 3 );

        return b;
    }


    @Test
    public void copy_submatrix_matrix_case2() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,2,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a, "a", b, "b");
        eq.process("b(:,2:)=a");

        int rows[] = new int[]{0,1,2};
        int cols[] = new int[]{2,3};

        checkSubMatrixArraysInsert(a, b, rows, cols);
        // eq: b(:,2:)=a -> b
        DMatrixRMaj b_coded = copy_submatrix_matrix_case2_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_matrix_case2_Coded(DMatrixRMaj a, final DMatrixRMaj b_in) {
        // b(:,2:)=a
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( a, b, 0, 2 );

        return b;
    }


    @Test
    public void copy_submatrix_matrix_case3() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(6,1,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a, "a", b, "b");
        eq.process("b(2 3 4 5 6 7)=a");

        for (int i = 0; i < 6; i++) {
            assertEquals(b.get(i+2),a.get(i),UtilEjml.TEST_F64);
        }
        // eq: b(2 3 4 5 6 7)=a -> b
        DMatrixRMaj b_coded = copy_submatrix_matrix_case3_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_matrix_case3_Coded(DMatrixRMaj a, final DMatrixRMaj b_in) {
        // b(2 3 4 5 6 7)=a
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( a, b, new int[] {2,3,4,5,6,7}, 6 );

        return b;
    }


    @Test
    public void copy_submatrix_matrix_case4() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(7,1,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a, "a", b, "b");
        eq.process("b(2:8)=a");

        for (int i = 0; i < 7; i++) {
            assertEquals(b.get(i+2),a.get(i),UtilEjml.TEST_F64);
        }
        // eq: b(2:8)=a -> b
        DMatrixRMaj b_coded = copy_submatrix_matrix_case4_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_matrix_case4_Coded(DMatrixRMaj a, final DMatrixRMaj b_in) {
        // b(2:8)=a
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( a, b, IntStream.iterate(2, n -> n + 1).limit(1+(8 - 2) / 1).toArray(), (8+1 - 2) );

        return b;
    }


    @Test
    public void copy_submatrix_matrix_case5() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3*4-2,1,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a, "a", b, "b");
        eq.process("b(2:)=a");

        for (int i = 0; i < a.getNumElements(); i++) {
            assertEquals(b.get(i+2),a.get(i),UtilEjml.TEST_F64);
        }
        // eq: b(2:)=a -> b
        DMatrixRMaj b_coded = copy_submatrix_matrix_case5_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_matrix_case5_Coded(DMatrixRMaj a, final DMatrixRMaj b_in) {
        // b(2:)=a
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( a, b, IntStream.iterate(2, n -> n + 1).limit(1+(b.numRows*b.numCols - 2) / 1).toArray(), (b.numRows*b.numCols - 2) );

        return b;
    }


    @Test
    public void copy_submatrix_matrix_case6() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3*4-2,1,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a, "a", b, "b");
        eq.process("b(2 3:)=a");

        for (int i = 0; i < a.getNumElements(); i++) {
            assertEquals(b.get(i+2),a.get(i),UtilEjml.TEST_F64);
        }
        // eq: b(2 3:)=a -> b
        DMatrixRMaj b_coded = copy_submatrix_matrix_case6_Coded(a.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_matrix_case6_Coded(DMatrixRMaj a, final DMatrixRMaj b_in) {
        // b(2 3:)=a
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( a, b, Stream.of(new int[] {2},IntStream.iterate(3, n -> n + 1).limit(1+(b.numRows*b.numCols - 3) / 1).toArray()).flatMapToInt(IntStream::of).toArray(), (1+(b.numRows*b.numCols-3)) );

        return b;
    }


    @Test
    public void copy_submatrix_scalar_case0() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(b, "b");
        eq.process("b(2,3)=4.5");
        eq.process("b(0,0)=3.5");

        assertEquals(3.5, b.get(0, 0), UtilEjml.TEST_F64);
        assertEquals(4.5, b.get(2, 3), UtilEjml.TEST_F64);
    }


    @Test
    public void copy_submatrix_scalar_case1() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(b, "b");
        eq.process("b(0:1,1:3)=4.5");

        int rows[] = new int[]{0,1};
        int cols[] = new int[]{1,2,3};

        checkSubMatrixArraysInsert(4.5, b, rows, cols);
        // eq: b(0:1,1:3)=4.5 -> b
        DMatrixRMaj b_coded = copy_submatrix_scalar_case1_Coded(b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_scalar_case1_Coded(final DMatrixRMaj b_in) {
        // b(0:1,1:3)=4.5
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( new DMatrixRMaj((1+1 - 0), (3+1 - 1), 4.5), b, 0, 1 );

        return b;
    }


    @Test
    public void copy_submatrix_scalar_case2() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(b, "b");
        eq.process("b(:,2:)=4.5");

        int rows[] = new int[]{0, 1, 2};
        int cols[] = new int[]{2,3};

        checkSubMatrixArraysInsert(4.5,b,rows,cols);
        // eq: b(:,2:)=4.5 -> b
        DMatrixRMaj b_coded = copy_submatrix_scalar_case2_Coded(b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_scalar_case2_Coded(final DMatrixRMaj b_in) {
        // b(:,2:)=4.5
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( new DMatrixRMaj((b.numRows - 0), (b.numCols - 2), 4.5), b, 0, 2 );

        return b;
    }


    @Test
    public void copy_submatrix_scalar_case3() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(b, "b");
        eq.process("b(1 0 3)=4.5");

        int indexes[] = new int[]{1,0,3};
        for (int i = 0; i < indexes.length; i++) {
            assertEquals(b.get(indexes[i]),4.5,UtilEjml.TEST_F64);
        }
        // eq: b(1 0 3)=4.5 -> b
        DMatrixRMaj b_coded = copy_submatrix_scalar_case3_Coded(b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_scalar_case3_Coded(final DMatrixRMaj b_in) {
        // b(1 0 3)=4.5
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( new DMatrixRMaj(1, 3, 4.5), b, new int[] {1,0,3}, 3 );

        return b;
    }


    @Test
    public void copy_submatrix_scalar_case4() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(b, "b");
        eq.process("b(1:3)=4.5");

        int indexes[] = new int[]{1,2,3};
        for (int i = 0; i < indexes.length; i++) {
            assertEquals(b.get(indexes[i]),4.5,UtilEjml.TEST_F64);
        }
        // eq: b(1:3)=4.5 -> b
        DMatrixRMaj b_coded = copy_submatrix_scalar_case4_Coded(b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_scalar_case4_Coded(final DMatrixRMaj b_in) {
        // b(1:3)=4.5
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( new DMatrixRMaj(1, (3+1 - 1), 4.5), b, IntStream.iterate(1, n -> n + 1).limit(1+(3 - 1) / 1).toArray(), (3+1 - 1) );

        return b;
    }


    @Test
    public void copy_submatrix_scalar_case5() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(2,3,-1,1,rand);

        eq.alias(b, "b");
        eq.process("b(2 3:)=4.5");

        int indexes[] = new int[]{2,3,4,5};
        for (int i = 0; i < indexes.length; i++) {
            assertEquals(b.get(indexes[i]),4.5,UtilEjml.TEST_F64);
        }
        // eq: b(2 3:)=4.5 -> b
        DMatrixRMaj b_coded = copy_submatrix_scalar_case5_Coded(b.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj copy_submatrix_scalar_case5_Coded(final DMatrixRMaj b_in) {
        // b(2 3:)=4.5
        DMatrixRMaj	b = new DMatrixRMaj(b_in);

        CommonOps_DDRM.insert( new DMatrixRMaj(1, (1+(b.numRows*b.numCols-3)), 4.5), b, Stream.of(new int[] {2},IntStream.iterate(3, n -> n + 1).limit(1+(b.numRows*b.numCols - 3) / 1).toArray()).flatMapToInt(IntStream::of).toArray(), (1+(b.numRows*b.numCols-3)) );

        return b;
    }


    @Test
    public void extract_one_case0() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(b,"b");
        eq.process("c=b(1 2)");
        DMatrixRMaj found = eq.lookupDDRM("c");

        assertTrue(found.numRows == 1 && found.numCols == 2);
        assertEquals(b.get(1), found.get(0), UtilEjml.TEST_F64);
        assertEquals(b.get(2), found.get(1), UtilEjml.TEST_F64);
        // eq: c=b(1 2) -> c
        DMatrixRMaj c_coded = extract_one_case0_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_one_case0_Coded(DMatrixRMaj b) {
        // c=b(1 2)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( 1, 2 );
        CommonOps_DDRM.extract( b, new int[] {1,2}, 2, c );

        return c;
    }


    @Test
    public void extract_one_case1() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 4, -1, 1, rand);

        eq.alias(b, "b");
        eq.process("c=b(1:3)");
        DMatrixRMaj found = eq.lookupDDRM("c");

        assertTrue(found.numRows == 1 && found.numCols == 3);
        for (int i = 0; i < found.numCols; i++) {
            assertEquals(b.get(i+1), found.get(i), UtilEjml.TEST_F64);
        }
        // eq: c=b(1:3) -> c
        DMatrixRMaj c_coded = extract_one_case1_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_one_case1_Coded(DMatrixRMaj b) {
        // c=b(1:3)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( 1, (3+1 - 1) );
        CommonOps_DDRM.extract( b, IntStream.iterate(1, n -> n + 1).limit(1+(3 - 1) / 1).toArray(), (3+1 - 1), c );

        return c;
    }


    @Test
    public void extract_one_case2() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 4, -1, 1, rand);

        eq.alias(b, "b");
        eq.process("c=b(4:)");
        DMatrixRMaj found = eq.lookupDDRM("c");

        assertTrue(found.numRows == 1 && found.numCols == b.getNumElements()-4);
        for (int i = 0; i < found.numCols; i++) {
            assertEquals(b.get(i+4), found.get(i), UtilEjml.TEST_F64);
        }
        // eq: c=b(4:) -> c
        DMatrixRMaj c_coded = extract_one_case2_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_one_case2_Coded(DMatrixRMaj b) {
        // c=b(4:)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( 1, (b.numRows*b.numCols - 4) );
        CommonOps_DDRM.extract( b, IntStream.iterate(4, n -> n + 1).limit(1+(b.numRows*b.numCols - 4) / 1).toArray(), (b.numRows*b.numCols - 4), c );

        return c;
    }


    @Test
    public void extract_one_case3() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 4, -1, 1, rand);

        eq.alias(b, "b");
        eq.process("c=b(:)");
        DMatrixRMaj found = eq.lookupDDRM("c");

        assertTrue(found.numRows == 1 && found.numCols == b.getNumElements());
        for (int i = 0; i < found.numCols; i++) {
            assertEquals(b.get(i), found.get(i), UtilEjml.TEST_F64);
        }
        // eq: c=b(:) -> c
        DMatrixRMaj c_coded = extract_one_case3_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_one_case3_Coded(DMatrixRMaj b) {
        // c=b(:)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( 1, (b.numRows*b.numCols - 0) );
        CommonOps_DDRM.extract( b, IntStream.iterate(0, n -> n + 1).limit(1+(b.numRows*b.numCols - 0) / 1).toArray(), (b.numRows*b.numCols - 0), c );

        return c;
    }


    @Test
    public void extract_two_case0() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(b,"b");
        eq.process("c=b(1 2,1 0 2)");
        DMatrixRMaj found = eq.lookupDDRM("c");

        int rows[] = new int[]{1,2};
        int cols[] = new int[]{1,0,2};

        checkSubMatrixArraysExtract(b, found, rows, cols);
        // eq: c=b(1 2,1 0 2) -> c
        DMatrixRMaj c_coded = extract_two_case0_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_two_case0_Coded(DMatrixRMaj b) {
        // c=b(1 2,1 0 2)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( 2, 3 );
        CommonOps_DDRM.extract( b, new int[] {1,2}, 2, new int[] {1,0,2}, 3, c );

        return c;
    }


    @Test
    public void extract_two_case1() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 4, -1, 1, rand);

        eq.alias(b,"b");
        eq.process("c=b(1:2,2:3)");
        DMatrixRMaj found = eq.lookupDDRM("c");

        int rows[] = new int[]{1,2};
        int cols[] = new int[]{2,3};

        checkSubMatrixArraysExtract(b, found, rows, cols);
        // eq: c=b(1:2,2:3) -> c
        DMatrixRMaj c_coded = extract_two_case1_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_two_case1_Coded(DMatrixRMaj b) {
        // c=b(1:2,2:3)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( (2+1 - 1), (3+1 - 2) );
        CommonOps_DDRM.extract( b, 1, 2+1, 2, 3+1, c );

        return c;
    }


    @Test
    public void extract_two_case2() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 4, -1, 1, rand);

        eq.alias(b, "b");
        eq.process("c=b(2:,1:)");
        DMatrixRMaj found = eq.lookupDDRM("c");

        int rows[] = new int[]{2};
        int cols[] = new int[]{1,2,3};

        checkSubMatrixArraysExtract(b, found, rows, cols);
        // eq: c=b(2:,1:) -> c
        DMatrixRMaj c_coded = extract_two_case2_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_two_case2_Coded(DMatrixRMaj b) {
        // c=b(2:,1:)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( (b.numRows - 2), (b.numCols - 1) );
        CommonOps_DDRM.extract( b, 2, b.numRows, 1, b.numCols, c );

        return c;
    }


    @Test
    public void extract_two_case3() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 4, -1, 1, rand);

        eq.alias(b, "b");
        eq.process("c=b(:,:)");
        DMatrixRMaj found = eq.lookupDDRM("c");

        int rows[] = new int[]{0,1,2};
        int cols[] = new int[]{0,1,2,3};

        checkSubMatrixArraysExtract(b, found, rows, cols);
        // eq: c=b(:,:) -> c
        DMatrixRMaj c_coded = extract_two_case3_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_two_case3_Coded(DMatrixRMaj b) {
        // c=b(:,:)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( (b.numRows - 0), (b.numCols - 0) );
        CommonOps_DDRM.extract( b, 0, b.numRows, 0, b.numCols, c );

        return c;
    }


    @Test
    public void extract_two_case4() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 4, -1, 1, rand);

        eq.alias(b, "b");
        int i1 = 1; 
        int i2 = 2;
        int j1 = 1;
        int j2 = 3;
        eq.alias(i1, "i1", i2, "i2", j1, "j1", j2, "j2");
        eq.process("c=b(i1:i2,j1:j2)");
        DMatrixRMaj found = eq.lookupDDRM("c");
        // eq: c=b(i1:i2,j1:j2) -> c
        DMatrixRMaj c_coded = extract_two_case4_Coded(b.getDDRM(), j1, i1, j2, i2);
        assertTrue(isIdentical(c_coded, found));
    }

    protected DMatrixRMaj extract_two_case4_Coded(DMatrixRMaj b, int        j1, int        i1, int        j2, int        i2) {
        // c=b(i1:i2,j1:j2)
        DMatrixRMaj	c = new DMatrixRMaj(1,1);

        c.reshape( (i2+1 - i1), (j2+1 - j1) );
        CommonOps_DDRM.extract( b, i1, i2+1, j1, j2+1, c );

        return c;
    }


    @Test
    public void extractScalar_one() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(b,"b");
        eq.process("c=b(3)");
        double found = eq.lookupDouble("c");

        assertEquals(b.get(3), found, UtilEjml.TEST_F64);
        // eq: c=b(3) -> c
        double c_coded = extractScalar_one_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected double extractScalar_one_Coded(DMatrixRMaj b) {
        // c=b(3)
        double    	c = 0;

        c = b.get(3);

        return c;
    }


    @Test
    public void extractScalar_two() {
        Equation eq = new Equation();

        SimpleMatrix b = SimpleMatrix.random_DDRM(3, 4, -1, 1, rand);

        eq.alias(b,"b");
        eq.process("c=b(2,3)");
        double found = eq.lookupDouble("c");

        assertEquals(b.get(2,3), found, UtilEjml.TEST_F64);
        // eq: c=b(2,3) -> c
        double c_coded = extractScalar_two_Coded(b.getDDRM());
        assertTrue(isIdentical(c_coded, found));
    }

    protected double extractScalar_two_Coded(DMatrixRMaj b) {
        // c=b(2,3)
        double    	c = 0;

        c = b.get(2, 3);

        return c;
    }


    @Test
    public void transpose_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a",b,"b");
        eq.process("b=a'");

        assertTrue(a.transpose().isIdentical(b, UtilEjml.TEST_F64));
        // eq: b=a' -> b
        DMatrixRMaj b_coded = transpose_matrix_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj transpose_matrix_Coded(DMatrixRMaj a) {
        // b=a'
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numCols, a.numRows );
        CommonOps_DDRM.transpose( a, b );

        return b;
    }


    @Test
    public void transpose_then_subtract() {
        SimpleMatrix a = SimpleMatrix.random_DDRM(3,3,-1,1,rand);
        SimpleMatrix c = SimpleMatrix.random_DDRM(3,3,-1,1,rand);

        Equation eq = new Equation();
        eq.alias(a,"a",c,"c");
        eq.process("z = a' - c");

        SimpleMatrix z = eq.lookupSimple("z");

        SimpleMatrix expected = a.transpose().minus(c);
        assertTrue(expected.isIdentical(z, UtilEjml.TEST_F64));
        // eq: z = a' - c -> z
        DMatrixRMaj z_coded = transpose_then_subtract_Coded(a.getDDRM(), c.getDDRM());
        assertTrue(isIdentical(z_coded, z));
    }

    protected DMatrixRMaj transpose_then_subtract_Coded(DMatrixRMaj a, DMatrixRMaj c) {
        // z = a' - c
        DMatrixRMaj	z = new DMatrixRMaj(1,1);
        DMatrixRMaj	tm1 = new DMatrixRMaj(1,1);

        tm1.reshape( a.numCols, a.numRows );
        CommonOps_DDRM.transpose( a, tm1 );
        z.reshape( tm1.numRows, tm1.numCols );
        CommonOps_DDRM.subtract( tm1, c, z );

        return z;
    }


    @Test
    public void inv_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,3,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(3,3,-1,1,rand);

        eq.alias(a,"a",b,"b");
        eq.process("b=inv(a)");

        assertTrue(a.invert().isIdentical(b, UtilEjml.TEST_F64));
        // eq: b=inv(a) -> b
        DMatrixRMaj b_coded = inv_matrix_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj inv_matrix_Coded(DMatrixRMaj a) {
        // b=inv(a)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, a.numCols );
        boolean ok = CommonOps_DDRM.invert(a, b);

        return b;
    }


    @Test
    public void inv_scalar() {
        Equation eq = new Equation();

        eq.alias(2.2,"a",3.3,"b");
        eq.process("b=inv(a)");

        assertEquals(1.0 / 2.2, eq.lookupDouble("b"), UtilEjml.TEST_F64);
        // eq: b=inv(a) -> b
        double b_coded = inv_scalar_Coded(2.2);
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double inv_scalar_Coded(double     a) {
        // b=inv(a)
        double    	b = 0;

        b = 1.0 / a;

        return b;
    }


    @Test
    public void pinv_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(4,3,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(1,1,-1,1,rand);

        eq.alias(a,"a",b,"b");
        eq.process("b=pinv(a)");

        assertTrue(a.pseudoInverse().isIdentical(b, UtilEjml.TEST_F64));
        // eq: b=pinv(a) -> b
        DMatrixRMaj b_coded = pinv_matrix_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj pinv_matrix_Coded(DMatrixRMaj a) {
        // b=pinv(a)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, a.numCols );
        CommonOps_DDRM.pinv( a, b );

        return b;
    }


    @Test
    public void pinv_scalar() {
        Equation eq = new Equation();

        eq.alias(2.2,"a",3.3,"b");
        eq.process("b=pinv(a)");

        assertEquals(1.0 / 2.2, eq.lookupDouble("b"), UtilEjml.TEST_F64);
        // eq: b=pinv(a) -> b
        double b_coded = pinv_scalar_Coded(2.2);
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double pinv_scalar_Coded(double     a) {
        // b=pinv(a)
        double    	b = 0;

        b = 1.0 / a;

        return b;
    }


    @Test
    public void rref_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(4,3,-1,1,rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(1,1,-1,1,rand);

        eq.alias(a,"a",b,"b");
        eq.process("b=rref(a)");

        DMatrixRMaj expected = new DMatrixRMaj(4,3);
        CommonOps_DDRM.rref((DMatrixRMaj)a.getMatrix(),-1,expected);

        assertTrue(MatrixFeatures_DDRM.isIdentical(expected,(DMatrixRMaj)b.getMatrix(),UtilEjml.TEST_F64));
        // eq: b=rref(a) -> b
        DMatrixRMaj b_coded = rref_matrix_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, b));
    }

    protected DMatrixRMaj rref_matrix_Coded(DMatrixRMaj a) {
        // b=rref(a)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, a.numCols );
        CommonOps_DDRM.rref( a, -1, b );

        return b;
    }


    @Test
    public void rref_scalar() {
        Equation eq = new Equation();

        eq.process("a=rref(2.3)");
        assertEquals(1,eq.lookupDouble("a"),UtilEjml.TEST_F64);

        eq.process("a=rref(0)");
        assertEquals(0,eq.lookupDouble("a"),UtilEjml.TEST_F64);

        eq.process("a=rref(-1.2)");
        assertEquals(1,eq.lookupDouble("a"),UtilEjml.TEST_F64);
    }


    @Test
    public void det_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(4,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=det(a)");

        assertEquals(a.determinant(),eq.lookupDouble("b"),UtilEjml.TEST_F64);
        // eq: b=det(a) -> b
        double b_coded = det_matrix_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double det_matrix_Coded(DMatrixRMaj a) {
        // b=det(a)
        double    	b = 0;

        b = CommonOps_DDRM.det(a);

        return b;
    }


    @Test
    public void det_scalar() {
        Equation eq = new Equation();

        eq.process("b=det(5.6)");

        assertEquals(5.6, eq.lookupDouble("b"), UtilEjml.TEST_F64);
        // eq: b=det(5.6) -> b
        double b_coded = det_scalar_Coded();
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double det_scalar_Coded() {
        // b=det(5.6)
        double    	b = 0;

        b = (5.6);

        return b;
    }


    @Test
    public void trace_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=trace(a)");

        assertEquals(a.trace(), eq.lookupDouble("b"), UtilEjml.TEST_F64);
        // eq: b=trace(a) -> b
        double b_coded = trace_matrix_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double trace_matrix_Coded(DMatrixRMaj a) {
        // b=trace(a)
        double    	b = 0;

        b = CommonOps_DDRM.trace(a);

        return b;
    }


    @Test
    public void normF_matrix() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=normF(a)");

        assertEquals(a.normF(), eq.lookupDouble("b"), UtilEjml.TEST_F64);
        // eq: b=normF(a) -> b
        double b_coded = normF_matrix_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double normF_matrix_Coded(DMatrixRMaj a) {
        // b=normF(a)
        double    	b = 0;

        b = NormOps_DDRM.normF(a);

        return b;
    }


    @Test
    public void normF_scalar() {
        Equation eq = new Equation();

        eq.process("b=normF(5.6)");

        assertEquals(5.6, eq.lookupDouble("b"), UtilEjml.TEST_F64);
        // eq: b=normF(5.6) -> b
        double b_coded = normF_scalar_Coded();
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double normF_scalar_Coded() {
        // b=normF(5.6)
        double    	b = 0;

        b = (Math.abs(5.6));

        return b;
    }


    @Test
    public void normP() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=normP(a,2)");

        double expected = NormOps_DDRM.normP((DMatrixRMaj)a.getMatrix(),2);

        assertEquals(expected, eq.lookupDouble("b"), UtilEjml.TEST_F64);
        // eq: b=normP(a,2) -> b
        double b_coded = normP_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double normP_Coded(DMatrixRMaj a) {
        // b=normP(a,2)
        double    	b = 0;

        b = NormOps_DDRM.normP(a, 2);

        return b;
    }


    @Test
    public void sum_one() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=sum(a)");

        double expected = CommonOps_DDRM.elementSum((DMatrixRMaj)a.getMatrix());

        assertEquals(expected, eq.lookupDouble("b"), UtilEjml.TEST_F64);
        // eq: b=sum(a) -> b
        double b_coded = sum_one_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDouble("b")));
    }

    protected double sum_one_Coded(DMatrixRMaj a) {
        // b=sum(a)
        double    	b = 0;

        b = CommonOps_DDRM.elementSum(a);

        return b;
    }


    @Test
    public void sum_rows() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=sum(a,0)");

        DMatrixRMaj expected = new DMatrixRMaj(3,1);
        CommonOps_DDRM.sumRows((DMatrixRMaj)a.getMatrix(),expected);

        assertTrue(MatrixFeatures_DDRM.isIdentical(expected,eq.lookupDDRM("b"), UtilEjml.TEST_F64));
        // eq: b=sum(a,0) -> b
        DMatrixRMaj b_coded = sum_rows_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDDRM("b")));
    }

    protected DMatrixRMaj sum_rows_Coded(DMatrixRMaj a) {
        // b=sum(a,0)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, 1 );
        CommonOps_DDRM.sumRows( a, b );

        return b;
    }


    @Test
    public void sum_cols() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=sum(a,1)");

        DMatrixRMaj expected = new DMatrixRMaj(1,4);
        CommonOps_DDRM.sumCols((DMatrixRMaj)a.getMatrix(),expected);

        assertTrue(MatrixFeatures_DDRM.isIdentical(expected,eq.lookupDDRM("b"), UtilEjml.TEST_F64));
        // eq: b=sum(a,1) -> b
        DMatrixRMaj b_coded = sum_cols_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDDRM("b")));
    }

    protected DMatrixRMaj sum_cols_Coded(DMatrixRMaj a) {
        // b=sum(a,1)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( 1, a.numCols );
        CommonOps_DDRM.sumCols( a, b );

        return b;
    }


    @Test
    public void max_rows() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=max(a,0)");

        DMatrixRMaj expected = new DMatrixRMaj(3,1);
        CommonOps_DDRM.maxRows((DMatrixRMaj)a.getMatrix(),expected);

        assertTrue(MatrixFeatures_DDRM.isIdentical(expected,eq.lookupDDRM("b"), UtilEjml.TEST_F64));
        // eq: b=max(a,0) -> b
        DMatrixRMaj b_coded = max_rows_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDDRM("b")));
    }

    protected DMatrixRMaj max_rows_Coded(DMatrixRMaj a) {
        // b=max(a,0)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, 1 );
        CommonOps_DDRM.maxRows( a, b );

        return b;
    }


    @Test
    public void max_cols() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=max(a,1)");

        DMatrixRMaj expected = new DMatrixRMaj(1,4);
        CommonOps_DDRM.maxCols((DMatrixRMaj)a.getMatrix(),expected);

        assertTrue(MatrixFeatures_DDRM.isIdentical(expected,eq.lookupDDRM("b"), UtilEjml.TEST_F64));
        // eq: b=max(a,1) -> b
        DMatrixRMaj b_coded = max_cols_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDDRM("b")));
    }

    protected DMatrixRMaj max_cols_Coded(DMatrixRMaj a) {
        // b=max(a,1)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( 1, a.numCols );
        CommonOps_DDRM.maxCols( a, b );

        return b;
    }


    @Test
    public void min_rows() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=min(a,0)");

        DMatrixRMaj expected = new DMatrixRMaj(3,1);
        CommonOps_DDRM.minRows((DMatrixRMaj)a.getMatrix(),expected);

        assertTrue(MatrixFeatures_DDRM.isIdentical(expected,eq.lookupDDRM("b"), UtilEjml.TEST_F64));
        // eq: b=min(a,0) -> b
        DMatrixRMaj b_coded = min_rows_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDDRM("b")));
    }

    protected DMatrixRMaj min_rows_Coded(DMatrixRMaj a) {
        // b=min(a,0)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( a.numRows, 1 );
        CommonOps_DDRM.minRows( a, b );

        return b;
    }


    @Test
    public void min_cols() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("b=min(a,1)");

        DMatrixRMaj expected = new DMatrixRMaj(1,4);
        CommonOps_DDRM.minCols((DMatrixRMaj)a.getMatrix(),expected);

        assertTrue(MatrixFeatures_DDRM.isIdentical(expected,eq.lookupDDRM("b"), UtilEjml.TEST_F64));
        // eq: b=min(a,1) -> b
        DMatrixRMaj b_coded = min_cols_Coded(a.getDDRM());
        assertTrue(isIdentical(b_coded, eq.lookupDDRM("b")));
    }

    protected DMatrixRMaj min_cols_Coded(DMatrixRMaj a) {
        // b=min(a,1)
        DMatrixRMaj	b = new DMatrixRMaj(1,1);

        b.reshape( 1, a.numCols );
        CommonOps_DDRM.minCols( a, b );

        return b;
    }


    @Test
    public void eye() {
        Equation eq = new Equation();

        SimpleMatrix a = SimpleMatrix.random_DDRM(3,4,-1,1,rand);

        eq.alias(a,"a");
        eq.process("a=eye(3)");

        assertTrue(SimpleMatrix.identity(3).isIdentical(a, UtilEjml.TEST_F64));
        // eq: a=eye(3) -> a
        DMatrixRMaj a_coded = eye_Coded();
        assertTrue(isIdentical(a_coded, a));
    }

    protected DMatrixRMaj eye_Coded() {
        // a=eye(3)
        DMatrixRMaj	a = new DMatrixRMaj(1,1);

        a.reshape(3, 3);
        CommonOps_DDRM.setIdentity( a );

        return a;
    }


    @Test
    public void abs_matrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(B, "B");

        eq.process("B=abs(A)");

        for (int i = 0; i < A.numRows(); i++) {
            for (int j = 0; j < A.numCols(); j++) {
                assertTrue(B.get(i,j)==Math.abs(A.get(i,j)));
            }
        }
        // eq: B=abs(A) -> B
        DMatrixRMaj B_coded = abs_matrix_Coded(A.getDDRM());
        assertTrue(isIdentical(B_coded, B));
    }

    protected DMatrixRMaj abs_matrix_Coded(DMatrixRMaj A) {
        // B=abs(A)
        DMatrixRMaj	B = new DMatrixRMaj(1,1);

        B.reshape( A.numRows, A.numCols );
        CommonOps_DDRM.abs( A, B );

        return B;
    }


    @Test
    public void abs_int() {
        Equation eq = new Equation();

        int b = 1;
        
        eq.alias(-4, "A");
        eq.alias(b, "B");

        eq.process("B=abs(A)");

        int found = eq.lookupInteger("B");
        assertEquals(4,found,UtilEjml.TEST_F64);
        // eq: B=abs(A) -> B
        int B_coded = abs_int_Coded(-4);
        assertTrue(isIdentical(B_coded, found));
    }

    protected int abs_int_Coded(int        A) {
        // B=abs(A)
        int       	B = 0;

        B = Math.abs(A);

        return B;
    }


    @Test
    public void abs_scalar() {
        Equation eq = new Equation();

        double b = 1.1;
        
        eq.alias(-4.6, "A");
        eq.alias(b, "B");

        eq.process("B=abs(A)");

        double found = eq.lookupDouble("B");
        assertEquals(4.6,found,UtilEjml.TEST_F64);
        // eq: B=abs(A) -> B
        double B_coded = abs_scalar_Coded(-4.6);
        assertTrue(isIdentical(B_coded, found));
    }

    protected double abs_scalar_Coded(double     A) {
        // B=abs(A)
        double    	B = 0;

        B = Math.abs(A);

        return B;
    }


    @Test
    public void max_matrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        double b = 1.1;
        
        eq.alias(A, "A");
        eq.alias(b, "B");

        eq.process("B=max(A)");

        double found = eq.lookupDouble("B");
        double expected = CommonOps_DDRM.elementMax((DMatrixRMaj)A.getMatrix());
        assertEquals(expected,found,UtilEjml.TEST_F64);
        // eq: B=max(A) -> B
        double B_coded = max_matrix_Coded(A.getDDRM());
        assertTrue(isIdentical(B_coded, found));
    }

    protected double max_matrix_Coded(DMatrixRMaj A) {
        // B=max(A)
        double    	B = 0;

        B = CommonOps_DDRM.elementMax(A);

        return B;
    }


    @Test
    public void max_int() {
        Equation eq = new Equation();
        int b = 1;
        eq.alias(4, "A");
        eq.alias(b, "B");

        eq.process("B=max(A)");

        int found = eq.lookupInteger("B");
        assertEquals(4,found,UtilEjml.TEST_F64);
        // eq: B=max(A) -> B
        int B_coded = max_int_Coded(4);
        assertTrue(isIdentical(B_coded, found));
    }

    protected int max_int_Coded(int        A) {
        // B=max(A)
        int       	B = 0;

        B = A;

        return B;
    }


    @Test
    public void max_scalar() {
        Equation eq = new Equation();

        eq.alias(4.6, "A");
        eq.alias(1.1, "B");

        eq.process("B=max(A)");

        double found = eq.lookupDouble("B");
        assertEquals(4.6,found,UtilEjml.TEST_F64);
        // eq: B=max(A) -> B
        double B_coded = max_scalar_Coded(4.6);
        assertTrue(isIdentical(B_coded, found));
    }

    protected double max_scalar_Coded(double     A) {
        // B=max(A)
        double    	B = 0;

        B = A;

        return B;
    }


    @Test
    public void min_matrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(1.0, "B");

        eq.process("B=min(A)");

        double found = eq.lookupDouble("B");
        double expected = CommonOps_DDRM.elementMin((DMatrixRMaj)A.getMatrix());
        assertEquals(expected,found,UtilEjml.TEST_F64);
        // eq: B=min(A) -> B
        double B_coded = min_matrix_Coded(A.getDDRM());
        assertTrue(isIdentical(B_coded, found));
    }

    protected double min_matrix_Coded(DMatrixRMaj A) {
        // B=min(A)
        double    	B = 0;

        B = CommonOps_DDRM.elementMin(A);

        return B;
    }


    @Test
    public void min_int() {
        Equation eq = new Equation();

        eq.alias(4, "A");
        eq.alias(1, "B");

        eq.process("B=min(A)");

        int found = eq.lookupInteger("B");
        assertEquals(4,found,UtilEjml.TEST_F64);
        // eq: B=min(A) -> B
        int B_coded = min_int_Coded(4);
        assertTrue(isIdentical(B_coded, found));
    }

    protected int min_int_Coded(int        A) {
        // B=min(A)
        int       	B = 0;

        B = A;

        return B;
    }


    @Test
    public void min_scalar() {
        Equation eq = new Equation();

        eq.alias(4.6, "A");
        eq.alias(1.1, "B");

        eq.process("B=min(A)");

        double found = eq.lookupDouble("B");
        assertEquals(4.6,found,UtilEjml.TEST_F64);
        // eq: B=min(A) -> B
        double B_coded = min_scalar_Coded(4.6);
        assertTrue(isIdentical(B_coded, found));
    }

    protected double min_scalar_Coded(double     A) {
        // B=min(A)
        double    	B = 0;

        B = A;

        return B;
    }


    @Test
    public void zeros() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 8, -1, 1, rand);

        eq.alias(A, "A");

        eq.process("A=zeros(6,8)");

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                assertEquals(0,A.get(i,j),UtilEjml.TEST_F64);
            }
        }
        // eq: A=zeros(6,8) -> A
        DMatrixRMaj A_coded = zeros_Coded();
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj zeros_Coded() {
        // A=zeros(6,8)
        DMatrixRMaj	A = new DMatrixRMaj(1,1);

        A.reshape( 6, 8 );
        CommonOps_DDRM.fill( A, 0 );

        return A;
    }


    @Test
    public void ones() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 8, -1, 1, rand);

        eq.alias(A, "A");

        eq.process("A=ones(6,8)");

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                assertEquals(1,A.get(i,j),UtilEjml.TEST_F64);
            }
        }
        // eq: A=ones(6,8) -> A
        DMatrixRMaj A_coded = ones_Coded();
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj ones_Coded() {
        // A=ones(6,8)
        DMatrixRMaj	A = new DMatrixRMaj(1,1);

        A.reshape( 6, 8 );
        CommonOps_DDRM.fill( A, 1 );

        return A;
    }


    @Test
    public void diag_vector() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 6, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 1, -1, 1, rand);


        eq.alias(A, "A");
        eq.alias(B, "B");

        eq.process("A=diag(B)");

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if( i == j )
                    assertEquals(B.get(i,0),A.get(i,j),UtilEjml.TEST_F64);
                else
                    assertEquals(0,A.get(i,j),UtilEjml.TEST_F64);
            }
        }
        // eq: A=diag(B) -> A
        DMatrixRMaj A_coded = diag_vector_Coded(B.getDDRM());
        assertTrue(isIdentical(A_coded, A));
    }

    protected DMatrixRMaj diag_vector_Coded(DMatrixRMaj B) {
        // A=diag(B)
        DMatrixRMaj	A = new DMatrixRMaj(1,1);

        if (MatrixFeatures_DDRM.isVector(B)) { //;
        	A.reshape( B.numRows, B.numRows );
        	CommonOps_DDRM.diag(A, B.numRows, B.data);
        } else { //;
        	A.reshape( B.numRows, 1 );
        	CommonOps_DDRM.extractDiag(B, A);
        }//;

        return A;
    }


    @Test
    public void diag_matrix() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 8, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 1, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(B, "B");

        eq.process("B=diag(A)");

        assertEquals(6,B.numRows());
        assertEquals(1,B.numCols());

        for (int i = 0; i < 6; i++) {
            assertEquals(A.get(i,i),B.get(i,0),UtilEjml.TEST_F64);
        }
        // eq: B=diag(A) -> B
        DMatrixRMaj B_coded = diag_matrix_Coded(A.getDDRM());
        assertTrue(isIdentical(B_coded, B));
    }

    protected DMatrixRMaj diag_matrix_Coded(DMatrixRMaj A) {
        // B=diag(A)
        DMatrixRMaj	B = new DMatrixRMaj(1,1);

        if (MatrixFeatures_DDRM.isVector(A)) { //;
        	B.reshape( A.numRows, A.numRows );
        	CommonOps_DDRM.diag(B, A.numRows, A.data);
        } else { //;
        	B.reshape( A.numRows, 1 );
        	CommonOps_DDRM.extractDiag(A, B);
        }//;

        return B;
    }


    @Test
    public void dot() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 1, -1, 1, rand);
        SimpleMatrix B = SimpleMatrix.random_DDRM(6, 1, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(B, "B");
        eq.alias(1.0, "found");

        eq.process("found=dot(A,B)");

        double found = ((VariableDouble)eq.lookupVariable("found")).value;

        assertEquals(A.dot(B),found,UtilEjml.TEST_F64);
        // eq: found=dot(A,B) -> found
        double found_coded = dot_Coded(A.getDDRM(), B.getDDRM());
        assertTrue(isIdentical(found_coded, eq.lookupVariable("found")));
    }

    protected double dot_Coded(DMatrixRMaj A, DMatrixRMaj B) {
        // found=dot(A,B)
        double    	found = 0;

        found = VectorVectorMult_DDRM.innerProd( A, B );

        return found;
    }


    @Test
    public void solve() {
        Equation eq = new Equation();

        SimpleMatrix A = SimpleMatrix.random_DDRM(6, 5, -1, 1, rand);
        SimpleMatrix x = SimpleMatrix.random_DDRM(5, 3, -1, 1, rand);
        SimpleMatrix b = SimpleMatrix.random_DDRM(6, 3, -1, 1, rand);

        eq.alias(A, "A");
        eq.alias(b, "b");
        eq.alias(x, "x");

        eq.process("x=solve(A,b)");

        assertTrue(A.solve(b).isIdentical(x, UtilEjml.TEST_F64));
        // eq: x=solve(A,b) -> x
        DMatrixRMaj x_coded = solve_Coded(A.getDDRM(), b.getDDRM());
        assertTrue(isIdentical(x_coded, x));
    }

    protected DMatrixRMaj solve_Coded(DMatrixRMaj A, DMatrixRMaj b) {
        // x=solve(A,b)
        DMatrixRMaj	x = new DMatrixRMaj(1,1);

        x.reshape( A.numRows, b.numCols );
        LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(A.numRows, A.numCols);
        boolean ok = solver.setA(A);
        solver.solve(b, x);

        return x;
    }


}
