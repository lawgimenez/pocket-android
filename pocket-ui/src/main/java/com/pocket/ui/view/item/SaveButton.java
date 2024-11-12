package com.pocket.ui.view.item;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.pocket.ui.R;
import com.pocket.ui.util.CheckableHelper;
import com.pocket.ui.view.checkable.CheckableConstraintLayout;
import com.pocket.ui.view.themed.ThemedTextView;

import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class SaveButton extends CheckableConstraintLayout {

	private final Binder binder = new Binder(this);
	private CheckableHelper.OnCheckedChangeListener checkedListener;
	
	private ThemedTextView label;
	
	public SaveButton(Context context) {
		super(context);
		init();
	}

	public SaveButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SaveButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_save, this, true);
		findViewById(R.id.save_icon).setLongClickable(false); // By default, IconButton's have tooltips on long press, we don't want this icon to take touches.
		label = findViewById(R.id.save_label);
		setCheckable(true);
		setBackgroundResource(R.drawable.pkt_ripple_borderless);
		checkedListener = (view, isChecked) -> {
			if (isChecked != binder.listener.onSaveButtonClicked(SaveButton.this, isChecked)) {
				bind().setSaved(!isChecked);
				return; // Don't change.
			}
			updateSaveLabel();
		};
		setOnCheckedChangeListener(checkedListener);
		bind().clear();
		engageable.setUiEntityType(Type.BUTTON);
	}
	
	private void updateSaveLabel() {
		label.setTextAndUpdateEnUsLabel(isChecked() ? R.string.ic_saved : R.string.ic_save);
		setContentDescription(label.getText());
	}

	@Nullable @Override public String getUiEntityLabel() {
		return label.getUiEntityLabel();
	}

	public Binder bind() {
		return binder;
	}
	
	public static class Binder {
		
		private static final OnSaveButtonClickListener NO_OP_LISTENER = (view, saved) -> saved;
		
		private final SaveButton view;
		private OnSaveButtonClickListener listener = NO_OP_LISTENER;
		
		public Binder(SaveButton view) {
			this.view = view;
		}
		
		public Binder clear() {
			label(true);
			setSaved(false);
			setOnSaveButtonClickListener(null);
			return this;
		}

		public Binder label(boolean visible) {
			view.label.setVisibility(visible ? View.VISIBLE : View.GONE);
			return this;
		}
		
		public Binder setSaved(boolean isSaved) {
			view.setOnCheckedChangeListener(null); // Clear the listener while changing bindings, so it only triggers from actual clicks.
			view.setChecked(isSaved);
			view.setOnCheckedChangeListener(view.checkedListener);
			view.updateSaveLabel();
			return this;
		}
		
		public Binder setOnSaveButtonClickListener(OnSaveButtonClickListener listener) {
			this.listener = listener != null ? listener : NO_OP_LISTENER;
			return this;
		}
		
		public interface OnSaveButtonClickListener {
			/**
			 * @param saved true if the button was clicked and wants to move to the saved state. false if wants to become not saved.
			 * @return the state the button should be in
			 */
			boolean onSaveButtonClicked(SaveButton view, boolean saved);
		}
		
	}
	
	
}
