package com.pocket.app.premium;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.ideashower.readitlater.R;
import com.pocket.analytics.ImpressionableInfoPageAdapter;
import com.pocket.app.help.Help;
import com.pocket.app.premium.view.PremiumUpgradeWebView;
import com.pocket.sdk.api.generated.enums.CxtSource;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.premium.billing.PremiumPurchaseHelper;
import com.pocket.sdk.premium.billing.google.GooglePlayProduct;
import com.pocket.sdk.premium.billing.google.Products;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.sync.value.Parceller;
import com.pocket.ui.view.AppBar;
import com.pocket.ui.view.button.PurchaseStateButtons;
import com.pocket.ui.view.info.InfoPage;
import com.pocket.ui.view.info.InfoPagingView;
import com.pocket.ui.view.progress.FullscreenProgressView;
import com.pocket.util.android.FormFactor;

import java.util.Arrays;

public class PremiumPurchaseFragment extends AbsPocketFragment implements PremiumPurchase.View {

    private static final String ARG_START_SOURCE = "start_source";
    private static final String ARG_RENEW = "renew";

    private static final String STATE_PENDING_PURCHASE = "pendingPurchase";
    private static final String STATE_PURCHASING = "purchasingState";

    public static PremiumPurchaseFragment newInstance(CxtSource startSource, boolean isRenew) {
        PremiumPurchaseFragment frag = new PremiumPurchaseFragment();

        Bundle args = new Bundle();
        Parceller.put(args, ARG_START_SOURCE, startSource);
        args.putBoolean(ARG_RENEW, isRenew);
        frag.setArguments(args);

        return frag;
    }

    private PurchasePresenter presenter;

    private FullscreenProgressView progress;
    private PurchaseStateButtons purchaseButton;
    private PremiumUpgradeWebView webView;
    private AppBar appBar;

    private PremiumPurchaseHelper.PurchasingState purchasingState;

    @Override
    protected View onCreateViewImpl(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_premium_purchase, container, false);
        CxtSource source = Parceller.getStringEnum(getArguments(), ARG_START_SOURCE, CxtSource.JSON_CREATOR);
        if (source != null) {
            app().tracker().bindUiEntityValue(root, source.value);
        }
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app(); // Initialize the app instance so we make sure we have access to it later no matter what (for example in onPurchaseComplete)

        presenter = new PurchasePresenter(app().build().isAmazonBuild());

        purchasingState = savedInstanceState == null || !savedInstanceState.containsKey(STATE_PURCHASING) ? PremiumPurchaseHelper.PurchasingState.IDLE
                : (PremiumPurchaseHelper.PurchasingState) savedInstanceState.getSerializable(STATE_PURCHASING);

        progress = findViewById(R.id.progress);
        purchaseButton = findViewById(R.id.purchase_button);
        webView = findViewById(R.id.flow_upgrade_web_layout);

        final PremiumAnalytics analytics = new PremiumAnalytics(pocket(), FeatureSet.PREMIUM, Interaction.on(getContext()).context, Parceller.getStringEnum(getArguments(), ARG_START_SOURCE, CxtSource.JSON_CREATOR));

