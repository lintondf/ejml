package org.ejml.equation;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.ejml.data.DMatrixRMaj;
import org.ejml.equation.GenerateCodeOperations.Usage;


public class CodeEquationMain {
	/*
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
 
	 */
	
	public CodeEquationMain(StringBuilder block, 
			List<String> integers,
			List<String> doubles,
			List<String> matrices, 
			List<String> equations) {
		
		Equation eq = new Equation();
		OperationCodeFactory factory = new OperationCodeFactory();
		ManagerFunctions mf = new ManagerFunctions(factory);
		eq.setManagerFunctions(mf);
		
		StringBuilder body = new StringBuilder();
		StringBuilder header = new StringBuilder();
		
		for (String v : integers ) {
			eq.alias(new Integer(0), v);
			header.append( String.format("%-10s %s;\n", "int", v ));
		}
		for (String v : doubles ) {
			eq.alias(new Double(0.0), v);
			header.append( String.format("%-10s %s;\n", "double", v ));
		}
		for (String v : matrices ) {
			eq.alias(new DMatrixRMaj(1,1), v);
			header.append( String.format("%-10s %s = new DMatrixRMaj(1,1);\n", "DMatrixRMaj", v ));
		}

		TreeSet<String> declaredTemps = new TreeSet<>();
		for (String equationText : equations) {
			body.append(String.format("// %s\n",  equationText));
			Sequence sequence = eq.compile(equationText);//, true, true);//); //
			List<Operation> operations = sequence.getOperations();
			GenerateCodeOperations generator = new GenerateCodeOperations(operations);
			generator.optimize();
			for (Usage usage : generator.integerUsages) {
				Variable variable = usage.variable;
				if (! declaredTemps.contains(variable.getOperand())) {
					declaredTemps.add(variable.getOperand() );
					GenerateEquationCoders.declareTemporary( header, "", variable );
				}
			}
			for (Usage usage : generator.doubleUsages) {
				Variable variable = usage.variable;
				if (! declaredTemps.contains(variable.getOperand())) {
					declaredTemps.add(variable.getOperand() );
					GenerateEquationCoders.declareTemporary( header, "", variable );
				}
			}
			for (Usage usage : generator.matrixUsages) {
				Variable variable = usage.variable;
				if (! declaredTemps.contains(variable.getOperand())) {
					declaredTemps.add(variable.getOperand() );
					GenerateEquationCoders.declareTemporary( header, "", variable );
				}
			}
			for (Operation operation : operations) {
	    		CodeOperation codeOp = (CodeOperation) operation;
	    		EmitCodeOperation.emitJavaOperation( body, codeOp );
	    	}	
			generator.releaseTemporaries(eq);
		}
    	block.append(header);
    	block.append(body);
		
	}
	
	public static void main(String[] args) {
		List<String> integers = new ArrayList<>();
		List<String> doubles = new ArrayList<>();
		List<String> matrices = new ArrayList<>();
		
		matrices.add("A");
		
		List<String> equations = new ArrayList<>();
		equations.add("A((1-1):2,2:3)=0.5");
		
		StringBuilder block = new StringBuilder();
		CodeEquationMain main = new CodeEquationMain( block, integers, doubles, matrices, equations);
		try {
			PrintStream code = System.out; // new PrintWriter("code.java");
			code.println("public class code {");
			code.println("public void test() {");
			code.println(block.toString());
			code.println("}");
			code.println("}");
			code.close();
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

}
