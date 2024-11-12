package com.pocket.app;

import org.apache.commons.lang3.StringUtils;

/**
 * Help convert between versionCode and versionName based on how we format them for Pocket builds. See the Pocket build.gradle file for more details or the RELEASE_WORKFLOW.md version section.
 */
public class VersionUtil {

    public static int toVersionCode(int versionMajor, int versionMinor) {
        return toVersionCode(versionMajor, versionMinor, 0, 0);
    }

    public static int toVersionCode(int versionMajor, int versionMinor, int versionPatch) {
        return toVersionCode(versionMajor, versionMinor, versionPatch, 0);
    }

    public static int toVersionCode(int versionMajor, int versionMinor, int versionPatch, int versionBuild) {
        return versionMajor * 10000000 + versionMinor * 100000 + versionPatch * 1000 + versionBuild;
    }
    
    public static String toVersionName(int versionCode) {
        // MMM.mm.pp.bbb   <-- version names look like this  (M major, m minor, p patch, b build)
        // MMMmmppbbb      <-- version codes are made up of the various parts with leading zeros as needed
        // 0123456789      <-- string indexes of each character/part
        
        String str = StringUtils.leftPad(String.valueOf(versionCode), 10, '0');
        int major = extractNumber(str, 0, 3);
        int minor = extractNumber(str, 3, 2);
        int patch = extractNumber(str, 5, 2);
        int build = extractNumber(str, 7, 3);
        return major + "." + minor + "." + patch + "." + build;
    }
    
    private static int extractNumber(String numberStr, int start, int length) {
        return Integer.parseInt(numberStr.substring(start, start+length)
                .replaceFirst("^0+(?!$)", "")); // Remove leading zeros
    }
}
