package com.pocket.util.android;

/**
 * Any classes implementing this interface will be kept by Proguard.  Provided the proguard.cfg file has the following definitions
 * 
 * -keep public class com.pocket.util.android.NoObfuscation
 * -keep public class * implements com.pocket.util.android.NoObfuscation
 * -keepclassmembers class * implements com.pocket.util.android.NoObfuscation {
 *   <methods>;
 * } 
 * 
 * 
 * @author max
 *
 */
public interface NoObfuscation {

}
