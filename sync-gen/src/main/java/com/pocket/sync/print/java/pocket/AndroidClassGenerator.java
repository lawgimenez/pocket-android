package com.pocket.sync.print.java.pocket;

import com.pocket.sync.print.java.CommandLineGeneration;

import java.io.IOException;

/**
 * Generates Java classes from Figment into the neighboring Android project's sources.
 */
public class AndroidClassGenerator {

	public static void main(String[] arg) throws IOException {
		CommandLineGeneration.main(PocketConfig::new, arg);
	}

}
