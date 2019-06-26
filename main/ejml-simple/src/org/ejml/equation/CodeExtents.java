/*
 * Copyright (c) 2009-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.equation;

import java.util.List;

/** Handles array extents when generating code;
 *   Mirrors ArrayExtent and Extents classes
 * 
 * @author D. F. Linton, Blue Lightning Development, LLC 2019.
 */
public class CodeExtents {

	protected Variable startRow;
	protected Variable stepRow;
	protected Variable endRow;
	protected Variable startCol;
	protected Variable stepCol;
	protected Variable endCol;

	protected IntegerSequence rowSequence;
	protected IntegerSequence colSequence;

	protected boolean is1D;

	public boolean is1D() {
		return is1D;
	}

	protected boolean isBlock;

	public boolean isBlock() {
		return isBlock;
	}

	protected List<Variable> extents; // one or two elements (row and then optional col extents

	private boolean isSimpleStep(Variable step) {
		if (step == null) {
			return true;
		} else if (step.getType() == VariableType.SCALAR) {
			VariableScalar scalar = (VariableScalar) step;
			if (scalar.getScalarType() == VariableScalar.Type.INTEGER && scalar.isConstant()) {
				return step.getOperand().equals("1");
			}
		}
		return false;
	}
	
	
	/** Parses row and column extents
	 * @param extents - one or two elements (cols only if one element, rows, then cols if two elements)
	 */
	public CodeExtents(IEmitOperation coder, List<Variable> extents ) {
		this(coder, extents, 0);
	}
	
	/** Parses row and column extents
	 * @param extents - N elements
	 * @param iFirst - skip iFirst elements of extents, then cols if one element; rows, then cols if two elements
	 */
	public CodeExtents(IEmitOperation coder, List<Variable> extents, int iFirst) {
		this.extents = extents;
		isBlock = false;
		is1D = false;
		if (extents.size() == 1 + iFirst) {
			startRow = null;
			endRow = coder.getZero();
			rowSequence = null;
			is1D = true;
			Variable var = extents.get(iFirst + 0);
			if (var.getType() == VariableType.INTEGER_SEQUENCE) {
				IntegerSequence sequence = ((VariableIntegerSequence) var).sequence;
				colSequence = sequence;
				switch (sequence.getType()) {
				case FOR:
					IntegerSequence.For seqFor = (IntegerSequence.For) sequence;
					isBlock = isSimpleStep(seqFor.step);
					startCol = seqFor.start;
					endCol = seqFor.end;
					break;
				case RANGE:
					IntegerSequence.Range seqRange = (IntegerSequence.Range) sequence;
					isBlock = isSimpleStep(seqRange.step);
					startCol = seqRange.start;
					endCol = null;
					break;
				default:
					isBlock = false;
					break;
				}
			} else if (var.getType() == VariableType.SCALAR) {
				isBlock = true;
				startCol = var;
				endCol = var;
			}
		} else {
			Variable rowVar = extents.get(iFirst + 0);
			if (rowVar.getType() == VariableType.INTEGER_SEQUENCE) {
				IntegerSequence sequence = ((VariableIntegerSequence) rowVar).sequence;
				rowSequence = sequence;
				switch (sequence.getType()) {
				case FOR:
					IntegerSequence.For seqFor = (IntegerSequence.For) sequence;
					isBlock = isSimpleStep(seqFor.step);
					startRow = seqFor.start;
					endRow = seqFor.end;
					break;
				case RANGE:
					IntegerSequence.Range seqRange = (IntegerSequence.Range) sequence;
					isBlock = isSimpleStep(seqRange.step);
					startRow = seqRange.start;
					endRow = null;
					break;
				default:
					isBlock = false;
					break;
				}
			} else if (rowVar.getType() == VariableType.SCALAR) {
				isBlock = true;
				startRow = rowVar;
				endRow = rowVar;
			}
			Variable colVar = extents.get(iFirst + 1);
			if (colVar.getType() == VariableType.INTEGER_SEQUENCE) {
				IntegerSequence sequence = ((VariableIntegerSequence) colVar).sequence;
				colSequence = sequence;
				switch (sequence.getType()) {
				case FOR:
					IntegerSequence.For seqFor = (IntegerSequence.For) sequence;
					isBlock = isSimpleStep(seqFor.step);
					startCol = seqFor.start;
					endCol = seqFor.end;
					break;
				case RANGE:
					IntegerSequence.Range seqRange = (IntegerSequence.Range) sequence;
					isBlock = isSimpleStep(seqRange.step);
					startCol = seqRange.start;
					endCol = null;
					break;
				default:
					isBlock = false;
					break;
				}
			} else if (colVar.getType() == VariableType.SCALAR) {
				isBlock = true;
				startCol = colVar;
				endCol = colVar;
			}
		}
	}

