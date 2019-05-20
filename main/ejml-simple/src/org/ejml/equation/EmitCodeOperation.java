package org.ejml.equation;

public class EmitCodeOperation {

	final static String formatReshape = "%s.reshape( %s.numRows, %s.numCols );";
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
			/*
						//CommonOps_DDRM.kron(mA, mB, output.matrix);
			sb.append( String.format(formatCommonOps3, "kron", A.getName(), B.getName(), output.getName()) );
DMatrixRMaj mA = ((VariableMatrix)A).matrix;
DMatrixRMaj mB = ((VariableMatrix)B).matrix;
output.matrix.reshape(mA.numRows * mB.numRows, mA.numCols * mB.numCols);
CommonOps_DDRM.kron(mA, mB, output.matrix);
			*/
			//TODO MANUAL
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
			/*
			//%s = VectorVectorMult_DDRM.innerProd(a,b);
			sb.append( String.format("%s = VectorVectorMult_DDRM.innerProd(%s, %s);", output.getName(), A.getName(), B.getName()) );
DMatrixRMaj a = ((VariableMatrix)A).matrix;
DMatrixRMaj b = ((VariableMatrix)B).matrix;
if( !MatrixFeatures_DDRM.isVector(a) || !MatrixFeatures_DDRM.isVector(b))
throw new RuntimeException("Both inputs to dot() must be vectors");
output.value = VectorVectorMult_DDRM.innerProd(a,b);
			*/
			//TODO MANUAL
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
			/*
			sb.append( String.format(formatReshape, output.getName(), A.getName(), B.getName()) );
int numRows = ((VariableInteger)A).value;
int numCols = ((VariableInteger)B).value;
output.matrix.reshape(numRows,numCols);
RandomMatrices_DDRM.fillUniform(output.matrix, 0,1,manager.getRandom());
			*/
			//TODO MANUAL
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
		case "copy": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			/*
((VariableInteger)dst).value = ((VariableInteger)src).value;
			*/
			//TODO MANUAL
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
			/*
			sb.append( String.format(formatReshape, output.getName(), A.getName(), B.getName()) );
int numRows = ((VariableInteger)A).value;
int numCols = ((VariableInteger)B).value;
output.matrix.reshape(numRows,numCols);
RandomMatrices_DDRM.fillGaussian(output.matrix, 0,1,manager.getRandom());
			*/
			//TODO MANUAL
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
		case "copy": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			/*
((VariableDouble)dst).value = ((VariableScalar)src).getDouble();
			*/
			//TODO MANUAL
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
		Variable B = codeOp.input.get(1);
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
			/*
int seed = ((VariableInteger)A).value;
manager.getRandom().setSeed(seed);
			*/
			//TODO MANUAL
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
		case "matrixConstructor": // Info matrixConstructor( final MatrixConstructor m )
			/*
m.construct();
			*/
			//TODO MANUAL
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
		case "copy": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			/*
((VariableIntegerSequence)dst).sequence = ((VariableIntegerSequence)src).sequence;
			*/
			//TODO MANUAL
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
		Variable B = codeOp.input.get(1);
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
			/*
codeOutputValue1: %s = 1.0/%s;
VariableScalar mA = (VariableScalar)A;
output.value = 1.0/mA.getDouble();
			*/
			//TODO MANUAL
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
			/*
codeOutputValue1: %s = a == 0 ? 0 : 1;
double a = ((VariableScalar)A).getDouble();
output.value = a == 0 ? 0 : 1;
			*/
			//TODO MANUAL
			return sb.toString();
		case "sqrt": // Info sqrt(final Variable A, ManagerTempVariables manager)
			//%s = Math.sqrt(a);
			sb.append( String.format("%s = Math.sqrt(%s);", output.getName(), A.getName()) );
			return sb.toString();
		case "pinv": // Info pinv( final Variable A , ManagerTempVariables manager)
			/*
codeOutputValue1: %s = 1.0/%s;
VariableScalar mA = (VariableScalar)A;
output.value = 1.0/mA.getDouble();
			*/
			//TODO MANUAL
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


	protected static String sm1Op(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "copy": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			/*
DMatrixRMaj s = ((VariableMatrix) src).matrix;
if( s.numRows != 1 || s.numCols != 1 ) {
throw new RuntimeException("Attempting to assign a non 1x1 matrix to a double");
((VariableDouble) dst).value = s.unsafe_get(0,0);
			*/
			//TODO MANUAL
			return sb.toString();
		}
		return sb.toString();
	}


	protected static String iOp(String op, CodeOperation codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "eye": // Info eye( final Variable A , ManagerTempVariables manager)
			sb.append( String.format(formatReshape, output.getName(), A.getName(), A.getName()) );
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
		case "copyR": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			/*
codeIntVariableInteger: int index = i*mdst.numCols + extents.col0;
OperationExecuteFactory.Extents extents = new OperationExecuteFactory.Extents();
OperationExecuteFactory.ArrayExtent rowExtent = new OperationExecuteFactory.ArrayExtent();
OperationExecuteFactory.ArrayExtent colExtent = new OperationExecuteFactory.ArrayExtent();
double msrc = ((VariableScalar)src).getDouble();
DMatrixRMaj mdst = ((VariableMatrix)dst).matrix;
if( range.size() == 1 ) {
if(extractSimpleExtents(range.get(0),extents,false,mdst.getNumElements())) {
Arrays.fill(mdst.data,extents.col0,extents.col1+1,msrc);
} else {
extractArrayExtent(range.get(0),mdst.getNumElements(),colExtent);
for (int i = 0; i < colExtent.length; i++) {
mdst.data[colExtent.array[i]] = msrc;
} else if( range.size() == 2 ) {
if(extractSimpleExtents(range.get(0),extents,true,mdst.getNumRows()) &&
extractSimpleExtents(range.get(1),extents,false,mdst.getNumCols()) ) {
extents.row1 += 1;
extents.col1 += 1;
for (int i = extents.row0; i < extents.row1; i++) {
int index = i*mdst.numCols + extents.col0;
for (int j = extents.col0; j < extents.col1; j++) {
mdst.data[index++] = msrc;
} else {
extractArrayExtent(range.get(0),mdst.numRows,rowExtent);
extractArrayExtent(range.get(1),mdst.numCols,colExtent);
for (int i = 0; i < rowExtent.length; i++) {
for (int j = 0; j < colExtent.length; j++) {
mdst.unsafe_set(rowExtent.array[i],colExtent.array[j],msrc);
} else {
throw new RuntimeException("Unexpected number of ranges.  Should have been caught earlier");
			*/
			//TODO MANUAL
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
		Variable B = codeOp.input.get(1);
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


	public static void emitJavaOperation(StringBuilder body, CodeOperation codeOp) {
		String[] fields = codeOp.name().split("-");
		String inputs = "";
		if (fields.length > 1) {
			inputs = fields[1];
		}
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
		case "sm1":
			body.append( sm1Op( fields[0], codeOp ) );
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