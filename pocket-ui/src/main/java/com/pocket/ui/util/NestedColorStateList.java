package com.pocket.ui.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.util.SparseArray;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.pocket.util.java.Logs;
import com.pocket.util.java.RangeF;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Allows color state lists to reference other color state lists in xml.
 * <p>
 * Write your color state xml like normal. Reference other state lists just like normal color references like `android:color="@color/your_state_state_list".
 * Then load them at runtime with {@link #get(Context, int)}, which will return a normal {@link ColorStateList}.
 * <p>
 * Note, that state rules are included on any referenced rules.
 * So for example, say you a color state list called "amber_selector" that looks like this:
 *
 * <pre>{@literal
 * <?xml version="1.0" encoding="utf-8"?>
 * <selector xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto">
 *     <item app:state_dark="true" android:drawable="@color/pkt_dm_amber" />
 *     <item android:drawable="@color/pkt_amber" />
 * </selector>
 * }</pre>
 *
 * If you reference that with a rule like:
 *
 * {@code <item android:state_checked="true" android:drawable="@color/amber_selector" />}
 *
 * It will be the equivalent of having written:
 *
 * <pre>{@literal
 *     <item android:state_checked="true" app:state_dark="true" android:drawable="@color/pkt_dm_amber" />
 *     <item android:state_checked="true" android:drawable="@color/pkt_amber" />
 * }</pre>
 *
 */
public class NestedColorStateList {
	
	private static class Builder {
		private final ArrayList<int[]> states = new ArrayList<>();
		private final ArrayList<Integer> colors = new ArrayList<>();
		private final TypedValue v = new TypedValue();
		
		void recycle() {
			states.clear();
			colors.clear();
		}
	}
	
	private static final Builder builder = new Builder();
	private static final SparseArray<ColorStateList> cache = new SparseArray<>();
	
	/**
	 * This must be invoked from the ui thread.
	 */
	public static ColorStateList get(Context context, int resId) {
		// TODO force ui thread or synchronize
		// TODO we don't have any config dependant colors yet, but if we start doing that, we need to remove things from the cache in certain cases.  See TypedValue.changingConfigurations
		// TODO improve performance, though with the cache, this isn't urgent at all. the biggest gain we can do is from getting rid of usage of Collection and object classes and use primitive arrays, see ColorStateList.inflate and how they handle arrays for inspiration.
		//			another potential gain is using the cache for internal nested color state list look ups as well
		
		ColorStateList cached = cache.get(resId);
		if (cached != null) {
			return cached;
		}
		
		try {
			builder.recycle();
			
			Resources resources = context.getResources();
			resources.getValue(resId, builder.v, true);
			if (builder.v.type != TypedValue.TYPE_STRING) {
				// Single color
				return ContextCompat.getColorStateList(context, resId);
			}
			
			collectColors(builder, resources, resId, null);
			
			int[] colors = new int[builder.colors.size()];
			int[][] states = new int[colors.length][];
			for (int i = 0, len = colors.length; i < len; i++) {
				colors[i] = builder.colors.get(i);
				states[i] = builder.states.get(i);
			}
			
			ColorStateList result = new ColorStateList(states, colors);
			
			cache.put(resId, result);
			return result;
			
		} catch (Throwable t) {
			Logs.printStackTrace(t);
			return null;
			
			// Comment out above two lines and uncomment below to make layout preview work in Android Studio:
			// return new ColorStateList(new int[0][], new int[0]);
		}
	}
	
	/**
	 * Parse a color state list xml, adding its colors/rules into the builder.
	 *
	 * @param into Where to add the rules
	 * @param resources
	 * @param resId The resource id of the color state list
	 * @param parentStates Any rules to merge/add to all rules found in this color state list (optional, can be null to just add the rules as they are found)
	 */
	private static void collectColors(Builder into, Resources resources, int resId, Set<Integer> parentStates) throws IOException, XmlPullParserException {
		/*
			Inspirations for how to make this work are from ColorStateList.inflate, Resources.getColorStateList and similar framework methods
		 */
		XmlResourceParser parser = resources.getXml(resId);
		int token;
		while ((token = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (token == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
				Set<Integer> states = new HashSet<>(parser.getAttributeCount() - 1);
				int nestedStateList = 0;
				Integer rawColor = null;
				float alpha = 1f;
				
				for (int i = 0, count = parser.getAttributeCount(); i < count; i++) {
					int attribute = parser.getAttributeNameResource(i);
					
					if (attribute == android.R.attr.color) {
						int color = parser.getAttributeIntValue(i, -1);
						if (color != -1)  {
							// Directly declared color
							rawColor = color;
							
						} else {
							resources.getValue(parser.getAttributeResourceValue(i, -1), into.v, true);
							if (into.v.type == TypedValue.TYPE_STRING) {
								// ColorStateList
								nestedStateList = into.v.resourceId;
								
							} else  {
								// Color resource
								rawColor = into.v.data;
							}
						}
					} else if (attribute == android.R.attr.alpha) {
						alpha = parser.getAttributeFloatValue(i, 1f);
						
					} else {
						if (!parser.getAttributeBooleanValue(i, false)) {
							attribute = -attribute;
						}
						states.add(attribute);
					}
				}
				
				if (parentStates != null) {
					states.addAll(parentStates);
				}
				
				if (nestedStateList != 0)  {
					collectColors(into, resources, nestedStateList, states);
				} else {
					into.colors.add(modulateColorAlpha(rawColor, alpha));
					
					int[] array = new int[states.size()];
					int i =  0;
					for (Integer state : states)  {
						array[i++] = state;
					}
					into.states.add(array);
				}
			}
		}
	}
	
	/**
	 * Based on source of ColorStateList's implementation
	 */
	private static int modulateColorAlpha(int baseColor, float alphaMod) {
		if (alphaMod == 1.0f) {
			return baseColor;
		}
		
		final int baseAlpha = Color.alpha(baseColor);
		final int alpha = (int) RangeF.constrain(0, 255, baseAlpha * alphaMod + 0.5f);
		return (baseColor & 0xFFFFFF) | (alpha << 24);
	}
	
}
