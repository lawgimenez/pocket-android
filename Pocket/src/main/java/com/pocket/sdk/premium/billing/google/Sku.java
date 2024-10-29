package com.pocket.sdk.premium.billing.google;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A list of skus that represent a single product. Contains the primary sku, which
 * is the current one that the user can buy, and if needed, a list of legacy skus
 * for that product that the app used in the past and they may have purchased.
 *
 * A single product can have legacy skus because in order to change the prices
 * of a subscription product, we have to make a new sku and start serving that
 * one instead of the old one.
 *
 */
class Sku {

    private final String mCurrent;
    private final Set<String> mAll = new HashSet<>();

    Sku(String current, String... legacy) {
        mCurrent = current;
        mAll.add(current);
        if (legacy != null) {
            Collections.addAll(mAll, legacy);
        }
    }

    Set<String> getAll() {
        return mAll;
    }

    boolean isCurrent(String sku) {
        return mCurrent.equals(sku);
    }
}
