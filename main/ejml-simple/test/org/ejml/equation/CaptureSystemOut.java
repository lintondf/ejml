package org.ejml.equation;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public abstract class CaptureSystemOut {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	PrintStream saveOut = System.out;
	
	public CaptureSystemOut() {}
	
	public abstract boolean run();
	
	public String capture() {
		try {
			System.setOut(new PrintStream(baos));
			if( ! run() )
				return null;
			return baos.toString();
		} catch (Exception x) {
			throw x;
		} finally {
			System.setOut(saveOut);
		}
	}
}