package com.pocket.app.listen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.appcompat.content.res.AppCompatResources;

import com.ideashower.readitlater.R;
import com.ideashower.readitlater.databinding.ViewListenPlayerBinding;
import com.pocket.app.App;
import com.pocket.data.models.CollectionKt;
import com.pocket.sdk.tts.Track;
import com.pocket.util.DisplayUtil;
import com.pocket.sdk.api.generated.enums.CxtUi;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.tts.Controls;
import com.pocket.sdk.tts.Listen;
import com.pocket.sdk.tts.ListenState;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.sdk2.analytics.context.Contextual;
import com.pocket.sdk2.analytics.context.ContextualRoot;
import com.pocket.ui.view.bottom.BottomSheetBackgroundDrawable;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;
import com.pocket.util.android.ViewUtil;

import org.threeten.bp.Duration;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/** The big full player component visible above the playlist in expanded Listen drawer. */
public final class ListenPlayerView extends VisualMarginConstraintLayout implements Contextual {
	private final ViewListenPlayerBinding views;
	private final int maxProgress;
	private final Controls controls;
	private final Controls scrubberControls;
	private final Controls coverflowControls;
	private final NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
	
	private ListenState state;
	
	public ListenPlayerView(Context context) {
		this(context, null);
	}
	
	public ListenPlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setBackground(new BottomSheetBackgroundDrawable(getContext()));
		setClipChildren(false);
		views = ViewListenPlayerBinding.inflate(LayoutInflater.from(context), this);
		maxProgress = getResources().getInteger(R.integer.listen_max_progress);
		
		final ContextualRoot analytics = AbsPocketActivity.from(getContext());
		if (analytics != null) {
			analytics.bindViewContext(views.scrubber, () -> new ActionContext.Builder().cxt_ui(CxtUi.SCRUBBER).build());
		}
		
		final Listen listen = App.from(context).listen();
		controls = listen.trackedControls(this, null);
		coverflowControls = listen.trackedControls(views.coverflow, null);
		scrubberControls = listen.trackedControls(views.scrubber, null);
		
		views.listenSettings.setOnClickListener(v -> ListenSettingsFragment.show(AbsPocketActivity.from(context)));
		
		views.coverflow.setOnSnappedPositionChangedListener(position -> {
			final Runnable action;
			if (position == state.index) {
				// Already playing the correct track. Do nothing.
				action = null;
				
			} else if (position == state.index + 1) {
				action = coverflowControls::next;
				
			} else if (position == state.index - 1) {
				action = coverflowControls::previous;
				
			} else if (position >= 0) {
				// Shouldn't happen as we only allow swiping by a single item, but.. I guess handle gracefully.
				action = () -> coverflowControls.moveTo(position);
				
			} else {
				// Ignore invalid positions.
				action = null;
			}
			
			if (action != null) {
				// We shouldn't update the adapter from within a scroll callback (see RecyclerView.isComputingLayout())
				// so let's post it to postpone until after the scroll calculation finishes.
				post(action);
			}
		});
		
		// Have to set the thumb in code, so VectorDrawableCompat can handle it for older OS'es.
		views.scrubber.setThumb(AppCompatResources.getDrawable(context, R.drawable.listen_scrub_button));
		views.scrubber.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
			
