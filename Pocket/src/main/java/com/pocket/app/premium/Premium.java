package com.pocket.app.premium;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import com.ideashower.readitlater.R;
import com.pocket.app.settings.premium.PremiumSettingsFragment;
import com.pocket.sdk.api.generated.enums.CxtSource;
import com.pocket.sdk2.api.legacy.PocketCache;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper methods for common views and screens related to Premium.
 *
 * Pocket Premium is the paid version of Pocket that offers additional features.
 *
 * <h3> How do client's know if the user has premium and what features they have? </h3>
 * The best way is to check {Account.premium_features}. Also check out the other premium related fields on {Account}.
 * Also check out {purchase_status}.
 *
 * <h3> How do clients purchase Premium? </h3>
 * Using whatever payment platform they have available, such as iTunes like [Google Play](https://developer.android.com/google/play/billing/billing_overview) to do the purchase, and then using {purchase} to pass along and validate the purchase, which actives premium on their account.
 *
 * <h3> What features come with Premium? </h3>
 * See the list in {PremiumFeature} and https://help.getpocket.com/article/1152-what-is-the-difference-between-pocket-free-and-premium-accounts
 */
@Singleton
public class Premium {
    
    private final PocketCache pktcache;

    @Inject
    public Premium(PocketCache pktcache) {
        this.pktcache = pktcache;
    }

    public Intent newPremiumDeeplink(Context context) {
        return PremiumPurchaseActivity.newStartIntent(context, CxtSource.URL_SCHEME, false);
    }

    /**
     *
     * @param context Where the screen can be shown. This will automatically the default behaviour for contexts
     *                and current views and screens.
     * @param source An analytics key to represent where the user is as this triggers or what they iteracted with
     *               to cause this screen to open. The why.
     */
    public void showUpgradeScreen(Context context, CxtSource source) {
        showUpgradeScreen(context, source, false);
    }

    private void showUpgradeScreen(Context context, CxtSource source, boolean isRenew) {
        PremiumPurchaseActivity.startActivity(context, source, isRenew);
    }

    /**
     * Show a page where the user can renew their expired or soon to expire premium.
     * @param context
     * @param source
     */
    public void showRenewScreen(Context context, CxtSource source) {
        showUpgradeScreen(context, source, true);
    }

    /**
     * If the current user is premium and paid, show their premium status page, otherwise show the purchase page.
     * @param context
     * @param source
     */
    public void showPremiumForUserState(FragmentActivity context, CxtSource source) {
        if (pktcache.hasPremiumAndPaid()) { // Consider making context an AbsPocketActivity and just grabbing the pktcache from there rather than requiring the dependency
            PremiumSettingsFragment.show(context, null);
        } else {
            PremiumPurchaseActivity.startActivity(context, source);
        }
    }

    /**
     * Show the "you've successfully purchased premium"-like page.
     * @param activity
     * @param source
     */
    public void showPurchaseComplete(FragmentActivity activity, CxtSource source) { // TODO source not used?
        PremiumMessageActivity.startActivity(activity,
                activity.getString(R.string.lb_prem_purchase_complete_title),
                activity.getString(R.string.lb_prem_purchase_complete_message),
                activity.getString(R.string.lb_prem_purchase_complete_button),
                null);
    }
}
