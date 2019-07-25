/**
 * 
 */
package org.ejml.equation;

import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author lintondf
 *
 */
public class Simplifier {
	
	public interface Rewriter {
		public String rewrite( String expr );
	}
	
	ArrayList<Rewriter> rewriters = new ArrayList<>();

	final String variable = "\\b[_$a-zA-Z][_$a-zA-Z0-9]*\\b";
	final String integer = "\\b[0-9]+\\b";
	final String word = "\\b[_$a-zA-Z0-9]+\\b";
	final String V = variable;
	final String gV = "(" + V + ")";
	final String W = word;
	final String gW = "(" + W + ")";
	final String I = integer;
	final String gI = "(" + I + ")";
	final String S = "\\s*";
	final String O = "\\(";
	final String C = "\\)";
	final String plus = S+"\\+"+S;
	final String minus = S+"\\-"+S;
	final String times = S+"\\*"+S;
	final String divide = S+"\\/"+S;
	final String modulus = S+"\\%"+S;
	final String gPM = "(" + plus + "|" + minus + ")";
	final String gOP = "(" + plus + "|" + minus + "|" + times + "|" + divide + "|" + modulus + ")";
	
	final Pattern iXi = Pattern.compile(gI+gOP+gI); // --> eval(I1 ? I2)
	final Pattern vXv = Pattern.compile(gV+gOP+gV); // --> if I1==I2 +:2*D,-:0,*:D**2,/:1
	final Pattern iXv = Pattern.compile(gI+gOP+gV);
	final Pattern vXi = Pattern.compile(gV+gOP+gI);
	final Pattern twoXtwo = Pattern.compile(O+gW+gPM+gW+C+gPM+O+gW+gPM+gW+C);
	final Pattern oneXtwo = Pattern.compile(gW+gPM+O+gW+gPM+gW+C);
	final Pattern twoXone = Pattern.compile(O+gW+gPM+gW+C+gPM+gW);
	final Pattern doubleParen = Pattern.compile(O + O + "([^\\)]*)" + C + C);
	
	final Pattern oWc = Pattern.compile(O+gW+C); // --> W
	final Pattern omWc = Pattern.compile(O+"\\-"+gW+C); // --> W
	
