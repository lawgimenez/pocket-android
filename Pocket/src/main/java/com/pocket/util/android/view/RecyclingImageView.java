package com.pocket.util.android.view;

/*
 * An ImageView that calls recycle on old bitmaps when setting a new one.
 * 
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RecyclingImageView extends ImageView  {
	
	public RecyclingImageView(Context context) {
        super(context);
    }
    
    public RecyclingImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public RecyclingImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public void setImageBitmap(Bitmap bitmap) { 
		Drawable drawable = getDrawable();
		if (drawable != null && drawable instanceof BitmapDrawable){
			Bitmap oldBitmap = ((BitmapDrawable)getDrawable()).getBitmap();
			if (oldBitmap != null) {
				oldBitmap.recycle();
			}
		}
		
		super.setImageBitmap(bitmap);
    }
	
}

