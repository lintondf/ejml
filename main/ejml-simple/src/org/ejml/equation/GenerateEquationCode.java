/**
 * 
 */
package org.ejml.equation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ejml.equation.CompileCodeOperations.Usage;

/**
 * @author lintondf
 *
 */
public class GenerateEquationCode {
	
	Equation eq; 
	IEmitOperation coder;
	Set<String> parameters;
	List<String> matrices; 
	TreeSet<String> declaredTemps;
	StringBuilder header;
	List<String> code;
	
	public GenerateEquationCode(Equation eq, IEmitOperation coder, Set<String> parameters, List<String> matrices, TreeSet<String> declaredTemps) {
		this.eq = eq;
		this.coder = coder;
		this.parameters = parameters;
		this.matrices = matrices;
		this.declaredTemps = declaredTemps;
		this.header = new StringBuilder();
		this.code = new ArrayList<>();
	}

	final static Pattern reshapePattern = Pattern.compile("(\\w+)\\.reshape\\((.+),(.+)\\)");
	final static Pattern reshapeReferencePattern = Pattern.compile("(\\w+)\\.num(\\w+)");
	
	private static class ReshapeSources {
		public ReshapeSources( String target) {
			this.target = target;
		}
		public String target;
		public String rowVar;
		public String rowWhich;
		public String colVar;
		public String colWhich;
		
//		public String toString() {
//			return String.format("%s %s.%s, %s.%s", target, rowVar, rowWhich, colVar, colWhich);
//		}
	}
	
	private HashMap<String, ReshapeSources> reshapeSourceMap = new HashMap<>();
	
	private String generateReshape( Matcher reshapeMatcher ) {
		String var = reshapeMatcher.group(1);
		String rows = reshapeMatcher.group(2);
		String cols = reshapeMatcher.group(3);
		
		ReshapeSources reshapeSources = new ReshapeSources(var);
		Matcher sourceMatcher = reshapeReferencePattern.matcher(rows);
		if (sourceMatcher.find()) {
			reshapeSources.rowVar = sourceMatcher.group(1);
			reshapeSources.rowWhich = "num" + sourceMatcher.group(2);
		}
		sourceMatcher = reshapeReferencePattern.matcher(cols);
		if (sourceMatcher.find()) {
			reshapeSources.colVar = sourceMatcher.group(1);
			reshapeSources.colWhich = "num" + sourceMatcher.group(2);
		}
		if (reshapeSources.rowVar != null && reshapeSources.colVar != null) {
			ReshapeSources prior = reshapeSourceMap.get(reshapeSources.rowVar);
			if (prior != null) {
				reshapeSources.rowVar = prior.rowVar;
				reshapeSources.rowWhich = prior.rowWhich;
				rows = String.format("%s.%s", reshapeSources.rowVar, reshapeSources.rowWhich );
			}
			prior = reshapeSourceMap.get(reshapeSources.colVar);
			if (prior != null) {
				reshapeSources.colVar = prior.colVar;
				reshapeSources.colWhich = prior.colWhich;
				cols = String.format("%s.%s", reshapeSources.colVar, reshapeSources.colWhich );
			}
			reshapeSourceMap.put(var, reshapeSources);
		}
		String decl = String.format("DMatrixRMaj %s = new DMatrixRMaj(%s,%s);", var, rows, cols );
		return decl;
	}
	
	public boolean generate(String equationText ) {
		return generate(equationText, true);
	}
	
	CompileCodeOperations generator;
	Exception lastError;
	
	/**
	 * @return the lastError
	 */
	public Exception getLastError() {
		return lastError;
	}

	public boolean generate(String equationText, boolean releaseTemps ) {
		StringBuilder body = new StringBuilder();
		body.append(String.format("//  %s\n",  equationText));
		Sequence sequence = null;
		lastError = null;
		try {
			sequence = eq.compile(equationText, true, false, releaseTemps);
		} catch (Exception x) {
			lastError = x;
			return false;
		}
		generator = new CompileCodeOperations(coder, sequence, eq.getTemporariesManager());
		generator.optimize();
		for (Info info : sequence.getInfos()) {
    		coder.emitOperation( body, info );
    	}	

		List<String> codeLines = Arrays.asList(body.toString().split("\n"));

		for (Usage usage : generator.integerUsages) {
			Variable variable = usage.variable;
			if (! declaredTemps.contains(variable.getOperand())) {
				declaredTemps.add(variable.getOperand() );
				if (! variable.getName().endsWith("}")) {
					StringBuilder decl = new StringBuilder();
					coder.declare( decl, "", variable );
					for (int i = 0; i < codeLines.size(); i++) {
						if (codeLines.get(i).startsWith(variable.getName() + " = ")) {
							decl.delete(decl.length()-2, decl.length());
							String line = codeLines.get(i).replace(variable.getName(), decl.toString());
							codeLines.set(i, line);
							decl = new StringBuilder();
							break;
						}
					}
					if (decl.length() > 0) {
						header.append(decl);
					}
				}
			}
		}
		for (Usage usage : generator.doubleUsages) {
			Variable variable = usage.variable;
			if (! declaredTemps.contains(variable.getOperand())) {
				declaredTemps.add(variable.getOperand() );
				if (! variable.getName().endsWith("}")) {
					StringBuilder decl = new StringBuilder();
					coder.declare( decl, "", variable );
					for (int i = 0; i < codeLines.size(); i++) {
						if (codeLines.get(i).startsWith(variable.getName() + " = ")) {
							decl.delete(decl.length()-2, decl.length());
							String line = codeLines.get(i).replace(variable.getName(), decl.toString());
							codeLines.set(i, line);
							decl = new StringBuilder();
							break;
						}
					}
					if (decl.length() > 0) {
						header.append(decl);
					}
				}
			}
		}

		// replace first reshape with declaration of matrix variables and temporaries
		for (Usage usage : generator.matrixUsages) {
			Variable variable = usage.variable;
			if (! declaredTemps.contains(variable.getOperand())) {
				declaredTemps.add(variable.getOperand() );
				//GenerateEquationCoders.declareTemporary( header, "", variable );
				for (int i = 0; i < codeLines.size(); i++) {
					Matcher matcher = reshapePattern.matcher( codeLines.get(i) );
					if (matcher.find()) {
						if (matcher.group(1).equals(variable.getName())) {
							//System.out.printf("%s in %s\n", variable.getName(), matcher.group(0));
							String decl = generateReshape( matcher );
							codeLines.set(i, decl);
							break;
						}
					}
				}
			}
		}
		if (matrices != null) {
			for (String v : matrices ) {
				if (! parameters.contains(v)) {
					for (int i = 0; i < codeLines.size(); i++) {
						Matcher matcher = reshapePattern.matcher( codeLines.get(i) );
						if (matcher.find()) {
							if (matcher.group(1).equals(v)) {
								//System.out.printf("%s in %s [%s, %s]\n", v, matcher.group(0), matcher.group(2), matcher.group(3));
								String decl = generateReshape( matcher );
								codeLines.set(i, decl);
								break;
							}
						}
					}
				}
			}
		}
		if (codeLines.size() == 2) { // if only comments and only line of source; remove comment
			code.add( codeLines.get(1));
		} else {
			code.addAll( codeLines );
		}
		if (releaseTemps)
			generator.releaseTemporaries(eq);
		return true;
	}

	/**
	 * @return the header
	 */
	public StringBuilder getHeader() {
		return header;
	}

	/**
	 * @return the code
	 */
	public List<String> getCode() {
		return code;
	}

	public String toString() {
		return generator.toString();
	}
}
