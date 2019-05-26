package org.ejml.equation;

import java.util.List;

import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.equation.MatrixConstructor.Item;

public class EmitCodeOperation {

	final static String formatReshape = "%s.reshape( %s.numRows, %s.numCols );";
	final static String formatGeneral3 = "%s.%s( %s, %s, %s );";
	final static String formatGeneral2 = "%s.%s( %s, %s );";
	final static String formatGeneral1 = "%s.%s( %s );";
	final static String formatCommonOps3 = "CommonOps_DDRM.%s( %s, %s, %s );";
	final static String formatCommonOps2 = "CommonOps_DDRM.%s( %s, %s );";
	final static String formatCommonOps1 = "CommonOps_DDRM.%s( %s );";
	
	protected static boolean isNumeric(String str) {
	    return str.matches("[+-]?\\d*(\\.\\d+)?");
	}
	
	protected static void emitReshape(StringBuilder sb, Variable output, Variable A, Variable B) {
		String o = output.getOperand();
		String a = A.getOperand();
		String b = B.getOperand();
		if (A.getType() == VariableType.MATRIX)
			a += ".numRows";
		if (B.getType() == VariableType.MATRIX)
			b += ".numCols";
		sb.append( String.format(formatGeneral2, o, "reshape", a, b) );
	}