        final InfoPagingView info = findViewById(R.id.info);
        ImpressionableInfoPageAdapter adapter = new ImpressionableInfoPageAdapter(
                getContext(),
                FormFactor.getWindowWidthPx(getActivity()),
                Arrays.asList(
                        new InfoPage(
                                R.drawable.pkt_prem_purchase_library,
                                getString(R.string.lb_prem_purchase_perm_lib_title),
                                getString(R.string.lb_prem_purchase_perm_lib_desc),
                                null,
                                null,
                                null,
                                null,
                                UiEntityIdentifier.PERMANENT_LIBRARY.name
                        ),
                        new InfoPage(
                                R.drawable.pkt_prem_purchase_ad_free,
                                getString(R.string.lb_prem_purchase_ad_free_title),
                                getString(R.string.lb_prem_purchase_ad_free_desc),
                                null,
                                null,
                                null,
                                null,
                                UiEntityIdentifier.NO_ADS.name
                        ),
                        new InfoPage(
                                R.drawable.pkt_prem_purchase_search,
                                getString(R.string.lb_prem_purchase_search_title),
                                getString(R.string.lb_prem_purchase_search_desc),
                                null,
                                null,
                                null,
                                null,
                                UiEntityIdentifier.TEXT_SEARCH.name
                        ),
                        new InfoPage(
                                R.drawable.pkt_prem_purchase_tags,
                                getString(R.string.lb_prem_purchase_tags_title),
                                getString(R.string.lb_prem_purchase_tags_desc),
                                null,
                                null,
                                null,
                                null,
                                UiEntityIdentifier.SMART_TAGS.name
                        ),
                        new InfoPage(
                                R.drawable.pkt_prem_purchase_highlights,
                                getString(R.string.lb_prem_purchase_highlights_title),
                                getString(R.string.lb_prem_purchase_highlights_desc),
                                null,
                                null,
                                null,
                                null,
                                UiEntityIdentifier.UNLIMITED_HIGHLIGHTS.name
                        ),
                        new InfoPage(
                                R.drawable.pkt_prem_purchase_reader,
                                getString(R.string.lb_prem_purchase_reader_title),
                                getString(R.string.lb_prem_purchase_reader_desc),
                                null,
                                null,
                                null,
                                null,
                                UiEntityIdentifier.CUSTOM_FONT.name
                        )
                )
        );
        info.bind().clear()
                .adapter(adapter)
                .addOnPageChangeListener(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        analytics.trackCarouselView(position);
                        app().tracker().bindUiEntityValue(appBar.getLeftIcon(), Integer.toString(position));
                        appBar.getLeftIcon().setUiEntityLabel(adapter.getPages().get(position).getUiEntityIdentifier());
                    }
                });

        // This captures any unhandled touches above and below the centered scrollable content and passes them to the pager for swiping.
        // This gives the view the feeling that the whole view is part of the pager
        final View content = findViewById(R.id.content);
        content.setOnTouchListener((v, event) -> content.onTouchEvent(event) || info.onTouchEvent(event));

        appBar = findViewById(R.id.appbar);
        appBar.bind()
                .withCloseIcon(UiEntityIdentifier.CLOSE_PREMIUM.name)
                .onLeftIconClick(v -> finish());

        purchaseButton.optionUnknown().setOnClickListener(v -> {
            purchaseButton.setState(PurchaseStateButtons.State.LOADING);
            presenter.getProducts();
        });

        setProgress(purchasingState);

        // bind the view, analytics, and current purchase state
        presenter.bindView(this,
                analytics,
                savedInstanceState == null ? null : savedInstanceState.getParcelable(STATE_PENDING_PURCHASE));

        // now bind the PurchaseHelper, which kicks off a request to get products
        presenter.bindPurchaseHelper(new PremiumPurchaseHelper(FeatureSet.PREMIUM.skus, getActivity(), presenter, savedInstanceState));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_PURCHASING, purchasingState);
        presenter.purchaseHelper().onSaveInstanceState(outState);
    }

    @Override
    public CxtView getActionViewName() {
        return CxtView.UPGRADE;
    }
    
    @Nullable @Override public UiEntityIdentifier getScreenIdentifier() {
        return UiEntityIdentifier.PREMIUM;
    }
    
    @Override
    public void onProductsLoading() {
        purchaseButton.setState(PurchaseStateButtons.State.LOADING);
    }

    @Override
    public void onProductsLoaded(Products products) {
        GooglePlayProduct monthly = products.getMonthly();
        GooglePlayProduct yearly = products.getYearly();
        if (monthly != null) {
            purchaseButton.option1().bind()
                    .onClick(v -> presenter.option1Click())
                    .text(R.string.prem_setting_subscription_monthly)
                    .subtext(getResources().getString(R.string.lb_prem_purchase_per_month, monthly.getPrice()));
        }
        purchaseButton.setState(PurchaseStateButtons.State.SHOW_PRICES);
    }

    @Override
    public void onPurchaseComplete() {
        boolean isRenew = getArguments().getBoolean(ARG_RENEW);

        // Find an available context to show the success message on
        // If they left the screen while the purchase was in progress, (we saw this happen for a web based purchase)
        // getContext() could be null, so try to find a visible screen, or give up showing it
        Context context = getContext();
        if (context == null) context = app().activities().getVisible();
        if (context != null) {
            PremiumMessageActivity.startActivity(
                    context,
                    context.getString(isRenew ? R.string.lb_prem_purchase_complete_renew_title : R.string.lb_prem_purchase_complete_title),
                    context.getString(isRenew ? R.string.lb_prem_purchase_complete_renew_message : R.string.lb_prem_purchase_complete_message),
                    context.getString(R.string.lb_prem_purchase_complete_button),
                    null,
                    Parceller.getStringEnum(getArguments(), ARG_START_SOURCE, CxtSource.JSON_CREATOR).name
            );
        }
        finish();
    }

    @Override
    public void setWebPaymentVisible(boolean visible) {
        if (visible) {
            webView.inflate(new PremiumUpgradeWebView.WebViewPurchaseCallbacks() {
                @Override
                public void onWebPurchaseLoadingStateChanged(boolean isLoading) {
                    progress.bind().clear().visible(isLoading);
                }

                @Override
                public void onWebPurchaseComplete() {
                    onPurchaseComplete();
                }
            });
            webView.setVisibility(View.VISIBLE);
        } else {
            webView.setVisibility(View.GONE);
        }
    }

    @Override
    public void showAmazonHelpAlert() {
        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setMessage(R.string.purchase_error_amazon_m)
                .setPositiveButton(R.string.ac_ok, (dialog, which) -> finish())
                .setNegativeButton(R.string.ac_get_help, (dialog, which) -> {Help.requestHelp(getActivity(),
                        Help.getSupportEmail(),
                        getStringSafely(R.string.purchase_error_amazon_subject),
                        null, true, false, null, null);
                        finish();
                })
                .create();
        d.setCanceledOnTouchOutside(false);
        d.show();
    }

    @Override
    public void onPurchasingStateChanged(PremiumPurchaseHelper.PurchasingState state) {
        purchasingState = state;
        setProgress(state);
    }

    private void setProgress(PremiumPurchaseHelper.PurchasingState state) {
        progress.bind().clear()
                .visible(state != PremiumPurchaseHelper.PurchasingState.IDLE)
                .progressCircle(state == PremiumPurchaseHelper.PurchasingState.ACTIVATING)
                .message(state == PremiumPurchaseHelper.PurchasingState.ACTIVATING ? getStringSafely(R.string.purchase_error_progress) : null);
    }

    @Override
    public void onProductPurchaseActivationFailedDialogDismissed() {
        finish();
    }

    @Override
    public void onProductsLoadFailed() {
        purchaseButton.setState(PurchaseStateButtons.State.SHOW_UNKNOWN_PRICE);
    }

    @Override
    public void finish() {
        super.finish();
        presenter.onClose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unbind();
    }
}
