package com.pocket.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.view.ContextThemeWrapper;

import com.pocket.ui.R;
import com.pocket.ui.text.TextViewUtil;
import com.pocket.ui.util.CheckableHelper;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.button.BoxButton;
import com.pocket.ui.view.button.IconButton;
import com.pocket.ui.view.button.ToggleButton;
import com.pocket.ui.view.themed.ThemedConstraintLayout;
import com.pocket.ui.view.themed.ThemedTextView;

/**
 * A view for displaying a standard Pocket themed "AppBar" (Toolbar, Actionbar, etc).
 * <p>
 * Example usage:
 * <pre>
 * &lt;com.pocket.ui.view.AppBar
 *     android:id="@+id/appbar"
 *     app:leftIcon="up"
 *     android:title="This is a title"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 *
 * Options for "app:leftAction" include none, up, and close.
 *
 * AppBar appBar = findViewById(R.id.appbar);
 * appBar.bind()
 *         .onLeftIconClick(v -> finish())
 *         .addIconAction(R.drawable.ic_pkt_listen_line, R.string.ic_listen, v -> Toast.makeText(this, "Clicked Listen!", Toast.LENGTH_LONG).show())
 *         .addIconAction(R.drawable.ic_pkt_android_overflow_solid, R.string.ic_overflow, v -> Toast.makeText(this, "Clicked Overflow!", Toast.LENGTH_LONG).show());
 * </pre>
 */
public class AppBar extends ThemedConstraintLayout {
    
    private final int minIconSize = DimenUtil.dpToPxInt(getContext(), 24);
    private final int rightPaddingDefault = (int) getResources().getDimension(R.dimen.pkt_side_grid);

    private final Binder binder = new Binder();

    private IconButton leftIcon;
    private TextView title;
    private ViewGroup centerContainer;
    private ViewGroup actions;
    private @Nullable View divider;

    public AppBar(Context context) {
        super(context);
        init(context, null);
    }

    public AppBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AppBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected @LayoutRes int layout() {
        return R.layout.view_app_bar;
    }

    protected @DrawableRes int upRes() {
        return R.drawable.ic_pkt_back_arrow_line;
    }

    protected @DrawableRes int closeRes() {
        return R.drawable.ic_pkt_close_x_line;
    }

    protected @DrawableRes int chevronRes() {
        return R.drawable.ic_pkt_back_chevron_line;
    }

