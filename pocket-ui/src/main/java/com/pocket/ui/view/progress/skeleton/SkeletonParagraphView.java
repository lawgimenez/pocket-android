package com.pocket.ui.view.progress.skeleton;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.view.themed.ThemedLinearLayout;
import com.pocket.util.java.RandomSingleton;

public class SkeletonParagraphView extends ThemedLinearLayout {

    public SkeletonParagraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SkeletonParagraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        setOrientation(VERTICAL);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SkeletonParagraphView);

        int minLines = ta.getInt(R.styleable.SkeletonParagraphView_minLines, 1);
        int maxLines = ta.getInt(R.styleable.SkeletonParagraphView_maxLines, 2);

        if (minLines > maxLines) {
            throw new IllegalArgumentException("minLines must be less than maxLines");
        }

        int totalLines = RandomSingleton.get().nextInt(maxLines - minLines + 1) + minLines;
        if (totalLines == 0) {
            // if no lines, set ourself to GONE to avoid extra padding
            setVisibility(View.GONE);
        } else {
            for (int i = 0; i < totalLines; i++) {
                addLine(context, i == totalLines - 1);
            }
        }

        ta.recycle();
    }

    private void addLine(Context context, boolean isLast) {
        SkeletonView view = new SkeletonView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) context.getResources().getDimension(R.dimen.pkt_skeleton_text_height));
        int margin = DimenUtil.dpToPxInt(context, 5);
        params.setMargins(0, margin, 0, margin);
        view.setLayoutParams(params);
        view.bind().background(isLast ? R.color.pkt_themed_grey_5 : R.color.pkt_themed_grey_6, context.getResources().getDimension(R.dimen.pkt_skeleton_text_corner_radius)).randomWidth(isLast ? 0.2f : 0.7f, isLast ? 0.7f : 1f);
        this.addView(view);
    }

}
