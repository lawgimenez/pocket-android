package com.pocket.app.premium;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ideashower.readitlater.R;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.ui.view.AppBar;
import com.pocket.util.android.ViewUtilKt;

public class PremiumMessageFragment extends AbsPocketFragment {

    public static PremiumMessageFragment newInstance(
            boolean isGiftMessage,
            String title,
            String message,
            String buttonText,
            String disclaimer,
            String startScreen
    ) {
        Bundle args = new Bundle();
        PremiumMessageFragment frag = new PremiumMessageFragment();
        args.putBoolean(PremiumMessageActivity.EXTRA_IS_GIFT_MESSAGE, isGiftMessage);
        args.putString(PremiumMessageActivity.EXTRA_TITLE, title);
        args.putString(PremiumMessageActivity.EXTRA_MESSAGE, message);
        args.putString(PremiumMessageActivity.EXTRA_BUTTON_TEXT, buttonText);
        args.putString(PremiumMessageActivity.EXTRA_DISCLAIMER, disclaimer);
        args.putString(PremiumMessageActivity.EXTRA_START_SCREEN, startScreen);
        frag.setArguments(args);
        return frag;
    }

    private ImageView header;
    private TextView title;
    private TextView text;
    private TextView button;
    private TextView disclaimer;

    @Override
    public CxtView getActionViewName() {
        if (requireArguments().getBoolean(PremiumMessageActivity.EXTRA_IS_GIFT_MESSAGE)) {
            return CxtView.GIFTED;
        } else {
            return CxtView.UPGRADE;
        }
    }
    
    @Nullable @Override public UiEntityIdentifier getScreenIdentifier() {
        if (requireArguments().getBoolean(PremiumMessageActivity.EXTRA_IS_GIFT_MESSAGE)) {
            return UiEntityIdentifier.PREMIUM_GIFT_MESSAGE;
        } else {
            return UiEntityIdentifier.JOINED_PREMIUM;
        }
    }
    
    @Override
    protected View onCreateViewImpl(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_premium_message, container, false);
        var startScreen = requireArguments().getString(PremiumMessageActivity.EXTRA_START_SCREEN);
        if (startScreen != null) {
            app().tracker().bindUiEntityValue(root, startScreen);
        }
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppBar appBar = findViewById(R.id.appbar);
        header = findViewById(R.id.header);
        title = findViewById(R.id.message_title);
        text = findViewById(R.id.message_text);
        button = findViewById(R.id.button);
        disclaimer = findViewById(R.id.disclaimer);

        appBar.bind()
                .withCloseIcon("close_premium")
                .onLeftIconClick(v -> startDefaultActivity());
        button.setOnClickListener(v -> startDefaultActivity());

        if (getArguments().getString(PremiumMessageActivity.EXTRA_TITLE) != null) {
            showIntentMessage();
        } else {
            startDefaultActivity();
        }
    }

    private void showIntentMessage() {
        title.setText(getArguments().getString(PremiumMessageActivity.EXTRA_TITLE));
        text.setText(getArguments().getString(PremiumMessageActivity.EXTRA_MESSAGE));
        button.setText(getArguments().getString(PremiumMessageActivity.EXTRA_BUTTON_TEXT));
        ViewUtilKt.setTextOrHide(disclaimer, getArguments().getString(PremiumMessageActivity.EXTRA_DISCLAIMER));
    }

    private void startDefaultActivity() {
        getAbsPocketActivity().startDefaultActivity();
        finish();
    }
}
