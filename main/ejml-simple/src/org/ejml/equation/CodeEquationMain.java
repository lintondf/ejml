package org.ejml.equation;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

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
			List<VariableInteger> integers,
			List<VariableScalar> doubles,
			List<VariableMatrix> matrices, 
			List<String> equations) {
		
		Equation eq = new Equation();
		OperationCodeFactory factory = new OperationCodeFactory();
		ManagerFunctions mf = new ManagerFunctions(factory);
		eq.setManagerFunctions(mf);
		
		for (VariableInteger v : integers ) {
			eq.alias(v.getValue(), v.getName());
		}
		for (Variable v : doubles ) {
			eq.alias(v, v.getName());
		}
		for (Variable v : matrices ) {
			eq.alias(v, v.getName());
		}
		
		for (String equationText : equations) {
			Sequence sequence = eq.compile(equationText); //, true, true);
			List<Operation> operations = sequence.getOperations();
			GenerateCodeOperations optimizer = new GenerateCodeOperations(operations);
			optimizer.mapVariableUsage();
	    	for (Operation operation : operations) {
	    		CodeOperation codeOp = (CodeOperation) operation;
	    		EmitCodeOperation.emitJavaOperation( block, codeOp );
	    	}			
		}
	}

	public static void main(String[] args) {
		List<VariableInteger> integers = new ArrayList<>();
		List<VariableScalar> doubles = new ArrayList<>();
		List<VariableMatrix> matrices = new ArrayList<>();
		
		VariableInteger v = VariableInteger.factory(1);
		integers.add(new VariableInteger(1, "A"));
		integers.add(new VariableInteger(2, "B"));
		
		List<String> equations = new ArrayList<>();
		equations.add("A=-B");
		equations.add("A=B--B");
		equations.add("A=B+-B");
		equations.add("A=B---5");
		equations.add("A=B--5");
		
		StringBuilder block = new StringBuilder();
		CodeEquationMain main = new CodeEquationMain( block, integers, doubles, matrices, equations);
		System.out.println(block.toString());
	}

}
