package org.ejml.equation;

import java.util.List;

public class CodeExtents {
		
		protected Variable startRow;
		protected Variable stepRow;
		protected Variable endRow;
		protected Variable startCol;
		protected Variable stepCol;
		protected Variable endCol;
		
		protected IntegerSequence rowSequence;
		protected IntegerSequence colSequence;
		
		protected boolean  is1D;
		
		public boolean is1D() {
			return is1D;
		}
		
		protected boolean  isBlock;
		
		public boolean isBlock() {
			return isBlock;
		}
		
		protected List<Variable> range;
		
		private boolean isSimpleStep( Variable step ) {
            if (step == null) {
            	return true;
            } else if (step.getType() == VariableType.SCALAR) {
            	VariableScalar scalar = (VariableScalar) step;
            	if (scalar.getScalarType() == VariableScalar.Type.INTEGER && step.isConstant()) {
            		return step.getOperand().equals("1");
            	}
            } 
            return false;
		}
		
		public CodeExtents( List<Variable> range, int iFirst ) {
			this.range = range;
			isBlock = false;
			is1D = false;
			if (range.size() == 1+iFirst) {
				startRow = null;
				endRow = EmitJavaCodeOperation.zero;
				rowSequence = null;
				is1D = true;
				Variable var = range.get(iFirst+0);
		        if( var.getType() == VariableType.INTEGER_SEQUENCE ) {
		            IntegerSequence sequence = ((VariableIntegerSequence)var).sequence;
		            colSequence = sequence;
		            switch( sequence.getType() ) {
		            case FOR:
		                IntegerSequence.For seqFor = (IntegerSequence.For)sequence;
		                isBlock = isSimpleStep(seqFor.step);
		                startCol = seqFor.start;
		                endCol = seqFor.end;
		                break;
		            case RANGE:
		                IntegerSequence.Range seqRange = (IntegerSequence.Range)sequence;
		                isBlock = isSimpleStep(seqRange.step);
		                startCol = seqRange.start;
		                endCol = null;
		            	break;
		            default:
		            	isBlock = false;
		            	break;
		            }
		        } else if( var.getType() == VariableType.SCALAR ) {
		            isBlock = true;
	                startCol = var;
	                endCol = var;
		        }
			} else {
				Variable rowVar = range.get(iFirst+0);
		        if( rowVar.getType() == VariableType.INTEGER_SEQUENCE ) {
		            IntegerSequence sequence = ((VariableIntegerSequence)rowVar).sequence;
		            rowSequence = sequence;
		            switch( sequence.getType() ) {
		            case FOR:
		                IntegerSequence.For seqFor = (IntegerSequence.For)sequence;
		                isBlock = isSimpleStep(seqFor.step);
		                startRow = seqFor.start;
		                endRow = seqFor.end;
		                break;
		            case RANGE:
		                IntegerSequence.Range seqRange = (IntegerSequence.Range)sequence;
		                isBlock = isSimpleStep(seqRange.step);
		                startRow = seqRange.start;
		                endRow = null;
		            	break;
		            default:
		            	isBlock = false;
		            	break;
		            }
		        } else if( rowVar.getType() == VariableType.SCALAR ) {
		            isBlock = true;
	                startRow = rowVar;
	                endRow = rowVar;
		        }
				Variable colVar = range.get(iFirst+1);
		        if( colVar.getType() == VariableType.INTEGER_SEQUENCE ) {
		            IntegerSequence sequence = ((VariableIntegerSequence)colVar).sequence;
		            colSequence = sequence;
		            switch( sequence.getType() ) {
		            case FOR:
		                IntegerSequence.For seqFor = (IntegerSequence.For)sequence;
		                isBlock = isSimpleStep(seqFor.step);
		                startCol = seqFor.start;
		                endCol = seqFor.end;
		                break;
		            case RANGE:
		                IntegerSequence.Range seqRange = (IntegerSequence.Range)sequence;
		                isBlock = isSimpleStep(seqRange.step);
		                startCol = seqRange.start;
		                endCol = null;
		            	break;
		            default:
		            	isBlock = false;
		            	break;
		            }
		        } else if( colVar.getType() == VariableType.SCALAR ) {
		            isBlock = true;
	                startCol = colVar;
	                endCol = colVar;
		        }
			}
		}
		
		public String codeSimpleStartRow() {
			if (startRow == null) {
				return "0";
			} else {
				return startRow.getOperand();
			}
		}
		
		public String codeSimpleEndRow(String[] lastRowsCols) {
			if (endRow == null) {
				return lastRowsCols[0];
			} else {
				return endRow.getOperand() + "+1";
			}
		}

		public String codeSimpleStartCol() {
			if (startCol == null) {
				return "0";
			} else {
				return startCol.getOperand();
			}
		}
		
		public String codeSimpleEndCol(String[] lastRowsCols) {
			if (endCol == null) {
				return lastRowsCols[1];
			} else {
				return endCol.getOperand() + "+1";
			}
		}
		
		public String codeSimpleRows(String[] lastRowsCols) {
			return String.format("(%s - %s)", codeSimpleEndRow(lastRowsCols), codeSimpleStartRow() );
		}
		
		public String codeSimpleCols(String[] lastRowsCols) {
			return String.format("(%s - %s)", codeSimpleEndCol(lastRowsCols), codeSimpleStartCol() );
		}
		
