package com.pocket.app.premium;

import com.pocket.sdk.premium.billing.PremiumPurchaseHelper;
import com.pocket.sdk.premium.billing.google.GooglePlayProduct;
import com.pocket.sdk.premium.billing.google.Products;

public class PurchasePresenter implements PremiumPurchase.Presenter, PremiumPurchaseHelper.PurchaseListener {

    private PremiumPurchase.View view;

    private Products products;
    private PremiumPurchaseHelper purchaseHelper;
    private PremiumPurchase.Analytics analytics;
    private final boolean isAmazonBuild;
    private GooglePlayProduct pendingPurchase;

    PurchasePresenter(boolean isAmazonBuild) {
        this.isAmazonBuild = isAmazonBuild;
    }

    @Override
    public void bindView(PremiumPurchase.View view, PremiumPurchase.Analytics analytics, GooglePlayProduct pendingPurchase) {
        this.view = view;
        this.analytics = analytics;
        this.pendingPurchase = pendingPurchase;

        this.view.onProductsLoading(); // purchase helper instantiation makes the first call to get products
        this.analytics.trackView();
    }

    @Override
    public void bindPurchaseHelper(PremiumPurchaseHelper helper) {
        this.purchaseHelper = helper;
    }

    @Override
    public void unbind() {
        purchaseHelper.onDestroy();
        purchaseHelper = null;
        analytics = null;
        view = null;
    }

    @Override
    public void getProducts() {
        view.onProductsLoading();
        purchaseHelper.getProductsAsync();
    }

    @Override
    public void option1Click() {
        startPurchase(products.getMonthly());
    }

    @Override
    public void option2Click() {
        startPurchase(products.getYearly());
    }

    private void startPurchase(GooglePlayProduct product) {
        pendingPurchase = product;
        purchaseHelper.startPurchase(product);
        analytics.trackPurchaseClick(product);
    }

    @Override
    public PremiumPurchaseHelper purchaseHelper() {
        return purchaseHelper;
    }

    @Override
    public void onClose() {
        analytics.trackClose();
    }

    @Override
    public void onProductsLoaded(Products products) {
        PurchasePresenter.this.products = products;
        if (view != null) view.onProductsLoaded(products);
    }

    @Override
    public void onProductsLoadFailed() {
        if (view != null) view.onProductsLoadFailed();
    }

    @Override
    public void onGooglePlayUnavailable() {
        showWebPaymentFlow();
    }

    @Override
    public void showWebPaymentFlow() {
        if (isAmazonBuild) {
            if (view != null) view.showAmazonHelpAlert();
        } else {
            if (view != null) view.setWebPaymentVisible(true);
        }
    }

    @Override
    public void onPurchasingStateChanged(PremiumPurchaseHelper.PurchasingState state) {
        if (view != null) view.onPurchasingStateChanged(state);
        if (state == PremiumPurchaseHelper.PurchasingState.ACTIVATING) {
            if (analytics != null) analytics.trackPurchaseSuccess(pendingPurchase);
        }
    }

    @Override
    public void onPurchaseFailed(boolean isCancel) {
        if (analytics != null) analytics.trackPurchaseFailure(pendingPurchase, isCancel);
    }

    @Override
    public void onPremiumPurchased() {
        if (view != null) view.onPurchaseComplete();
        if (analytics != null) analytics.trackPurchaseSuccess(pendingPurchase);
    }

    @Override
    public void onProductPurchaseActivationFailedDialogDismissed() {
        if (view != null) view.onProductPurchaseActivationFailedDialogDismissed();
    }
}
