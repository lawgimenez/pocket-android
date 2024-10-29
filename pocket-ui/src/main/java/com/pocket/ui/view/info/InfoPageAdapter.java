package com.pocket.ui.view.info;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pocket.analytics.api.UiEntityable;
import com.pocket.ui.R;
import com.pocket.ui.text.Fonts;
import com.pocket.ui.text.TextViewUtil;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InfoPageAdapter extends InfoPagingView.InfoAdapter {

    private final List<InfoPage> pages;

    private final int maxPageWidth;
    private float captionHeight;
    private TextPaint textPaint;

    public InfoPageAdapter(Context context, int screenWidth, List<InfoPage> pages) {
        super();
        this.pages = pages;
        this.maxPageWidth = context.getResources().getDimensionPixelSize(R.dimen.pkt_info_page_max_width);

        setCaptionHeight(context, screenWidth);
    }

    public List<InfoPage> getPages() {
        return pages;
    }

    /**
     * This method calculates the total expected height of the title and subtitle text of the info page, from among
     * the list of pages provided.  This is done because in order to have a consistent height when wrapping the view,
     * we must know the maximum possible height.
     *
     * Previous implementation using a regular ViewPager did this by not recycling the views.  Since we would like to
     * maintain View recycling with ViewPager2, this ensures that each item row will have a consistent height.
     */
    private void setCaptionHeight(Context context, int screenWidth) {

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        captionHeight = 0;

        final Typeface titleTypeface = Fonts.get(context, Fonts.Font.GRAPHIK_LCG_MEDIUM);
        final float titleTextSize = context.getResources().getDimension(R.dimen.pkt_medium_text);

        final Typeface subtextTypeface = Fonts.get(context, Fonts.Font.GRAPHIK_LCG_REGULAR);
        final float subtextTextSize = context.getResources().getDimension(R.dimen.pkt_small_text);

        final float lineHeight = context.getResources().getDimension(R.dimen.pkt_line_height_default);

        final int width = maxPageWidth < screenWidth ? maxPageWidth : screenWidth;

        float titleHeight, subtextHeight;

        for (InfoPage page : pages) {

            titleHeight = getTextHeight(page.getTitle(), titleTypeface, titleTextSize, width, lineHeight);
            subtextHeight = getTextHeight(page.getText(), subtextTypeface, subtextTextSize, width, lineHeight);

            if (captionHeight < titleHeight + subtextHeight) {
                captionHeight = titleHeight + subtextHeight;
            }
        }

        // each TextView has top padding of pkt_space_md
        captionHeight = captionHeight + (2 * context.getResources().getDimension(R.dimen.pkt_space_md));
    }

    private float getTextHeight(CharSequence text, Typeface typeface, float textSize, int width, float lineHeight) {
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSize);
        return TextViewUtil.getExpectedTextViewHeight(textPaint, text, Layout.Alignment.ALIGN_CENTER, width, lineHeight);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    public class InfoViewHolder extends RecyclerView.ViewHolder {
        final CaptionedImageView page;
        private final InfoWrap root;
        InfoViewHolder(InfoWrap root, CaptionedImageView page) {
            super(root);
            this.page = page;
            this.root = root;
        }

        public void setUiEntityIdentifier(String identifier) {
            root.setUiEntityIdentifier(identifier);
        }
    }

    private class InfoWrap extends FrameLayout implements UiEntityable {

        private String uiEntityIdentifier = "info_pager";
        private String uiEntityComponentDetail = "info_page";

        public InfoWrap(@NonNull Context context) { super(context); }
        public InfoWrap(@NonNull Context context, @androidx.annotation.Nullable AttributeSet attrs) { super(context, attrs); }
        public InfoWrap(@NonNull Context context, @androidx.annotation.Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }
        @Nullable @Override public String getUiEntityIdentifier() {
            return uiEntityIdentifier;
        }

        @Override public void setUiEntityIdentifier(@Nullable String uiEntityIdentifier) {
            this.uiEntityIdentifier = uiEntityIdentifier;
        }
    
        @Nullable @Override public Type getUiEntityType() {
            return Type.PAGE;
        }

        @Nullable @Override public String getUiEntityComponentDetail() {
            return uiEntityComponentDetail;
        }

        @Override public void setUiEntityComponentDetail(@Nullable String value) {
            uiEntityComponentDetail = value;
        }

        @Nullable @Override public String getUiEntityLabel() {
            return null;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        CaptionedImageView v = new CaptionedImageView(parent.getContext());
        v.setMaxWidth(maxPageWidth);
        v.bind().captionHeight((int) captionHeight);

        // Use a wrapper view to center it if the screen is larger than the max width
        InfoWrap wrap = new InfoWrap(parent.getContext());
        wrap.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wrap.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL));

        return new InfoViewHolder(wrap, v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        InfoPage page = pages.get(position);
        ((InfoViewHolder) holder).page.bind()
                .image(page.getImageResId())
                .title(page.getTitle())
                .text(page.getText());
        if (page.getUiEntityIdentifier() != null) {
            ((InfoViewHolder) holder).setUiEntityIdentifier(page.getUiEntityIdentifier());
        }
    }

    @Override
    public List<InfoPage> getData() {
        return pages;
    }
}
