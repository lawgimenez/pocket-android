package com.pocket.app.premium;

import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.premium.billing.google.GoogleBillingUtil;
import com.pocket.sdk.premium.billing.google.ProductList;

/**
 * What features should be shown in the {@link com.pocket.app.premium.view.PremiumUpgradeView} and
 * what {@link ProductList} should be used for pricing options?
 * <p>
 * There are some premade options as static fields in this class.
 */
public class FeatureSet {

    public static FeatureSet PREMIUM = new FeatureSet(CxtSection.PREMIUM, GoogleBillingUtil.PREMIUM);

    public final boolean archive;
    public final boolean spoc;
    public final boolean tags;
    public final boolean search;

    public final ProductList skus;

    public final CxtSection trackSection;

    public FeatureSet(CxtSection trackSection, ProductList skus) {
        this(trackSection, true, true, true, true, skus);
    }

    public FeatureSet(CxtSection trackSection, boolean archive, boolean spoc, boolean tags, boolean search, ProductList skus) {
        this.trackSection = trackSection;
        this.archive = archive;
        this.spoc = spoc;
        this.tags = tags;
        this.search = search;
        this.skus = skus;
    }

}
