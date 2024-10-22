package com.pocket.app.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ideashower.readitlater.R;
import com.pocket.app.settings.view.preferences.CacheLimitPreferenceView;
import com.pocket.app.settings.view.preferences.Preference;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.sdk.util.ErrorReport;
import com.pocket.sdk.util.dialog.AlertMessaging;
import com.pocket.ui.view.AppBar;
import com.pocket.ui.view.empty.LoadableLayout;
import com.pocket.ui.view.menu.SectionHeaderView;
import com.pocket.ui.view.settings.SettingsImportantButton;
import com.pocket.ui.view.settings.SettingsSwitchView;
import com.pocket.util.java.UserFacingErrorMessage;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;

public abstract class AbsPrefsFragment extends AbsPocketFragment {

	protected ViewGroup layout;
	protected RecyclerView list;

	protected PrefAdapter prefAdapter;

	protected View bannerView;
	protected AppBar appbar;
	protected LoadableLayout loading;
	
	private final ArrayList<Preference> mPrefs = new ArrayList<>();
	private Disposable listener;
	
	@Override
	protected View onCreateViewImpl(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_settings, container, false);
	}
	
	@Override
	public void onViewCreatedImpl(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreatedImpl(view, savedInstanceState);
		
		layout = findViewById(R.id.rootView);

		appbar = findViewById(R.id.appbar);
		appbar.bind().title(getTitle() != 0 ? getString(getTitle()) : "").onLeftIconClick(v -> finish());

		loading = findViewById(R.id.loading);
		loading.bind().clear();

		prefAdapter = new PrefAdapter();

		list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
		list.setAdapter(prefAdapter);

		createPrefs(mPrefs);
    }
	
	@Override
	public void onStart() {
		super.onStart();
		listener = app().prefs().changes().subscribe(key -> onPreferenceChange(false));
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (listener != null) {
			listener.dispose();
			listener = null;
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		prefAdapter = null;
		layout = null;
		list = null;
		bannerView = null;
		appbar = null;
		loading = null;
	}

	protected void showProgress() {
		loading.bind().showProgressIndeterminate();
		list.setVisibility(View.GONE);
	}
	
	protected void hideProgress() {
		list.setVisibility(View.VISIBLE);
	}
	
	protected void showError(Throwable e, View.OnClickListener retry) {
		if (isDetachedOrFinishing()) return;
		String msg = StringUtils.defaultIfBlank(UserFacingErrorMessage.find(e), getString(R.string.dg_api_generic_error));
		loading.bind().showEmptyOrError().clear()
				.title(getResources().getText(R.string.dg_error_t))
				.message(msg)
				.errorButton(getResources().getText(R.string.ac_retry))
				.buttonOnClick(retry)
				.buttonOnLongClick(v -> {
					AlertMessaging.askIfTheyWantToReport(getAbsPocketActivity(), new ErrorReport(e, msg), null);
					return true;
				});
	}
	
	protected abstract int getTitle();

	protected abstract void createPrefs(ArrayList<Preference> prefs);
	
	@Nullable
	protected abstract View getBannerView();
	
	protected void rebuildPrefs() {
		if (isDetachedOrFinishing()) {
			return;
		}
		mPrefs.clear();
		createPrefs(mPrefs);
		prefAdapter.notifyDataSetChanged();
	}

	public class PrefAdapter extends RecyclerView.Adapter<PrefAdapter.ViewHolder> {

		private PrefAdapter() {
			setHasStableIds(true);
		}

		@Override
		public long getItemId(int position) {
			// allows for predictive animations and not messing with the toggle switch animation,
			// even though we're calling notifyDatasetChanged all the time.
			return mPrefs.get(position).hashCode();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
			public void bind(Preference pref) {
				View itemView = this.itemView;
				if (pref.isClickable()) {
					itemView.setOnClickListener(pref);
					itemView.setOnLongClickListener(pref);
					itemView.setClickable(true);
				} else {
					itemView.setOnClickListener(null);
					itemView.setOnLongClickListener(null);
					itemView.setClickable(false);
				}
				itemView.setEnabled(pref.isEnabled());
			}
		}

		@Override
		public PrefAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v;
			Preference.PrefViewType type = Preference.PrefViewType.values()[viewType];
			switch (type) {
				case BANNER:
					v = bannerView;
					break;
				case HEADER:
					v = new SectionHeaderView(getContext());
					break;
				case ACTION:
				case TOGGLE:
					v = new SettingsSwitchView(getContext());
					break;
				case CACHE_LIMIT:
					v = new CacheLimitPreferenceView(getContext());
					addOnFragmentDestoryListener(fragment -> ((CacheLimitPreferenceView)v).releaseResources());
					break;
				case IMPORTANT:
				default:
					v = new SettingsImportantButton(getContext());
					break;
			}
			v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			ViewHolder vh = new ViewHolder(v);
			return vh;
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {

			Preference pref = mPrefs.get(position);

			// just display the banner
			if (pref.getType() == Preference.PrefViewType.BANNER) {
				return;
			}

			pref.applyToView(holder.itemView);
			holder.bind(pref);
		}

		@Override
		public int getItemCount() {
			return mPrefs.size();
		}

        @Override
        public int getItemViewType(int position) {
            return mPrefs.get(position).getType().ordinal();
        }

		public void remove(Preference pref) {
			int index = mPrefs.indexOf(pref);
			mPrefs.remove(pref);
			prefAdapter.notifyItemRemoved(index);
		}
	}
	
	public void onPreferenceChange(boolean forceViewUpdate) {
		boolean changed = forceViewUpdate;
		for (Preference pref : mPrefs) {
			if (pref.update()) {
				changed = true;
			}
		}
		if (changed) {
			prefAdapter.notifyDataSetChanged();
		}
	}
	
	public void setHeaderVisibility(boolean visible) {
		if (bannerView == null) {
			bannerView = getBannerView();
			if (bannerView == null) {
				return;
			}
		}
		if (visible && mPrefs.get(0).getType() != Preference.PrefViewType.BANNER) {
			mPrefs.add(0, new Preference.SimplePreference(this) {
				@Override
				public PrefViewType getType() {
					return PrefViewType.BANNER;
				}
			});
			prefAdapter.notifyItemInserted(0);
		} else if (!visible && mPrefs.get(0).getType() == Preference.PrefViewType.BANNER) {
			mPrefs.remove(0);
			prefAdapter.notifyItemRemoved(0);
		}
	}

}

