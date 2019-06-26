package org.ejml.equation;

import java.util.Random;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.tools4j.meanvar.MeanVarianceSampler;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

public class TestPerformance {
	
	
	
	Random rand = new Random(234);
	Equation eq;
	Sequence updateK, updateP;
	IEmitOperation coder;
	ManagerTempVariables tempManager;
	
	DMatrixRMaj K,P, H, R;
	
	public TestPerformance() {
		final int N = 6;
		final int M = 3;
		P = RandomMatrices_DDRM.symmetricPosDef( N, rand );
		R = RandomMatrices_DDRM.symmetricPosDef( M, rand );
		H = RandomMatrices_DDRM.rectangle(M, N, rand);
		K = new DMatrixRMaj(N, N);
		eq = new Equation();
		coder = new EmitJavaOperation();
		tempManager = eq.getTemporariesManager();
		eq.alias(K, "K", P, "P", H, "H", R, "R");
		updateK = eq.compile("K = P*H'*inv( H*P*H' + R )");
		updateP = eq.compile("P = P-K*(H*P)");
		
	}
	 
	public void optimize() {
		CompileCodeOperations compiler = new CompileCodeOperations(coder, updateK, tempManager );
		compiler.optimize();
		String s = compiler.toString();
		compiler = new CompileCodeOperations(coder, updateP, tempManager );
		compiler.optimize();
		s = compiler.toString();
	}
	
	public void perform() {
		updateK.perform();
		updateP.perform();
	}

	public static void oldmain(String[] args) {
		TestPerformance test = new TestPerformance();
		test.perform();
		DMatrixRMaj Pcheck = new DMatrixRMaj( new double[][] {
			{ 1.10796861,  .108237222,  .054670884, -.103915554, -.000302041, -.020132301},
			{  .108237222,  .866308058, -.168636667, -.034412212,  .059299924, -.088842881},
			{ .054670884, -.168636667,  .757391788, -.057331986, -.030395786, -.2025452  },
			{ -.103915554, -.034412212, -.057331986,  .847044247, -.257494929, -.168183085},
			{ -.000302041,  .059299924, -.030395786, -.257494929,  .732316361, -.218706797},
			{ -.020132301, -.088842881, -.2025452,   -.168183085, -.218706797,  .695311382}
		} );
		assert( MatrixFeatures_DDRM.isEquals(test.P, Pcheck));
		test = new TestPerformance();
		test.optimize();
		test.perform();
		assert( MatrixFeatures_DDRM.isEquals(test.P, Pcheck));
		final int numRuns = 1000;
		final int numSamples = 1000;
		final MeanVarianceSampler overall = new MeanVarianceSampler();
		for (int j = 0; j < 1000; j++) {
			final MeanVarianceSampler sampler = new MeanVarianceSampler();
			
			test = new TestPerformance();
			test.optimize();
			for (int iSample = 0; iSample < numSamples; iSample++) {
				long start = System.nanoTime();
				for (int i = 0; i < numRuns; i++) {
					test.perform();
				}
				long finish = System.nanoTime();
				sampler.add(  (double)(finish - start) / (double) numRuns );
			}
			System.out.printf("%10.6g ns run time\n", sampler.getMean() );
			overall.add(sampler.getMean());
		}
		System.out.println( overall.getMean() + " " + overall.getStdDevUnbiased());
	}
	
