package com.pocket.ui.view.progress.skeleton.row;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.LayoutRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.facebook.shimmer.Shimmer;
import com.pocket.ui.R;
import com.pocket.ui.util.OnlyWhenVisibleHelper;
import com.pocket.ui.view.themed.ThemedConstraintLayout;
import com.pocket.ui.view.themed.ThemedShimmerFrameLayout;

public abstract class AbsSkeletonRow extends ThemedShimmerFrameLayout {

    protected ConstraintLayout content;

    public AbsSkeletonRow(Context context) {
        super(context);
        init(context);
    }

    public AbsSkeletonRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AbsSkeletonRow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        content = new ThemedConstraintLayout(getContext());
        LayoutInflater.from(context).inflate(getLayout(), content, true);

        final int padding = defaultPadding();
        content.setPadding(padding, 0, padding, 0);

        Shimmer.AlphaHighlightBuilder builder = new Shimmer.AlphaHighlightBuilder();
        builder.setRepeatDelay(2000);
        builder.setDuration(100);
        builder.setBaseAlpha(1f);
        builder.setHighlightAlpha(0.3f);
        builder.setAutoStart(false);

        setShimmer(builder.build());

        addView(content);
    
        OnlyWhenVisibleHelper.install(this, this::startShimmer, this::stopShimmer);
    }
    
    protected int defaultPadding() {
        return (int) getResources().getDimension(R.dimen.pkt_side_grid);
    }

    protected abstract @LayoutRes int getLayout();
}
