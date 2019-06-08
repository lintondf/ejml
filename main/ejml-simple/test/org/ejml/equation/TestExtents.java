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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;


/** Combined tests for hoisted Extents and ArrayExtent classes comparing to new CodeExtents class
 * 
 * @author D. F. Linton, Blue Lightning Development, LLC 2019.
 */
public class TestExtents {
	
	//Extent
	@Test
	public void testExtents_construct() {
		Extents extent = new Extents();
		assertTrue( extent.col0 == 0);
		assertTrue( extent.col1 == 0);
		assertTrue( extent.row0 == 0);
		assertTrue( extent.row1 == 0);
	}
	
	@Test
	public void testExtents_Integer() {
		Extents extents = new Extents();
		VariableInteger v = new VariableInteger(1);
		boolean isSimple = extents.extractSimpleExtents(v, true, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 0);
		assertTrue( extents.col1 == 0);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 1);
		v = new VariableInteger(2);
		isSimple = extents.extractSimpleExtents(v, false, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 2);
		assertTrue( extents.col1 == 2);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 1);
	}
	
	@Test
	public void testExtents_Double() {
		Extents extents = new Extents();
		VariableDouble d = new VariableDouble(3.14);
		try {
			extents.extractSimpleExtents(d, true, -1);
			fail("Should throw with non-integer input");
		} catch (Exception x) {
		}
	}
	
	protected VariableIntegerSequence variablesToCombined( Variable[] vars ) {
		TokenList list = new TokenList();
		for (Variable var : vars) {
			list.add(var);
		}
		return new VariableIntegerSequence(new IntegerSequence.Combined(list.getFirst(), list.getLast()));
	}
	
	protected VariableIntegerSequence variablesToExplicit( int[] vars ) {
		TokenList list = new TokenList();
		for (int var : vars) {
			list.add(VariableInteger.factory(var));
		}
		return new VariableIntegerSequence(new IntegerSequence.Explicit(list.getFirst(), list.getLast()));
	}
	
	protected VariableIntegerSequence variablesToFor( int[] vars ) { //{start,step,end} || {start, step}
		TokenList list = new TokenList();
		for (int var : vars) {
			list.add(VariableInteger.factory(var));
		}
		if (vars.length == 3)
			return new VariableIntegerSequence(new IntegerSequence.For(list.getFirst(), list.getFirst().next, list.getLast()));
		else
			return new VariableIntegerSequence(new IntegerSequence.For(list.getFirst(), null, list.getLast()));
	}
	
	protected VariableIntegerSequence variablesToRange( Variable startVar, Variable stepVar ) {
		TokenList.Token start = (startVar == null) ? null : new TokenList.Token(startVar);
		TokenList.Token step = (stepVar == null) ? null : new TokenList.Token(stepVar);
		return new VariableIntegerSequence(new IntegerSequence.Range(start, step));
	}
	
	@Test
	public void testExtents_SequenceFor() {
		Extents extents = new Extents();
		VariableIntegerSequence seq = variablesToFor( new int[] {1, 1, 10} );
		boolean isSimple = extents.extractSimpleExtents(seq, true, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 0);
		assertTrue( extents.col1 == 0);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 10);
		seq = variablesToFor( new int[] {5, 1, 9} );
		isSimple = extents.extractSimpleExtents(seq, false, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 5);
		assertTrue( extents.col1 == 9);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 10);
		
		extents = new Extents();
		seq = variablesToFor( new int[] {1, 10} );
		isSimple = extents.extractSimpleExtents(seq, true, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 0);
		assertTrue( extents.col1 == 0);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 10);
		seq = variablesToFor( new int[] {5, 9} );
		isSimple = extents.extractSimpleExtents(seq, false, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 5);
		assertTrue( extents.col1 == 9);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 10);
	}
	
	@Test 
	public void testExtents_SequencesOther() {
		Extents extents = new Extents();
		VariableIntegerSequence seq = variablesToRange( new VariableInteger(1), new VariableInteger(2));
		boolean isSimple = extents.extractSimpleExtents(seq, true, -1);
		assertFalse(isSimple);
		seq = variablesToExplicit( new int[] {1, 3, 5, 7});
		isSimple = extents.extractSimpleExtents(seq, true, -1);
		assertFalse(isSimple);
		seq = variablesToCombined( new Variable[] {new VariableInteger(1), variablesToExplicit( new int[] {1, 3, 5, 7})} );
		isSimple = extents.extractSimpleExtents(seq, true, -1);
		assertFalse(isSimple);
	}
	
	//ArrayExtent
	@Test
	public void testArrayExtent_construct() {
		ArrayExtent arrayExtent = new ArrayExtent();
		assertTrue( arrayExtent.length == 0); //TODO is this actual correct
		assertTrue( arrayExtent.array.length == 1);
	}
	
	@Test
	public void testArrayExtent_setLength() {
		ArrayExtent arrayExtent = new ArrayExtent();
		assertTrue( arrayExtent.length == 0);
		assertTrue( arrayExtent.array.length == 1);
		arrayExtent.setLength(5);
		assertTrue( arrayExtent.length == 5); 
		assertTrue( arrayExtent.array.length == 5);
		arrayExtent.setLength(3);
		assertTrue( arrayExtent.length == 3); 
		assertTrue( arrayExtent.array.length == 5);
		arrayExtent.setLength(15);
		assertTrue( arrayExtent.length == 15); 
		assertTrue( arrayExtent.array.length == 15);
	}
	
	@Test
	public void testArrayExtent_Double() {
		ArrayExtent arrayExtent = new ArrayExtent();
		VariableDouble d = new VariableDouble(3.14);
		try {
			arrayExtent.extractArrayExtent(d, 1);
			fail("Should throw with non-integer input");
		} catch (Exception x) {
		}
	}
	
	@Test
	public void testArrayExtent_Integer() {
		ArrayExtent arrayExtent = new ArrayExtent();
		VariableInteger i = new VariableInteger(123);
		arrayExtent.extractArrayExtent(i, 1);
		assertTrue( arrayExtent.length == 1);
		assertTrue( arrayExtent.array.length == 1);
		assertTrue( arrayExtent.array[0] == 123);
	}	
	
	@Test
	public void testArrayExtent_Sequences() {
		ArrayExtent arrayExtent = new ArrayExtent();
		VariableIntegerSequence seq = variablesToFor( new int[] {1, 1, 10} );
		arrayExtent.extractArrayExtent(seq, 1+(10-1)/1);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 1+1*(i) );
		}
		seq = variablesToFor( new int[] {5, 9} );
		arrayExtent.extractArrayExtent(seq, 1+(9-5)/1);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 5+1*(i) );
		}
		seq = variablesToFor( new int[] {1, 3, 10} );
		arrayExtent.extractArrayExtent(seq, 1+(10-1)/3);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 1+3*(i) );
		}
		
		//range 5:1: 
		seq = variablesToRange( new VariableInteger(5), new VariableInteger(1) );
		arrayExtent.extractArrayExtent(seq, 1+(10-5)/1);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 5+1*(i) );
		}
		//range 5:
		seq = variablesToRange( new VariableInteger(5), null );
		arrayExtent.extractArrayExtent(seq, 1+(10-5)/1);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 5+1*(i) );
		}
		//range :2:
		seq = variablesToRange( null, new VariableInteger(2) );
		arrayExtent.extractArrayExtent(seq, 1+(10-0)/2);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 0+2*(i) );
		}

		//explicit: 1, 3, 5, 7
		seq = variablesToExplicit( new int[] {1, 3, 5, 7});
		arrayExtent.extractArrayExtent(seq, 4);
		IntegerSequence.Explicit explicit = (IntegerSequence.Explicit) seq.sequence;
		List<VariableInteger> vars = explicit.getSequence();
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == vars.get(i).value );
		}
		
		// combined : 1, 1, 3, 5, 7
		seq = variablesToCombined( new Variable[] {new VariableInteger(1), variablesToExplicit( new int[] {1, 3, 5, 7})} );
		arrayExtent.extractArrayExtent(seq, 5);
		IntegerSequence.Combined combined = (IntegerSequence.Combined) seq.sequence;
		combined.initialize(4);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == combined.next() );
		}
	}
	
	//CodeExtents
	@Test
	public void testCodeExtents_Integer() {
		Extents extents = new Extents();
		VariableInteger r = VariableInteger.factory(1);
		boolean isSimple = extents.extractSimpleExtents(r, true, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 0);
		assertTrue( extents.col1 == 0);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 1);
		VariableInteger c = VariableInteger.factory(2);
		isSimple = extents.extractSimpleExtents(c, false, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 2);
		assertTrue( extents.col1 == 2);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 1);
		
		// Extents col1 is last col number, CodeExtents returns +1 last als numRows/numCols
		String[] lastRowCol = {Integer.toString(extents.row1)+"+1", Integer.toString(extents.col1)+"+1"};
		CodeExtents codeExtents = new CodeExtents( Arrays.asList( new Variable[] {c} ) );
		assertTrue( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());
		assertTrue( Integer.toString(extents.col0).equals(codeExtents.codeSimpleStartCol()) );
		assertTrue( lastRowCol[1].equals(codeExtents.codeSimpleEndCol(lastRowCol)) );
		
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {r, c} ) );
		assertFalse( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());
		assertTrue( Integer.toString(extents.row0).equals(codeExtents.codeSimpleStartRow()) );
		assertTrue( lastRowCol[0].equals(codeExtents.codeSimpleEndRow(lastRowCol)) );
		assertTrue( Integer.toString(extents.col0).equals(codeExtents.codeSimpleStartCol()) );
		assertTrue( lastRowCol[1].equals(codeExtents.codeSimpleEndCol(lastRowCol)) );
	}	
	
	@Test
	public void testCodeExtents_vsExtents() {
		Extents extents = new Extents();
		VariableIntegerSequence rseq = variablesToFor( new int[] {1, 1, 10} );
		boolean isSimple = extents.extractSimpleExtents(rseq, true, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 0);
		assertTrue( extents.col1 == 0);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 10);
		
		VariableIntegerSequence cseq = variablesToFor( new int[] {5, 1, 9} );
		isSimple = extents.extractSimpleExtents(cseq, false, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 5);
		assertTrue( extents.col1 == 9);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 10);
		
		// Extents col1 is last col number, CodeExtents returns +1 last als numRows/numCols
		String[] lastRowCol = {Integer.toString(extents.row1)+"+1", Integer.toString(extents.col1)+"+1"};
		CodeExtents codeExtents = new CodeExtents( Arrays.asList( new Variable[] {cseq} ) );
		assertTrue( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());
		assertTrue( Integer.toString(extents.col0).equals(codeExtents.codeSimpleStartCol()) );
		assertTrue( lastRowCol[1].equals(codeExtents.codeSimpleEndCol(lastRowCol)) );
		
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {rseq, cseq} ) );
		assertFalse( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());
		assertTrue( Integer.toString(extents.row0).equals(codeExtents.codeSimpleStartRow()) );
		assertTrue( lastRowCol[0].equals(codeExtents.codeSimpleEndRow(lastRowCol)) );
		assertTrue( Integer.toString(extents.col0).equals(codeExtents.codeSimpleStartCol()) );
		assertTrue( lastRowCol[1].equals(codeExtents.codeSimpleEndCol(lastRowCol)) );
		
		
		extents = new Extents();
		rseq = variablesToFor( new int[] {1, 10} );
		isSimple = extents.extractSimpleExtents(rseq, true, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 0);
		assertTrue( extents.col1 == 0);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 10);
		
		
		cseq = variablesToFor( new int[] {5, 9} );
		isSimple = extents.extractSimpleExtents(cseq, false, -1);
		assertTrue(isSimple);
		assertTrue( extents.col0 == 5);
		assertTrue( extents.col1 == 9);
		assertTrue( extents.row0 == 1);
		assertTrue( extents.row1 == 10);

		// Extents col1 is last col number, CodeExtents returns +1 last als numRows/numCols
		lastRowCol = new String[] {Integer.toString(extents.row1)+"+1", Integer.toString(extents.col1)+"+1"};
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {cseq} ) );
		assertTrue( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());
		assertTrue( Integer.toString(extents.col0).equals(codeExtents.codeSimpleStartCol()) );
		assertTrue( lastRowCol[1].equals(codeExtents.codeSimpleEndCol(lastRowCol)) );
		
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {rseq, cseq} ) );
		assertFalse( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());
		assertTrue( Integer.toString(extents.row0).equals(codeExtents.codeSimpleStartRow()) );
		assertTrue( lastRowCol[0].equals(codeExtents.codeSimpleEndRow(lastRowCol)) );
		assertTrue( Integer.toString(extents.col0).equals(codeExtents.codeSimpleStartCol()) );
		assertTrue( lastRowCol[1].equals(codeExtents.codeSimpleEndCol(lastRowCol)) );
	}
	
	@Test
	public void testCodeExtents_vsArrayExtents() {
		ArrayExtent arrayExtent = new ArrayExtent();
		VariableIntegerSequence seq = variablesToFor( new int[] {1, 1, 10} );
		arrayExtent.extractArrayExtent(seq, 1+(10-1)/1);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 1+1*(i) );
		}
		int first = arrayExtent.array[0];
		int last = arrayExtent.array[arrayExtent.length-1];
		String[] lastRowCol = new String[] {"", Integer.toString(last)+"+1"};
		CodeExtents codeExtents = new CodeExtents( Arrays.asList( new Variable[] {seq} ) );
		assertTrue( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());
		assertTrue( Integer.toString(first).equals(codeExtents.codeSimpleStartCol()) );
		assertTrue( lastRowCol[1].equals(codeExtents.codeSimpleEndCol(lastRowCol)) );
		
		seq = variablesToFor( new int[] {5, 9} );
		arrayExtent.extractArrayExtent(seq, 1+(9-5)/1);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 5+1*(i) );
		}
		first = arrayExtent.array[0];
		last = arrayExtent.array[arrayExtent.length-1];
		lastRowCol = new String[] {"", Integer.toString(last)+"+1"};
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {seq} ) );
		assertTrue( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());
		assertTrue( Integer.toString(first).equals(codeExtents.codeSimpleStartCol()) );
		assertTrue( lastRowCol[1].equals(codeExtents.codeSimpleEndCol(lastRowCol)) );

		seq = variablesToFor( new int[] {1, 3, 10} );
		arrayExtent.extractArrayExtent(seq, 1+(10-1)/3);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 1+3*(i) );
		}
		first = arrayExtent.array[0];
		last = arrayExtent.array[arrayExtent.length-1];
		lastRowCol = new String[] {"", Integer.toString(last)+"+1"};
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {seq} ) );
		assertTrue( codeExtents.is1D());
		assertFalse( codeExtents.isBlock());

		ComplexExtentInterpreter interpreter = new ComplexExtentInterpreter();
		
		int[] r = interpreter.interpret(codeExtents.codeComplexColIndices(lastRowCol));
		Arrays.equals(r, Arrays.copyOf(arrayExtent.array, arrayExtent.length));

		//range 5:1: 
		seq = variablesToRange( new VariableInteger(5), new VariableInteger(1) );
		arrayExtent.extractArrayExtent(seq, 1+(10-5)/1);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 5+1*(i) );
		}
		first = arrayExtent.array[0];
		last = arrayExtent.array[arrayExtent.length-1];
		lastRowCol = new String[] {"", Integer.toString(last)+"+1"};
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {seq} ) );
		assertTrue( codeExtents.is1D());
		assertFalse( codeExtents.isBlock());

		r = interpreter.interpret(codeExtents.codeComplexColIndices(lastRowCol));
		Arrays.equals(r, Arrays.copyOf(arrayExtent.array, arrayExtent.length));

		//range 5:
		seq = variablesToRange( new VariableInteger(5), null );
		arrayExtent.extractArrayExtent(seq, 1+(10-5)/1);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 5+1*(i) );
		}
		first = arrayExtent.array[0];
		last = arrayExtent.array[arrayExtent.length-1];
		lastRowCol = new String[] {"", Integer.toString(last)+"+1"};
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {seq} ) );
		assertTrue( codeExtents.is1D());
		assertTrue( codeExtents.isBlock());

		r = interpreter.interpret(codeExtents.codeComplexColIndices(lastRowCol));
		Arrays.equals(r, Arrays.copyOf(arrayExtent.array, arrayExtent.length));

		//range :2:
		seq = variablesToRange( null, new VariableInteger(2) );
		arrayExtent.extractArrayExtent(seq, 1+(10-0)/2);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == 0+2*(i) );
		}
		first = arrayExtent.array[0];
		last = arrayExtent.array[arrayExtent.length-1];
		lastRowCol = new String[] {"", Integer.toString(last)+"+1"};
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {seq} ) );
		assertTrue( codeExtents.is1D());
		assertFalse( codeExtents.isBlock());

		r = interpreter.interpret(codeExtents.codeComplexColIndices(lastRowCol));
		Arrays.equals(r, Arrays.copyOf(arrayExtent.array, arrayExtent.length));

		//explicit: 1, 3, 5, 7
		seq = variablesToExplicit( new int[] {1, 3, 5, 7});
		arrayExtent.extractArrayExtent(seq, 4);
		IntegerSequence.Explicit explicit = (IntegerSequence.Explicit) seq.sequence;
		List<VariableInteger> vars = explicit.getSequence();
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == vars.get(i).value );
		}
		first = arrayExtent.array[0];
		last = arrayExtent.array[arrayExtent.length-1];
		lastRowCol = new String[] {"", Integer.toString(last)+"+1"};
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {seq} ) );
		assertTrue( codeExtents.is1D());
		assertFalse( codeExtents.isBlock());

		r = interpreter.interpret(codeExtents.codeComplexColIndices(lastRowCol));
		Arrays.equals(r, Arrays.copyOf(arrayExtent.array, arrayExtent.length));

		
		// combined : 1, 1, 3, 5, 7
		seq = variablesToCombined( new Variable[] {VariableInteger.factory(1), variablesToExplicit( new int[] {1, 3, 5, 7})} );
		arrayExtent.extractArrayExtent(seq, 5);
		IntegerSequence.Combined combined = (IntegerSequence.Combined) seq.sequence;
		combined.initialize(4);
		for (int i = 0; i < arrayExtent.length; i++) {
			assertTrue( arrayExtent.array[i] == combined.next() );
		}
		first = arrayExtent.array[0];
		last = arrayExtent.array[arrayExtent.length-1];
		lastRowCol = new String[] {"", Integer.toString(last)+"+1"};
		codeExtents = new CodeExtents( Arrays.asList( new Variable[] {seq} ) );
		assertTrue( codeExtents.is1D());
		assertFalse( codeExtents.isBlock());

		r = interpreter.interpret(codeExtents.codeComplexColIndices(lastRowCol));
		Arrays.equals(r, Arrays.copyOf(arrayExtent.array, arrayExtent.length));

	}
	
	/** Interpret Stream java code to generate represented int[]
	 *    For Java 9, may be replaced with REPL invocation.
	 */
	private static class ComplexExtentInterpreter {
		public ComplexExtentInterpreter() {}
		
		final Pattern intArrayStartPattern = Pattern.compile("^new int\\[\\] \\{");
		final Pattern intArrayEndsPattern = Pattern.compile("}");
		final Pattern intStreamStartPattern = Pattern.compile("^IntStream\\.iterate\\(");
		final Pattern intStreamContentPattern = Pattern.compile("(\\d+), n -> n \\+ (\\d+)\\)\\.limit\\(1\\+\\((\\d+) - (\\d+)\\) / (\\d+)");
		final Pattern intStreamEndsPattern = Pattern.compile("\\)\\.toArray\\(\\)");
		final Pattern streamOfStartPattern = Pattern.compile("^Stream.of\\(");
		final Pattern streamOfEndsPattern = Pattern.compile("\\)\\.flatMapToInt\\(IntStream\\:\\:of\\)\\.toArray\\(\\)");
		
		List<Integer> elements = new ArrayList<>();
		
		private void addElements( String csvInts ) {
			String[] els = csvInts.split(",");
			for (String el : els) {
				elements.add(Integer.parseInt(el));
			}
		}
		
		private void interpretParts( String sequence ) {
			while (! sequence.isEmpty()) {
				if (sequence.startsWith(",")) {
					sequence = sequence.substring(1);
				}
				Matcher starts = intArrayStartPattern.matcher(sequence);
				if (starts.find()) {
					Matcher finish = intArrayEndsPattern.matcher(sequence);
					if (finish.find()) {
						addElements( sequence.substring(starts.end(), finish.start()));
						sequence = sequence.substring(finish.end());
						continue;
					}
				}
				starts = intStreamStartPattern.matcher(sequence);
				if (starts.find()) {
					Matcher finish = intStreamEndsPattern.matcher(sequence);
					if (finish.find()) {
						String middle = sequence.substring(starts.end(), finish.start());
						Matcher content = intStreamContentPattern.matcher( middle );
						if (content.find()) {
							int start = Integer.parseInt(content.group(1));
							int step = Integer.parseInt(content.group(5));
							int end = Integer.parseInt(content.group(3));
							for (int i = start; i < end; i += step) {
								elements.add(i);
							}
						}
						sequence = sequence.substring(finish.end());
						continue;
					}
				}
				if (sequence.startsWith(",")) {
					sequence = sequence.substring(1);
				} else if (! sequence.isEmpty()) {
					System.err.println(sequence);
				}
			}			
		}
		
		public int[] interpret( String sequence ) {
			elements = new ArrayList<>();
			while (! sequence.isEmpty()) {
				Matcher starts = intArrayStartPattern.matcher(sequence);
				if (starts.find()) {
					Matcher finish = intArrayEndsPattern.matcher(sequence);
					if (finish.find()) {
						addElements( sequence.substring(starts.end(), finish.start()));
						sequence = sequence.substring(finish.end());
						continue;
					}
				}
				starts = intStreamStartPattern.matcher(sequence);
				if (starts.find()) {
					Matcher finish = intStreamEndsPattern.matcher(sequence);
					if (finish.find()) {
						String middle = sequence.substring(starts.end(), finish.start());
						Matcher content = intStreamContentPattern.matcher( middle );
						if (content.find()) {
//							for (int g = 0; g <= content.groupCount(); g++) {
//								System.out.printf("%d: %s\n", g, content.group(g));
//							}
							int start = Integer.parseInt(content.group(1));
							int step = Integer.parseInt(content.group(5));
							int end = Integer.parseInt(content.group(3));
							for (int i = start; i < end; i += step) {
								elements.add(i);
							}
						}
						sequence = sequence.substring(finish.end());
						continue;
					}
				}
				starts = streamOfStartPattern.matcher(sequence);
				if (starts.find()) {
					Matcher finish = streamOfEndsPattern.matcher(sequence);
					if (finish.find()) {
						String middle = sequence.substring(starts.end(), finish.start());
						interpretParts( middle );
 						sequence = sequence.substring(finish.end());
						continue;
					}
				}
				System.err.println(sequence);
			}
			int[] result = new int[elements.size()];
			for (int i = 0; i < result.length; i++){
				result[i] = elements.get(i);
			}
			return result;
		}
	}
	
	@Test
	public void testComplexExtentInterpreter() {
		ComplexExtentInterpreter interpreter = new ComplexExtentInterpreter();
		
		int[] r = interpreter.interpret("new int[] {1,2,3}");
		assertTrue( Arrays.equals(r, new int[] {1,2,3} ));
		r = interpreter.interpret("IntStream.iterate(1, n -> n + 3).limit(1+(10 - 1) / 3).toArray()");
		assertTrue( Arrays.equals(r, new int[] {1, 4, 7} ));
		r = interpreter.interpret("Stream.of(new int[] {1,2,3},IntStream.iterate(1, n -> n + 3).limit(1+(10 - 1) / 3).toArray()).flatMapToInt(IntStream::of).toArray()");
		assertTrue( Arrays.equals(r, new int[] {1, 2, 3, 1, 4, 7} ));
	}

}
