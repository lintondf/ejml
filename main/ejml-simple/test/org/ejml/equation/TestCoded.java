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
        int        A;
        int        B;
int        ti1;
        //A=-B
A = -B;
        assert(isIdentical(-2, A));
        //A=B--B
ti1 = -B;
A = B - ti1;
        assert(isIdentical(4, A));
        //A=B+-B
ti1 = -B;
A = B + ti1;
        assert(isIdentical(0, A));
        //A=B---5
ti1 = --5;
A = B - ti1;
        assert(isIdentical(2 - 5, A));
        //A=B--5
A = B - -5;
        assert(isIdentical(2+5, A));


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
    public void compile_assign_IntSequence_Case0() {
        Equation eq = new Equation();

        eq.process("a=5:1:8");
        eq.process("b=[a]");
        SimpleMatrix found = SimpleMatrix.wrap(eq.lookupDDRM("b"));
        assertEquals(1,found.numRows());
        assertEquals(4,found.numCols());

        for (int x = 0; x < 4; x++) {
            assertEquals(x+5,found.get(0,x),UtilEjml.TEST_F64);
        }
    }


    @Test
    public void compile_assign_IntSequence_Case1() {
        Equation eq = new Equation();

        eq.process("a=2 3 4 5 6");
        eq.process("b=[a]");
        SimpleMatrix found = SimpleMatrix.wrap(eq.lookupDDRM("b"));
        assertEquals(1,found.numRows());
        assertEquals(5,found.numCols());

        for (int x = 0; x < 5; x++) {
            assertEquals(x+2,found.get(0,x),UtilEjml.TEST_F64);
        }
    }


    @Test
    public void compile_assign_IntSequence_Case2() {
        Equation eq = new Equation();

        String tests[] = new String[]{"2 3 4 5 6 7:9","2:4 5 6 7 8 9"};

        for( String s : tests ) {
            eq.process("a=" + s);
            eq.process("b=[a]");
            SimpleMatrix found = SimpleMatrix.wrap(eq.lookupDDRM("b"));
            assertEquals(1, found.numRows());
            assertEquals(8, found.numCols());

            for (int x = 0; x < 8; x++) {
                assertEquals(x + 2, found.get(0, x), UtilEjml.TEST_F64);
            }
        }
    }


