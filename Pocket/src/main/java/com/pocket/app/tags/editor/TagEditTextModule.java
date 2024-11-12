package com.pocket.app.tags.editor;

import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ideashower.readitlater.R;
import com.pocket.app.tags.ItemsTaggingFragment;
import com.pocket.util.android.SimpleTextWatcher;
import com.pocket.util.android.view.chip.ChipEditText;
import com.pocket.util.android.view.chip.ChipEditText.ChipInputCommitListener;
import com.pocket.util.android.view.chip.ChipLayout.ChipViewCreator;

import java.util.ArrayList;

/**
 * A tagging module that controls {@link ItemsTaggingFragment#mEditText}.
 * 
 * @see ItemsTaggingFragment This is the entry point for all related code. See the documentation on this class for the big picture.
 * @see TagModule
 * @see TagModuleManager
 */
public class TagEditTextModule extends TagModule {
	
	private final ChipEditText mEditText;

	public TagEditTextModule(TagModuleManager manager, VisibilityListener visListener, ChipEditText editText) {
		super(manager, visListener, editText.getContext());
		
		mEditText = editText;
		mEditText.setMimicChipAdapterStyleEnabled(true);
		mEditText.defaultAdapter();
		mEditText.addTextWatcher(new SimpleTextWatcher() {
			
			@Override
			public void afterTextChanged(Editable s) {
				CharSequence text; // Don't pass the Editable along directly to avoid listeners accidently changing it.
				if (s.length() == 0) {
					text = "";
				} else {
					text = s.subSequence(0, s.length());
				}
				getManager().onTagInputTextChanged(TagEditTextModule.this, text);
			}
			
		});
		mEditText.setValidator(getManager().getTagValidator());
		mEditText.setOnChipsChangedListener(new ChipInputCommitListener() {
			
			@Override
			public void onChipError(String error) {
				getManager().setError(error);
			}
			
			@Override
			public void onChipDeleted(CharSequence text) {
				getManager().removeTag(TagEditTextModule.this, text.toString());
			}
			
			@Override
			public void onChipCommitted(CharSequence text) {
				getManager().addTag(TagEditTextModule.this, text.toString());
			}
		});
	}
	
	@Override
	public View getView() {
		return mEditText;
	}
	
	@Override
	public void load(final TagModuleLoadedListener callback) {
		callback.onTagModuleLoaded();
	}
	
	@Override
	protected void onTagsLoaded(ArrayList<String> allTags) {
		if (allTags.isEmpty()) {
			mEditText.setHint(getContext().getString(R.string.lb_select_tags_hint_no_tags));
		}
	}

	@Override
	public void onTagInputTextChanged(CharSequence text) {}

	@Override
	public void onTagAdded(String tag) {
		mEditText.clearText();
		mEditText.addChip(tag);
	}
	
	@Override
	public void onTagRemoved(String tag) {
		mEditText.removeChip(tag);
	}

}