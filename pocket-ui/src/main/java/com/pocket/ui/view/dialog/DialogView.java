package com.pocket.ui.view.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.view.themed.ThemedRelativeLayout;
import com.pocket.util.android.ViewUtilKt;

/**
 * A simple prompt/messaging view. Typically used in a dialog popup.
 * Use {@link Binder#showAsAlertDialog(DialogInterface.OnDismissListener, boolean)} for convenience.
 */
public class DialogView extends ThemedRelativeLayout {
	
	private final DialogView.Binder binder = new DialogView.Binder();
	private final int maxHeight = DimenUtil.dpToPxInt(getContext(), 309);
	private TextView title;
	private TextView message;
	private TextView buttonPrimary;
	private TextView buttonSecondary;
	private OnClickListener onClickPrimary;
	private OnClickListener onClickSecondary;
	private AlertDialog dialog;
	
	public DialogView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public DialogView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public DialogView(Context context) {
		super(context);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_dialog_popup, this, true);
		title = findViewById(R.id.title);
		message = findViewById(R.id.message);
		buttonPrimary = findViewById(R.id.button_primary);
		buttonSecondary = findViewById(R.id.button_secondary);
		buttonPrimary.setOnClickListener(v -> {
			if (dialog != null) dialog.dismiss();
			if (onClickPrimary != null) onClickPrimary.onClick(v);
		});
		buttonSecondary.setOnClickListener(v -> {
			if (dialog != null) dialog.dismiss();
			if (onClickSecondary != null) onClickSecondary.onClick(v);
		});
		bind().clear();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int mode = View.MeasureSpec.getMode(heightMeasureSpec);
		int measuredHeight = View.MeasureSpec.getSize(heightMeasureSpec);
		int adjustedHeight = Math.min(measuredHeight, maxHeight);
		int adjustedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(adjustedHeight, mode);
		super.onMeasure(widthMeasureSpec, adjustedHeightMeasureSpec);
	}
	
	public DialogView.Binder bind() {
		return binder;
	}
	
	public class Binder {
		
		public DialogView.Binder clear() {
			title(null);
			message(null);
			buttonPrimary(null, null);
			buttonSecondary(null, null);
			return this;
		}
		
		public DialogView.Binder title(int value) {
			return title(getResources().getText(value));
		}
		
		public DialogView.Binder title(CharSequence value) {
			ViewUtilKt.setTextOrHide(title, value);
			return this;
		}
		
		public DialogView.Binder message(int value) {
			return message(getResources().getText(value));
		}
		
		public DialogView.Binder message(CharSequence value) {
			ViewUtilKt.setTextOrHide(message, value);
			return this;
		}
		
		public DialogView.Binder buttonPrimary(int value, OnClickListener listener) {
			return buttonPrimary(getResources().getText(value), listener);
		}
		
		public DialogView.Binder buttonPrimary(CharSequence value, OnClickListener listener) {
			ViewUtilKt.setTextOrHide(buttonPrimary, value);
			onClickPrimary = listener;
			return this;
		}
		
		public DialogView.Binder buttonSecondary(int value, OnClickListener listener) {
			return buttonSecondary(getResources().getText(value), listener);
		}
		
		public DialogView.Binder buttonSecondary(CharSequence value, OnClickListener listener) {
			ViewUtilKt.setTextOrHide(buttonSecondary, value);
			onClickSecondary = listener;
			return this;
		}
		
		public DialogView.Binder showAsAlertDialog(DialogInterface.OnDismissListener onDismissListener, boolean cancelable) {
			View self = DialogView.this;
			if (getParent() instanceof ViewGroup) {
				((ViewGroup) getParent()).removeView(self);
			}
			dialog = new AlertDialog.Builder(getContext()) // TODO do we need to use an app compat theme to ensure this looks right?
					.setView(self)
					.setCancelable(cancelable)
					.show();
			dialog.setOnDismissListener(d -> {
				dialog = null;
				if (onDismissListener != null) {
					onDismissListener.onDismiss(d);
				}
			});
			return this;
		}
		
	}
	
}
