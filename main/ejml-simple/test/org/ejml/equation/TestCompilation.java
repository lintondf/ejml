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

import static org.junit.Assert.assertEquals;
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

import com.google.googlejavaformat.java.Formatter;

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
		               ">i, j\n" +
		               ">x , y\n" +
		               "><m\n" +
		               "m = 2*i + 4.5*x * m\n" +
		               "j = 5*i\n" +
		               "y = 1.2 * x\n" +
		               "rng(i)\n" +
		               "x = rand(i,i)\n" +
		               "x = randn(i,i)\n" +
		               "\n" + 
		               "checkvoid\n" +
		               ">i\n" +
		               "\n" + 
		               "\n" +
		               "i = i + 1\n" +
		               "\n" +
				       "\n";
		
		String expected = "public class Test {\n" + 
				"  public DMatrixRMaj check(int i, double x, DMatrixRMaj m) {\n" + 
				"    int j;\n" + 
				"    double y;\n" + 
				"    // m = 2*i + 4.5*x * m\n" + 
				"    DMatrixRMaj tm3 = new DMatrixRMaj(m.numRows, m.numCols);\n" + 
				"    CommonOps_DDRM.scale(4.5 * x, m, tm3);\n" + 
				"    m.reshape(tm3.numRows, tm3.numCols);\n" + 
				"    CommonOps_DDRM.add(tm3, 2 * i, m); // j = 5*i\n" + 
				"    j = 5 * i; // y = 1.2 * x\n" + 
				"    y = 1.2 * x; // rng(i)\n" + 
				"    Random rand = new Random();\n" + 
				"    rand.setSeed(i); // x = rand(i,i)\n" + 
				"    DMatrixRMaj tm1 = new DMatrixRMaj(i, i);\n" + 
				"    Random rand = new Random();\n" + 
				"    RandomMatrices_DDRM.fillUniform(tm1, 0, 1, rand);\n" + 
				"    x = tm1.unsafe_get(0, 0); // x = randn(i,i)\n" + 
				"    tm1.reshape(i, i);\n" + 
				"    Random rand = new Random();\n" + 
				"    RandomMatrices_DDRM.fillGaussian(tm1, 0, 1, rand);\n" + 
				"    x = tm1.unsafe_get(0, 0);\n" + 
				"    return m;\n" + 
				"  }\n" + 
				"\n" + 
				"  public void checkvoid(int i) {\n" + 
				"    // i = i + 1\n" + 
				"    i = i + 1;\n" + 
				"  }\n" + 
				"}\n";
		CodeEquationMain.interactive( new BufferedReader( new StringReader(input)), new PrintWriter( results ) );
		try {
			List<String> code = Files.readAllLines(out.toPath());
			code.forEach(System.out::println);
			String[] expectedLines = expected.split("\n");
			assertTrue( code.size()-1 == expectedLines.length); // extra \n expected
			for (int i = 0; i < expectedLines.length; i++) {
				assertEquals( expectedLines[i], code.get(i) );
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testComplexPython() {
		List<String> integers = new ArrayList<>();
		List<String> doubles = new ArrayList<>();
		List<String> matrices = new ArrayList<>();
		
		matrices.add("><M");
		integers.add(">n");
		doubles.add(">tau");
		
		List<String> equations = new ArrayList<>();
		equations.add("M(2,4) = 588*(25*n**8-100*n**7+250*n**6-700*n**5+1585*n**4-280*n**3-540*n**2-600*n+288)/(n*tau**2*(n**10+11*n**9-330*n**7-627*n**6+3003*n**5+7370*n**4-9020*n**3-24024*n**2+6336*n+17280))");		
		StringBuilder block = new StringBuilder();
		CodeEquationMain main = new CodeEquationMain( block, "code" );
		main.startMethod("test");
		main.declareIntegerVariables(integers);
		main.declareDoubleVariables(doubles);
		main.declareMatrixVariables(matrices);
		main.finishMethod(equations);
		main.finishClass();

		System.out.println(block.toString());
		String expected = "public class code {\n" + 
				"public DMatrixRMaj test(int n, double tau, DMatrixRMaj M) {\n" + 
				"// M(2,4) = 588*(25*n**8-100*n**7+250*n**6-700*n**5+1585*n**4-280*n**3-540*n**2-600*n+288)/(n*tau**2*(n**10+11*n**9-330*n**7-627*n**6+3003*n**5+7370*n**4-9020*n**3-24024*n**2+6336*n+17280))\n" + 
				"CommonOps_DDRM.insert( new DMatrixRMaj(1, 1, 588 * 25 * Math.pow(n, 8) - 100 * Math.pow(n, 7) + 250 * Math.pow(n, 6) - 700 * Math.pow(n, 5) + 1585 * Math.pow(n, 4) - 280 * Math.pow(n, 3) - 540 * Math.pow(n, 2) - 600 * n + 288 / n * Math.pow(tau, 2) * Math.pow(n, 10) + 11 * Math.pow(n, 9) - 330 * Math.pow(n, 7) - 627 * Math.pow(n, 6) + 3003 * Math.pow(n, 5) + 7370 * Math.pow(n, 4) - 9020 * Math.pow(n, 3) - 24024 * Math.pow(n, 2) + 6336 * n + 17280), M, 2, 4 );return M;}\n" + 
				"}\n";
		assertEquals( block.toString(), expected);
//		try {
//			Formatter formatter = new Formatter();
//			String pretty = formatter.formatSource(block.toString());
//			System.out.println(pretty);
//		} catch (Exception x) {}
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
    	String actual = new CaptureSystemOut() {
			@Override
			public boolean run() {
				GenerateTestCoded.main( new String[] {} );
				return true;
			}
    	}.capture();
		
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