	protected static String mmOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.add(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "add", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "elementDivision": // Info elementDivision( final Variable A , final Variable B , ManagerTempVariables manager )
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.elementDiv(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementDiv", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "elementMult": // Info elementMult( final Variable A , final Variable B , ManagerTempVariables manager )
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.elementMult(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementMult", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "kron": // Info kron( final Variable A , final Variable B, ManagerTempVariables manager)
			String rows = String.format("%s.numRows * %s.numRows", A.getName(), B.getName());
			String cols = String.format("%s.numCols * %s.numCols", A.getName(), B.getName());
			sb.append( String.format(formatGeneral2, output.getName(), "reshape", rows, cols) );
			sb.append( String.format(formatCommonOps3, "kron", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.subtract(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "subtract", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "solve": // Info solve( final Variable A , final Variable B , ManagerTempVariables manager)
			/*
			emitReshape(sb, output, A, B);
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
			sb.append("//TODO: " + op);//TODO MANUAL
			return sb.toString();
		case "dot": // Info dot( final Variable A , final Variable B , ManagerTempVariables manager)
			sb.append(output.getOperand());
			sb.append(" = ");
			sb.append( String.format(formatGeneral2, "VectorVectorMult_DDRM", "innerProd", A.getName(), B.getName()));
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, B);
			//CommonOps_DDRM.mult(mA.matrix, mB.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "mult", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
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
			sb.append( String.format("%s = %s + %s;", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "rand": // Info rand( final Variable A , final Variable B , ManagerTempVariables manager)
			sb.append( String.format(formatGeneral2, output.getOperand(), "reshape", A.getOperand(), B.getOperand()) );
			sb.append("Random rand = new Random();");
			final String fillUniform = "RandomMatrices_DDRM.fillUniform(%s, 0, 1, rand );";
			sb.append( String.format(fillUniform, output.getOperand()));
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s - %s;
			sb.append( String.format("%s = %s - %s;", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "ones": // Info ones( final Variable A , final Variable B , ManagerTempVariables manager)
			emitReshape(sb, output, A, B);
			//CommonOps_DDRM.fill(output.matrix, 1);
			sb.append( String.format(formatCommonOps2, "fill", output.getOperand(), 1) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s/%s;
			sb.append( String.format("%s = %s / %s;", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s*%s;
			sb.append( String.format("%s = %s * %s;", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "zeros": // Info zeros( final Variable A , final Variable B , ManagerTempVariables manager)
			emitReshape(sb, output, A, B);
			//CommonOps_DDRM.fill(output.matrix, 0);
			sb.append( String.format(formatCommonOps2, "fill", output.getOperand(), 0) );
			return sb.toString();
		case "randn": // Info randn( final Variable A , final Variable B , ManagerTempVariables manager)
			sb.append( String.format(formatGeneral2, output.getOperand(), "reshape", A.getOperand(), B.getOperand()) );
			sb.append("Random rand = new Random();");
			final String fillGaussian = "RandomMatrices_DDRM.fillGaussian(%s, 0, 1, rand );";
			sb.append( String.format(fillGaussian, output.getOperand()));
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
			sb.append( String.format("%s = %s + %s;", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s - %s;
			sb.append( String.format("%s = %s - %s;", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "pow": // Info pow(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.pow(a, b);
			sb.append( String.format("%s = Math.pow(%s, %s);", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s/%s;
			sb.append( String.format("%s = %s / %s;", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s*%s;
			sb.append( String.format("%s = %s * %s;", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "atan2": // Info atan2(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.atan2(a, b);
			sb.append( String.format("%s = Math.atan2(%s, %s);", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.pow(a, b);
			sb.append( String.format("%s = Math.pow(%s, %s);", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String Op(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		VariableInteger one = new VariableInteger(1, "Integer{1}");
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "max_cols": // Info max_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, one, A );
			//CommonOps_DDRM.maxCols(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "maxCols", A.getName(), output.getName()) );
			return sb.toString();
		case "max_rows": // Info max_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, A, one );
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
			sb.append("//TODO: " + op);//TODO MANUAL
			return sb.toString();
		case "rng": // Info rng( final Variable A , ManagerTempVariables manager)
			sb.append("Random rand = new Random();");
			sb.append(String.format("rand.setSeed(%s);", A.getOperand()));
			return sb.toString();
		case "min_cols": // Info min_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, one, A );
			//CommonOps_DDRM.minCols(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "minCols", A.getName(), output.getName()) );
			return sb.toString();
		case "sum_all": // Info sum_one( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.elementSum(varA.matrix);
			sb.append( String.format("%s = CommonOps_DDRM.elementSum(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "sum_cols": // Info sum_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, one, A );
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
			sb.append("//TODO: " + op);//TODO MANUAL
			return sb.toString();
		case "min_rows": // Info min_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, A, one );
			//CommonOps_DDRM.minRows(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "minRows", A.getName(), output.getName()) );
			return sb.toString();
		case "normP": // Info normP( final Variable A , final Variable P , ManagerTempVariables manager)
			//%s = NormOps_DDRM.normP(varA.matrix,valueP);
			sb.append( String.format("%s = NormOps_DDRM.normP(%s, %s);", output.getName(), 
					A.getName(), codeOp.input.get(1).getOperand()) );
			return sb.toString();
		case "sum_rows": // Info sum_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, A, one );
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
			sb.append( String.format("%s = Math.log(%s);", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "max": // Info max( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "cos": // Info cos(final Variable A, ManagerTempVariables manager)
			//%s = Math.cos(%s);
			sb.append( String.format("%s = Math.cos(%s);", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "normF": // Info normF( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "atan": // Info atan(final Variable A, ManagerTempVariables manager)
			//%s = Math.atan(%s);
			sb.append( String.format("%s = Math.atan(%s);", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "inv": // Info inv( final Variable A , ManagerTempVariables manager)
			sb.append(String.format("%s = 1.0 / %s;", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "neg": // Info neg(final Variable A, ManagerTempVariables manager)
			//%s = -%s;
			sb.append( String.format("%s = -%s;", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "det": // Info det( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "trace": // Info trace( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "min": // Info min( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "abs": // Info abs( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "rref": // Info rref( final Variable A , ManagerTempVariables manager)
			sb.append(String.format("%s = (%s == 0) ? 0.0 : 1.0;", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "sqrt": // Info sqrt(final Variable A, ManagerTempVariables manager)
			//%s = Math.sqrt(a);
			sb.append( String.format("%s = Math.sqrt(%s);", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "pinv": // Info pinv( final Variable A , ManagerTempVariables manager)
			sb.append(String.format("%s = 1.0 / %s;", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "sin": // Info sin(final Variable A, ManagerTempVariables manager)
			//%s = Math.sin(%s);
			sb.append( String.format("%s = Math.sin(%s);", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "exp": // Info exp(final Variable A, ManagerTempVariables manager)
			//%s = Math.exp(%s);
			sb.append( String.format("%s = Math.exp(%s);", output.getOperand(), A.getOperand()) );
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
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.add(m.matrix, s.getDouble(), output.matrix);
			sb.append( String.format(formatCommonOps3, "add", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.subtract(m, v, output.matrix);
			sb.append( String.format(formatCommonOps3, "subtract", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.divide(s.getDouble(), m.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "divide", B.getName(), A.getName(), output.getName()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.scale(s.getDouble(),m.matrix,output.matrix);
			sb.append( String.format(formatCommonOps3, "scale", B.getName(), A.getName(), output.getName()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
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
			sb.append( String.format("%s.reshape(%s, %s);", output.getOperand(), A.getOperand(), A.getOperand()) );
			//CommonOps_DDRM.setIdentity(output.matrix);
			sb.append( String.format(formatCommonOps1, "setIdentity", output.getOperand()) );
			return sb.toString();
		case "neg": // Info neg(final Variable A, ManagerTempVariables manager)
			//%s = -%s;
			sb.append( String.format("%s = -%s;", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "min": // Info min( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "abs": // Info abs( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "max": // Info max( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;", output.getOperand(), A.getOperand()) );
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
			emitReshape(sb, output, B, B);
			//CommonOps_DDRM.add(m.matrix, s.getDouble(), output.matrix);
			sb.append( String.format(formatCommonOps3, "add", B.getName(), A.getName(), output.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, B, B);
			//CommonOps_DDRM.subtract(v, m, output.matrix);
			sb.append( String.format(formatCommonOps3, "subtract", A.getName(), B.getName(), output.getName()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, B, B);
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
			sb.append("//TODO: " + op);//TODO MANUAL
			return sb.toString();
		case "log": // Info log(final Variable A, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.elementLog(a, out);
			sb.append( String.format(formatCommonOps2, "elementLog", A.getName(), output.getName()) );
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
			emitReshape(sb, output, A, A);
VariableMatrix mA = (VariableMatrix)A;
output.matrix.reshape(mA.matrix.numRows, mA.matrix.numCols);
if( !CommonOps_DDRM.invert(mA.matrix,output.matrix) )
throw new RuntimeException("Inverse failed!");
			*/
			sb.append("//TODO: " + op);//TODO MANUAL
			return sb.toString();
		case "eye": // Info eye( final Variable A , ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.setIdentity(output.matrix);
			sb.append( String.format(formatCommonOps1, "setIdentity", output.getName()) );
			return sb.toString();
		case "neg": // Info neg(final Variable A, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
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
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.abs(a, output.matrix);
			sb.append( String.format(formatCommonOps2, "abs", A.getName(), output.getName()) );
			return sb.toString();
		case "rref": // Info rref( final Variable A , ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.rref(a, -1, output.matrix);
			sb.append( String.format(formatCommonOps3, "rref", A.getName(), "-1", output.getName()) );
			return sb.toString();
		case "pinv": // Info pinv( final Variable A , ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.pinv(mA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "pinv", A.getName(), output.getName()) );
			return sb.toString();
		case "transpose": // Info transpose( final Variable A , ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.transpose(mA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "transpose", A.getName(), output.getName()) );
			return sb.toString();
		case "exp": // Info exp(final Variable A, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
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
			emitReshape( sb, codeOp.output, codeOp.input.get(0), codeOp.input.get(0) );
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

	private static String copyROp(String[] operands, CodeOperation codeOp) {
    	Extents extents = new Extents();

    	ArrayExtent rowExtent = new ArrayExtent();
    	ArrayExtent colExtent = new ArrayExtent();
    	
    	StringBuilder sb = new StringBuilder();
    	
        if( codeOp.range.size() == 1 ) {
        	sb.append( String.format("//if( !MatrixFeatures_DDRM.isVector(%s))", codeOp.input.get(0).getOperand() ) );
        	sb.append( "\tthrow new Exception(\"Source must be a vector for copy into elements\");" );
            if( extents.extractSimpleExtents(codeOp.range.get(0),false,-1)) {
                int length = extents.col1-extents.col0+1;
//                if( msrc.getNumElements() != length )
//                    throw new IllegalArgumentException("Source vector not the right length.");
//                if( extents.col1+1 > mdst.getNumElements() )
//                    throw new IllegalArgumentException("Requested range is outside of dst length");
                String source = codeOp.input.get(0).getOperand();
                if (codeOp.input.get(0).getType() != VariableType.MATRIX) {
                	StringBuffer s = new StringBuffer();
                	s.append("new DMatrixRMaj(new double[] {");
                	for (int i = 0; i < length; i++) {
            			if (i != 0) s.append(",");
            			s.append(source);
                	}
                	s.append("})");
                	source = s.toString();
                }
                sb.append( String.format("System.arraycopy(%s.data,0,%s.data,%d,%d);", source, codeOp.output.getOperand(), extents.col0, length ));
            } else {
            	throw new IllegalArgumentException("Complex target ranges can not be compiled.");
//            	colExtent.extractArrayExtent(codeOp.range.get(0),-1);
////                if( colExtent.length > msrc.getNumElements() )
////                    throw new IllegalArgumentException("src doesn't have enough elements");
//                for (int i = 0; i < colExtent.length; i++) {
//                    mdst.data[colExtent.array[i]] = msrc.data[i];
//                }
            }
        } else if( codeOp.range.size() == 2 ) {
            if(extents.extractSimpleExtents(codeOp.range.get(0),true,-1) &&
            		extents.extractSimpleExtents(codeOp.range.get(1),false,-1) ) {

                int numRows = extents.row1 - extents.row0 + 1;
                int numCols = extents.col1 - extents.col0 + 1;

                String source = codeOp.input.get(0).getOperand();
                if (codeOp.input.get(0).getType() != VariableType.MATRIX) {
                	StringBuffer s = new StringBuffer();
                	s.append("new DMatrixRMaj(new double[][] {");
                	for (int iRow = 0; iRow < numRows; iRow++) {
                		if (iRow != 0) s.append(",");
                		s.append("{");
                		for (int jCol = 0; jCol < numCols; jCol++) {
                			if (jCol != 0) s.append(",");
                			s.append(source);
                		}
                		s.append("}");
                	}
                	s.append("})");
                	source = s.toString();
                }
                sb.append( String.format("CommonOps_DDRM.extract(%s, 0, %d, 0, %d, %s, %d, %d);", 
                		source, numRows, numCols, codeOp.output.getOperand(), extents.row0, extents.col0 ) );
            } else {
            	throw new IllegalArgumentException("Complex target ranges can not be compiled.");
//            	rowExtent.extractArrayExtent(codeOp.range.get(0),mdst.numRows);
//            	colExtent.extractArrayExtent(codeOp.range.get(1),mdst.numCols);
//
//                CommonOps_DDRM.insert(msrc, mdst, rowExtent.array, rowExtent.length,
//                        colExtent.array, colExtent.length);
            }
        } else {
            throw new RuntimeException("Unexpected number of ranges.  Should have been caught earlier");
        }
		//TODO CommonOps_DDRM.extract(src, 0, src.numRows, 0, src.numCols, dst, range[0].start, range[1].start);
		return sb.toString();
	}
	
	public static void emitJavaOperation(StringBuilder body, CodeOperation codeOp) {
		String[] fields = codeOp.name().split("-");
		String inputs = "";
		if (fields.length > 1) {
			inputs = fields[1];
		}
		if (codeOp.name().equals("matrixConstructor")) {
			body.append( construct( codeOp ) );
		} else if (fields[0].equals("copy")) {
			body.append( copyOp( fields, codeOp) );
		} else if (fields[0].equals("copyR")) {
			body.append( copyROp( fields, codeOp) );
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