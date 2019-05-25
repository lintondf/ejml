package org.ejml.equation;

import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.Random;

import static org.ejml.equation.TokenList.Type;
import static java.lang.Math.exp;
import static org.ejml.dense.row.CommonOps_DDRM.*;
//import static org.ejml.dense.row.CommonOps_DDRM.mult;
//import static org.ejml.dense.row.CommonOps_DDRM.scale;
//import static org.ejml.dense.row.CommonOps_DDRM.subtract;
import static org.junit.Assert.*;

public class TestCoded {

	Random rand = new Random(234);

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
		assertTrue(expected.isIdentical(A, 1e-15));
		// eq: A=B+C*D-B -> A
		DMatrixRMaj A_coded = compile_basic_Coded(B.getDDRM(), C.getDDRM(), D.getDDRM());
		assertTrue(new SimpleMatrix(A_coded).isIdentical(A, 1e-15));
	}

	protected DMatrixRMaj compile_basic_Coded(DMatrixRMaj B, DMatrixRMaj C, DMatrixRMaj D) {
		// A=B+C*D-B
		DMatrixRMaj A = new DMatrixRMaj(1, 1);


		A.reshape( C.numRows, D.numCols );
		CommonOps_DDRM.mult( C, D, A );
		A.reshape( B.numRows, B.numCols );
		CommonOps_DDRM.add( B, A, A );
		A.reshape( A.numRows, A.numCols );
		CommonOps_DDRM.subtract( A, B, A );
		return A;
	}

	public static void main(String[] args) {
		TestCoded test = new TestCoded();
		test.compile_basic();
	}
}
