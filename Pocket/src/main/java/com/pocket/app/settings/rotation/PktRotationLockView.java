package com.pocket.app.settings.rotation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.settings.rotation.interf.RotationLockView;
import com.pocket.sdk.api.generated.action.Pv;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.button.ButtonBoxDrawable;
import com.pocket.ui.view.checkable.CheckableImageView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

public class PktRotationLockView extends FrameLayout implements RotationLockView {

    private static final int LOCK_SHOW_DURATION_MS = 4000;
    private static final int LOCK_FADEOUT_DURATION_MS = 620;

    private FadeOut fadeOut;
    private CheckableImageView toggle;
    private boolean fading = false;

    public PktRotationLockView(Context context) {
        super(context);
        init();
    }

    public PktRotationLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PktRotationLockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_rotation_lock, this, true);
        setOnTouchListener((v, event) -> {
            scheduleFadeOut(0);
            return false;
        });
        toggle = findViewById(R.id.rotation_lock_toggle);
        toggle.setCheckable(true);

        final Drawable bg = new ButtonBoxDrawable(getContext(), R.color.pkt_rotation_lock_bg, 0);
        bg.setAlpha((int)(0.8 * 255));
        toggle.setBackgroundDrawable(bg);

        final Drawable lockimage = AppCompatResources.getDrawable(getContext(), R.drawable.ic_rotation);
        DrawableCompat.setTintList(lockimage, NestedColorStateList.get(getContext(), R.color.pkt_rotation_lock));
        toggle.setImageDrawable(lockimage);
    }

    private void scheduleFadeOut(long delay) {
        if (!fading) {
            if (fadeOut == null) {
                fadeOut = new FadeOut();
            }
            removeCallbacks(fadeOut);
            postDelayed(fadeOut, delay);
        }
    }

    @Override
    public void setOnToggleClick(OnClick onclick) {
        toggle.setOnClickListener(v -> {
            onclick.onClick(toggle.isChecked());

            // orientation lock analytics event:
            // https://docs.google.com/spreadsheets/d/1ckENLwHRc2iIZMdUyM9rwsLc9p5ibRxPfWLRM9Rkt_U
            App.getApp().pocket().sync(null, new Pv.Builder()
                    .time(Timestamp.now())
                    .event_type(6)
                    .section(CxtSection.ROTATION_LOCK)
                    .event(toggle.isChecked() ? CxtEvent.ENABLED : CxtEvent.DISABLED)
                    .context(Interaction.on(toggle).context).build()
            );
        });
    }

    @Override
    public void show(boolean checked) {
        toggle.clearAnimation();
        setVisibility(View.VISIBLE);
        toggle.setEnabled(true);
        toggle.setOnCheckedChangeListener((view, isChecked) ->
                toggle.setUiEntityIdentifier(isChecked ? UiEntityIdentifier.ROTATION_LOCK.value : UiEntityIdentifier.ROTATION_UNLOCK.value));
        toggle.setChecked(checked);
        toggle.setVisibility(View.VISIBLE);
        scheduleFadeOut(LOCK_SHOW_DURATION_MS);
    }

    @Override
    public void hide() {
        setVisibility(View.GONE);
    }

    private class FadeOut implements Runnable {
        @Override
        public void run() {
            Animation outAlpha = new AlphaAnimation(1, 0);
            outAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
            outAlpha.setDuration(LOCK_FADEOUT_DURATION_MS);
            outAlpha.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    fading = true;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    toggle.setEnabled(false);
                    hide();
                    fading = false;
                }
            });
            toggle.startAnimation(outAlpha);
        }
    }
}
