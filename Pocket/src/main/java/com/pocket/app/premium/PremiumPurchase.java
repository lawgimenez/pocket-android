package com.pocket.app.premium;

import com.pocket.sdk.premium.billing.PremiumPurchaseHelper;
import com.pocket.sdk.premium.billing.google.GooglePlayProduct;
import com.pocket.sdk.premium.billing.google.Products;

interface PremiumPurchase {
    interface View {
        void onProductsLoading();
        void onProductsLoaded(Products products);
        void onPurchaseComplete();
        void setWebPaymentVisible(boolean visible);
        void showAmazonHelpAlert();
        void onPurchasingStateChanged(PremiumPurchaseHelper.PurchasingState state);
        void onProductPurchaseActivationFailedDialogDismissed();
        void onProductsLoadFailed();
    }
    interface Presenter {
        /** Bind everything needed for a purchase, aside from the {@link PremiumPurchaseHelper} */
        void bindView(View view, Analytics analytics, GooglePlayProduct pendingPurchase);
        /**
         * Binds the {@link PremiumPurchaseHelper}. Because instantiation of the helper makes a request for products
         * immediately, binding it after the {@link View} is necessary to avoid race conditions
         */
        void bindPurchaseHelper(PremiumPurchaseHelper helper);
        void unbind();
        void getProducts();
        void option1Click();
        void option2Click();
        void onClose();
        PremiumPurchaseHelper purchaseHelper();
    }
    interface Analytics {
        void trackCarouselView(int index);
        void trackPurchaseClick(GooglePlayProduct product);
        void trackPurchaseFailure(GooglePlayProduct product, boolean isCancel);
        void trackPurchaseSuccess(GooglePlayProduct product);
        void trackView();
        void trackClose();
    }
}
