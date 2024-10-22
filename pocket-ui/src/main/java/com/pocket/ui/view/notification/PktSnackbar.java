package com.pocket.ui.view.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.pocket.ui.R;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.button.ButtonBoxDrawable;
import com.pocket.ui.view.button.IconButton;
import com.pocket.ui.view.themed.ThemedTextView;
import com.pocket.util.android.ViewUtilKt;

import java.lang.ref.WeakReference;

/**
 * A custom view for showing notifications.
 * <p>
 * Design:
 * https://www.figma.com/file/Qqwh8xKl4Gy4YMv6mzw2gCO9/CLEAN?node-id=0%3A1
 */
public class PktSnackbar extends CoordinatorLayout {

    public enum Type {
        // used for Pocket themed notifications inside of the Pocket app
        ERROR_DISMISSABLE,
        ERROR_EXCLAIM,
        DEFAULT_DISMISSABLE,
        DEFAULT,

        // used for Pocket themed notifications outside of the Pocket app, such as the add overlay
        DEFAULT_OUTSIDE,
        ERROR_EXCLAIM_OUTSIDE
    }

    private static final int FADE_ANIM_MS = 500;

    public interface OnDismissListener {
        void onDismiss(DismissReason reason);
    }
    
    public enum DismissReason {
        USER, PROGRAMMATIC
    }

    private final Binder binder = new Binder();

    private ConstraintLayout root;
    private IconButton icon;
    private TextView title;
    private TextView message;
    private OnDismissListener onDismissListener;
    private ThemedTextView action;
    private boolean isDismissed = false;

    /**
     * A static reference to the currently shown PktSnackbar, to prevent multiples stacked on top of each other.
     * This is a WeakReference to avoid it sticking around after an Activity has been destroyed and leaking the Activity.
     */
    private static WeakReference<PktSnackbar> currentBar;

    /**
     * A global error reporter, which handles what to do when long clicking on error snackbars.
     */
    public interface ErrorReporter {
        void reportError(Context context, String message, Throwable t);
    }

    public static void init(@NonNull ErrorReporter reporter) {
        errorReporter = reporter;
    }

    private static ErrorReporter errorReporter = (context, message, t) -> {}; // empty implementation

    public static PktSnackbar make(Activity activity, Type type, View anchor, CharSequence message, OnDismissListener listener) {
        return make(activity, type, anchor, message, listener, 0, null, null);
    }

    public static PktSnackbar make(Activity activity, Type type, CharSequence message, OnDismissListener listener) {
        return make(activity, type, null, message, listener, 0, null, null);
    }
    
    public static PktSnackbar make(Activity activity, Type type, CharSequence message, OnDismissListener listener, @StringRes int actionText, OnClickListener actionListener) {
        return make(activity, type, message, listener, actionText, null, actionListener);
    }

    public static PktSnackbar make(Activity activity, Type type, CharSequence message, OnDismissListener listener, @StringRes int actionText, String actionIdentifier, OnClickListener actionListener) {
        return make(activity, type, null, message, listener, actionText, actionIdentifier, actionListener);
    }

    /**
     * Creates a PktSnackbar and attaches it to the provided Activity's root content view.
     * @param activity          The current Activity context.
     * @param type              The type of notification (ERROR_DISMISSABLE, ERROR_EXCLAIM, DEFAULT)
     * @param anchor            An optional anchor view, which will add extra margin to the bottom of the notification.  Useful for placing it above bottom aligned navigation / buttons.
     * @param message           The message to display.
     * @param onDismissListener An optional listener to trigger on dismiss of the view.
     * @param actionText        Text for an optional "action button" which appears right aligned in the view. Both text and listener must be nonnull for it to appear.
     * @param actionIdentifier  UI entity identifier for the "action button".
     * @param actionListener    A click listener for the "action button".
     */
    public static PktSnackbar make(Activity activity, Type type, @Nullable View anchor, CharSequence message, OnDismissListener onDismissListener, @StringRes int actionText, String actionIdentifier, OnClickListener actionListener) {
        dismissCurrent();
        return makeInternal(activity, type, anchor, message, onDismissListener, actionText, actionIdentifier, actionListener);
    }

