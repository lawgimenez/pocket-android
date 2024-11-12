package com.pocket.sdk.premium.billing.google;

import java.util.ArrayList;
import java.util.List;

/**
 * The monthly and yearly skus that we can present to the user to purchase.
 */
public class ProductList {

    private final ArrayList<String> all = new ArrayList<>();
    private final ArrayList<String> monthlys = new ArrayList<>();
    private final ArrayList<String> yearlys = new ArrayList<>();
    private final String type;
    private final Sku monthlySku;
    private final Sku yearlySku;

    ProductList(String type, Sku monthlySku, Sku yearlySku) {
        this.type = type;
        this.monthlySku = monthlySku;
        this.yearlySku = yearlySku;

        addAlternativeSkusMonthly(monthlySku);
        addAlternativeSkusYearly(yearlySku);
    }

    ProductList addAlternativeSkusMonthly(Sku value) {
        all.addAll(value.getAll());
        monthlys.addAll(value.getAll());
        return this;
    }

    ProductList addAlternativeSkusYearly(Sku value) {
        all.addAll(value.getAll());
        yearlys.addAll(value.getAll());
        return this;
    }

    public List<String> getAll() {
        return all;
    }

    public boolean isMonthly(String sku) {
        return monthlys.contains(sku);
    }

    public boolean isYearly(String sku) {
        return yearlys.contains(sku);
    }

    public boolean isCurrentMonthly(String sku) {
        return monthlySku.isCurrent(sku);
    }

    public boolean isCurrentYearly(String sku) {
        return yearlySku.isCurrent(sku);
    }

    String getType() {
        return type;
    }
}
