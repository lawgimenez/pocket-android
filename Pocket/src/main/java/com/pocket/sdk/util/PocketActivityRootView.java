package com.pocket.sdk.util;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.RelativeLayout;

import com.ideashower.readitlater.R;
import com.pocket.app.listen.ListenView;
import com.pocket.app.settings.rotation.AndroidOSRotationLock;
import com.pocket.app.settings.rotation.PktFineOrientationManager;
import com.pocket.app.settings.rotation.RotationLockComponents;
import com.pocket.app.settings.rotation.interf.RotationLockView;
import com.pocket.sdk.tts.Listen;
import com.pocket.sdk.tts.ListenState;
import com.pocket.sdk.tts.PlayState;
import com.pocket.util.android.view.ResizeDetectRelativeLayout;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.subjects.PublishSubject;

/**
 * The root view of {@link AbsPocketActivity} that holds the content view, plus all of the
 * standard accessory views used on every screen of the app.
 * <p>
 * After inflating, call {@link #attach(AbsPocketActivity)} to connect the activity to this view.
 *
 * TODO move the other stubs like rotate lock etc into this place for management.
 */

public class PocketActivityRootView extends ResizeDetectRelativeLayout {
	
	private PocketActivityContentView content;
	private @Nullable ListenComponents listenComponent;
	private @Nullable RotationLockComponents rotationLockComponents;
	
	public PocketActivityRootView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}
	
	public PocketActivityRootView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public PocketActivityRootView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public PocketActivityRootView(Context context) {
		super(context);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.ril_root, this, true);
	}
	
	public void attach(AbsPocketActivity activity) {
		content = findViewById(R.id.content);
		if (activity.isListenUiEnabled()) {
			listenComponent = new ListenComponents(this, activity);
		}

		if (activity.supportsRotationLock()) {
			RotationLockView view = (RotationLockView) ((ViewStub) this.findViewById(R.id.stub_lock)).inflate();
			rotationLockComponents = new RotationLockComponents(activity, activity.app().prefs().ROTATION_LOCK, new AndroidOSRotationLock(activity, activity.app().rotationLock()), view, new PktFineOrientationManager(activity), activity.app().rotationLock());

			activity.addOnLifeCycleChangedListener(rotationLockComponents);
			activity.addOnConfigurationChangedListener(rotationLockComponents);
		}
	}
	
	public void setListenInsets(Rect insets) {
		if (listenComponent != null) {
			listenComponent.setInsets(insets);
		}
	}
	
	/** Set the bottom space needed to show the listen component. */
	void setListenSpacing(int height) {
		RelativeLayout.LayoutParams lp = (LayoutParams) content.getLayoutParams();
		lp.bottomMargin = height;
		content.setLayoutParams(lp);
	}
	
	public PocketActivityContentView getContentView() {
		return content;
	}
	
	/**
	 * Called when the activity has detected the user's press of the back key.
	 * @return true if handled the back press, false to let something else handle
	 */
	public boolean onBackPressed() {
		if (listenComponent != null && listenComponent.view != null && listenComponent.view.isExpanded()) {
			listenComponent.view.collapse();
			return true;
		}
		
		return false;
	}
	
	public void expandListen() {
		if (listenComponent != null) {
			listenComponent.expandView();
		}
	}
	
	public Observable<ListenView.State> getListenViewStates() {
		if (listenComponent != null) {
			return listenComponent.getListenViewStates();
		}
		
		return Observable.empty();
	}
	
	/**
	 * Manages the view stub and view changes related to showing and hiding the collapsing/expanding Media/Listen
	 * controls that are allowed to show on all screens.
	 */
	private static class ListenComponents extends AbsPocketActivity.SimpleOnLifeCycleChangedListener {
		
		private final Listen listen;
		private final PocketActivityRootView rootView;
		private final int listenSpacing;
		private final Rect insets = new Rect();
		
		private final PublishSubject<ListenView.State> playerStates = PublishSubject.create();
		
		private ListenView view;
		private boolean isActive;
		private boolean expand;
		private Disposable listenSubscription = Disposables.empty();
		
		ListenComponents(PocketActivityRootView rootView, AbsPocketActivity activity) {
			this.rootView = rootView;
			this.listen = activity.app().listen();
			listenSpacing = activity.getResources().getDimensionPixelSize(R.dimen.listen_mini_player_height);
			activity.addOnLifeCycleChangedListener(this);
		}
		
		@Override
		public void onActivityStart(AbsPocketActivity activity) {
			listenSubscription = listen.states().startWith(listen.state()).subscribe(this::setListenState);
		}
		
		@Override
		public void onActivityStop(AbsPocketActivity activity) {
			listenSubscription.dispose();
		}
		
		private void setListenState(ListenState state) {
			boolean active = state.playstate != PlayState.STOPPED;
			
			// Update View Visibility
			if (active != isActive) {
				isActive = active;
				if (active) {
					if (view == null) {
						view = (ListenView) ((ViewStub) rootView.findViewById(R.id.stub_listen)).inflate();
						view.getStates().subscribe(playerStates);
						setInsets(insets);
						if (expand) {
							expand = false;
							view.expand();
						}
					}
					if (view.getVisibility() != View.VISIBLE) {
						view.setVisibility(View.VISIBLE);
					}
					rootView.setListenSpacing(listenSpacing);
					
				} else if (view != null) {
					view.collapse();
					view.setVisibility(View.GONE);
					rootView.setListenSpacing(0);
				}
			}
			
			// Rebind as needed
			if (view != null) {
				if (isActive) {
					view.bind(state);
				} else {
					view.unbind();
				}
			}
		}
		
		void setInsets(Rect insets) {
			this.insets.set(insets);
			if (view != null) {
				view.setTranslationY(-insets.bottom);
				
				MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
				lp.topMargin = insets.top + insets.bottom;
				lp.leftMargin = insets.left;
				lp.rightMargin = insets.right;
				view.setLayoutParams(lp);
			}
		}
		
		void expandView() {
			if (view != null) {
				view.expand();
			} else {
				expand = true;
			}
		}
		
		PublishSubject<ListenView.State> getListenViewStates() {
			return playerStates;
		}
	}
	
	
}
