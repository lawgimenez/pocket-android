package com.pocket.util.android.webkit;

import android.webkit.WebView;

import com.fasterxml.jackson.databind.JsonNode;
import com.pocket.app.App;
import com.pocket.util.java.StringBuilders;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * A function that can be invoked in Javascript.
 * <p>
 * <h3>To use:</h3>
 * Choose one of the constructors and then add optional parameter values with the various {@code value()} methods. They will be included in the parameters of the function in the order in which you call {@code value()}.
 * <p>
 * To invoke it, call {@link #execute(WebView)}. {@code execute()} may be used multiple times but after executing the function, it may not longer be changed. This means if you attempt to call a {@code value()} method after {@code execute()}
 * an exception will be thrown.  
 * @author max
 *
 */
public class JavascriptFunction {

	private StringBuilder mBuilder;
	private String mQuery;
	private boolean mHasParams;
	
	/**
	 * @param functionName The method name. For example, "alert" would correspond to {@code alert()}
	 */
	public JavascriptFunction(String functionName) {
		this(null, functionName);
	}
	
	/**
	 * @param object The object to invoke the function on. For example, "video" from this example: {@code video.play()}
	 * @param functionName The method name. For example, "play" from this example: {@code video.play()}
	 */
	public JavascriptFunction(String object, String functionName) {
		mBuilder = StringBuilders.get();
		
		if (object != null) {
			mBuilder.append(object)
				.append(".");
		}
		mBuilder.append(functionName)
			.append("(");
	}

	/**
	 * @see {@link #value(String, boolean)} that escapes the value.
     */
	public JavascriptFunction value(String value) {
		return value(value, true);
	}
	
	/**
	 * Adds a value to the function's parameters. For example, adding "Hello World" would add it to the function like {@code object.functionName('Hello World')}
	 * <p>
	 * Parameters are added in the order in which these {@code value()} methods are called.
	 * @param value
	 * @param escape true if this string should {@link #escapeForSingleQuote(String)}, false if already escaped. If you are passing a huge string here, you should escape it ahead of time asynchoronusly so it doesn't block the ui thread.
	 * @return This {@link JavascriptFunction} for chaining calls.
	 */
	public JavascriptFunction value(String value, boolean escape) {
		prepareNextParam();

		if (escape) {
			value = escapeForSingleQuote(value);
		}

		mBuilder.append("'")
			.append(value)
			.append("'");

		return this;
	}

	public JavascriptFunction valueJsonString(String value) {
		prepareNextParam();
		mBuilder.append(escapeJsonForString(value));
		return this;
	}

	public static String escapeForSingleQuote(String value) {
		// Properly escape special characters or transform encoded ' into ' so we can escape them.
		value = StringEscapeUtils.escapeJava(value);
		value = value.replaceAll("(?<!\\\\)'", "\\\\'"); // Escape ' if not escaped
		return value;
	}
	
	/**
	 * Adds a value to the function's parameters. For example, adding {"key":"value"} would add it to the function like {@code object.functionName({"key":"value"})}
	 * <p>
	 * Parameters are added in the order in which these {@code value()} methods are called.
	 * @param value
	 * @return This {@link JavascriptFunction} for chaining calls.
	 */
	public JavascriptFunction value(JsonNode value) {
		prepareNextParam();

		String string = escapeJsonForString(value);

		mBuilder.append(string);
		return this;
	}

	public static String escapeJsonForString(JsonNode value) {
		return escapeJsonForString(value.toString());
	}

	public static String escapeJsonForString(String string) {
		// Escape some characters that break things if they aren't already escaped.
		String notEscapedPrefix = "(?<!\\\\)";
		string = string.replaceAll(notEscapedPrefix + "\u2028", "\\\\u2028");
		string = string.replaceAll(notEscapedPrefix + "\u2029", "\\\\u2029");
		return string;
	}

	/**
	 * Adds a value to the function's parameters. For example, adding {@code 108} would add it to the function like {@code object.functionName(108)}
	 * <p>																	 
	 * Parameters are added in the order in which these {@code value()} methods are called.
	 * @param value
	 * @return This {@link JavascriptFunction} for chaining calls.
	 */
	public JavascriptFunction value(int value) {
		prepareNextParam();
		mBuilder.append(value);
		return this;
	}
	
	/**
	 * Adds a value to the function's parameters. For example, adding {@code 4815162342} would add it to the function like {@code object.functionName(4815162342)}
	 * <p>																	 
	 * Parameters are added in the order in which these {@code value()} methods are called.
	 * @param value
	 * @return This {@link JavascriptFunction} for chaining calls.
	 */
	public JavascriptFunction value(long value) {
		prepareNextParam();
		mBuilder.append(value);
		return this;
	}
	
	/**
	 * Adds a value to the function's parameters. For example, adding {@code 108.21} would add it to the function like {@code object.functionName(108.21)}
	 * <p>																	 
	 * Parameters are added in the order in which these {@code value()} methods are called.
	 * @param value
	 * @return This {@link JavascriptFunction} for chaining calls.
	 */
	public JavascriptFunction value(float value) {
		prepareNextParam();
		mBuilder.append(value);
		return this;
	}
	
	/**
	 * Adds a value to the function's parameters. For example, adding {@code 48.15162342} would add it to the function like {@code object.functionName(48.15162342)}
	 * <p>																	 
	 * Parameters are added in the order in which these {@code value()} methods are called.
	 * @param value
	 * @return This {@link JavascriptFunction} for chaining calls.
	 */
	public JavascriptFunction value(double value) {
		prepareNextParam();
		mBuilder.append(value);
		return this;
	}
	
	/**
	 * Adds a value to the function's parameters. For example, adding {@code true} would add it to the function like {@code object.functionName(true)}
	 * <p>																	 
	 * Parameters are added in the order in which these {@code value()} methods are called.
	 * @param value
	 * @return This {@link JavascriptFunction} for chaining calls.
	 */
	public JavascriptFunction value(boolean value) {
		prepareNextParam();
		mBuilder.append(value ? "true" : "false");
		return this;
	}
	
	/**
	 * Invokes the function constructed thus far on the provided WebView. If not on the UI Thread, it will be posted to it.
	 * <p>
	 * <b>Note:</b> While you may call execute multiple times, after calling this method, no further changes may be made to this function. 
	 * @param webview
	 */
	public void execute(final WebView webview) {
		App.from(App.getContext()).threads().runOrPostOnUiThread(() -> {
			if (mQuery == null) {
				mBuilder.append(");");
				mQuery = mBuilder.toString();
				StringBuilders.recycle(mBuilder);
				mBuilder = null;
			}

			webview.evaluateJavascript(mQuery, s -> {
			});
			
			if (webview instanceof JavascriptMethodListener) {
				((JavascriptMethodListener) webview).onJavascriptExecuted();
			}
		});
	}
	
	private void prepareNextParam() {
		if (mHasParams) {
			mBuilder.append(", ");
		} else {
			mHasParams = true;
		}
	}
	
	public interface JavascriptMethodListener {
		/** Invoked after a {@link JavascriptFunction} has been executed within this WebView.*/
		public void onJavascriptExecuted();
	}

}