	/** Code for staring row of a simple block
	 * 
	 * @return java code string
	 */
	public String codeSimpleStartRow() {
		if (startRow == null) {
			return "0";
		} else {
			return startRow.getOperand();
		}
	}

	/** Code for the ending row +1 of a simple block 
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	public String codeSimpleEndRow(String[] lastRowsCols) {
		if (endRow == null) {
			return lastRowsCols[0];
		} else {
			return endRow.getOperand() + "+1";
		}
	}

	/** Code for staring column of a simple block
	 * 
	 * @return java code string
	 */
	public String codeSimpleStartCol() {
		if (startCol == null) {
			return "0";
		} else {
			return startCol.getOperand();
		}
	}

	/** Code for the ending column +1 of a simple block 
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	public String codeSimpleEndCol(String[] lastRowsCols) {
		if (endCol == null) {
			return lastRowsCols[1];
		} else {
			return endCol.getOperand() + "+1";
		}
	}

	/** Code for the number of rows of a simple block
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	String codeSimpleRows(String[] lastRowsCols) {
		return String.format("(%s - %s)", codeSimpleEndRow(lastRowsCols), codeSimpleStartRow());
	}

	/** Code for the number of columns of a simple block
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	String codeSimpleCols(String[] lastRowsCols) {
		return String.format("(%s - %s)", codeSimpleEndCol(lastRowsCols), codeSimpleStartCol());
	}

	/** Code for a stepping range
	 * 
	 * @param start - String operand for range starting point
	 * @param step - String operand for range step increment
	 * @param end - String operand of final value in range (not +1)
	 * @return java code string
	 */
	private String codeIndiciesArray(String start, String step, String end) {
		return String.format("IntStream.iterate(%s, n -> n + %s).limit(1+(%s - %s) / %s).toArray()", start, step, end,
				start, step);
	}

