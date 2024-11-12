package com.pocket.ui.view.info;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.pocket.ui.R;
import com.pocket.ui.view.button.BoxButton;
import com.pocket.ui.view.themed.ThemedConstraintLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * A fullscreen view which takes lists of {@link InfoPage} data and displays them.  An InfoPage consists of an image, title text, subtext,
 * and optionally a button text, button listener, and link text and listener.
 * <p>
 * Design: https://www.figma.com/file/Qqwh8xKl4Gy4YMv6mzw2gCO9/CLEAN?node-id=640%3A8213
 */
public class InfoPagingView extends ThemedConstraintLayout {
    
    public abstract static class InfoAdapter extends RecyclerView.Adapter {
        public abstract List<InfoPage> getData();
    }

    private final Binder binder = new Binder();

    private List<InfoPage> pages = new ArrayList<>();

    private ImageView header;
    private PageIndicatedViewPager pager;
    private BoxButton actionButton;
    private TextView linkText;

    public InfoPagingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InfoPagingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public InfoPagingView(Context context) {
        super(context);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_info_paging_view, this, true);
        setBackgroundResource(R.drawable.cl_pkt_bg);
        header = findViewById(R.id.header);
        pager = findViewById(R.id.viewPager);
        actionButton = findViewById(R.id.actionButton);
        linkText = findViewById(R.id.actionLinkText);
    
        // This captures any touches to the content area that didn't get used by buttons or paging yet and passes them to the pager so swiping can happen from anywhere in this area.
        // This gives the view the feeling that the whole view is part of the pager
        // This is needed in addition to the onTouchEvent override below, otherwise the scroll view eats all events. If needed the scroll view will intercept these for vertical scrolling.
        findViewById(R.id.info_page_content).setOnTouchListener((v, event) -> pager.onTouchEvent(event));
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // This captures any unhandled touches above and below the centered scrollable content and passes them to the pager for swiping.
        // This gives the view the feeling that the whole view is part of the pager
        return super.onTouchEvent(event) || pager.onTouchEvent(event);
    }
    
    public int getCurrentPage() {
        return pager.getCurrentPage();
    }
    
    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            adapter(null);
            header(null);
            pager.bind().clear();
            return this;
        }
    
        /**
         * Change/set the adapter. Any listeners previously added via {@link #addOnPageChangeListener(ViewPager2.OnPageChangeCallback)} will be cleared.
         */
        public Binder adapter(InfoAdapter adapter) {
            if (adapter != null) {
                pages = adapter.getData();

                pager.bind().clearOnPageChangeListeners(); // clear any previous adapter's OnPageChangeListener

                pager.bind().adapter(adapter).addOnPageChangeListener(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {

                        final InfoPage page = pages.get(position);

                        // post to ensure we're running on the UI thread
                        actionButton.post(() -> {
                            // check for top blue button
                            if (page.getButtonListener() == null) {
                                actionButton.setVisibility(View.GONE);
                                actionButton.setOnClickListener(null);
                            } else {
                                actionButton.setVisibility(View.VISIBLE);
                                actionButton.setOnClickListener(page.getButtonListener());
                                actionButton.setText(page.getButtonText());
                            }

                            // check for link text
                            if (page.getLinkButtonListener() == null) {
                                // Always keep this view visible if the top button is, so it holds a consistent height.
                                linkText.setVisibility(actionButton.getVisibility() == View.GONE ? View.GONE : View.VISIBLE);
                                // The text is just emptied rather than using View.INVISIBLE because View.INVISIBLE causes
                                // its ripple animation to flicker or on some Android versions, stick around after the page changes.
                                linkText.setText("");
                                linkText.setOnClickListener(null);
                                linkText.setClickable(false);
                            } else {
                                linkText.setVisibility(View.VISIBLE);
                                linkText.setOnClickListener(page.getLinkButtonListener());
                                linkText.setText(page.getLinkButtonText());
                            }
                        });
                    }
                }).setPage(0); // init to first page
            } else {
                pages = null;
                pager.bind().adapter(null);
            }

            return this;
        }
    
        public Binder header(@DrawableRes int drawable) {
            return header(getResources().getDrawable(drawable));
        }

        public Binder header(Drawable drawable) {
            header.setImageDrawable(drawable);
            header.setVisibility(drawable != null ? VISIBLE : GONE);
            return this;
        }
    
        /**
         * Move to the previous page.
         * @return true if advanced, false if already at first page
         */
        public boolean previousPage() {
            return pager.bind().previousPage();
        }
    
        /**
         * Move to the next page.
         * @return true if advanced, false if already at final page
         */
        public boolean nextPage() {
            return pager.bind().nextPage();
        }
    
        /**
         * Adds a page change listener. Invoke this after {@link #adapter(InfoAdapter)}.
         */
        public Binder addOnPageChangeListener(ViewPager2.OnPageChangeCallback listener) {
            pager.bind().addOnPageChangeListener(listener);
            return this;
        }
    
        public Binder removeOnPageChangeListener(ViewPager2.OnPageChangeCallback listener) {
            pager.bind().removeOnPageChangeListener(listener);
            return this;
        }

    }

}
