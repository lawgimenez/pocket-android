package com.pocket.legacy;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * An assortment of command line scripts useful when working in this project.
 * This includes making java based scripts for our CI to use so we can write CI scripts in Java.
 *
 * After updating this file, you should run ./updateToolJars.sh
 * <p>
 * From the root dir of this the Android project/repo you can do the following:
 * <p>
 * {@code java -jar project-tools/legacyTools.jar -YOUR_COMMAND}
 */
public class ProjectTools {
	/* If you need to debug any of these commands, add a configuration in Android Studio.
	 * 1. At the top, click the configuration dropdown, select Edit Configurations
	 * 2. Click the JAR Application section on the left
	 * 3. Click the + at the top left
	 * 4. For Path to Jar, enter the absolute path to `project-tools/legacyTools.jar`  it may look something like `/Users/yourname/RIL/Android/alpha/project-tools/legacyTools.jar`
	 * 5. For program arguments enter whatever command you want to test like `-printVersionCode Pocket/build.gradle`
	 * 6. For working directory, the absolute path to the project root directory
	 * 7. Add a gradle task before launch: toolJar
	 * 8. Now you can run or debug that configuration
	 */
	
	public static void main(String[] args) {
		try {
			String command = args[0];
			switch (command) {
				case "-setAppVersionPart": setAppVersionPart(args[1], Integer.valueOf(args[2]), new File(args[3])); break;
				case "-incrementAppVersionPart": incrementAppVersionPart(args[1], Integer.valueOf(args[2]), new File(args[3])); break;
				case "-printVersionCode": printVersionCode(new File(args[1])); break;
				case "-printVersionName": printVersionName(new File(args[1])); break;
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	private enum VersionPart {
		MAJOR("Major", 210),
		MINOR("Minor", 99),
		PATCH("Patch", 99),
		BUILD("Build", 999);
		
		private final String variablePostfix;
		private final int max;
		
		VersionPart(String variablePostfix, int max) {
			this.variablePostfix = variablePostfix;
			this.max = max;
		}
	}
	
	
	/**
	 * Change one of the version number parts of the main Pocket app.
	 *
	 * For example, if the Pocket app's version was currently 7.0.1.3, doing the following:
	 *
	 * java -jar project-tools/legacyTools.jar -setAppVersionPart build 5 Pocket/build.gradle
	 *
	 * Would change it to 7.0.1.5
	 *
	 * @param part One of the {@link VersionPart#variablePostfix} names of the part you want to change.
	 * @param version The number to set for this version part.
	 * @param buildFile The path to the build file. Mostly likely just `Pocket/build.gradle`
	 */
	private static void setAppVersionPart(String part, int version, File buildFile) throws IOException {
		part = part.trim();
		for (VersionPart vp : VersionPart.values()) {
			if (part.equalsIgnoreCase(vp.variablePostfix)) {
				setAppVersionPart(vp, version, buildFile);
				return;
			}
		}
		throw new IllegalArgumentException("unknown part name: " + part);
	}
	
	/** See {@link #setAppVersionPart(String, int, File)} */
	private static void setAppVersionPart(VersionPart part, int version, File buildFile) throws IOException {
		if (version < 0 || version > part.max) throw new IllegalArgumentException("invalid build version: " + version);
		
		String linePrefix = "val version"+ part.variablePostfix + " = ";
		BufferedReader file = new BufferedReader(new FileReader(buildFile));
		String line;
		StringBuilder output = new StringBuilder();
		while ((line = file.readLine()) != null) {
			if (line.startsWith(linePrefix)) {
				String comment = line.substring(line.indexOf(" /"));
				line = linePrefix + version + comment;
			}
			output.append(line);
			output.append('\n');
		}
		file.close();
		
		FileUtils.writeStringToFile(buildFile, output.toString());
	}
	
	/**
	 * Increment one of the version number parts of the main Pocket app by a given amount.
	 *
	 * For example, if the Pocket app's version was currently 7.0.1.3, doing the following:
	 *
	 * java -jar project-tools/legacyTools.jar -incrementAppVersionPart build 1 Pocket/build.gradle
	 *
	 * Would change it to 7.0.1.4
	 *
	 * @param part One of the {@link VersionPart#variablePostfix} names of the part you want to change.
	 * @param increment The amount to add to the version part.
	 * @param buildFile The path to the build file. Mostly likely just `Pocket/build.gradle`
	 */
	private static void incrementAppVersionPart(String part, int increment, File buildFile) throws IOException {
		part = part.trim();
		for (VersionPart vp : VersionPart.values()) {
			if (part.equalsIgnoreCase(vp.variablePostfix)) {
				incrementAppVersionPart(vp, increment, buildFile);
				return;
			}
		}
		throw new IllegalArgumentException("unknown part name: " + part);
	}
	
	/** see {@link #incrementAppVersionPart(String, int, File)} */
	private static void incrementAppVersionPart(VersionPart part, int increment, File buildFile) throws IOException {
		String linePrefix = "val version"+ part.variablePostfix + " = ";
		BufferedReader file = new BufferedReader(new FileReader(buildFile));
		String line;
		StringBuilder output = new StringBuilder();
		while ((line = file.readLine()) != null) {
			if (line.startsWith(linePrefix)) {
				String comment = line.substring(line.indexOf(" /"));
				Integer version = Integer.valueOf(line.substring(linePrefix.length(), line.length()-comment.length()).trim());
				version += increment;
				if (version < 0 || version > part.max) throw new IllegalArgumentException("invalid build version: " + version + " after increment: " + increment);
				line = linePrefix + version + comment;
			}
			output.append(line);
			output.append('\n');
		}
		file.close();
		
		FileUtils.writeStringToFile(buildFile, output.toString());
	}
	
	/**
	 * Print out the current Pocket app's versionCode.
	 *
	 * java -jar project-tools/legacyTools.jar -printVersionCode Pocket/build.gradle
	 *
	 *  @param buildFile The path to the build file. Mostly likely just `Pocket/build.gradle`
	 */
	private static void printVersionCode(File buildFile) throws IOException {
		int[] v = getVersionParts(buildFile);
		int versionCode = v[0] * 10000000 + v[1] * 100000 + v[2] * 1000 + v[3];
		System.out.print(versionCode);
	}
	
	/**
	 * Print out the current Pocket app's versionName.
	 *
	 * java -jar project-tools/legacyTools.jar -printVersionCode Pocket/build.gradle
	 *
	 *  @param buildFile The path to the build file. Mostly likely just `Pocket/build.gradle`
	 */
	private static void printVersionName(File buildFile) throws IOException {
		int[] v = getVersionParts(buildFile);
		StringBuilder out = new StringBuilder();
		for (int part : v) {
			if (out.length() > 0) out.append(".");
			out.append(part);
		}
		System.out.print(out.toString());
	}

	/**
	 * Get the current version number parts from the build/gradle file.
	 * [0] = Major, [1] = Minor, etc.
	 *
	 * @param buildFile The path to the build file. Mostly likely just `Pocket/build.gradle`
	 */
	private static int[] getVersionParts(File buildFile) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(buildFile));
		String line;
		int[] parts = new int[4];
		int i = 0;
		while ((line = file.readLine()) != null) {
			if (line.startsWith("val version")) {
				String part = line.substring(
									line.indexOf("=")+1,
									line.indexOf("//")
								).trim();
				parts[i++] = Integer.valueOf(part);
				if (i == 4) break; // Done collecting version.
			}
		}
		file.close();
		return parts;
	}
}