	/** Code for one extent of a complex (non-contiguous) block
	 * 
	 * @param sequence - sequence for the extent
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	protected String codeComplexExtent(IntegerSequence sequence, String lastRowsCols) {
		StringBuilder sb = new StringBuilder();
		if (sequence == null) {
			sb.append("new int[] {0}");
		} else {
			String step = null;
			switch (sequence.getType()) {
			case FOR:
				IntegerSequence.For seqFor = (IntegerSequence.For) sequence;
				if (seqFor.step == null) {
					step = "1";
				} else {
					step = seqFor.step.getOperand();
				}
//	                sb.append(String.format("indiciesArray(%s, %s, %s)", 
				sb.append(codeIndiciesArray(seqFor.start.getOperand(), step, seqFor.end.getOperand()));
				break;
			case RANGE:
				IntegerSequence.Range seqRange = (IntegerSequence.Range) sequence;
				if (seqRange.step == null) {
					step = "1";
				} else {
					step = seqRange.step.getOperand();
				}
				String start = "0";
				if (seqRange.start != null)
					start = seqRange.start.getOperand();
//	                sb.append(String.format("indiciesArray(%s, %s, %s-1)", 
				sb.append(codeIndiciesArray(start, step, lastRowsCols));
				break;
			case EXPLICIT:
				IntegerSequence.Explicit seqExplicit = (IntegerSequence.Explicit) sequence;
				sb.append("new int[] {");
				for (VariableInteger var : seqExplicit.getSequence()) {
					sb.append(var.getOperand());
					sb.append(",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append("}");
				break;
			case COMBINED:
				sb.append("Stream.of(");
				IntegerSequence.Combined seqCombined = (IntegerSequence.Combined) sequence;
				for (IntegerSequence s : seqCombined.sequences) {
					sb.append(codeComplexExtent(s, lastRowsCols));
					sb.append(",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append(").flatMapToInt(IntStream::of).toArray()");
				break;
			}
		}
		return sb.toString();
	}

	/** Code for the length of a complex (non-contiguous) block
	 * 
	 * @param sequence - sequence for the extent
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	protected String codeComplexLength(IntegerSequence sequence, String lastRowsCols) {
		StringBuilder sb = new StringBuilder();
		if (sequence == null) {
			sb.append("1");
		} else {
			switch (sequence.getType()) {
			case FOR:
				IntegerSequence.For seqFor = (IntegerSequence.For) sequence;
				if (seqFor.step == null) {
					sb.append(String.format("(%s-%s)", seqFor.end.getOperand(), seqFor.start.getOperand()));
				} else {
					sb.append(String.format("(%s-%s)/%s", seqFor.end.getOperand(), seqFor.start.getOperand(),
							seqFor.step.getOperand()));
				}
				break;
			case RANGE:
				IntegerSequence.Range seqRange = (IntegerSequence.Range) sequence;
				if (seqRange.step == null) {
					sb.append(String.format("(%s-%s)", lastRowsCols, seqRange.start.getOperand()));
				} else {
					sb.append(String.format("(%s-%s)/%s", lastRowsCols, seqRange.start.getOperand(),
							seqRange.step.getOperand()));
				}
				break;
			case EXPLICIT:
				IntegerSequence.Explicit seqExplicit = (IntegerSequence.Explicit) sequence;
				sb.append(Integer.toString(seqExplicit.getSequence().size()));
				break;
			case COMBINED:
				sb.append("(");
				IntegerSequence.Combined seqCombined = (IntegerSequence.Combined) sequence;
				for (IntegerSequence s : seqCombined.sequences) {
					sb.append(codeComplexLength(s, lastRowsCols));
					sb.append("+");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append(")");
				break;
			}
		}
		return sb.toString();
	}

	/** Code for the row indices of a complex (non-contiguous) block
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	public String codeComplexRowIndices(String[] lastRowsCols) {
		return codeComplexExtent(rowSequence, lastRowsCols[0]);
	}

	/** Code for the column indices of a complex (non-contiguous) block
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	public String codeComplexColIndices(String[] lastRowsCols) {
		return codeComplexExtent(colSequence, lastRowsCols[1]);
	}

	/** Code for the number of rows of a complex (non-contiguous) block
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	private String codeComplexRows(String[] lastRowsCols) {
		return codeComplexLength(rowSequence, lastRowsCols[0]);
	}

	/** Code for the number of columns of a complex (non-contiguous) block
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	private String codeComplexCols(String[] lastRowsCols) {
		return codeComplexLength(colSequence, lastRowsCols[1]);
	}

	/** Code for the number of rows of this extent
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	public String codeNumRows(String[] lastRowsCols) {
		if (isBlock()) {
			return simplify(codeSimpleRows(lastRowsCols));
		} else {
			return simplify(codeComplexRows(lastRowsCols));
		}
	}

	/** Code for the number of columns of this extent
	 * 
	 * @param lastRowsCols - Strings for the last rows/cols (generally M.numRows, M.numCols)
	 * @return java code string
	 */
	public String codeNumCols(String[] lastRowsCols) {
		if (isBlock()) {
			return simplify(codeSimpleCols(lastRowsCols));
		} else {
			return simplify(codeComplexCols(lastRowsCols));
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String[] M = { "M.numRows", "M.numCols" };
		if (isBlock) {
			sb.append("BLOCK{");
			sb.append(codeSimpleStartRow());
			sb.append(",");
			sb.append(codeSimpleEndRow(M));
			sb.append(",");
			sb.append(codeSimpleStartCol());
			sb.append(",");
			sb.append(codeSimpleEndCol(M));
			sb.append("} [");
			sb.append(codeSimpleRows(M));
			sb.append(",");
			sb.append(codeSimpleCols(M));
			sb.append("]");
		} else {
			sb.append("ELEMENTS{");
			sb.append(codeComplexRowIndices(M));
			sb.append(",");
			sb.append(codeComplexColIndices(M));
			sb.append("} [");
			sb.append(codeComplexRows(M));
			sb.append(",");
			sb.append(codeComplexCols(M));
			sb.append("]");
		}
		return sb.toString();
	}

	public static String simplify( String expr ) {
		Equation eq = new Equation();
		try {
			eq.compile("out = " + expr ).perform();
			Variable v = eq.lookupVariable("out");
			if (v instanceof VariableScalar) {
				VariableScalar vs = (VariableScalar) v;
				if (vs.getScalarType() == VariableScalar.Type.INTEGER) {
					VariableInteger vi = (VariableInteger) vs;
					return Integer.toString( vi.getValue() );
				} else {
					return Double.toString( vs.getDouble() );
				}
			}
			return expr;
		} catch (Exception x) {
			return expr;
		}
	}
	
	
	public static void main(String[] args) {
		System.out.println( simplify("1+3*4"));
		System.out.println( simplify("1+3*4*a"));
	}
}