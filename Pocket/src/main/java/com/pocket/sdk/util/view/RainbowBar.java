package com.pocket.sdk.util.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.sdk.util.drawable.RainbowDrawable;
import com.pocket.ui.view.themed.ThemedView;

public class RainbowBar extends ThemedView {

	public static final int MIN_RAINBOW_HEIGHT = App.getContext().getResources().getDimensionPixelSize(R.dimen.rainbow_bar_height);
	private RainbowDrawable mRainbow;
	
	public RainbowBar(Context context) {
		super(context);
		init();
	}
	
	public RainbowBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public RainbowBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init() {
		mRainbow = new RainbowDrawable(this);
	}
	
	@Override
	protected boolean verifyDrawable(Drawable who) {
		if (who == mRainbow && mRainbow != null) {
			return true;
		}
		return super.verifyDrawable(who);
	}
	
	public RainbowDrawable getRainbow() {
		return mRainbow;
	}
	
	@Override
	protected int getSuggestedMinimumHeight() {
		return MIN_RAINBOW_HEIGHT;
	}
	
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		mRainbow.setState(getDrawableState());
		invalidate();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mRainbow.setBounds(0, 0, w, h);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		mRainbow.draw(canvas);
	}

}