    public static PktSnackbar getCurrent() {
        return currentBar == null ? null : currentBar.get();
    }

    public static void dismissCurrent() {
        if (currentBar != null && currentBar.get() != null) {
            currentBar.get().bind().dismiss();
            currentBar = null;
        }
    }

    private static ViewGroup getNotificationViewGroup(Activity activity, View anchor) {
        if (anchor == null || !(anchor.getParent() instanceof ViewGroup)) {
            return activity.findViewById(android.R.id.content);
        } else {
            return (ViewGroup) anchor.getParent();
        }
    }

    /**
     * Creates a customized {@link ViewGroup.LayoutParams} depending on the parent View of the anchor in order to position the notification directly above the anchor (with margin).
     * TODO implement TooltipView instead. See {@link com.pocket.sdk.util.view.tooltip} for example usage.
     */
    private static ViewGroup.MarginLayoutParams getNotificationLayoutParams(Activity activity, View anchor, ViewGroup notificationParent) {

        ViewGroup.MarginLayoutParams params;
        int margin = (int) activity.getResources().getDimension(R.dimen.pkt_space_sm);

        if (notificationParent.getId() == View.NO_ID) {
            notificationParent.setId(View.generateViewId());
        }

        if (anchor != null && anchor.getId() == View.NO_ID) {
            anchor.setId(View.generateViewId());
        }

        if (notificationParent instanceof FrameLayout) {
            params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ((FrameLayout.LayoutParams) params).gravity = Gravity.BOTTOM;
            params.setMargins(margin, margin, margin, margin + (anchor == null ? 0 : anchor.getHeight())); // if anchor is null here that means android.R.id.content was used as the parent (which is a FrameLayout)
        } else if (notificationParent instanceof ConstraintLayout) {
            params = new ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            ((ConstraintLayout.LayoutParams) params).startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            ((ConstraintLayout.LayoutParams) params).endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            ((ConstraintLayout.LayoutParams) params).bottomToTop = anchor.getId();
            params.setMargins(margin, margin, margin, margin);
        } else if (notificationParent instanceof RelativeLayout) {
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ((RelativeLayout.LayoutParams) params).addRule(RelativeLayout.ABOVE, anchor.getId());
            params.setMargins(margin, margin, margin, margin);
        } else if (notificationParent instanceof CoordinatorLayout) {
            params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(margin, notificationParent.getHeight() - anchor.getHeight() - (int) activity.getResources().getDimension(R.dimen.pkt_snackbar_height) - margin, margin, margin);
        } else {
            throw new UnsupportedOperationException("The anchor's ViewGroup is not supported for PktSnackbar.");
        }
        return params;
    }

    private static PktSnackbar makeInternal(Activity activity,
            Type type,
            View anchor,
            CharSequence message,
            OnDismissListener listener,
            @StringRes int actionText,
            @Nullable String actionIdentifier,
            OnClickListener actionListener) {
        PktSnackbar bar = new PktSnackbar(activity);

        setAnchor(activity, bar, anchor);

        bar.bind().onDismiss(reason -> {
            // hijack user supplied listener with our own to remove the view from decorview
            getNotificationViewGroup(activity, anchor).removeView(bar);
            if (listener != null) {
                listener.onDismiss(reason);
            }
        }).type(type).onAction(actionText, actionIdentifier, v -> {
            bar.bind().dismiss(); // dismiss on click
            if (actionListener != null) {
                actionListener.onClick(v);
            }
        }).message(message);

        // GONE until show() is called
        bar.setVisibility(View.GONE);

        currentBar = new WeakReference<>(bar);

        return bar;
    }

