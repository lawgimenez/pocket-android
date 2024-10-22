package com.pocket.app.premium;

import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.action.PvWt;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtPage;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtSource;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.premium.billing.google.GooglePlayProduct;
import com.pocket.sdk.premium.billing.google.ProductList;

/**
 * Premium purchase page analytics, originally defined here:
 * https://docs.google.com/spreadsheets/d/1dfP1GhyIgg2GEa4uXXIOR6My3dlxW9WSBDOCwgO2LGg/edit#gid=1214765845
 */
class PremiumAnalytics implements PremiumPurchase.Analytics {

    private Pocket pocket;
    private ActionContext actionContext;

    private CxtSource startSource;
    private CxtSection trackSection;

    private ProductList products;
    private boolean hasTrackedPurchase;

    PremiumAnalytics(Pocket pocket, FeatureSet featureSet, ActionContext actionContext, CxtSource startSource) {
        this.pocket = pocket;
        this.products = featureSet.skus;
        this.startSource = startSource;
        this.trackSection = featureSet.trackSection;
        // Override `cxt_view` to be the same value we send in `pv_wt.view`.
        this.actionContext = actionContext.builder().cxt_view(CxtView.MOBILE).build();
    }

    private boolean isMonthlySku(GooglePlayProduct product) {
        return products.isMonthly(product.getSku());
    }

    @Override
    public void trackCarouselView(int index) {
        track(CxtPage.UPGRADE, CxtEvent.SCROLL_ACROSS, Integer.toString(index + 1)); // event indexing starts at 1
    }

    @Override
    public void trackPurchaseClick(GooglePlayProduct product) {
        String price = product.getPrice();
        if (isMonthlySku(product)) {
            track(CxtPage.UPGRADE, CxtEvent.CLICK_MONTHLY, price);
        } else {
            track(CxtPage.UPGRADE, CxtEvent.CLICK_ANNUAL, price);
        }
    }

    @Override
    public void trackPurchaseFailure(GooglePlayProduct product, boolean isCancel) {
        if (product == null) {
            return; // Not expected, but avoid crashing
        }
        boolean isMonthly = isMonthlySku(product);
        String price = product.getPrice();

        if (isCancel) {
            if (isMonthly) {
                track(CxtPage.UPGRADE, CxtEvent.CANCEL_MONTHLY, price);
            } else {
                track(CxtPage.UPGRADE, CxtEvent.CANCEL_ANNUAL, price);
            }
        } else {
            if (isMonthly) {
                track(CxtPage.UPGRADE, CxtEvent.ERROR_MONTHLY, price);
            } else {
                track(CxtPage.UPGRADE, CxtEvent.ERROR_ANNUAL, price);
            }
        }
    }

    @Override
    public void trackPurchaseSuccess(GooglePlayProduct product) {
        if (product == null) {
            return; // Not expected, but avoid crashing
        }
        if (hasTrackedPurchase) {
            return; // It may come through the activation state or directly to the purchase callback, so to avoid triggering twice we check if we have already been here.
        }
        hasTrackedPurchase = true;
        boolean isMonthly = isMonthlySku(product);
        String price = product.getPrice();

        if (isMonthly) {
            track(CxtPage.CONFIRMATION, CxtEvent.CONFIRM_MONTHLY, price);
        } else {
            track(CxtPage.CONFIRMATION, CxtEvent.CONFIRM_ANNUAL, price);
        }
    }

    @Override
    public void trackView() {
        track(CxtPage.UPGRADE, CxtEvent.VIEW_UPSELL, null);
    }

    @Override
    public void trackClose() {
        track(CxtPage.UPGRADE, CxtEvent.CLICK_CLOSE, null);
    }

    private void track(CxtPage page, CxtEvent action, String pageParams) {
        PvWt.Builder pvwt = pocket.spec().actions().pv_wt()
                .page(page)
                .view(CxtView.MOBILE)
                .action_identifier(action)
                .source(startSource)
                .time(Timestamp.now())
                .section(trackSection)
                .context(actionContext);
        if (pageParams != null) {
            pvwt.page_params(pageParams);
        }
        pocket.sync(null, pvwt.build());
    }
}
