package org.ejml.equation;

import java.util.HashMap;

/**
	 * If a temporary exists solely to hold a constant expression eliminate it.
	 * 
	 * Convert the output temp to a scalar constant holding the constant expression.
	 */
    
	class Precedence extends HashMap<String, Integer> {
		
		public static final int MIN_PRECEDENCE = 0;
		public static final int MAX_PRECEDENCE = 100;
		
		public Precedence() {
			this.put("add-ii", 10);
			this.put("add-ss", 10);
			this.put("subtract-ii", 10);
			this.put("subtract-ss", 10);
			this.put("multiply-ii", 20);
			this.put("multiply-ss", 20);
			this.put("divide-ii", 20);
			this.put("divide-ss", 20);
			this.put("pow-ii", 30);
			this.put("pow-ss", 30);
			this.put("neg-i", 40);
			this.put("neg-s", 40);
		}
		
		public Integer get(String key) {
			Integer v = super.get(key);
			if (v != null) {
				return v;
			}
//			System.out.println(key + " missing");
			return MAX_PRECEDENCE;
		}
 	}