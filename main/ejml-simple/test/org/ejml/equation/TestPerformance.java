package org.ejml.equation;

import java.util.Random;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.tools4j.meanvar.MeanVarianceSampler;

public class TestPerformance {
	
//	public class KalmanFilterEquation {
//
//	    // system state estimate
//	    private DMatrixRMaj x,P;
//
//	    private Equation eq;
//
//	    // Storage for precompiled code for predict and update
//	    Sequence predictX,predictP;
//	    Sequence updateY,updateK,updateX,updateP;
//
//	    public void configure(DMatrixRMaj F, DMatrixRMaj Q, DMatrixRMaj H) {
//	        int dimenX = F.numCols;
//
//	        x = new DMatrixRMaj(dimenX,1);
//	        P = new DMatrixRMaj(dimenX,dimenX);
//
//	        eq = new Equation();
//
//	        // Provide aliases between the symbolic variables and matrices we normally interact with
//	        // The names do not have to be the same.
//	        eq.alias(x,"x",P,"P",Q,"Q",F,"F",H,"H");
//
//	        // Dummy matrix place holder to avoid compiler errors.  Will be replaced later on
//	        eq.alias(new DMatrixRMaj(1,1),"z");
//	        eq.alias(new DMatrixRMaj(1,1),"R");
//
//	        // Pre-compile so that it doesn't have to compile it each time it's invoked.  More cumbersome
//	        // but for small matrices the overhead is significant
//	        predictX = eq.compile("x = F*x");
//	        predictP = eq.compile("P = F*P*F' + Q");
//
//	        updateY = eq.compile("y = z - H*x");
//	        updateK = eq.compile("K = P*H'*inv( H*P*H' + R )");
//	        updateX = eq.compile("x = x + K*y");
//	        updateP = eq.compile("P = P-K*(H*P)");
//	    }
//
//	    public void setState(DMatrixRMaj x, DMatrixRMaj P) {
//	        this.x.set(x);
//	        this.P.set(P);
//	    }
//
//	    public void predict() {
//	        predictX.perform();
//	        predictP.perform();
//	    }
//
//	    public void update(DMatrixRMaj z, DMatrixRMaj R) {
//
//	        // Alias will overwrite the reference to the previous matrices with the same name
//	        eq.alias(z,"z"); eq.alias(R,"R");
//
//	        updateY.perform();
//	        updateK.perform();
//	        updateX.perform();
//	        updateP.perform();
//	    }
//
//	    public DMatrixRMaj getState() {
//	        return x;
//	    }
//
//	    public DMatrixRMaj getCovariance() {
//	        return P;
//	    }
//	}	
	
	
	Random rand = new Random(234);
	Equation eq;
	Sequence updateK, updateP;
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
		tempManager = eq.getTemporariesManager();
		eq.alias(K, "K", P, "P", H, "H", R, "R");
		updateK = eq.compile("K = P*H'*inv( H*P*H' + R )");
		updateP = eq.compile("P = P-K*(H*P)");
		
	}
	
	public void optimize() {
		String s = updateK.optimize(tempManager);
//		System.out.println(s);
		s = updateP.optimize(tempManager);
//		System.out.println(s);
	}
	
	public void perform() {
		updateK.perform();
		updateP.perform();
	}

	public static void main(String[] args) {
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

}