    /**
     * Sets the anchor of the given PktSnackbar to the provided View.
     *
     * @param activity The Activity context.
     * @param bar The PktSnackbar to reposition.
     * @param anchor A View on which to anchor the PktSnackbar.
     */
    public static void setAnchor(Activity activity, PktSnackbar bar, View anchor) {

        if (bar.getParent() != null) {
            ((ViewGroup) bar.getParent()).removeView(bar);
        }

        ViewGroup root = getNotificationViewGroup(activity, anchor);

        ViewGroup.MarginLayoutParams params = getNotificationLayoutParams(activity, anchor, root);

        // set provided margins as padding on the Snackbar instead, so swipe dismiss isn't cut off on the sides
        bar.setPadding(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin);
        params.setMargins(0, 0, 0, 0);

        bar.setLayoutParams(params);
        root.addView(bar);
    }

    public PktSnackbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PktSnackbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PktSnackbar(Context context) {
        super(context);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_pkt_snackbar, this, true);

        root = findViewById(R.id.snackbar);
        icon = findViewById(R.id.icon);
        title = findViewById(R.id.title);
        message = findViewById(R.id.message);
        action = findViewById(R.id.actionButton);

        setMinimumHeight((int) getResources().getDimension(R.dimen.pkt_snackbar_height));
        setClipToPadding(false);
    }

    private void userDismissable(boolean dismissable) {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) root.getLayoutParams();
        if (dismissable) {
            PktSwipeDismissBehavior swipeDismissBehavior = new PktSwipeDismissBehavior();
            swipeDismissBehavior.setSwipeDirection(PktSwipeDismissBehavior.SWIPE_DIRECTION_ANY);
            swipeDismissBehavior.setListener(new PktSwipeDismissBehavior.OnDismissListener() {
                @Override
                public void onDismiss(View view) {
                    dismissMessage(DismissReason.USER);
                }

                @Override
                public void onDragStateChanged(int i) {
                    //
                }
            });
            layoutParams.setBehavior(swipeDismissBehavior);
        } else {
            layoutParams.setBehavior(null);
        }
    }

    private void displayMessage() {
        setAlpha(0f);
        setVisibility(View.VISIBLE);
        animate().alpha(1f).setDuration(FADE_ANIM_MS).setInterpolator(new DecelerateInterpolator()).setListener(null);
    }

    private void dismissMessage(DismissReason reason) {
        if (isDismissed) return;

        isDismissed = true;
        animate().alpha(0f).setDuration(FADE_ANIM_MS).setInterpolator(new DecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(View.GONE);
                if (onDismissListener != null) {
                    onDismissListener.onDismiss(reason);
                }
                
                // Clear out the listener, because the view caches the animator.
                animate().setListener(null);
            }
        });
    }

    public void show() {
        binder.show();
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        private Throwable error;

        public Binder clear() {
            setVisibility(View.VISIBLE);
            isDismissed = false;
            type(Type.DEFAULT_DISMISSABLE);
            title(null);
            message(null);
            singleLineMessage(false);
            onDismiss(null);
            onAction(0, null);
            error(null);
            return this;
        }

        public Binder type(Type type) {

            // default to no error icon / long press function
            icon.setVisibility(View.GONE);
            root.setOnLongClickListener(null);
            root.setLongClickable(false);

            Drawable background;
            switch (type) {
                case ERROR_DISMISSABLE:
                    background = new ButtonBoxDrawable(getContext(), R.color.pkt_themed_apricot_1, 0);
                    userDismissable(true);
                    textColor(R.color.pkt_button_text);
                    actionColor(R.color.pkt_button_text);
                    setupErrorView(R.color.pkt_button_text);
                    break;
                case ERROR_EXCLAIM:
                    background = new ButtonBoxDrawable(getContext(), R.color.pkt_themed_apricot_1, 0);
                    userDismissable(false);
                    textColor(R.color.pkt_button_text);
                    actionColor(R.color.pkt_button_text);
                    setupErrorView(R.color.pkt_button_text);
                    break;
                case DEFAULT_DISMISSABLE:
                    background = new ButtonBoxDrawable(getContext(), R.color.pkt_themed_teal_2, 0);
                    userDismissable(true);
                    textColor(R.color.pkt_button_text);
                    actionColor(R.color.pkt_button_text);
                    setupDismissView(R.color.pkt_button_text);
                    break;

                case ERROR_EXCLAIM_OUTSIDE:
                    background = new ButtonBoxDrawable(getContext(), R.color.pkt_bg, 0);
                    userDismissable(false);
                    textColor(R.color.pkt_themed_grey_1);
                    actionColor(R.color.pkt_themed_teal_2_clickable);
                    setupErrorView(R.color.pkt_themed_apricot_1);
                    break;
                case DEFAULT_OUTSIDE:
                    background = new ButtonBoxDrawable(getContext(), R.color.pkt_bg, 0);
                    userDismissable(false);
                    textColor(R.color.pkt_themed_grey_1);
                    actionColor(R.color.pkt_themed_teal_2_clickable);
                    break;

                case DEFAULT:
                default:
                    background = new ButtonBoxDrawable(getContext(), R.color.pkt_themed_teal_2, 0);
                    userDismissable(false);
                    textColor(R.color.pkt_button_text);
                    actionColor(R.color.pkt_button_text);
                    break;
            }
            
            root.setBackground(background);
            return this;
        }

        private void setupDismissView(@ColorRes int iconColors) {
            setIcon(R.drawable.ic_pkt_close_x_mini, iconColors);
            icon.setOnClickListener(v -> dismissMessage(DismissReason.USER));
            icon.setContentDescription(getContext().getResources().getText(R.string.ic_close));
        }

        private void setupErrorView(@ColorRes int iconColors) {
            setIcon(R.drawable.ic_pkt_error_mini, iconColors);
            icon.setContentDescription(null);
            icon.setOnClickListener(v -> reportError());
            root.setOnLongClickListener(v -> {
                reportError();
                return false;
            });
        }

        private void reportError() {
            errorReporter.reportError(getContext(), "Error: " + title.getText().toString() + " " + message.getText().toString(), error);
        }

        private void setIcon(@DrawableRes int iconRes, @ColorRes int iconColors) {
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(VectorDrawableCompat.create(getResources(), iconRes, null));
            icon.setVisualMarginStart(R.dimen.pkt_space_md);
            icon.setVisualMarginEnd(R.dimen.pkt_space_md);
            icon.setDrawableColor(NestedColorStateList.get(getContext(), iconColors));
        }

        private void textColor(int textColors) {
            title.setTextColor(NestedColorStateList.get(getContext(), textColors));
            message.setTextColor(NestedColorStateList.get(getContext(), textColors));
        }

        private void actionColor(int actionColors) {
            action.setTextColor(NestedColorStateList.get(getContext(), actionColors));
        }

        public Binder onDismiss(OnDismissListener listener) {
            onDismissListener = listener;
            return this;
        }

        public Binder onAction(@StringRes int actionText, OnClickListener listener) {
            return onAction(actionText, null, listener);
        }
    
        public Binder onAction(@StringRes int actionText,
                String uiIdentifier,
                OnClickListener listener) {
            action.setTextAndUpdateEnUsLabel(actionText);
            action.setOnClickListener(listener);
            action.setUiEntityIdentifier(uiIdentifier);
            if (actionText == 0 || listener == null) {
                action.setVisibility(View.GONE);
            } else {
                action.setVisibility(View.VISIBLE);
            }
            return this;
        }

        public Binder error(Throwable t) {
            this.error = t;
            return this;
        }

        public Binder title(CharSequence val) {
            ViewUtilKt.setTextOrHide(title, val);
            return this;
        }

        public Binder message(CharSequence val) {
            ViewUtilKt.setTextOrHide(message, val);
            return this;
        }

        public Binder singleLineMessage(boolean singleLine) {
            message.setSingleLine(singleLine);
            return this;
        }

        public void show() {
            displayMessage();
        }

        public void dismiss() {
            dismissMessage(DismissReason.PROGRAMMATIC);
        }

    }

}
