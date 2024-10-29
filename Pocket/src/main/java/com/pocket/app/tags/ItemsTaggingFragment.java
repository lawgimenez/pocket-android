package com.pocket.app.tags;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ideashower.readitlater.R;
import com.pocket.app.AbsRecyclerViewViewAdapter;
import com.pocket.app.tags.editor.AddNewTagModule;
import com.pocket.app.tags.editor.EmptyTagListModule;
import com.pocket.app.tags.editor.RecentTagsModule;
import com.pocket.app.tags.editor.SuggestedTagsModule;
import com.pocket.app.tags.editor.TagEditTextModule;
import com.pocket.app.tags.editor.TagListModule;
import com.pocket.app.tags.editor.TagModule;
import com.pocket.app.tags.editor.TagModuleManager;
import com.pocket.app.tags.editor.TagModuleManager.TagModuleManagerListener;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.Tag;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.sync.value.Parceller;
import com.pocket.ui.text.TextViewUtil;
import com.pocket.ui.view.AppBar;
import com.pocket.ui.view.button.ButtonBoxDrawable;
import com.pocket.ui.view.menu.SectionHeaderView;
import com.pocket.ui.view.notification.PktSnackbar;
import com.pocket.ui.view.progress.RainbowProgressCircleView;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.SimpleTextWatcher;
import com.pocket.util.android.ViewUtil;
import com.pocket.util.android.fragment.FragmentUtil;
import com.pocket.util.android.fragment.FragmentUtil.FragmentLaunchMode;
import com.pocket.util.android.view.ViewEnabledManager;
import com.pocket.util.android.view.ViewVisibleManager;
import com.pocket.util.android.view.ViewVisibleManager.ViewVisibilityCondition;
import com.pocket.util.android.view.chip.ChipEditText;
import com.pocket.util.java.Safe;
import com.squareup.phrase.Phrase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An fragment that helps the user edit the tags for one or many items.
 * <p>
 * If only one item is passed, it will edit the tags for that item, loading in those items existing tags and allowing you to add or remove them.
 * <p>
 * If you pass in multiple items, it will allow you to only add tags to all of those items. Any tags selected will be added to all of those items.
 * It won't affect any tags those items already have, it will just add any new selections to them.
 * <p>
 * Can be shown as a DialogFragment or as an Activity.
 * <ul>
 * <li><b>Recommended</b>To automatically use the correct {@link FragmentLaunchMode}, use invoke {@link #show(FragmentActivity, List, boolean, List)} </li>
 * <li>To create a new fragment, use {@link #newInstance(List, boolean, List)}
 * <li>To launch as an Activity use {@link ItemsTaggingActivity#startActivity(Context, boolean, List, boolean, List)}</li>
 * </ul>
 * 
 * <h2>Implementation Notes</h2>
 * This class sets up all of the UI but the heavy lifting is done by classes in the com.pocket.app.tags.editor package.
 * Specifically the {@link TagModuleManager}. See that classes' documentation for more details.
 */
public class ItemsTaggingFragment extends AbsPocketFragment {
	
	static final String ARG_ITEMS = "items";
	static final String ARG_UI_CONTEXTS = "ui_contexts";
	static final String ARG_ADD_ONLY = "add_only";

	public static FragmentLaunchMode getLaunchMode(Activity activity) {
		if (FormFactor.showSecondaryScreensInDialogs(activity)) {
			return FragmentLaunchMode.DIALOG;
		} else {
			return FragmentLaunchMode.ACTIVITY;
		}
	}
	
	/**
	 * @param items One or many items. Will use {@link ItemMode#MUTLI_ITEM} if there are more than one items. See the {@link ItemsTaggingFragment} docs for details.
	 * @param addOnly true to force {@link ItemMode#MUTLI_ITEM} even if there is one only item. false to leave as default based on how many items there are.
	 * @param cxts A context for each item
	 */
	public static ItemsTaggingFragment newInstance(List<Item> items, boolean addOnly, List<ActionContext> cxts) {
		ItemsTaggingFragment frag = new ItemsTaggingFragment();
		Bundle args = new Bundle();
		Parceller.put(args, ARG_ITEMS, copyAndReduceItemsForParcel(items));
		Parceller.put(args, ARG_UI_CONTEXTS, cxts);
		args.putBoolean(ARG_ADD_ONLY, addOnly);
		frag.setArguments(args);
		return frag;
	}

	/**
	 * Reduces the list of items to only their identities to minimize the amount of data being sent in the parcel
	 * and avoiding android.os.TransactionTooLargeException
	 *
	 * Technically this could still happen. If it does and is a problem, we'll need to look at an alternative to passing large amounts of state between activities,
	 * perhaps just using Pocket's state and syncing/remembering something.
	 * @return a copy of the list, where all but the first item have been reduced to only their identity.
	 */
	static ArrayList<Item> copyAndReduceItemsForParcel(List<Item> items) {
		ArrayList<Item> copy = new ArrayList<>(items.size());
		for (Item item : items) {
			copy.add(item.identity());
		}
		// Keep the first one with its full state, since it might be used to source the already set tags if it isn't known locally.
		if (!items.isEmpty()) copy.set(0, items.get(0));
		return copy;
	}
	
	public static void show(FragmentActivity activity, Item item, ActionContext cxt) {
		show(activity, Arrays.asList(item), false, Arrays.asList(cxt));
	}
	
	/**
	 * Show the fragment in whatever launch mode is default for this device.
	 * See {@link #newInstance(List, boolean, List)} for param info.
	 */
	public static void show(FragmentActivity activity, List<Item> items, boolean addOnly, List<ActionContext> cxts) {
		if (getLaunchMode(activity) == FragmentLaunchMode.DIALOG) {
			ItemsTaggingFragment frag = newInstance(items, addOnly, cxts);
			FragmentUtil.addFragmentAsDialog(frag, activity);
		} else {
			ItemsTaggingActivity.startActivity(activity, false, items, addOnly, cxts);
		}
	}
	
	private enum ItemMode {
		/** Editing the tags for a single tag. Saving will replace the item's tags with the tags in the field. */
		SINGLE_ITEM,
		/** Bulk adding tags to multiple items. Saving will add any tags in the field to each item. */
		MUTLI_ITEM
	}
	
	private ItemMode itemMode;
	/** NOTE: Do not trust the state of these, since they came via parcel and may only have their identities or out of date state. */
	private List<Item> items;
	
	private ChipEditText tagsEditText;
	private RecyclerView recyclerView;
	private LinearLayoutManager layoutManager;
	private ViewGroup contentViewGroup;
	private RainbowProgressCircleView progress;
	private View saveButton;

	private TagModuleManager tagModuleManager;
	private boolean isLoading;
	private ViewEnabledManager saveButtonEnabledManager;
	private boolean isSaved;
	private SuggestedTagsModule suggestedTagsModule;
	private TagListModule allTagsModule;

	// the index of the tags inserted into the main recyclerview, which is just below the "All Tags" header
	// since tags are added asynchronously from the modules below them, and can also be removed / inserted later on
	private int tagsIndex;

	// we keep track of how many tags were inserted into the recyclerview in order to
	// remove / update them during auto-completing while typing
	private int tagsCount = 0;

	@Override
	public CxtView getActionViewName() {
		return CxtView.EDIT_TAGS;
	}
	
	@Nullable @Override public UiEntityIdentifier getScreenIdentifier() {
		return UiEntityIdentifier.TAGS_EDIT;
	}
	
	@Override
	protected View onCreateViewImpl(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_item_tagging, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		items = Parceller.getList(getArguments(), ARG_ITEMS, Item.JSON_CREATOR);
		itemMode = items.size() == 1 && !getArguments().getBoolean(ARG_ADD_ONLY) ? ItemMode.SINGLE_ITEM : ItemMode.MUTLI_ITEM;
		
		contentViewGroup = findViewById(R.id.content);
		progress = findViewById(R.id.progress);
		tagsEditText = findViewById(R.id.edit_tags);
		recyclerView = findViewById(R.id.list);
		saveButton = findViewById(R.id.save);
		AppBar appBar = findViewById(R.id.appbar);
		appBar.bind().title(R.string.nm_add_tags).onLeftIconClick(v -> finish());
		saveButton.setOnClickListener(v -> saveAndFinish());

		final NestedScrollView tagsContainer = findViewById(R.id.edit_tags_container);
		tagsContainer.setBackground(new ButtonBoxDrawable(getContext(), com.pocket.ui.R.color.pkt_bg, com.pocket.ui.R.color.pkt_focusable_grey_4));
		tagsEditText.addTextWatcher(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				super.onTextChanged(s, start, before, count);
				tagsContainer.fullScroll(View.FOCUS_DOWN);
			}
		});
		tagsEditText.setFilters(new InputFilter[]{new TextViewUtil.AllLowerCase()});
		tagsEditText.setBackground(null);
		
		// Create the Manager that does all of the heavy lifting in this fragment
		tagModuleManager = new TagModuleManager(pocket(), new TagModuleManagerListener() {
			
			@Override
			public void onCancelError() {
				PktSnackbar.dismissCurrent();
			}
			
			@Override
			public void onError(String string) {
				PktSnackbar.make(getActivity(), PktSnackbar.Type.DEFAULT_DISMISSABLE, saveButton, string, null).show();
			}
			
			@Override
			public void onModified() {
				if (saveButtonEnabledManager != null) {
					saveButtonEnabledManager.invalidate();
				}
			}
			
			@Override
			public void onTagModulesLoadingStateChanged(boolean isLoading) {
				setLoading(isLoading);
			}
			
		}, savedInstanceState);

		recyclerView.setItemAnimator(null); // default add / remove animations look janky with all the modules, so removing for now
		layoutManager = new LinearLayoutManager(getContext());
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setAdapter(onCreateModules());

		// Setup the manager for when the Save button is enabled.
		saveButtonEnabledManager = new ViewEnabledManager(saveButton);
		saveButtonEnabledManager.addCondition(tagModuleManager);
		saveButtonEnabledManager.addCondition(() -> !isLoading);
		
		// Start the loading process.
		// First find what tags we'll pre select, if any
		setLoading(true);
		List<String> selected = new ArrayList<>();
		if (savedInstanceState != null) {
			// Restore selections
			selected.addAll(savedInstanceState.getStringArrayList(TagModuleManager.STATE_CURR_TAG_LIST));
			tagModuleManager.load(selected);
			
		} else if (itemMode == ItemMode.SINGLE_ITEM) {
			// Show this items tags as selected
			pocket().syncLocal(items.get(0))
					.onSuccess(item -> {
						item = item != null ? item : items.get(0);
						if (item.tags != null) {
							for (Tag t : item.tags) {
								selected.add(t.tag);
							}
						}
					})
					// This code was adopted from something that didn't handle a failure case here, so didn't implement one in this new version either.
					.onComplete(() -> tagModuleManager.load(selected));
			
		} else {
			tagModuleManager.load(selected);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		tagModuleManager.onDestroy();
	}
	
	/**
	 * Initializes all of the {@link TagModule}s, adds them to {@link TagModuleManager} and to an adapter.
	 * @return The adapter for the listview to use.
	 */
	private ModuleAdapterView onCreateModules() {
		//  Modules that are not displayed within the ListView.
		addChipEditTextModule();
		
		// Modules that are part of the ListView
		final ModuleAdapterView adapter = new ModuleAdapterView();
		addSuggestedTagsModule(adapter);
		addRecentTagsModule(adapter);
		addTagListModule(adapter);
		addEmptyListModule(adapter);
		addNewTagModule(adapter);
		return adapter;
	}
	
	/** Used by {@link #onCreateModules()} for setup */
	private void addChipEditTextModule() {
		TagEditTextModule editTextModule = new TagEditTextModule(tagModuleManager, (module, visible) -> ViewUtil.setVisible(tagsEditText, visible), tagsEditText);
		tagModuleManager.addTagModule(editTextModule);
	}
		
	/** Used by {@link #onCreateModules()} for setup */
	private void addSuggestedTagsModule(final ModuleAdapterView adapter) {
		if (itemMode != ItemMode.SINGLE_ITEM) {
			return; // Tags are not suggested during bulk edit.
		}
		
		suggestedTagsModule = new SuggestedTagsModule(items.get(0).id_url.url, tagModuleManager, (module, visible) -> adapter.setActive(module.getView(), visible), getContext());
		tagModuleManager.addTagModule(suggestedTagsModule);
		adapter.addView(suggestedTagsModule.getView());
		suggestedTagsModule.invalidateVisibility();
	}

	private void addRecentTagsModule(ModuleAdapterView adapter) {
		var recentTagsModule = new RecentTagsModule(
				pocket(),
				tagModuleManager,
				(module, visible) -> adapter.setActive(module.getView(), visible),
				getContext()
		);
		tagModuleManager.addTagModule(recentTagsModule);
		adapter.addView(recentTagsModule.getView());
	}

	/** Used by {@link #onCreateModules()} for setup */
	private void addTagListModule(final ModuleAdapterView adapter) {
		/*
		 * The logic to handle all of these cases is fairly complex. To help future explorers of this
		 * method there are a lot of comments to err on the side of over explaining it. 
		 * 
		 * There are two copies of the header. One is inside of the MergeAdapter so it can scroll with the list.
		 * The other is fixed above the ListView. 
		 * 
		 * If the one inside of the MergeAdapter is scrolled to a position above the top of the ListView, so that it
		 * is no longer visible the fixed copy appears. This gives the user the appearance that the header docks when
		 * it gets to the top.
		 * 
		 * This is complicated by the fact that several views within the MergeAdapter, above the header can show and hide at
		 * different times. For example, the suggested tags component might be up there.
		 */

		// Create the header views
		final SectionHeaderView headerInList = new SectionHeaderView(getContext());
		headerInList.bind().clear().label(R.string.lb_all_tags);
		headerInList.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		final SectionHeaderView headerFixed = findViewById(R.id.header_fixed);

		// Create a manager to handle the different things that effect when the fixed one should be visible.
		final ViewVisibleManager fixedHeaderVisibility = new ViewVisibleManager(headerFixed, View.GONE);
		
		// Create the actual module
		SectionHeaderView[] headers = new SectionHeaderView[]{headerInList, headerFixed};
		allTagsModule = new TagListModule(tagModuleManager, (module, visible) -> {
			adapter.setTags(allTagsModule.getDisplayList());
			adapter.setActive(headerInList, visible);
			fixedHeaderVisibility.invalidate();
		}, getActivity(), headers);
		
		// Set the various conditions for when the fixed header is visible or not.
		fixedHeaderVisibility.addCondition(new ViewVisibilityCondition() {
			
			@Override
			public boolean isVisible() {
				boolean visible = shouldBeVisible();
				headerInList.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);

				return visible;
			}
			
			private boolean shouldBeVisible() {
				// Determine if the header has been scrolled away and if so, the fixed header should be visible
				return layoutManager.findFirstCompletelyVisibleItemPosition() >= tagsIndex;
			}
		});
		fixedHeaderVisibility.addCondition(() -> {
			// If the all tags module is hidden, the fixed header should be as well
			return allTagsModule.isPreferredVisible();
		});
		recyclerView.getViewTreeObserver().addOnScrollChangedListener(() -> fixedHeaderVisibility.invalidate());

		adapter.addView(headerInList);

		// Add everything to manager and adapter.
		tagModuleManager.addTagModule(allTagsModule);
		tagsIndex = adapter.getItemCount(); // this is where the tags will be inserted when they're ready
	}
		
	/** Used by {@link #onCreateModules()} for setup */
	private void addNewTagModule(final ModuleAdapterView adapter) {
		// Add Tag from Auto Complete
		AddNewTagModule addTagModule = new AddNewTagModule(tagModuleManager, (module, visible) -> adapter.setActive(module.getView(), visible), getContext());
		tagModuleManager.addTagModule(addTagModule);
		adapter.addView(addTagModule.getView());
	}
	
	/** Used by {@link #onCreateModules()} for setup */
	private void addEmptyListModule(final ModuleAdapterView adapter) {
		// Empty List Module
		EmptyTagListModule emptyModule = new EmptyTagListModule(tagModuleManager, (module, visible) -> adapter.setActive(module.getView(), visible), getContext());
		tagModuleManager.addTagModule(emptyModule);
		adapter.addView(emptyModule.getView());
	}

	/**
	 * Save the changes made. If single item mode, the tags on that item will be replaced with the currently selected tags.
	 * If multi-item mode, all of the selected tags will be added.
	 * <p>
	 * This will also finish the Activity or dismiss the DialogFragment and Toast a confirmation of the changes to the user.
	 * <p>
	 * If there is an error, and changes cannot be saved, an error will be shown and the save and finish will be canceled.
	 */
	private void saveAndFinish() {
		if (tagModuleManager.isModified()) {
			if (!tagsEditText.commitPending()) {
				return; // An error will show
			}
			
			ArrayList<String> tags = tagModuleManager.getListForReadOnly();
			Collections.sort(tags);
			
			if (itemMode == ItemMode.SINGLE_ITEM) {
				
				// Hack
				// Get the original UiContext to extract the UiTrigger
				List<ActionContext> list = Parceller.getList(getArguments(), ARG_UI_CONTEXTS, ActionContext.JSON_CREATOR);
				ActionContext ctx = null;
				if (list != null && !list.isEmpty()) {
					ctx = list.get(0);
				}
				ActionContext originalcxt = ctx;

				// Pass the UiTrigger from the original UiContext to the
				Interaction it = Interaction.on(getContext()).merge(cxt -> cxt
					.cxt_view(CxtView.ADD_TAGS)
					.cxt_ui(Safe.get(() -> originalcxt.cxt_ui))
					.cxt_tags_cnt(tagModuleManager.getContextOpenTagsCount())
					.cxt_suggested_available(suggestedTagsModule.getSuggestTagsInitialCount())
					.cxt_enter_cnt(tagModuleManager.getContextEnterTagsCount())
					.cxt_suggested_cnt(tagModuleManager.getContextSuggestedAddTagsCount())
					.cxt_tap_cnt(tagModuleManager.getContextTapTagsCount())
					.cxt_remove_cnt(tagModuleManager.getContextRemovedTagsCount()));
				
				pocket().sync(null, pocket().spec().actions().tags_replace()
						.time(it.time)
						.context(it.context)
						.url(items.get(0).id_url)
						.tags(new ArrayList<>(tags))
						.build());
				
				Toast.makeText(getActivity(), getString(R.string.ts_tags_changes_saved) , Toast.LENGTH_SHORT)
					.show();
				
			} else if (itemMode == ItemMode.MUTLI_ITEM) {
				for (Item item : items) {
					pocket().sync(null, pocket().spec().actions().tags_add()
							.url(item.id_url)
							.tags(tags)
							.time(Timestamp.now())
							.build());
				}
				
				final CharSequence message = Phrase.from(getString(R.string.ts_bulk_edit_tagged))
						.put("tags", getResources().getQuantityString(R.plurals.ts_bulk_edit_tagged_tags, tags.size(), tags.size()))
						.put("items", getResources().getQuantityString(R.plurals.ts_bulk_edit_tagged_items, items.size(), items.size()))
						.format();
				
				Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
			}
		}
		isSaved = true;
		finish();
	}
	
	@Override
	public void finish() {
		if (confirmSaveChanges()) {
			// Cancel finish
		} else {
			super.finish();

			if (getActivity() instanceof StandaloneItemsTaggingActivity) {
				getActivity().finish();
			}
		}
		
	}
	
	@Override
	public boolean onBackPressed() {
		if (confirmSaveChanges()) {
			return true;
		} else {
			return super.onBackPressed();
		}
	}
	
	/**
	 * Check if there are unsaved changes, and if so, prompt the user.
	 * @return true if there were unsaved changes and the user was prompted, false otherwise.
	 */
	private boolean confirmSaveChanges() {
		if (isSaved || !tagModuleManager.isModified()) {
			return false;
		}
		
		new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dg_changes_not_saved_t)
			.setMessage(R.string.dg_changes_not_saved_tags_m)
			.setPositiveButton(R.string.ac_discard_changes, (dialog, which) -> {
				isSaved = true;
				finish();
			})
			.setNegativeButton(R.string.ac_continue_editing, null)
			.show();
		return true;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		tagModuleManager.onSaveInstanceState(outState);
	}
	
	/**
	 * Toggle the loading UI. While loading, no UI can be interacted with and a loading indicator is shown.
	 * @param isLoading
	 */
	private void setLoading(boolean isLoading) {
		this.isLoading = isLoading;
		if (isLoading) {
			progress.setVisibility(View.VISIBLE);
			contentViewGroup.setVisibility(View.INVISIBLE);
		} else {
			progress.setVisibility(View.GONE);
			contentViewGroup.setVisibility(View.VISIBLE);
		}
		saveButtonEnabledManager.invalidate();
	}	
	
	private class ModuleAdapterView extends AbsRecyclerViewViewAdapter {

		private static final int VIEW_TYPE_TAG = 1;

		// we maintain a separate list for access to show / hide the module views
		private List<View> views = new ArrayList<>();

        /**
         * A wrapper class for tags, for use with {@link AbsRecyclerViewViewAdapter}
         */
		class TagRow extends Row {

			String tag;

			TagRow(String tag) {
				this.tag = tag;
			}

			@Override
			public int getViewType() {
				return VIEW_TYPE_TAG;
			}
		}

		class ViewHolderTag extends ViewHolder<TagRow> {

			final TextView textView;

			ViewHolderTag(View v) {
				super(v);
				textView = v.findViewById(R.id.text);
			}

			@Override
			public void bind(TagRow row) {
				textView.setText(row.tag);
				itemView.setTag(row.tag);
			}
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == VIEW_TYPE_TAG) {
				View view = LayoutInflater.from(getContext()).inflate(R.layout.view_simple_title_divider_row, parent, false);
				view.setOnClickListener(allTagsModule);
				return new ViewHolderTag(view);
			} else {
				return super.onCreateViewHolder(parent, viewType);
			}
		}

		@Override
		public void addView(View moduleView) {
			views.add(moduleView);
			super.addView(moduleView);
		}

		void setTags(List<String> tags) {

			removeItems(tagsIndex, tagsCount); // remove all the previous tags
			tagsCount = tags.size();

			List<Row> tagRows = new ArrayList<>(tags.size());
			for (String tag : tags) {
				tagRows.add(new TagRow(tag));
			}
			addItems(tagsIndex, tagRows);

			notifyDataSetChanged();
		}

		void setActive(View view, boolean visible) {
			int index = views.indexOf(view);
			if (index >= 0) {
				View v = views.get(index);
				if (v != null) {
					v.setVisibility(visible ? View.VISIBLE : View.GONE);
					// setting a view GONE in a recyclerview still shows its dead space, so we need to set layoutparams as well
					ViewGroup.LayoutParams params = v.getLayoutParams();
					params.height = visible ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
					v.setLayoutParams(params);
				}
			}
		}
	}
	
}
