package org.ejml.equation;

import java.util.Arrays;
import java.util.HashMap;

import org.ejml.equation.Info;;

public class EmitJavaOperation implements IEmitOperation {

	final static String formatReshape = "%s.reshape( %s.numRows, %s.numCols );\n";
	final static String formatGeneral3 = "%s.%s( %s, %s, %s );\n";
	final static String formatGeneral2 = "%s.%s( %s, %s );\n";
	final static String formatGeneral1 = "%s.%s( %s );\n";
	final static String formatCommonOps6 = "CommonOps_DDRM.%s( %s, %s, %s, %s, %s, %s );\n";
	final static String formatCommonOps5 = "CommonOps_DDRM.%s( %s, %s, %s, %s, %s );\n";
	final static String formatCommonOps4 = "CommonOps_DDRM.%s( %s, %s, %s, %s );\n";
	final static String formatCommonOps3 = "CommonOps_DDRM.%s( %s, %s, %s );\n";
	final static String formatCommonOps2 = "CommonOps_DDRM.%s( %s, %s );\n";
	final static String formatCommonOps1 = "CommonOps_DDRM.%s( %s );\n";
	
	final static VariableInteger zero = new VariableInteger(0, "Integer{0}");
	final static VariableInteger one = new VariableInteger(1, "Integer{1}");
	
//	private boolean isNumeric(String str) {
//	    return str.matches("[+-]?\\d*(\\.\\d+)?");
//	}
	
	private void emitReshape(StringBuilder sb, Variable output, Variable A) {
		String o = output.getOperand();
		String a = A.getOperand();
		if (A.getType() == VariableType.MATRIX)
			a += ".numRows";
		sb.append( String.format(formatGeneral2, o, "reshape", a, a) );
	}

	private void emitReshape(StringBuilder sb, Variable output, String a, String b) {
		String o = output.getOperand();
		sb.append( String.format(formatGeneral2, o, "reshape", a, b) );
	}

	private void emitReshape(StringBuilder sb, Variable output, Variable A, Variable B) {
		String o = output.getOperand();
		String a = A.getOperand();
		String b = B.getOperand();
		if (A.getType() == VariableType.MATRIX)
			a += ".numRows";
		if (B.getType() == VariableType.MATRIX)
			b += ".numCols";
		sb.append( String.format(formatGeneral2, o, "reshape", a, b) );
	}


	private void emitReshapeTranspose(StringBuilder sb, Variable output, Variable A, Variable B) {
		String o = output.getOperand();
		String a = A.getOperand();
		String b = B.getOperand();
		if (A.getType() == VariableType.MATRIX)
			a += ".numRows";
		if (B.getType() == VariableType.MATRIX)
			b += ".numCols";
		sb.append( String.format(formatGeneral2, o, "reshape", b, a) );
	}

	private String mmOp(String op, Info codeOp) {
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
			emitReshape(sb, output, A, B);
			sb.append(String.format("LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(%s.numRows, %s.numCols);\n",
					A.getOperand(), A.getOperand() ) );
			sb.append(String.format("boolean ok = solver.setA(%s);\n", A.getOperand()));
			sb.append(String.format("solver.solve(%s, %s);\n", B.getOperand(), output.getOperand()) );
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
		throw new RuntimeException(op + " not implemented");
	}