	public static void main(String[] args) {
		Equation eq = new Equation();
		Double a=1.,b=2.,c=3.,d=4.,g=5.,f=6.;
		eq.alias(a,"a", b,"b", c,"c", d,"d", g,"g", f,"f");
		Sequence s = eq.compile("out=-1+2*(3+4)+3/5-6");
//		Sequence s = eq.compile("out=-a+b*(c+d)+c/g-sin(f)");
		s.print();
		ManagerTempVariables tempManager = new ManagerTempVariables();
		EmitJavaOperation coder = new EmitJavaOperation();
		CompileCodeOperations compiler = new CompileCodeOperations(coder, s, tempManager );
		compiler.optimize();
		System.out.println(compiler.toString());
		
		
		Integer n = 10;
		Double tau = 0.1;
		DMatrixRMaj M = new DMatrixRMaj(5,5);
		M.zero();
		eq.alias(n, "n", tau, "tau", M, "M");
		s = eq.compile("M(2,4) = 588*(25*n**8-100*n**7+250*n**6-700*n**5+1585*n**4-280*n**3-540*n**2-600*n+288)/(n*tau**2*(n**10+11*n**9-330*n**7-627*n**6+3003*n**5+7370*n**4-9020*n**3-24024*n**2+6336*n+17280))");
		s.print();
		compiler = new CompileCodeOperations(coder, s, tempManager );
		compiler.optimize();
		System.out.println(compiler.toString());
		
		StringBuilder body = new StringBuilder();
		for (Info info : s.getInfos()) {
    		coder.emitOperation( body, info );
    	}
		System.out.println(body.toString());
		CommonOps_DDRM.insert( new DMatrixRMaj((2+1 - 2), (4+1 - 4), ((588 * (((((((((25 * (Math.pow(n, 8))) - (100 * (Math.pow(n, 7)))) + (250 * (Math.pow(n, 6)))) - (700 * (Math.pow(n, 5)))) + (1585 * (Math.pow(n, 4)))) - (280 * (Math.pow(n, 3)))) - (540 * (Math.pow(n, 2)))) - (600 * n)) + 288)) / ((n * (Math.pow(tau, 2))) * ((((((((((Math.pow(n, 10)) + (11 * (Math.pow(n, 9)))) - (330 * (Math.pow(n, 7)))) - (627 * (Math.pow(n, 6)))) + (3003 * (Math.pow(n, 5)))) + (7370 * (Math.pow(n, 4)))) - (9020 * (Math.pow(n, 3)))) - (24024 * (Math.pow(n, 2)))) + (6336 * n)) + 17280)))), M, 2, 4 );
		M.print();
	}
	
	public static void mainx(String[] args) {

        System.out.println(removeParenthesis(new StringBuilder("((aaa*b)+c*(e+f))")));
        System.out.println(removeParenthesis(new StringBuilder("(a+(b*c))*(d*(f*j))")));
        System.out.println(removeParenthesis(new StringBuilder("(a+(b))")));
        System.out.println(removeParenthesis(new StringBuilder("((a+b)+((c+d)))")));
    }

    public static String removeParenthesis(StringBuilder sb) {
        if (removeParenthesis(null, sb, 0)) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    public static boolean removeParenthesis(Integer leftPrecedence, StringBuilder sb, int start) {
        Integer lastPrecedence = null;
        Integer minPrecedence = null;
        while (start < sb.length()) {
            if (sb.charAt(start) == '(') {
                if (removeParenthesis(lastPrecedence, sb, start + 1)) {
                    sb.deleteCharAt(start);
                } else {
                    int count = 0;
                    do {
                        if (sb.charAt(start) == '(') {
                            count++;
                        } else if (sb.charAt(start) == ')') {
                            count--;
                        }
                        start++;
                    } while (start < sb.length() && count != 0);
                    continue;
                }
            } else if (sb.charAt(start) == ')') {
                if(minPrecedence == null) {
                    sb.deleteCharAt(start);
                    return true;
                }
                Integer rightPrecedence = start == sb.length() - 1 || sb.charAt(start + 1) == ')' ? null
                        : getPrecedence(sb.charAt(start + 1));
                if ((leftPrecedence != null && minPrecedence < leftPrecedence)
                        || (rightPrecedence != null && minPrecedence < rightPrecedence)) {
                    return false;
                } else {
                    sb.deleteCharAt(start);
                    return true;
                }
            } else if (sb.charAt(start) < 'a' || sb.charAt(start) > 'z') {
                lastPrecedence = getPrecedence(sb.charAt(start));
                if (minPrecedence == null || minPrecedence > lastPrecedence) {
                    minPrecedence = lastPrecedence;
                }
            }
            start++;
        }
        return false;
    }

    public static int getPrecedence(char operator) {
        switch (operator) {
        case '+':
        case '-':
            return 1;
        case '*':
        case '/':
            return 2;
        }
        throw new IllegalArgumentException(">>" + operator + "<<");
    }	

}