		protected String codeIndiciesArray(String start, String step, String end) {
			return String.format("IntStream.iterate(%s, n -> n + %s).limit(1+(%s - %s) / %s).toArray()", 
					start, step, end, start, step);
		}
		
		public String codeComplexExtent(IntegerSequence sequence, String lastRowsCols) {
			StringBuilder sb = new StringBuilder();
			if (sequence == null) {
				sb.append("new int[] {0}");
			} else {
				String step = null;
	            switch( sequence.getType() ) {
	            case FOR:
	                IntegerSequence.For seqFor = (IntegerSequence.For)sequence;
	                if (seqFor.step == null) {
	                	step = "1";
	                } else {
	                	step = seqFor.step.getOperand();
	                }
//	                sb.append(String.format("indiciesArray(%s, %s, %s)", 
   	                sb.append( codeIndiciesArray(
	                		seqFor.start.getOperand(), step, seqFor.end.getOperand() ) );
	                break;
	            case RANGE:
	                IntegerSequence.Range seqRange = (IntegerSequence.Range)sequence;
	                if (seqRange.step == null) {
	                	step = "1";
	                } else {
	                	step = seqRange.step.getOperand();
	                }
	                String start = "0";
	                if (seqRange.start != null)
	                	start = seqRange.start.getOperand();
//	                sb.append(String.format("indiciesArray(%s, %s, %s-1)", 
   	                sb.append( codeIndiciesArray(
	                		start, step, lastRowsCols ) );
	            	break;
	            case EXPLICIT:
	            	IntegerSequence.Explicit seqExplicit = (IntegerSequence.Explicit)sequence;
	            	sb.append("new int[] {");
	            	for (VariableInteger var : seqExplicit.getSequence()) {
	            		sb.append(var.getOperand());
	            		sb.append(",");
	            	}
	            	sb.deleteCharAt(sb.length()-1);
	            	sb.append("}");
	            	break;
	            case COMBINED:
                    sb.append("Stream.of(");
                    IntegerSequence.Combined seqCombined = (IntegerSequence.Combined) sequence;
                    for (IntegerSequence s : seqCombined.sequences) {
                    	sb.append(codeComplexExtent(s, lastRowsCols));
	            		sb.append(",");
                    }
	            	sb.deleteCharAt(sb.length()-1);
	            	sb.append(").flatMapToInt(IntStream::of).toArray()");
	            	break;
	            }
			}
			return sb.toString();
		}
		
		public String codeComplexLength(IntegerSequence sequence, String lastRowsCols) {
			StringBuilder sb = new StringBuilder();
			if (sequence == null) {
				sb.append("1");
			} else {
	            switch( sequence.getType() ) {
	            case FOR:
	                IntegerSequence.For seqFor = (IntegerSequence.For)sequence;
	                if (seqFor.step == null) {
	                	sb.append(String.format("(%s-%s)", 
		                		seqFor.end.getOperand(), seqFor.start.getOperand() ) );
	                } else {
	                	sb.append(String.format("(%s-%s)/%s", 
	                		seqFor.end.getOperand(), seqFor.start.getOperand(), seqFor.step.getOperand() ) );
	                }
	                break;
	            case RANGE:
	                IntegerSequence.Range seqRange = (IntegerSequence.Range)sequence;
	                if (seqRange.step == null) {
	                	sb.append(String.format("(%s-%s)", 
	                			lastRowsCols, seqRange.start.getOperand() ) );
	                } else {
	                	sb.append(String.format("(%s-%s)/%s", 
	                			lastRowsCols, seqRange.start.getOperand(), seqRange.step.getOperand() ) );
	                }
	            	break;
	            case EXPLICIT:
	            	IntegerSequence.Explicit seqExplicit = (IntegerSequence.Explicit)sequence;
	            	sb.append( Integer.toString( seqExplicit.getSequence().size()) );
	            	break;
	            case COMBINED:
                    sb.append("(");
                    IntegerSequence.Combined seqCombined = (IntegerSequence.Combined) sequence;
                    for (IntegerSequence s : seqCombined.sequences) {
                    	sb.append(codeComplexLength(s, lastRowsCols));
	            		sb.append("+");
                    }
	            	sb.deleteCharAt(sb.length()-1);
	            	sb.append(")");
	            	break;
	            }
			}
			return sb.toString();
		}
		
		public String codeComplexRowIndices(String[] lastRowsCols) {
			return codeComplexExtent(rowSequence, lastRowsCols[0]);
		}

		public String codeComplexColIndices(String[] lastRowsCols) {
			return codeComplexExtent(colSequence, lastRowsCols[1]);
		}
		
		private String codeComplexRows( String[] lastRowsCols ) {
			return codeComplexLength(rowSequence, lastRowsCols[0]);
		}
		
		private String codeComplexCols( String[] lastRowsCols ) {
			return codeComplexLength(colSequence, lastRowsCols[1]);
		}
		
		public String codeNumRows( String[] lastRowsCols ) {
			if (isBlock()) {
				return codeSimpleRows(lastRowsCols);
			} else {
				return codeComplexRows(lastRowsCols);
			}
		}
		
		public String codeNumCols( String[] lastRowsCols ) {
			if (isBlock()) {
				return codeSimpleCols(lastRowsCols);
			} else {
				return codeComplexCols(lastRowsCols);
			}
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			String[] M = {"M.numRows", "M.numCols"};
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
		
	}