    protected @StyleRes int textActionStyle() {
        return R.style.Pkt_Text_AppBar_Action;
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(layout(), this, true);

        leftIcon = findViewById(R.id.leftIcon);
        title = findViewById(R.id.title);
        centerContainer = findViewById(R.id.centerContainer);
        actions = findViewById(R.id.actions);
        divider = findViewById(R.id.divider);

        bind().clear();

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AppBar);
            bind().title(ta.getText(R.styleable.AppBar_android_title));
            int leftIcon = ta.getInt(R.styleable.AppBar_leftIcon, 1); // defaults to up arrow
            switch (leftIcon) {
                case 0:
                    bind().withNoLeftIcon();
                    break;
                case 1:
                    bind().withUpArrow();
                    break;
                case 2:
                    bind().withCloseIcon();
                    break;
                case 3:
                    bind().withChevron();
                    break;
            }
            bind().divider(ta.getBoolean(R.styleable.AppBar_bottomDivider, true));
            ta.recycle();
        }

        setBackgroundResource(R.drawable.cl_pkt_bg);
    }

    public IconButton getLeftIcon() {
        return leftIcon;
    }

    /**
     * Returns the number of actions in the right action menu.
     *
     * @return the number of actions
     */
    public int getActionCount() {
        return actions.getChildCount();
    }

    /**
     * Returns the action View at a specific index in the actions layout.
     *
     * @param index the position of the view
     * @return the view
     */
    public View getActionView(int index) {
        return actions.getChildAt(index);
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            title(null);
            withUpArrow();
            centerView(null);
            divider(true);
            actions.removeAllViews();
            return this;
        }

        /**
         * Adds a custom View to the center of the AppBar.
         *
         * @param view The View to display.
         */
        public Binder centerView(View view) {
            centerContainer.removeAllViews();
            if (view != null) {
                centerContainer.setVisibility(View.VISIBLE);
                centerContainer.addView(view);
            } else {
                centerContainer.setVisibility(View.GONE);
            }
            return this;
        }

        /**
         * Whether to show or hide the bottom divider line.
         *
         * @param show boolean whether to show or hide the line.
         */
        public Binder divider(boolean show) {
            // some AppBar extensions may not include a divider
            if (divider == null) {
                return this;
            }
            if (show) {
                divider.setVisibility(View.VISIBLE);
            } else {
                divider.setVisibility(View.GONE);
            }
            return this;
        }

        /**
         * Sets a custom left icon for the AppBar.
         *
         * @param drawable The icon drawable to use.
         * @param contentDescription The contentDescription which is displayed when long pressing the icon.
         * @return
         */
        public Binder leftIcon(@DrawableRes int drawable, @StringRes int contentDescription) {
            return leftIcon(drawable, contentDescription, null);
        }

        public Binder leftIcon(
                @DrawableRes int drawable,
                @StringRes int contentDescription,
                String uiEntityIdentifier
        ) {
            leftIcon.setVisibility(View.VISIBLE);
            leftIcon.setImageResource(drawable);
            leftIcon.setContentDescription(getResources().getString(contentDescription));
            leftIcon.setUiEntityIdentifier(uiEntityIdentifier);
            return this;
        }

        /**
         * Hides the left icon, aligning the title with the left of the AppBar.
         */
        public Binder withNoLeftIcon() {
            leftIcon.setVisibility(View.GONE);
            return this;
        }

        /**
         * Sets the AppBar to use an up left icon..
         */
        public Binder withUpArrow() {
            leftIcon(upRes(), R.string.ic_up);
            return this;
        }

        /**
         * Sets the AppBar to use a close ("X") left icon.
         */
        public Binder withCloseIcon() {
            leftIcon(closeRes(), R.string.ic_close);
            return this;
        }

        public Binder withCloseIcon(String uiEntityIdentifier) {
            leftIcon(closeRes(), R.string.ic_close, uiEntityIdentifier);
            return this;
        }

        /**
         * Sets the AppBar to use a left chevron.
         */
        public Binder withChevron() {
            leftIcon(chevronRes(), R.string.ic_cancel);
            return this;
        }

        /**
         * Sets the {@link android.view.View.OnClickListener} for the left icon.
         */
        public Binder onLeftIconClick(OnClickListener listener) {
            leftIcon.setOnClickListener(listener);
            return this;
        }

        public Binder onLeftIconClick(OnClickListener listener, String uiIdentifier) {
            onLeftIconClick(listener);
            leftIcon.setUiEntityIdentifier(uiIdentifier);
            return this;
        }

        /**
         * Sets the title of the AppBar.
         *
         * @param value The title.
         */
        public Binder title(CharSequence value) {
            title.setText(value);
            return this;
        }

        /**
         * Sets the title of the AppBar.
         *
         * @param value the title String resource.
         */
        public Binder title(@StringRes int value) {
            title(getResources().getText(value));
            return this;
        }

        /**
         * Adds a new {@link IconButton} with Pkt_IconButton style to the actions ViewGroup.
         *
         * @param drawable The drawable to use for the icon.  This should generally be 24dp in width, as per material design guidelines.
         * @param contentDescription The contentDescription to use when long pressing the icon.
         * @param listener An {@link android.view.View.OnClickListener} for the icon.
         * @return
         */
        public Binder addIconAction(@DrawableRes int drawable, @StringRes int contentDescription, OnClickListener listener) {
            if (!viewExists(drawable)) {
                IconButton action = new IconButton(new ContextThemeWrapper(getContext(), R.style.Pkt_IconButton));
                action.setImageResource(drawable);
                bindAction(action, getResources().getString(contentDescription), drawable, listener);
                fixSmallIcon(action, action.getDrawable());
                action.setDrawableColor(NestedColorStateList.get(getContext(), R.color.pkt_themed_grey_1_clickable));
            }
            return this;
        }

        /**
         * Adds a new {@link ThemedTextView} to the actions ViewGroup.
         *
         * @param label The text to display.
         * @param listener An {@link android.view.View.OnClickListener} for the text.
         */
        public Binder addTextAction(@StringRes int label, @NonNull OnClickListener listener) {
            if (!viewExists(label)) {
                ThemedTextView textAction = new ThemedTextView(new ContextThemeWrapper(getContext(), textActionStyle()));
                textAction.setTextAppearance(getContext(), textActionStyle());
                textAction.setTextAndUpdateEnUsLabel(label);
                bindAction(textAction, getResources().getString(label), label, listener);
                TextViewUtil.verticallyCenterPadding(textAction);
            }
            return this;
        }

        /**
         * Adds a new {@link BoxButton} to the actions ViewGroup.
         *
         * @param label The text to display in the button.
         * @param listener An {@link android.view.View.OnClickListener} for the button.
         */
        public Binder addButtonAction(@StringRes int label, @NonNull OnClickListener listener) {
            if (!viewExists(label)) {
                BoxButton action = new BoxButton(getContext());
                action.setText(label);
                bindAction(action, getResources().getString(label), label, listener);
                addButtonVerticalMargin(action);
            }
            return this;
        }

        /**
         * Adds a new {@link ToggleButton} to the actions ViewGroup.
         *
         * @param label The text to display in the toggle.
         * @param checked The initial checked state of the toggle.
         * @param listener An {@link com.pocket.ui.util.CheckableHelper.OnCheckedChangeListener} for the toggle button.
         */
        public Binder addToggleAction(@StringRes int label, boolean checked, @NonNull CheckableHelper.OnCheckedChangeListener listener) {
            if (!viewExists(label)) {
                ToggleButton toggleAction = new ToggleButton(getContext());
                toggleAction.setText(label);
                toggleAction.setChecked(checked);
                toggleAction.setOnCheckedChangeListener(listener);
                bindAction(toggleAction, getResources().getString(label), label, null);
                addButtonVerticalMargin(toggleAction);
            }
            return this;
        }

        /**
         * Checks if a view with an identical id has already been added to the actions.  This prevents re-adding an action in certain situations,
         * such as configuration changes.
         *
         * @param viewId the id of the view.
         * @return boolean whether a view with that id has already been added to the actions ViewGroup.
         */
        private boolean viewExists(int viewId) {
            for (int i = 0; i < actions.getChildCount(); i++) {
                if (actions.getChildAt(i).getId() == viewId) {
                    return true;
                }
            }
            return false;
        }

        private void addButtonVerticalMargin(View view) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            int verticalMargin = (int) getResources().getDimension(R.dimen.pkt_space_sm);
            params.setMargins(params.leftMargin, verticalMargin, params.rightMargin, verticalMargin);
        }

        private void bindAction(View view, CharSequence contentDescription, int viewId, OnClickListener listener) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins((int) getResources().getDimension(R.dimen.pkt_space_md), 0, 0, 0);
            view.setLayoutParams(params);
            view.setContentDescription(contentDescription);
            view.setOnClickListener(listener);
            view.setId(viewId);
            actions.addView(view);
            // reset the default right padding, in case a previously added icon action was smaller than 24dp
            actions.setPadding(0, 0, rightPaddingDefault, 0);
        }

        /**
         * Appbar action icons are generally 24dp in width (per Material design), the main exception being the overflow button.
         * The appbar uses WRAP_CONTENT for the action buttons, but this makes the overflow impossible to click because it is too thin.
         *
         * For this, we add left/right padding to any icon that is smaller than 24dp to make up the difference.
         *
         * However, if the icon is the last icon in the group, it should still align with the right side of the appbar, so we modify the marginRight of the actions
         * container so that the icon can still be clicked but the extra padding will not push it out of alignment.
         *
         * @param view The new action view that's been added.
         * @param drawable The icon drawable for the view.
         */
        private void fixSmallIcon(View view, Drawable drawable) {
            int iconWidth = drawable.getIntrinsicWidth();
            if (iconWidth < minIconSize) {
                int padding = (minIconSize - iconWidth) / 2;
                view.setPadding(padding, 0, padding, 0);
                actions.setPadding(0, 0, rightPaddingDefault - padding, 0);
            }
        }

    }
}
