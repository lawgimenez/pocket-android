package com.pocket.app.tags;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.ideashower.readitlater.R;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.sync.value.Parceller;
import com.pocket.ui.view.notification.PktSnackbar;
import com.pocket.util.android.fragment.FragmentUtil;
import com.pocket.util.android.fragment.FragmentUtil.FragmentLaunchMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A thin wrapper Activity that shows a full screen {@link ItemsTaggingFragment}. See that class for more details and usage.
 */
public class ItemsTaggingActivity extends AbsPocketActivity {

	private static final String FRAGMENT_MAIN = "main";
	private static final String EXTRA_IS_STANDALONE = "isStandAlone";
	
	/**
	 * Start a new Activity.
	 * @param standalone true to show in standalone mode
	 * @see ItemsTaggingFragment#newInstance(List, boolean, List)} for info on other params.
	 */
	public static void startActivity(Context context, boolean standalone, List<Item> items, boolean addOnly, List<ActionContext> cxts) {
		context.startActivity(newStartIntent(context, standalone, items, addOnly, cxts));
	}
	
	public static Intent newStartIntent(Context context, boolean isStandalone, Item item, ActionContext cxt) {
		return newStartIntent(context, isStandalone, Collections.singletonList(item), false, Collections.singletonList(cxt));
	}
	
	/**
	 * Create a new intent that will start this Activity.
	 * @see #startActivity(Context, boolean, List, boolean, List) for details on params
	 */
	public static Intent newStartIntent(Context context, boolean isStandalone, List<Item> items, boolean addOnly, List<ActionContext> cxts) {
		Intent intent;
		if (isStandalone) {
			intent = new Intent(context, StandaloneItemsTaggingActivity.class);
		} else {
			intent = new Intent(context, ItemsTaggingActivity.class);
		}
		Parceller.put(intent, ItemsTaggingFragment.ARG_ITEMS, ItemsTaggingFragment.copyAndReduceItemsForParcel(items));
		if (cxts != null) {
			Parceller.put(intent, ItemsTaggingFragment.ARG_UI_CONTEXTS, new ArrayList<>(cxts));
		}
		intent.putExtra(ItemsTaggingFragment.ARG_ADD_ONLY, addOnly);
		intent.putExtra(EXTRA_IS_STANDALONE, isStandalone);
		return intent;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null) {	
			// New instance
			Fragment fragment = ItemsTaggingFragment.newInstance(
					Parceller.getList(getIntent(), ItemsTaggingFragment.ARG_ITEMS, Item.JSON_CREATOR),
					getIntent().getBooleanExtra(ItemsTaggingFragment.ARG_ADD_ONLY, false),
					Parceller.getList(getIntent(), ItemsTaggingFragment.ARG_UI_CONTEXTS, ActionContext.JSON_CREATOR));
			
			if (ItemsTaggingFragment.getLaunchMode(this) == FragmentLaunchMode.ACTIVITY) {
				setContentFragment(fragment, FRAGMENT_MAIN);
			} else {
				FragmentUtil.addFragmentAsDialog((DialogFragment) fragment, this, FRAGMENT_MAIN);
			}
		} else {
			// Fragment is restored
		}
	}
	
	@Override
	public CxtView getActionViewName() {
		return CxtView.EDIT_TAGS;
	}

	@Override
	protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.REQUIRES_LOGIN;
	}

	@Override
	protected void onClipboardUrlPromptViewLayout(PktSnackbar view) {
		final AbsPocketFragment frag = ((AbsPocketFragment) getPocketFragmentManager().findFragmentByTag(FRAGMENT_MAIN));
		if (frag != null) {
			setClipboardUrlAnchor(view, frag.findViewById(R.id.save));
		}
	}
	
}
