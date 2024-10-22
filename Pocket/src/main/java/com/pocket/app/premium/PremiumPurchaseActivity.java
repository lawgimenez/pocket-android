package com.pocket.app.premium;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pocket.app.settings.Theme;
import com.pocket.sdk.api.generated.enums.CxtSource;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sync.value.Parceller;
import com.pocket.util.android.fragment.FragmentUtil;

public class PremiumPurchaseActivity extends AbsPocketActivity {

    private static final String FRAGMENT_MAIN = "main";
    private static final String EXTRA_START_SOURCE = "source";
    private static final String EXTRA_RENEW = "is_renew";

    public static void startActivity(Context context, CxtSource startSource) {
        startActivity(context, startSource, false);
    }

    public static void startActivity(Context context, CxtSource startSource, boolean isRenew) {
        Intent intent = newStartIntent(context, startSource, isRenew);
        context.startActivity(intent);
    }

    public static Intent newStartIntent(Context context, CxtSource startSource, boolean isRenew) {
        Intent intent = new Intent(context, PremiumPurchaseActivity.class);
        Parceller.put(intent, EXTRA_START_SOURCE, startSource);
        intent.putExtra(EXTRA_RENEW, isRenew);
        return intent;
    }

    private PremiumPurchaseFragment frag;

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
            frag = PremiumPurchaseFragment.newInstance(
                    Parceller.getStringEnum(getIntent(), EXTRA_START_SOURCE, CxtSource.JSON_CREATOR),
                    getIntent().getBooleanExtra(EXTRA_RENEW, false));

            setContentFragment(frag, FRAGMENT_MAIN, FragmentUtil.FragmentLaunchMode.ACTIVITY);
        } else {
            // Fragment is restored
            frag = (PremiumPurchaseFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_MAIN);
        }
    }

    @Override
    protected ActivityAccessRestriction getAccessType() {
        return ActivityAccessRestriction.REQUIRES_LOGIN;
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