	private String iiOp(String op, Info codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s + %s;
			sb.append( String.format("%s = %s + %s;\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "rand": // Info rand( final Variable A , final Variable B , ManagerTempVariables manager)
			sb.append( String.format(formatGeneral2, output.getOperand(), "reshape", A.getOperand(), B.getOperand()) );
			sb.append("Random rand = new Random();\n");
			final String fillUniform = "RandomMatrices_DDRM.fillUniform(%s, 0, 1, rand );\n";
			sb.append( String.format(fillUniform, output.getOperand()));
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s - %s;
			sb.append( String.format("%s = %s - %s;\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "ones": // Info ones( final Variable A , final Variable B , ManagerTempVariables manager)
			emitReshape(sb, output, A, B);
			//CommonOps_DDRM.fill(output.matrix, 1);
			sb.append( String.format(formatCommonOps2, "fill", output.getOperand(), 1) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s/%s;
			sb.append( String.format("%s = %s / %s;\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s*%s;
			sb.append( String.format("%s = %s * %s;\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "zeros": // Info zeros( final Variable A , final Variable B , ManagerTempVariables manager)
			emitReshape(sb, output, A, B);
			//CommonOps_DDRM.fill(output.matrix, 0);
			sb.append( String.format(formatCommonOps2, "fill", output.getOperand(), 0) );
			return sb.toString();
		case "randn": // Info randn( final Variable A , final Variable B , ManagerTempVariables manager)
			sb.append( String.format(formatGeneral2, output.getOperand(), "reshape", A.getOperand(), B.getOperand()) );
			sb.append("Random rand = new Random();\n");
			final String fillGaussian = "RandomMatrices_DDRM.fillGaussian(%s, 0, 1, rand );\n";
			sb.append( String.format(fillGaussian, output.getOperand()));
			return sb.toString();
		}
		throw new RuntimeException(op + " not implemented");
	}


	private String ssOp(String op, Info codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s + %s;
			sb.append( String.format("%s = %s + %s;\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s - %s;
			sb.append( String.format("%s = %s - %s;\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "pow": // Info pow(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.pow(a, b);
			sb.append( String.format("%s = Math.pow(%s, %s);\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s/%s;
			sb.append( String.format("%s = %s / %s;\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = %s*%s;
			sb.append( String.format("%s = %s * %s;\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "atan2": // Info atan2(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.atan2(a, b);
			sb.append( String.format("%s = Math.atan2(%s, %s);\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			//%s = Math.pow(a, b);
			sb.append( String.format("%s = Math.pow(%s, %s);\n", output.getOperand(), A.getOperand(), B.getOperand()) );
			return sb.toString();
		}
		throw new RuntimeException(op + " not implemented");
	}


	private String Op(String op, Info codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		
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
            if( codeOp.input.size() == 2 ) {
                sb.append(String.format("%s = %s.get(%s);\n", output.getOperand(), A.getOperand(), codeOp.input.get(1).getOperand() ));
            } else {
                sb.append(String.format("%s = %s.get(%s, %s);\n", output.getOperand(), A.getOperand(), 
                		codeOp.input.get(1).getOperand(), codeOp.input.get(2).getOperand() ));
            }
			
			return sb.toString();
		case "rng": // Info rng( final Variable A , ManagerTempVariables manager)
			sb.append("Random rand = new Random();\n");
			sb.append(String.format("rand.setSeed(%s);\n", A.getOperand()));
			return sb.toString();
		case "min_cols": // Info min_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, one, A );
			//CommonOps_DDRM.minCols(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "minCols", A.getName(), output.getName()) );
			return sb.toString();
		case "sum_all": // Info sum_one( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.elementSum(varA.matrix);
			sb.append( String.format("%s = CommonOps_DDRM.elementSum(%s);\n", output.getName(), A.getName()) );
			return sb.toString();
		case "sum_cols": // Info sum_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, one, A );
			//CommonOps_DDRM.sumCols(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "sumCols", A.getName(), output.getName()) );
			return sb.toString();
		case "extract": // Info extract( final List<Variable> inputs, ManagerTempVariables manager)
//			System.out.print("Extract/output: ");
//			System.out.println(codeOp.output);
//			for (Variable i : codeOp.input) {
//				System.out.print("Extract/input: ");
//				System.out.println(i);
//			}
	    	CodeExtents codeExtents = new CodeExtents( this, codeOp.input, 1 );
//	    	System.out.println( codeExtents.toString()); 
	    	
	    	String target = codeOp.output.getName();
	    	String source = codeOp.input.get(0).getName();
	    	String[] lastRowsCols = { source+".numRows", source+".numCols"};
	    	
	    	if (codeExtents.is1D) {
		    	lastRowsCols = new String[] {"", String.format("%s.numRows*%s.numCols", source, source) };
		    	emitReshape( sb, output, "1", codeExtents.codeNumCols(lastRowsCols) );
	    		sb.append(String.format(formatCommonOps4, "extract", source,
	    				codeExtents.codeComplexColIndices(lastRowsCols),
	    				codeExtents.codeNumCols(lastRowsCols),
	    				target) );
	    	} else if (codeExtents.isBlock) {
		    	emitReshape( sb, output, codeExtents.codeNumRows(lastRowsCols), codeExtents.codeNumCols(lastRowsCols) );
	    		sb.append(String.format(formatCommonOps6, "extract", source, 
	    				codeExtents.codeSimpleStartRow(), 
	    				codeExtents.codeSimpleEndRow(lastRowsCols), 
	    				codeExtents.codeSimpleStartCol(),
	    				codeExtents.codeSimpleEndCol(lastRowsCols),
	    				target) );
	    	} else {
		    	emitReshape( sb, output, codeExtents.codeNumRows(lastRowsCols), codeExtents.codeNumCols(lastRowsCols) );
	    		sb.append(String.format(formatCommonOps6, "extract", source, 
	    				codeExtents.codeComplexRowIndices(lastRowsCols), codeExtents.codeNumRows(lastRowsCols), 
	    				codeExtents.codeComplexColIndices(lastRowsCols), codeExtents.codeNumCols(lastRowsCols),
	    				target) );
	    	}
	    	
			return sb.toString();
		case "min_rows": // Info min_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, A, one );
			//CommonOps_DDRM.minRows(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "minRows", A.getName(), output.getName()) );
			return sb.toString();
		case "normP": // Info normP( final Variable A , final Variable P , ManagerTempVariables manager)
			//%s = NormOps_DDRM.normP(varA.matrix,valueP);
			sb.append( String.format("%s = NormOps_DDRM.normP(%s, %s);\n", output.getName(), 
					A.getName(), codeOp.input.get(1).getOperand()) );
			return sb.toString();
		case "sum_rows": // Info sum_two( final Variable A , final Variable P , ManagerTempVariables manager)
			emitReshape( sb, output, A, one );
			//CommonOps_DDRM.sumRows(varA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "sumRows", A.getName(), output.getName()) );
			return sb.toString();
		}
		throw new RuntimeException(op + " not implemented");
	}


	private String sOp(String op, Info codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "log": // Info log(final Variable A, ManagerTempVariables manager)
			//%s = Math.log(%s);
			sb.append( String.format("%s = Math.log(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "max": // Info max( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "cos": // Info cos(final Variable A, ManagerTempVariables manager)
			//%s = Math.cos(%s);
			sb.append( String.format("%s = Math.cos(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "normF": // Info normF( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "atan": // Info atan(final Variable A, ManagerTempVariables manager)
			//%s = Math.atan(%s);
			sb.append( String.format("%s = Math.atan(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "inv": // Info inv( final Variable A , ManagerTempVariables manager)
			sb.append(String.format("%s = 1.0 / %s;\n", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "neg": // Info neg(final Variable A, ManagerTempVariables manager)
			//%s = -%s;
			String negOp = A.getOperand();
			if (negOp.startsWith("-"))
				negOp = "("+negOp+")";
			sb.append( String.format("%s = -%s;\n", output.getOperand(), negOp) );
			return sb.toString();
		case "det": // Info det( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "trace": // Info trace( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "min": // Info min( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "abs": // Info abs( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "rref": // Info rref( final Variable A , ManagerTempVariables manager)
			sb.append(String.format("%s = (%s == 0) ? 0.0 : 1.0;\n", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "sqrt": // Info sqrt(final Variable A, ManagerTempVariables manager)
			//%s = Math.sqrt(a);
			sb.append( String.format("%s = Math.sqrt(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "pinv": // Info pinv( final Variable A , ManagerTempVariables manager)
			sb.append(String.format("%s = 1.0 / %s;\n", output.getOperand(), A.getOperand()));
			return sb.toString();
		case "sin": // Info sin(final Variable A, ManagerTempVariables manager)
			//%s = Math.sin(%s);
			sb.append( String.format("%s = Math.sin(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "exp": // Info exp(final Variable A, ManagerTempVariables manager)
			//%s = Math.exp(%s);
			sb.append( String.format("%s = Math.exp(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		}
		throw new RuntimeException(op + " not implemented");
	}


	private String msOp(String op, Info codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.add(m.matrix, s.getDouble(), output.matrix);
			sb.append( String.format(formatCommonOps3, "add", A.getOperand(), B.getOperand(), output.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.subtract(m, v, output.matrix);
			sb.append( String.format(formatCommonOps3, "subtract", A.getOperand(), B.getOperand(), output.getName()) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.divide(s.getDouble(), m.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "divide", A.getOperand(), B.getOperand(), output.getName()) );
			return sb.toString();
		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.scale(s.getDouble(),m.matrix,output.matrix);
			sb.append( String.format(formatCommonOps3, "scale", B.getOperand(), A.getOperand(), output.getName()) );
			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.elementPower(a, b, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementPower", A.getOperand(), B.getOperand(), output.getName()) );
			return sb.toString();
		}
		throw new RuntimeException(op + " not implemented");
	}


	private String iOp(String op, Info codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "eye": // Info eye( final Variable A , ManagerTempVariables manager)
			sb.append( String.format("%s.reshape(%s, %s);\n", output.getOperand(), A.getOperand(), A.getOperand()) );
			//CommonOps_DDRM.setIdentity(output.matrix);
			sb.append( String.format(formatCommonOps1, "setIdentity", output.getOperand()) );
			return sb.toString();
		case "neg": // Info neg(final Variable A, ManagerTempVariables manager)
			//%s = -%s;
			String negOp = A.getOperand();
			if (negOp.startsWith("-"))
				negOp = "("+negOp+")";
			sb.append( String.format("%s = -%s;\n", output.getOperand(), negOp) );
			return sb.toString();
		case "min": // Info min( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "abs": // Info abs( final Variable A , ManagerTempVariables manager)
			//%s = Math.abs(%s);
			sb.append( String.format("%s = Math.abs(%s);\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		case "max": // Info max( final Variable A , ManagerTempVariables manager)
			//%s = %s;
			sb.append( String.format("%s = %s;\n", output.getOperand(), A.getOperand()) );
			return sb.toString();
		}
		throw new RuntimeException(op + " not implemented");
	}


	private String smOp(String op, Info codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);
		Variable B = codeOp.input.get(1);
		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "add": // Info add(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, B, B);
			//CommonOps_DDRM.add(m.matrix, s.getDouble(), output.matrix);
			sb.append( String.format(formatCommonOps3, "add", B.getOperand(), A.getOperand(), output.getName()) );
			return sb.toString();
		case "subtract": // Info subtract(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, B, B);
//			CommonOps_DDRM.subtract(v, m, output.matrix);
			sb.append( String.format(formatCommonOps3, "subtract", A.getOperand(), B.getOperand(), output.getName()) );
			return sb.toString();
		case "divide": // Info divide(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.divide(s.getDouble(), m.matrix, output.matrix);
			sb.append( String.format(formatCommonOps3, "divide", B.getOperand(), A.getOperand(), output.getName()) );
			return sb.toString();
//		case "multiply": // Info multiply(final Variable A, final Variable B, ManagerTempVariables manager)
//			emitReshape(sb, output, A, A);
//			//CommonOps_DDRM.scale(s.getDouble(),m.matrix,output.matrix);
//			sb.append( String.format(formatCommonOps3, "scale", B.getOperand(), A.getOperand(), output.getName()) );
//			return sb.toString();
		case "elementPow": // Info elementPow(final Variable A, final Variable B, ManagerTempVariables manager)
			emitReshape(sb, output, B, B);
			//CommonOps_DDRM.elementPower(a, b, output.matrix);
			sb.append( String.format(formatCommonOps3, "elementPower", A.getOperand(), B.getOperand(), output.getName()) );
			return sb.toString();
		}
		throw new RuntimeException(op + " not implemented");
	}


	private String mOp(String op, Info codeOp) {
		Variable output = codeOp.output;
		Variable A = codeOp.input.get(0);

		StringBuilder sb = new StringBuilder();
		switch (op) {
		case "diag": // Info diag( final Variable A , ManagerTempVariables manager)
			sb.append(String.format("if (MatrixFeatures_DDRM.isVector(%s)) { //;\n", A.getOperand() ));
			sb.append("\t");
			emitReshape(sb, output, A);
			sb.append("\t");
			sb.append(String.format("CommonOps_DDRM.diag(%s, %s.numRows, %s.data);\n", output.getOperand(), A.getOperand(), A.getOperand()) );
			sb.append("} else { //;\n");
			sb.append("\t");
			emitReshape(sb, output, A, one);
			sb.append("\t");
			sb.append(String.format("CommonOps_DDRM.extractDiag(%s, %s);\n", A.getOperand(), output.getOperand()) );
			sb.append("}//;\n");
			return sb.toString();
		case "log": // Info log(final Variable A, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.elementLog(a, out);
			sb.append( String.format(formatCommonOps2, "elementLog", A.getName(), output.getName()) );
			return sb.toString();
		case "max": // Info max( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.elementMax(%s);
			sb.append( String.format("%s = CommonOps_DDRM.elementMax(%s);\n", output.getName(), A.getName()) );
			return sb.toString();
		case "normF": // Info normF( final Variable A , ManagerTempVariables manager)
			//%s = NormOps_DDRM.normF(%s);
			sb.append( String.format("%s = NormOps_DDRM.normF(%s);\n", output.getName(), A.getName()) );
			return sb.toString();
		case "inv": // Info inv( final Variable A , ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			sb.append(String.format("boolean ok = CommonOps_DDRM.invert(%s, %s);\n", A.getName(), output.getName()));
			sb.append("//TODO check ok\n");
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
			sb.append( String.format("%s = CommonOps_DDRM.det(%s);\n", output.getName(), A.getName()) );
			return sb.toString();
		case "trace": // Info trace( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.trace(mA.matrix);
			sb.append( String.format("%s = CommonOps_DDRM.trace(%s);\n", output.getName(), A.getName()) );
			return sb.toString();
		case "min": // Info min( final Variable A , ManagerTempVariables manager)
			//%s = CommonOps_DDRM.elementMin(%s);
			sb.append( String.format("%s = CommonOps_DDRM.elementMin(%s);\n", output.getName(), A.getName()) );
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
			emitReshapeTranspose(sb, output, A, A);
			//CommonOps_DDRM.transpose(mA.matrix, output.matrix);
			sb.append( String.format(formatCommonOps2, "transpose", A.getName(), output.getName()) );
			return sb.toString();
		case "exp": // Info exp(final Variable A, ManagerTempVariables manager)
			emitReshape(sb, output, A, A);
			//CommonOps_DDRM.elementExp(a, out);
			sb.append( String.format(formatCommonOps2, "elementExp", A.getName(), output.getName()) );
			return sb.toString();
		}
		throw new RuntimeException(op + " not implemented");
	}
	
	private String construct(Info codeOp) {
		StringBuilder sb = new StringBuilder();
		//codeOp.constructor.output = (VariableMatrix) codeOp.output;
		CodeMatrixConstructor cmc = new CodeMatrixConstructor( codeOp.constructor );
		cmc.construct(sb);
		return sb.toString();
	}

	private String copyOp(String[] operands, Info codeOp) {
		//copy: ii, ss, sm1, none
		switch (operands[1]) {
		case "mm":
			StringBuilder sb = new StringBuilder();
			emitReshape( sb, codeOp.output, codeOp.input.get(0), codeOp.input.get(0) );
			sb.append( String.format("%s.set( %s );\n", codeOp.output.getName(), codeOp.input.get(0).getName() ));
			return sb.toString();
		case "ii":
			return String.format("%s = %s;\n", codeOp.output.getOperand(), codeOp.input.get(0).getOperand());
		case "ss":
			return String.format("%s = %s;\n", codeOp.output.getOperand(), codeOp.input.get(0).getOperand());
		case "sm1":
			return String.format("%s = %s.unsafe_get(0,0);\n", codeOp.output.getOperand(), codeOp.input.get(0).getOperand() );
		default:
			break;
		}
		throw new RuntimeException(Arrays.toString(operands) + " " + codeOp.toString() + " not implemented");
	}
	
	private String copyROp(String[] operands, Info codeOp) {
    	StringBuilder sb = new StringBuilder();
    	
    	CodeExtents codeExtents = new CodeExtents( this, codeOp.range, 0 );
    	//System.out.println( codeExtents.toString());
    	
    	String target = codeOp.output.getName();
        String source = codeOp.input.get(0).getName();
        String[] lastRowsCols = { target+".numRows", target+".numCols"};
        if (codeOp.input.get(0).getType() != VariableType.MATRIX) {
        	source = String.format("new DMatrixRMaj(%s, %s, %s)", 
        			codeExtents.codeNumRows(lastRowsCols), 
        			codeExtents.codeNumCols(lastRowsCols), 
        			codeOp.input.get(0).getOperand());
        }
    	
    	if (codeExtents.is1D) {
	    	lastRowsCols = new String[] {"", String.format("%s.numRows*%s.numCols", target, target) };
	        if (codeOp.input.get(0).getType() != VariableType.MATRIX) {
	        	source = String.format("new DMatrixRMaj(1, %s, %s)", 
	        			codeExtents.codeNumCols(lastRowsCols), 
	        			codeOp.input.get(0).getOperand());
	        }
    		sb.append(String.format(formatCommonOps4, "insert", source, codeOp.output.getName(),
    				codeExtents.codeComplexColIndices(lastRowsCols),
    				codeExtents.codeNumCols(lastRowsCols)) );
    	} else if (codeExtents.isBlock) {
    		sb.append(String.format(formatCommonOps4, "insert", source, codeOp.output.getName(), 
    				codeExtents.codeSimpleStartRow(), codeExtents.codeSimpleStartCol()) );
    	} else {
    		sb.append(String.format(formatCommonOps6, "insert", source, codeOp.output.getName(), 
    				codeExtents.codeComplexRowIndices(lastRowsCols), codeExtents.codeNumRows(lastRowsCols), 
    				codeExtents.codeComplexColIndices(lastRowsCols), codeExtents.codeNumCols(lastRowsCols)) );
    	}
    	return sb.toString();
	}
	
	@Override
	public void declare( StringBuilder header, String indent, Variable variable ) {
		if (variable.isConstant())
			return;
		switch (variable.getType()) {
		case SCALAR:
			VariableScalar scalar = (VariableScalar) variable;
			if (scalar.getScalarType() == VariableScalar.Type.INTEGER) {
				header.append( String.format("%s%-10s %s;\n", indent, "int", variable.getOperand() ));
			} else {
				header.append( String.format("%s%-10s %s;\n", indent, "double", variable.getOperand() ));    					
			}
			break;
		case MATRIX:
			header.append( String.format("%s%-10s %s = new DMatrixRMaj(1,1);\n", indent, "DMatrixRMaj", variable.getName() ));
			break;
		default:
			throw new RuntimeException("Unhandled variable type encountered: " + variable);
		}
	}

	
	
	@Override
	public VariableInteger getOne() {
		return one;
	}
	
	@Override
	public VariableInteger getZero() {
		return zero;
	}
	
	@Override
	public void emitOperation(StringBuilder body, Info codeOp) {
		String[] fields = codeOp.op.name().split("-");
		String inputs = "";
		if (fields.length > 1) {
			inputs = fields[1];
		}
		if (codeOp.op.name().equals("matrixConstructor")) {
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
	
//	public static int[] indiciesArray( int start, int step, int end ) {
//		int n = (end - start) / step;
//		int[] a = new int[n+1];
//		for (int i = 0; i < a.length; i++) {
//			a[i] = start;
//			start += step;
//		}
//		return a;
//	}
//	
//	public static void main(String[] args) {
//		int[] a = indiciesArray(0, 2, 5);
//		System.out.println( Arrays.toString( a ) );
//		int[] b = indiciesArray(5, 3, 15);
//		System.out.println( Arrays.toString( b ) );
//		int[] c = new int[] {21, 25, 29};
//		System.out.println( Arrays.toString( c ) );
//		int[] Z = Stream.of(a, b, c).flatMapToInt(IntStream::of).toArray();
//		System.out.println( Arrays.toString( Z ) );
//		int[] W = Stream.of(indiciesArray(0, 2, 5), indiciesArray(5, 3, 15), new int[] {21, 25, 29}).flatMapToInt(IntStream::of).toArray();
//		System.out.println( Arrays.toString( W ) );
//		
//		DMatrixRMaj M = new DMatrixRMaj(5, 7, 3.14);
//		System.out.println(M);
//	}
}