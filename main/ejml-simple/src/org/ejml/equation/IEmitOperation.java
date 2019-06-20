package org.ejml.equation;

public interface IEmitOperation {

	VariableInteger getOne();

	VariableInteger getZero();

	void emitOperation(StringBuilder body, Info codeOp);

	void declare(StringBuilder header, String indent, Variable variable);

}