	public Simplifier() {
		rewriters.add( new Rewriter() {
			@Override
			public String rewrite(String expr) {
				Matcher matcher = iXi.matcher(expr);
				if (matcher.find()) {
					int n1 = Integer.parseInt(matcher.group(1));
					int n2 = Integer.parseInt(matcher.group(3));
					String op = matcher.group(2).trim();
					String replacement = matcher.group(0);
					switch (op) {
					case "+":
						replacement = Integer.toString(n1 + n2);
						break;
					case "-":
						replacement = Integer.toString(n1 - n2);
						break;
					case "*":
						replacement = Integer.toString(n1 * n2);
						break;
					case "/":
						replacement = Integer.toString(n1 / n2);
						break;
					case "%":
						replacement = Integer.toString(n1 % n2);
						break;
					}
					expr = matcher.replaceFirst(replacement);
				}
				return expr;
			}
		});
		rewriters.add( new Rewriter() {
			@Override
			public String rewrite(String expr) {
				Matcher matcher = vXv.matcher(expr);
				if (matcher.find()) {
					String v1 = matcher.group(1);
					String v2 = matcher.group(3);
					if (v1.equals(v2)) {
						String op = matcher.group(2).trim();
						String replacement = matcher.group(0);
						switch (op) {
						case "+":
							replacement = "2*" + v1;
							break;
						case "-":
							replacement = "0";
							break;
						case "*":
							replacement = v1 + "**2";
							break;
						case "/":
							replacement = "1";
							break;
						case "%":
							replacement = "0";
							break;
						}
						expr = matcher.replaceFirst(replacement);
					}
				}
				return expr;
			}
		});
		rewriters.add( new Rewriter() {
			@Override
			public String rewrite(String expr) {
				Matcher matcher = vXi.matcher(expr);
				if (matcher.find()) {
					String v1 = matcher.group(1);
					int n2 = Integer.parseInt(matcher.group(3));
					String op = matcher.group(2).trim();
					String replacement = matcher.group(0);
					switch (op) {
					case "+":
						break;
					case "-":
						break;
					case "*":
						if (n2 == 0) {
							replacement = "0";
						} else if (n2 == 1) {
							replacement = v1;
						}
						break;
					case "/":
						if (n2 == 1) {
							replacement = v1;
						}
						break;
					case "%":
						break;
					}
					expr = matcher.replaceFirst(replacement);
				}
				return expr;
			}
		});
		rewriters.add( new Rewriter() {
			@Override
			public String rewrite(String expr) {
				Matcher matcher = iXv.matcher(expr);
				if (matcher.find()) {
					String v2 = matcher.group(3);
					int n1 = Integer.parseInt(matcher.group(1));
					String op = matcher.group(2).trim();
					String replacement = matcher.group(0);
					switch (op) {
					case "+":
						break;
					case "-":
						break;
					case "*":
						if (n1 == 0) {
							replacement = "0";
						} else if (n1 == 1) {
							replacement = v2;
						}
						break;
					case "/":
						if (n1 == 1) {
							replacement = v2;
						}
						break;
					case "%":
						break;
					}
					expr = matcher.replaceFirst(replacement);
				}
				return expr;
			}
		});
		rewriters.add( new Rewriter() {
			//   a  b  c
			// (1+2)+(3+4) --> (1+3+2+4) [b,a,c]
			// (1+2)-(3+4) --> (1-3+2-4) [b,a,b^c]
			// (1-2)+(3+4) --> (1+3-2+4) [b,a,c]
			// (1-2)-(3+4) --> (1-3-2-4) [b,a,b^c]
			// (1+2)+(3-4) --> (1+3+2-4) [b,a,c]
			// (1+2)-(3-4) --> (1-3+2-4) [b,a,b^c]
			// (1-2)+(3-4) --> (1+3-2-4) [b,a,c]
			// (1-2)-(3-4) --> (1-3-2+4) [b,a,b^c]
			@Override
			public String rewrite(String expr) {
				Matcher matcher = twoXtwo.matcher(expr);
				if (matcher.find()) {
					String w1 = matcher.group(1);
					String w2 = matcher.group(3);
					String w3 = matcher.group(5);
					String w4 = matcher.group(7);
					String a = matcher.group(2).trim();
					String b = matcher.group(4).trim();
					String c = matcher.group(6).trim();
					if (b.equals("-")) {
						c = (c.equals("+")) ? "-" : "+";
					}
					String replacement = String.format("(%s %s %s %s %s %s %s)", w1, b, w3, a, w2, c, w4);
					expr = matcher.replaceFirst(replacement);
				}
				return expr;
			}
		});
		rewriters.add( new Rewriter() {
			//   a  b
			// (1+2)+3 --> 1+3+2 [b,a]
			// (1+2)-3 --> 1-3+2 [b,a]
			// (1-2)+3 --> 1+3-2 [b,a]
			// (1-2)-3 --> 1-3-2 [b,a]
			@Override
			public String rewrite(String expr) {
				Matcher matcher = twoXone.matcher(expr);
				if (matcher.find()) {
					String w1 = matcher.group(1);
					String w2 = matcher.group(3);
					String w3 = matcher.group(5);
					String a = matcher.group(2).trim();
					String b = matcher.group(4).trim();
					String replacement = String.format("(%s %s %s %s %s)", w1, b, w3, a, w2);
					expr = matcher.replaceFirst(replacement);
				}
				return expr;
			}			
		});
		rewriters.add( new Rewriter() {
			//  a  b       
			// 1+(2+3) -> 1+2+3 [a,b]
			// 1+(2-3) -> 1+2-3 [a,b]
			// 1-(2+3) -> 1-2-3 [a,^b]
			// 1-(2-3) -> 1-2+3 [a,^b]
			@Override
			public String rewrite(String expr) {
				Matcher matcher = oneXtwo.matcher(expr);
				if (matcher.find()) {
					String w1 = matcher.group(1);
					String w2 = matcher.group(3);
					String w3 = matcher.group(5);
					String a = matcher.group(2).trim();
					String b = matcher.group(4).trim();
					if (a.equals("-")) {
						if (b.equals("+")) {
							b = "-";
						} else {
							b = "+";
						}
					}
					String replacement = String.format("(%s %s %s %s %s)", w1, a, w2, b, w3);
					expr = matcher.replaceFirst(replacement);
				}
				return expr;
			}			
		});
		rewriters.add( new Rewriter() {
			@Override
			public String rewrite(String expr) {
				Matcher matcher = doubleParen.matcher(expr);
				if (matcher.find()) {
					String replacement = String.format("(%s)", matcher.group(1));
					expr = matcher.replaceFirst(replacement);
				}
				return expr;
			}
		});
		rewriters.add( new Rewriter() {
			@Override
			public String rewrite(String expr) {
				Matcher matcher = oWc.matcher(expr);
				if (matcher.find()) {
					String w = matcher.group(1);
					expr = matcher.replaceFirst(w);
				}
				return expr;
			}
		});
		rewriters.add( new Rewriter() {
			@Override
			public String rewrite(String expr) {
				Matcher matcher = omWc.matcher(expr);
				if (matcher.find()) {
					String w = matcher.group(1);
					expr = matcher.replaceFirst("-" + w);
				}
				return expr;
			}
		});
	}
	
	public String simplify( String expr ) {
		Stack<String> priors = new Stack<>();
		while (! expr.isEmpty()) {
			if (priors.contains(expr))
				break;  // we are cycling
			priors.push(expr);
			for (Rewriter rewriter : rewriters) {
				expr = rewriter.rewrite(expr);
			}
		}
		return expr;
	}
	
//	public String simplify0( String expr ) {
//		System.out.println("Simplify: " + expr);
//		Equation eq = new Equation();
//		try {
//			String text = "out = " + expr;
//			eq.autoDeclare(text);
//			Sequence seq = eq.compile(text);
//			CompileCodeOperations compiler = new CompileCodeOperations(new EmitJavaOperation(eq.getManagerFunctions()), seq, eq.getTemporariesManager() );
//			System.out.println(compiler.toString());
//			compiler.optimize();
//	    	if (seq.getInfos().size() == 1) {
//		    	StringBuilder block = new StringBuilder();
//		    	IEmitOperation coder = new EmitJavaOperation(eq.getManagerFunctions());
//		    	coder.emitOperation( block, seq.getInfos().get(0) );
//		    	return block.substring(6, block.length()-2);
//	    	}			
//			return expr;
//		} catch (Exception x) {
//			return expr;
//		}
//	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Simplifier simplifier = new Simplifier();
		String expr = simplifier.simplify("((i + 1 + 1) - (i + 1))");
		System.out.println(expr);
		expr = simplifier.simplify("(m - (m+1))");
		System.out.println(expr);
	}

}
