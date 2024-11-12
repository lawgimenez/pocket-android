package com.pocket.ui.view.info;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedConstraintLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

/**
 * A View which displays a ViewPager along with a PageIndicatorView (page dots) that tracks page changes.
 */
public class PageIndicatedViewPager extends ThemedConstraintLayout {

    /**
     * An OnPageChangeListener which tracks page changes in order to update the indicator dots.
     * If the OnPageChangeListener list gets cleared, this gets re-added internally, as it's
     * always needed.
     */
    ViewPager2.OnPageChangeCallback indicatorListener = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            indicators.bind().currentIndex(position);
        }
    };

    private final Binder binder = new Binder();

    private ViewPager2 pager;
    private PageIndicatorView indicators;

    private RecyclerView.Adapter adapter;

    private List<ViewPager2.OnPageChangeCallback> onPageChangeListeners;

    public PageIndicatedViewPager(Context context) {
        super(context);
        init();
    }

    public PageIndicatedViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageIndicatedViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_indicator_viewpager, this, true);
        pager = findViewById(R.id.pager);
        indicators = findViewById(R.id.indicators);
        onPageChangeListeners = new ArrayList<>();
        binder.addOnPageChangeListener(indicatorListener);
        pager.getChildAt(0).setOverScrollMode(OVER_SCROLL_NEVER); // sets the overscroll mode of the internal RecyclerView of ViewPager2
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return pager.getChildAt(0).onTouchEvent(event); // Let the internal RecyclerView handle, so that the indicator area is also swipable.
    }
    
    public int getCurrentPage() {
        return pager.getCurrentItem();
    }
    
    public Binder bind() {
        return binder;
    }
    
    public class Binder {

        public Binder clear() {
            adapter(null);
            indicators.bind().clear();
            clearOnPageChangeListeners();
            return this;
        }

        public Binder adapter(RecyclerView.Adapter value) {

            adapter = value;
            pager.setAdapter(adapter);

            if (adapter != null) {
                indicators.bind().pageCount(adapter.getItemCount());
            }

            indicators.setVisibility(adapter == null || adapter.getItemCount() < 2 ? View.GONE : View.VISIBLE);

            return this;
        }
    
        /**
         * Move to the previous page.
         * @return true if advanced, false if already at first page or there are no pages
         */
        public boolean previousPage() {
            if (adapter.getItemCount() > 0 && pager.getCurrentItem() > 0) {
                pager.setCurrentItem(pager.getCurrentItem() - 1);
                return true;
            } else {
                return false;
            }
        }
    
        /**
         * Move to the next page.
         * @return true if advanced, false if already at final page
         */
        public boolean nextPage() {
            if (pager.getCurrentItem() < adapter.getItemCount()) {
                pager.setCurrentItem(pager.getCurrentItem() + 1);
                return true;
            } else {
                return false;
            }
        }

        public void setPage(int index) {
            pager.setCurrentItem(index);
        }

        public Binder addOnPageChangeListener(ViewPager2.OnPageChangeCallback listener) {
            pager.registerOnPageChangeCallback(listener);
            onPageChangeListeners.add(listener);
            return this;
        }

        public Binder removeOnPageChangeListener(ViewPager2.OnPageChangeCallback listener) {
            pager.unregisterOnPageChangeCallback(listener);
            onPageChangeListeners.remove(listener);
            return this;
        }

        public void clearOnPageChangeListeners() {
            for (ViewPager2.OnPageChangeCallback callback : onPageChangeListeners) {
                pager.unregisterOnPageChangeCallback(callback);
            }
            onPageChangeListeners.clear();
            // the dot listener is always needed, so re-add it
            addOnPageChangeListener(indicatorListener);
        }

    }

}
