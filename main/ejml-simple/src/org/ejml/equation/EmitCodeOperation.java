package org.ejml.equation;

import java.util.List;

import org.ejml.equation.MatrixConstructor.Item;

public class EmitCodeOperation {

	final static String formatReshape = "%s.reshape( %s.numRows, %s.numCols );";
	final static String formatGeneral3 = "%s.%s( %s, %s, %s );";
	final static String formatGeneral2 = "%s.%s( %s, %s );";
	final static String formatGeneral1 = "%s.%s( %s );";
	final static String formatCommonOps3 = "CommonOps_DDRM.%s( %s, %s, %s );";
	final static String formatCommonOps2 = "CommonOps_DDRM.%s( %s, %s );";
	final static String formatCommonOps1 = "CommonOps_DDRM.%s( %s );";


	protected static String mmOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.add(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "add", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "elementDivision": // Info elementDivision( final Variable A , final Variable B , ManagerTempVariables manager )
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.elementDiv(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementDiv", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "elementMult": // Info elementMult( final Variable A , final Variable B , ManagerTempVariables manager )
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.elementMult(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementMult", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "kron": // Info kron( final Variable A , final Variable B, ManagerTempVariables manager)
			String rows = String.format("%s.numRows * %s.numRows", A.getName(), B.getName());
			String cols = String.format("%s.numCols * %s.numCols", A.getName(), B.getName());
			sb.append( String.format(formatReshape, output.getName(), rows, cols) );
			sb.append( String.format(formatCommonOps3, "kron", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.subtract(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "subtract", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "solve": // Info solve( final Variable A , final Variable B , ManagerTempVariables manager)
			/*
			sb.append( String.format(formatReshape, output.getName(), A.getName(), B.getName()) );
LinearSolverDense<DMatrixRMaj> solver;
DMatrixRMaj a = ((VariableMatrix)A).matrix;
DMatrixRMaj b = ((VariableMatrix)B).matrix;
if( solver == null ) {
solver = LinearSolverFactory_DDRM.leastSquares(a.numRows,a.numCols);
if( !solver.setA(a))
throw new RuntimeException("Solver failed!");
output.matrix.reshape(a.numCols,b.numCols);
solver.solve(b, output.matrix);
			*/
			//TODO MANUAL
			return sb.toString();
		case "dot": // Info dot( final Variable A , final Variable B , ManagerTempVariables manager)
			sb.append(output.getOperand());
			sb.append(" = ");
			sb.append( String.format(formatGeneral2, "VectorVectorMult_DDRM", "innerProd", A.getName(), B.getName()));
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), B.getName()) );
			//CommonOps_DDRM.mult(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "mult", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.elementPower(a, b, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementPower", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String iiOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s + %s;
			sb.append( String.format("%s = %s + %s;", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "rand": // Info rand( final Variable A , final Variable B , ManagerTempVariables manager)
			//TODO MANUAL
			sb.append( String.format(formatGeneral2, output.getName(), "reshape", A.getOperand(), B.getOperand()) );
			sb.append("Random rand = new Random();");
			final String fillUniform = "RandomMatrices_DDRM.fillUniform(%s, 0, 1, rand );";
			sb.append( String.format(fillUniform, output.getName()));
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s - %s;
			sb.append( String.format("%s = %s - %s;", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "ones": // Info ones( final Variable A , final Variable B , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), B.getName()) );
			//CommonOps_DDRM.fill(output.matrix, 1);
			sb.append( String.format(formatCommonOps2, "fill", output.getName(), 1) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s/%s;
			sb.append( String.format("%s = %s / %s;", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s*%s;
			sb.append( String.format("%s = %s * %s;", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "zeros": // Info zeros( final Variable A , final Variable B , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), B.getName()) );
			//CommonOps_DDRM.fill(output.matrix, 0);
			sb.append( String.format(formatCommonOps2, "fill", output.getName(), 0) );
			return sb.toString();
		case "randn": // Info randn( final Variable A , final Variable B , ManagerTempVariables manager)
			//TODO MANUAL
			sb.append( String.format(formatGeneral2, output.getName(), "reshape", A.getOperand(), B.getOperand()) );
			sb.append("Random rand = new Random();");
			final String fillGaussian = "RandomMatrices_DDRM.fillGaussian(%s, 0, 1, rand );";
			sb.append( String.format(fillGaussian, output.getName()));
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String ssOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s + %s;
			sb.append( String.format("%s = %s + %s;", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s - %s;
			sb.append( String.format("%s = %s - %s;", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "pow": // Info pow(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.pow(a, b);
			sb.append( String.format("%s = Math.pow(%s, %s);", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s/%s;
			sb.append( String.format("%s = %s / %s;", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s*%s;
			sb.append( String.format("%s = %s * %s;", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "atan2": // Info atan2(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.atan2(a, b);
			sb.append( String.format("%s = Math.atan2(%s, %s);", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.pow(a, b);
			sb.append( String.format("%s = Math.pow(%s, %s);", output.getName(), A.getName(), B.getName()) );
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String Op(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "max_cols": // Info max_two( final Variable A , final Variable P , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), "1", A.getName()) );
			//CommonOps_DDRM.maxCols(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "maxCols", A.getName(), output.getName()) );
			return sb.toString();
		case "max_rows": // Info max_two( final Variable A , final Variable P , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), "1") );
			//CommonOps_DDRM.maxRows(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "maxRows", A.getName(), output.getName()) );
			return sb.toString();
		case "extractScalar": // Info extractScalar( final List<Variable> inputs, ManagerTempVariables manager)
			/*
codeIntVariableInteger: int index = ((VariableInteger)inputs.get(1)).value;
			//%s = A.get(index);
			sb.append( String.format("%s = A.get(%s);", output.getName(), index.getName()) );
codeIntVariableInteger: int row = ((VariableInteger) inputs.get(1)).value;
codeIntVariableInteger: int col = ((VariableInteger) inputs.get(2)).value;
			//%s = A.get(row, col);
			sb.append( String.format("%s = A.get(%s, %s);", output.getName(), row.getName(), col.getName()) );
DMatrixRMaj A = ((VariableMatrix)inputs.get(0)).matrix;
if( inputs.size() == 2 ) {
int index = ((VariableInteger)inputs.get(1)).value;
output.value = A.get(index);
} else {
int row = ((VariableInteger) inputs.get(1)).value;
int col = ((VariableInteger) inputs.get(2)).value;
output.value = A.get(row, col);
			*/
			//TODO MANUAL
			return sb.toString();
		case "rng": // Info rng( final Variable A , ManagerTempVariables manager)
			//TODO MANUAL
			sb.append("Random rand = new Random();");
			sb.append(String.format("rand.setSeed(%s);", A.getOperand()));
			return sb.toString();
		case "min_cols": // Info min_two( final Variable A , final Variable P , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), "1", A.getName()) );
			//CommonOps_DDRM.minCols(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "minCols", A.getName(), output.getName()) );
			return sb.toString();
		case "sum_all": // Info sum_one( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.elementSum(varA.matrix);
			sb.append( String.format("%s = CommonOps_DDRM.elementSum(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "sum_cols": // Info sum_two( final Variable A , final Variable P , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), "1", A.getName()) );
			//CommonOps_DDRM.sumCols(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "sumCols", A.getName(), output.getName()) );
			return sb.toString();
		case "extract": // Info extract( final List<Variable> inputs, ManagerTempVariables manager)
			/*
						codeCommonOps: CommonOps_DDRM.extract(A,
			codeCommonOps: CommonOps_DDRM.extract(A,extents.row0,extents.row1,extents.col0,extents.col1,output.matrix,0,0);
			codeCommonOps: CommonOps_DDRM.extract(A,
OperationExecuteFactory.Extents extents = new OperationExecuteFactory.Extents();
OperationExecuteFactory.ArrayExtent rowExtent = new OperationExecuteFactory.ArrayExtent();
OperationExecuteFactory.ArrayExtent colExtent = new OperationExecuteFactory.ArrayExtent();
DMatrixRMaj A = ((VariableMatrix)inputs.get(0)).matrix;
if( inputs.size() == 2  ) {
if( extractSimpleExtents(inputs.get(1), extents, false, A.getNumElements()) ) {
extents.col1 += 1;
output.matrix.reshape(1,extents.col1-extents.col0);
System.arraycopy(A.data,extents.col0,output.matrix.data,0,extents.col1-extents.col0);
} else {
extractArrayExtent(inputs.get(1),A.getNumElements(),colExtent);
output.matrix.reshape(1, colExtent.length);
CommonOps_DDRM.extract(A,
colExtent.array, colExtent.length, output.matrix);
} else if( extractSimpleExtents(inputs.get(1), extents, true, A.numRows) &&
extractSimpleExtents(inputs.get(2), extents, false, A.numCols)) {
extents.row1 += 1;
extents.col1 += 1;
output.matrix.reshape(extents.row1-extents.row0,extents.col1-extents.col0);
CommonOps_DDRM.extract(A,extents.row0,extents.row1,extents.col0,extents.col1,output.matrix,0,0);
} else {
extractArrayExtent(inputs.get(1),A.numRows,rowExtent);
extractArrayExtent(inputs.get(2),A.numCols,colExtent);
output.matrix.reshape(rowExtent.length, colExtent.length);
CommonOps_DDRM.extract(A,
rowExtent.array,rowExtent.length,
colExtent.array,colExtent.length,output.matrix);
			*/
			//TODO MANUAL
			return sb.toString();
		case "min_rows": // Info min_two( final Variable A , final Variable P , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), "1") );
			//CommonOps_DDRM.minRows(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "minRows", A.getName(), output.getName()) );
			return sb.toString();
		case "normP": // Info normP( final Variable A , final Variable P , ManagerTempVariables manager)
			//%s = NormOps_DDRM.normP(varA.matrix,valueP);
			sb.append( String.format("%s = NormOps_DDRM.normP(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "sum_rows": // Info sum_two( final Variable A , final Variable P , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), "1") );
			//CommonOps_DDRM.sumRows(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "sumRows", A.getName(), output.getName()) );
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String sOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "log": // Info log(final Variable A, ManagerTempVariables manager)
			//%s = Math.log(%s);
			sb.append( String.format("%s = Math.log(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "max": // Info max( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getName(), A.getName()) );
			return sb.toString();
		case "cos": // Info cos(final Variable A, ManagerTempVariables manager)
			//%s = Math.cos(%s);
			sb.append( String.format("%s = Math.cos(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "normF": // Info normF( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "atan": // Info atan(final Variable A, ManagerTempVariables manager)
			//%s = Math.atan(%s);
			sb.append( String.format("%s = Math.atan(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "inv": // Info inv( final Variable A , ManagerTempVariables manager)
			//TODO MANUAL
			sb.append(String.format("%s = 1.0 / %s;", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "neg": // Info neg(final Variable A, ManagerTempVariables manager)
			//%s = -%s;
			sb.append( String.format("%s = -%s;", output.getName(), A.getName()) );
			return sb.toString();
		case "det": // Info det( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getName(), A.getName()) );
			return sb.toString();
		case "trace": // Info trace( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getName(), A.getName()) );
			return sb.toString();
		case "min": // Info min( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getName(), A.getName()) );
			return sb.toString();
		case "abs": // Info abs( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "rref": // Info rref( final Variable A , ManagerTempVariables manager)
			//TODO MANUAL
			sb.append(String.format("%s = (%s == 0) ? 0.0 : 1.0;", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "sqrt": // Info sqrt(final Variable A, ManagerTempVariables manager)
			//%s = Math.sqrt(a);
			sb.append( String.format("%s = Math.sqrt(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "pinv": // Info pinv( final Variable A , ManagerTempVariables manager)
			//TODO MANUAL
			sb.append(String.format("%s = 1.0 / %s;", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "sin": // Info sin(final Variable A, ManagerTempVariables manager)
			//%s = Math.sin(%s);
			sb.append( String.format("%s = Math.sin(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "exp": // Info exp(final Variable A, ManagerTempVariables manager)
			//%s = Math.exp(%s);
			sb.append( String.format("%s = Math.exp(%s);", output.getName(), A.getName()) );
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String msOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.add(m.matrix, s.getDouble(), output.matrix);
			sb.append( String.format(formatCommonOps3, "add", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.subtract(m, v, output.matrix);
			sb.append( String.format(formatCommonOps3, "subtract", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.divide(s.getDouble(), m.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "divide", B.getName(), A.getName(), output.getName()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.scale(s.getDouble(),m.matrix,output.matrix);
			sb.append( String.format(formatCommonOps3, "scale", B.getName(), A.getName(), output.getName()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.elementPower(a, b, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementPower", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String iOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "eye": // Info eye( final Variable A , ManagerTempVariables manager)
			sb.append( String.format("%s.reshape(%s, %s);", output.getName(), A.getOperand(), A.getOperand()) );
			//CommonOps_DDRM.setIdentity(output.matrix);
			sb.append( String.format(formatCommonOps1, "setIdentity", output.getName()) );
			return sb.toString();
		case "neg": // Info neg(final Variable A, ManagerTempVariables manager)
			//%s = -%s;
			sb.append( String.format("%s = -%s;", output.getName(), A.getName()) );
			return sb.toString();
		case "min": // Info min( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getName(), A.getName()) );
			return sb.toString();
		case "abs": // Info abs( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "max": // Info max( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getName(), A.getName()) );
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String smOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), B.getName(), B.getName()) );
			//CommonOps_DDRM.add(m.matrix, s.getDouble(), output.matrix);
			sb.append( String.format(formatCommonOps3, "add", B.getName(), A.getName(), output.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), B.getName(), B.getName()) );
			//CommonOps_DDRM.subtract(v, m, output.matrix);
			sb.append( String.format(formatCommonOps3, "subtract", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), B.getName(), B.getName()) );
			//CommonOps_DDRM.elementPower(a, b, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementPower", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String mOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
//		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "diag": // Info diag( final Variable A , ManagerTempVariables manager)
			/*
codeIntVariableInteger: int N = mA.getNumElements();
			sb.append( String.format(formatReshape, output.getName(), N.getName(), N.getName()) );
codeCommonOps: CommonOps_DDRM.diag(output.matrix,N,mA.data);
codeIntVariableInteger: int N = Math.min(mA.numCols,mA.numRows);
			sb.append( String.format(formatReshape, output.getName(), N.getName(), 1.getName()) );
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
			*/
			//TODO MANUAL
			return sb.toString();
		case "log": // Info log(final Variable A, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.elementLog(a, out);
			sb.append( String.format(formatCommonOps2, "elementLog", A.getName(), output) );
			return sb.toString();
		case "max": // Info max( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.elementMax(%s);
			sb.append( String.format("%s = CommonOps_DDRM.elementMax(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "normF": // Info normF( final Variable A , ManagerTempVariables manager)
			//%s = NormOps_DDRM.normF(%s);
			sb.append( String.format("%s = NormOps_DDRM.normF(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "inv": // Info inv( final Variable A , ManagerTempVariables manager)
			/*
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
VariableMatrix mA = (VariableMatrix)A;
output.matrix.reshape(mA.matrix.numRows, mA.matrix.numCols);
if( !CommonOps_DDRM.invert(mA.matrix,output.matrix) )
throw new RuntimeException("Inverse failed!");
			*/
			//TODO MANUAL
			return sb.toString();
		case "eye": // Info eye( final Variable A , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.setIdentity(output.matrix);
			sb.append( String.format(formatCommonOps1, "setIdentity", output.getName()) );
			return sb.toString();
		case "neg": // Info neg(final Variable A, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.changeSign(a, output.matrix);
			sb.append( String.format(formatCommonOps2, "changeSign", A.getName(), output.getName()) );
			return sb.toString();
		case "det": // Info det( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.det(mA.matrix);
			sb.append( String.format("%s = CommonOps_DDRM.det(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "trace": // Info trace( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.trace(mA.matrix);
			sb.append( String.format("%s = CommonOps_DDRM.trace(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "min": // Info min( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.elementMin(%s);
			sb.append( String.format("%s = CommonOps_DDRM.elementMin(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "abs": // Info abs( final Variable A , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.abs(a, output.matrix);
			sb.append( String.format(formatCommonOps2, "abs", A.getName(), output.getName()) );
			return sb.toString();
		case "rref": // Info rref( final Variable A , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.rref(a, -1, output.matrix);
			sb.append( String.format(formatCommonOps3, "rref", A.getName(), "-1", output.getName()) );
			return sb.toString();
		case "pinv": // Info pinv( final Variable A , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.pinv(mA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "pinv", A.getName(), output.getName()) );
			return sb.toString();
		case "transpose": // Info transpose( final Variable A , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.transpose(mA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "transpose", A.getName(), output.getName()) );
			return sb.toString();
		case "exp": // Info exp(final Variable A, ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
			//CommonOps_DDRM.elementExp(a, out);
			sb.append( String.format(formatCommonOps2, "elementExp", A.getName(), output.getName()) );
			return sb.toString();
		}
		return sb.toString();
	}
	
	private static String construct(CodeOperation codeOp) {
		StringBuilder sb = new StringBuilder();
		CodeMatrixConstructor cmc = new CodeMatrixConstructor( codeOp.constructor );
		cmc.construct(sb);
		return sb.toString();
	}

	private static String copyOp(String[] operands, CodeOperation codeOp) {
		//copy: ii, ss, sm1, none
		//copyR: sm
		switch (operands[1]) {
		case "mm":
			StringBuilder sb = new StringBuilder();
//			System.out.printf("copyOp: %s, %s\n", operands, codeOp.toString()); 
			sb.append( String.format(formatReshape, codeOp.output.getName(), codeOp.input.get(0).getName(), codeOp.input.get(0).getName()) );
			sb.append( String.format("%s.set( %s );", codeOp.output.getName(), codeOp.input.get(0).getName() ));
			return sb.toString();
		case "ii":
			if (operands.length > 2)
				System.out.printf("copyOp: %s, %s\n", operands, codeOp.toString());
			else
				return String.format("%s = %s;", codeOp.output.getOperand(), codeOp.input.get(0).getOperand());
		case "ss":
//			System.out.printf("copyOp: %s, %s\n", operands, codeOp.toString());
			return String.format("%s = %s;", codeOp.output.getOperand(), codeOp.input.get(0).getOperand());
		case "sm1":
			return String.format("%s =  = %s.unsafe_get(0,0);", codeOp.output.getOperand(), codeOp.input.get(0).getOperand() );
		case "":
			System.out.printf("copyOp: %s, %s\n", operands, codeOp.toString()); 
			break;
		case "sm":
			System.out.printf("copyOp: %s, %s\n", operands, codeOp.toString()); 
			break;
		default:
			System.out.printf("copyOp: %s, %s\n", operands, codeOp.toString()); 
			break;
		}
		return "//copyOp: " + codeOp.toString();
	}

	
	public static void emitJavaOperation(StringBuilder body, CodeOperation codeOp) {
		String[] fields = codeOp.name().split("-");
		String inputs = "";
		if (fields.length > 1) {
			inputs = fields[1];
		}
		if (codeOp.name().equals("matrixConstructor")) {
			body.append( construct( codeOp ) );
		} else if (fields[0].startsWith("copy")) {
			body.append( copyOp( fields, codeOp) );
		} else {
			switch (inputs) {
			case "mm":
				body.append( mmOp( fields[0], codeOp ) );
				break;
			case "ii":
				body.append( iiOp( fields[0], codeOp ) );
				break;
			case "ss":
				body.append( ssOp( fields[0], codeOp ) );
				break;
			case "":
				body.append( Op( fields[0], codeOp ) );
				break;
			case "s":
				body.append( sOp( fields[0], codeOp ) );
				break;
			case "ms":
				body.append( msOp( fields[0], codeOp ) );
				break;
			case "i":
				body.append( iOp( fields[0], codeOp ) );
				break;
			case "sm":
				body.append( smOp( fields[0], codeOp ) );
				break;
			case "m":
				body.append( mOp( fields[0], codeOp ) );
				break;
			}
		}
	}
}