package com.pocket.app.premium;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pocket.app.settings.Theme;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.util.android.fragment.FragmentUtil;

public class PremiumMessageActivity extends AbsPocketActivity {

    private static final String FRAGMENT_MAIN = "main";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_BUTTON_TEXT = "button_text";
    public static final String EXTRA_DISCLAIMER = "disclaimer";

    public static final String EXTRA_IS_GIFT_MESSAGE = "is_gift_message";
    public static final String EXTRA_START_SCREEN = "start_screen";

    public static void startActivity(
            Context context,
            String title,
            String message,
            String buttonText,
            String disclaimer
    ) {
        Intent intent = new Intent(context, PremiumMessageActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_BUTTON_TEXT, buttonText);
        intent.putExtra(EXTRA_DISCLAIMER, disclaimer);
        intent.putExtra(EXTRA_IS_GIFT_MESSAGE, false);
        context.startActivity(intent);
    }

    public static void startActivity(
            Context context,
            String title,
            String message,
            String buttonText,
            String disclaimer,
            String startScreen
    ) {
        Intent intent = new Intent(context, PremiumMessageActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_BUTTON_TEXT, buttonText);
        intent.putExtra(EXTRA_DISCLAIMER, disclaimer);
        intent.putExtra(EXTRA_IS_GIFT_MESSAGE, false);
        intent.putExtra(EXTRA_START_SCREEN, startScreen);
        context.startActivity(intent);
    }

    public static void startGiftMessageActivity(Context context) {
        Intent intent = new Intent(context, PremiumMessageActivity.class);
        intent.putExtra(EXTRA_IS_GIFT_MESSAGE, true);
        context.startActivity(intent);
    }

    private PremiumMessageFragment frag;

    @Override
    public CxtView getActionViewName() {
        if (frag != null) {
            return frag.getActionViewName();
        } else {
            // Not expected, but have a safe fallback
            return CxtView.UPGRADE;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // New instance
            Intent intent = getIntent();
            frag = PremiumMessageFragment.newInstance(
                    intent.getBooleanExtra(EXTRA_IS_GIFT_MESSAGE, true), // unless otherwise specified, this is a Gift Messsage (Galaxy Gifts) activity
                    intent.getStringExtra(EXTRA_TITLE),
                    intent.getStringExtra(EXTRA_MESSAGE),
                    intent.getStringExtra(EXTRA_BUTTON_TEXT),
                    intent.getStringExtra(EXTRA_DISCLAIMER),
                    intent.getStringExtra(EXTRA_START_SCREEN)
            );
            setContentFragment(frag, FRAGMENT_MAIN, FragmentUtil.FragmentLaunchMode.ACTIVITY);
        } else {
            // Fragment is restored
            frag = (PremiumMessageFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_MAIN);
        }
    }

    @Override
    protected AbsPocketActivity.ActivityAccessRestriction getAccessType() {
        return AbsPocketActivity.ActivityAccessRestriction.REQUIRES_LOGIN;
    }

    @Override
    protected void checkClipboardForUrl() {
        // Do not check in this Activity
    }

    @Override
    protected int getDefaultThemeFlag() {
        return Theme.FLAG_ONLY_LIGHT;
    }

    @Override
    public boolean isListenUiEnabled() {
        return false;
    }
}
