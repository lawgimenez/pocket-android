package com.pocket.sync.print.java.tests;

import com.pocket.sync.print.java.CommandLineGeneration;

/**
 * Generates Java classes from the tests.graphqls, for use in the base modules sync engine unit tests.
 */
public class SyncTestsGenerator {
	
	public static void main(String[] arg) {
		CommandLineGeneration.main(SyncTestsConfig::new, arg);
	}
}
