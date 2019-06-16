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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Tests for command line interface to compile EJML equations to procedural code
 * 
 * @author D. F. Linton, Blue Lightning Development, LLC 2019.
 */
public class TestCompilation {
	
	@Test
	public void testSimulatedConsole() {
		File out = new File("Test.java");
		try { out.delete(); } catch (Exception x) {}
		out.deleteOnExit();
		ByteArrayOutputStream results = new ByteArrayOutputStream();
		String input = "Test\n" +
		               "check\n" +
		               ">i\n" +
		               ">x\n" +
		               "><m\n" +
		               "m = i + x * m\n" +
		               "rng(i)\n" +
		               "x = rand(i,i)\n" +
		               "x = randn(i,i)\n" +
				       "\n";
		CodeEquationMain.interactive( new BufferedReader( new StringReader(input)), new PrintWriter( results ) );
		System.out.println( results.toString() );
		try {
			List<String> code = Files.readAllLines(out.toPath());
			code.forEach(System.out::println);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testLowerLevel() {
		List<String> integers = new ArrayList<>();
		List<String> doubles = new ArrayList<>();
		List<String> matrices = new ArrayList<>();
		
		matrices.add("<K");
		matrices.add(">P");
		matrices.add(">H");
		matrices.add(">R");
		
		List<String> equations = new ArrayList<>();
		equations.add("K = P*H'*inv( H*P*H' + R )");
		
		StringBuilder block = new StringBuilder();
		CodeEquationMain main = new CodeEquationMain( block, "code" );
		main.startMethod("test");
		main.declareIntegerVariables(integers);
		main.declareDoubleVariables(doubles);
		main.declareMatrixVariables(matrices);
		main.finishMethod(equations);
		main.finishClass();

//		System.out.println(block.toString());
		String expected = "public class code {\n" + 
				"public DMatrixRMaj test(DMatrixRMaj P, DMatrixRMaj H, DMatrixRMaj R) {\n" + 
				"// K = P*H'*inv( H*P*H' + R )\n" + 
				"DMatrixRMaj tm1 = new DMatrixRMaj( H.numCols, H.numRows );\n" + 
				"CommonOps_DDRM.transpose( H, tm1 );\n" + 
				"DMatrixRMaj tm2 = new DMatrixRMaj( H.numRows, P.numCols );\n" + 
				"CommonOps_DDRM.mult( H, P, tm2 );\n" + 
				"DMatrixRMaj tm3 = new DMatrixRMaj(H.numRows,H.numRows);\n" + 
				"CommonOps_DDRM.mult( tm2, tm1, tm3 );\n" + 
				"tm1.reshape( tm3.numRows, tm3.numCols );\n" + 
				"CommonOps_DDRM.add( tm3, R, tm1 );\n" + 
				"DMatrixRMaj tm5 = new DMatrixRMaj(H.numCols,H.numRows);\n" + 
				"boolean ok = CommonOps_DDRM.invert(tm1, tm5);\n" + 
				"//TODO check ok\n" + 
				"tm1.reshape( H.numCols, H.numRows );\n" + 
				"CommonOps_DDRM.transpose( H, tm1 );\n" + 
				"DMatrixRMaj tm7 = new DMatrixRMaj( P.numRows,H.numRows);\n" + 
				"CommonOps_DDRM.mult( P, tm1, tm7 );\n" + 
				"DMatrixRMaj K = new DMatrixRMaj(P.numRows,H.numRows);\n" + 
				"CommonOps_DDRM.mult( tm7, tm5, K );return K;}\n" + 
				"}\n";
		assertTrue( expected.equals(block.toString()));
	}

	/**
	 * Test to insure that the regenerated TestCoded.java matches the repository.
	 * 
	 */
	@Test
	public void testTestGeneration() {
		GenerateEquationCoders.main( new String[] {} );
		Path path = Paths.get("test/org/ejml/equation");
		try {
			Path in = path.resolve("TestCoded.java");
			Path out = Paths.get("TestCoded_output.java");
			List<String> file1 = Files.readAllLines(in);
			List<String> file2 = Files.readAllLines(out);
			/*
			 * If you have changed TestOperation or TestEquation.java this is expected to fail.
			 * Copy TestCoded_output.java from the top-level ejml-simple directory to the proper
			 * directory in the test source tree.  Otherwise, the compilation process is in error.
			 */
			assertTrue( file1.size() == file2.size());
			for (int i = 0; i < file1.size(); i++) {
				assertTrue( file1.get(i).equals(file2.get(i)));
			}
		} catch (Exception x) {
			fail(x.getMessage());
		}
	}
}