			@Override public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override public void onStopTrackingTouch(SeekBar seekBar) {
				final long position = state.duration.toMillis() * seekBar.getProgress() / maxProgress;
				scrubberControls.seekTo(Duration.ofMillis(position));
			}
		});
		
		initSpeedButtonFormat();
	}
	
	private void initSpeedButtonFormat() {
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(1);
		if (format instanceof DecimalFormat) {
			((DecimalFormat) format).setPositiveSuffix("x");
		}
	}
	
	void bind(ListenState state, boolean shouldShowDegradedView) {
		this.state = state;
		
		views.coverflow.bind(state);
		
		if (state.current != null) {
			final Track track = state.current;
			views.listenHeadline.setText(track.displayTitle);
			if (track.authors.isEmpty()) {
				views.listenSubhead.setText(DisplayUtil.displayHost(track.displayUrl));
			} else {
				views.listenSubhead.setText(String.format(Locale.getDefault(),
						"%1$s · %2$s",
						DisplayUtil.displayHost(track.displayUrl),
						DisplayUtil.displayAuthors(track.authors)));
			}
		}
		
		final int whenNotGone = state.voice == null ? INVISIBLE : VISIBLE;
		if (shouldShowDegradedView) {
			ViewUtil.setVisibility(whenNotGone, views.listenProgress);
			ViewUtil.setVisible(false,
					views.listenCurrentTime,
					views.listenTimeLeft,
					views.scrubber);
		} else {
			ViewUtil.setVisibility(whenNotGone,
					views.listenCurrentTime,
					views.listenTimeLeft,
					views.scrubber);
			ViewUtil.setVisible(false, views.listenProgress);
		}
		
		if (state.duration.getSeconds() == 0) {
			views.listenCurrentTime.setText(null);
			views.listenTimeLeft.setText(null);
			views.scrubber.setEnabled(false);
			views.scrubber.setProgress(0);
			views.listenProgress.setProgress(0);
		} else {
			final long elapsed = state.elapsed.getSeconds();
			final long duration = state.duration.getSeconds();
			views.listenCurrentTime.setText(formatTime(elapsed));
			views.listenTimeLeft.setText(formatTime("-", Math.max(0, duration - elapsed)));
			
			views.scrubber.setEnabled(true);
			final int progress = (int) (elapsed * maxProgress / duration);
			views.scrubber.setProgress(progress);
			views.listenProgress.setProgress(progress);
		}
		final int secondaryProgress = (int) (state.bufferingProgress * maxProgress);
		views.scrubber.setSecondaryProgress(secondaryProgress);
		views.listenProgress.setSecondaryProgress(secondaryProgress);
		
		views.listenControls.bind(state, controls, shouldShowDegradedView);
	}
	
	private String formatTime(long totalSeconds) {
		return formatTime("", totalSeconds);
	}
	
	private String formatTime(String prefix, long totalSeconds) {
		final long minutes = totalSeconds / 60;
		final long seconds = totalSeconds % 60;
		return String.format(Locale.getDefault(), "%3$s%1$d:%2$02d", minutes, seconds, prefix);
	}
	
	void showDataAlert() {
		Tooltip.DefaultTheme.showButton(views.listenControls, R.string.listen_data_alert, null);
	}
	
	Tooltip.TooltipController showOfflineHint(FrameLayout frame) {
		return Tooltip.DefaultTheme.showButton(views.listenSettings, frame, R.string.listen_offline_hint, null);
	}
	
	void applyBottomSheetOffset(float slideOffset) {
		if (getY() != 0) return; // We're during a nested scroll after fully expanding the bottom sheet.
		
		// Fade from 100% to 0% between 0.5 and 0.25 offset.
		final float alpha = slideOffset * 4 - 1;
		views.handle.setAlpha(alpha);
		views.listenPlayingFrom.setAlpha(alpha);
		views.listenSettings.setAlpha(alpha);
	}
	
	int getStickyOffset() {
		return views.listenPlaylistTopDivider.getBottom();
	}
	
	@Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (top != 0) {
			onScrolled();
		}
	}
	
	@Override public void offsetTopAndBottom(int offset) {
		super.offsetTopAndBottom(offset);
		onScrolled();
	}
	
	private void onScrolled() {
		final float offset = -getY();
		translateStickyViews(offset);
		fadeNonStickyViews(offset);
	}
	
	private void translateStickyViews(float offset) {
		views.handle.setTranslationY(offset);
		
		final float labelsOffset = offset - views.coverflow.getBottom() + views.handle.getHeight();
		views.listenHeadline.setTranslationY(Math.max(0, labelsOffset));
		views.listenSubhead.setTranslationY(Math.max(0, labelsOffset));
		
		// The next thing that should stick are the controls, but ListenView will take care of that,
		// by showing the sticky view on top at the same time
	}
	
	private void fadeNonStickyViews(float offset) {
		final float playingFromAlpha = 1 - offset / views.listenPlayingFrom.getBottom();
		views.listenPlayingFrom.setAlpha(playingFromAlpha);
		views.listenSettings.setAlpha(playingFromAlpha);
		
		final float coverflowAlpha = 1 - offset / views.coverflow.getBottom();
		views.coverflow.setAlpha(coverflowAlpha);
		
		final int scrubberFadeStart = views.coverflow.getBottom() - views.handle.getHeight();
		final int scrubberFadeEnd = scrubberFadeStart + views.scrubber.getTop() - views.listenSubhead.getBottom();
		final float scrubberAlpha = 1 - (offset - scrubberFadeStart) / (scrubberFadeEnd - scrubberFadeStart);
		views.listenCurrentTime.setAlpha(scrubberAlpha);
		views.listenTimeLeft.setAlpha(scrubberAlpha);
		views.scrubber.setAlpha(scrubberAlpha);
		views.listenProgress.setAlpha(scrubberAlpha);
	}
	
	@Override public ActionContext getActionContext() {
		return new ActionContext.Builder().cxt_ui(CxtUi.PLAYER).build();
	}
}
