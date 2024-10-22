package com.pocket.sdk.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pocket.util.android.animation.AnimatorEndListener;
import com.pocket.util.android.animation.Interpolators;
import com.pocket.util.android.animation.WebViewArgbEvaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.Transition;
import androidx.transition.TransitionValues;

class ThemeChange extends Transition {
	
	static final int DURATION = 300;
	static final TypeEvaluator ARGB_EVALUATOR = new WebViewArgbEvaluator();
	
	private static final String KEY_BACKGROUND = "pocket:themeChange:background";
	private static final String KEY_BACKGROUND_FOR_CURRENT_STATE =
			"pocket:themeChange:backgroundDrawableForCurrentState";
	private static final String KEY_TEXT_COLOR = "pocket:themeChange:textColor";
	private static final String KEY_CURRENT_TEXT_COLOR = "pocket:themeChange:currentTextColor";
	
	private static final Property<View, Integer> BACKGROUND_COLOR = new BackgroundColorProperty();
	private static final Property<TextView, Integer> TEXT_COLOR = new TextColorProperty();
	
	@Nullable
	@Override
	public Animator createAnimator(@NonNull ViewGroup sceneRoot,
			@Nullable TransitionValues startValues,
			@Nullable TransitionValues endValues) {
		if (startValues == null || endValues == null) return null;
		
		final Collection<Animator> animators = new ArrayList<>();
		
		final Integer startColor = getColor(startValues.values);
		final Integer endColor = getColor(endValues.values);
		if (startColor != null && endColor != null) {
			final ObjectAnimator background =
					ObjectAnimator.ofInt(startValues.view, BACKGROUND_COLOR, startColor, endColor);
			background.setEvaluator(ARGB_EVALUATOR);
			background.addListener(new AnimatorEndListener() {
				@Override public void onAnimationEnd(Animator animation) {
					background.removeAllListeners();
					startValues.view.setBackground((Drawable) startValues.values.get(KEY_BACKGROUND));
				}
			});
			animators.add(background);
		}
		
		if (startValues.view instanceof TextView) {
			final TextView textView = (TextView) startValues.view;
			final ObjectAnimator textColor = ObjectAnimator.ofInt(textView,
					TEXT_COLOR,
					((Integer) startValues.values.get(KEY_CURRENT_TEXT_COLOR)),
					((Integer) endValues.values.get(KEY_CURRENT_TEXT_COLOR)));
			textColor.setEvaluator(ARGB_EVALUATOR);
			textColor.addListener(new AnimatorEndListener() {
				@Override public void onAnimationEnd(Animator animation) {
					textColor.removeAllListeners();
					textView.setTextColor(((ColorStateList) startValues.values.get(KEY_TEXT_COLOR)));
				}
			});
			animators.add(textColor);
		}
		
		final AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(animators);
		animatorSet.setDuration(DURATION);
		animatorSet.setInterpolator(Interpolators.LINEAR);
		return animatorSet;
	}
	
	private @ColorInt Integer getColor(Map<String, Object> values) {
		final Object background = values.get(KEY_BACKGROUND);
		final Object backgroundForCurrentState = values.get(KEY_BACKGROUND_FOR_CURRENT_STATE);
		
		if (background instanceof ColorDrawable) {
			return ((ColorDrawable) background).getColor();
		} else if (backgroundForCurrentState instanceof ColorDrawable) {
			return ((ColorDrawable) backgroundForCurrentState).getColor();
		}
		
		return null;
	}
	
	@Override public void captureStartValues(@NonNull TransitionValues transitionValues) {
		captureValues(transitionValues);
	}
	
	@Override public void captureEndValues(@NonNull TransitionValues transitionValues) {
		captureValues(transitionValues);
	}
	
	private void captureValues(TransitionValues transitionValues) {
		// Capture background to try and animate it.
		final Drawable background = transitionValues.view.getBackground();
		transitionValues.values.put(KEY_BACKGROUND, background);
		
		// In our case background is usually stateful (that's how we change themes),
		// so let's capture drawable for current state.
		if (background instanceof StateListDrawable) {
			StateListDrawable stateListDrawable = (StateListDrawable) background;
			while (stateListDrawable.getCurrent() instanceof StateListDrawable) {
				stateListDrawable = (StateListDrawable) stateListDrawable.getCurrent();
			}
			transitionValues.values.put(KEY_BACKGROUND_FOR_CURRENT_STATE, stateListDrawable.getCurrent());
		}
		
		// Also capture text color if there is text
		if (transitionValues.view instanceof TextView) {
			final TextView textView = (TextView) transitionValues.view;
			transitionValues.values.put(KEY_TEXT_COLOR, textView.getTextColors());
			transitionValues.values.put(KEY_CURRENT_TEXT_COLOR, textView.getCurrentTextColor());
		}
	}
	
	private static class BackgroundColorProperty extends Property<View, Integer> {
		BackgroundColorProperty() {
			super(Integer.class, "backgroundColor");
		}
		
		@Override public void set(View object, Integer value) {
			object.setBackgroundColor(value);
		}
		
		@Override public Integer get(View object) {
			final Drawable background = object.getBackground();
			
			if (background instanceof ColorDrawable) {
				return ((ColorDrawable) background).getColor();
			}
			
			return null;
		}
	}
	
	private static class TextColorProperty extends Property<TextView, Integer> {
		TextColorProperty() {
			super(Integer.class, "textColor");
		}
		
		@Override public void set(TextView object, Integer value) {
			object.setTextColor(value);
		}
		
		@Override public Integer get(TextView object) {
			return object.getCurrentTextColor();
		}
	}
}
