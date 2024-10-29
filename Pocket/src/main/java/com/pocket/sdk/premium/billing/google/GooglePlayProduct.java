package com.pocket.sdk.premium.billing.google;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.billingclient.api.SkuDetails;
import com.pocket.sdk.premium.billing.PremiumProduct;

import org.json.JSONException;

import androidx.annotation.NonNull;

public class GooglePlayProduct extends PremiumProduct implements Parcelable {
	
	private final SkuDetails skuDetails;
	
	private String purchaseData;
	
	GooglePlayProduct(SkuDetails skuDetails) {
		super(skuDetails.getPrice(), skuDetails.getTitle(), skuDetails.getDescription());
		this.skuDetails = skuDetails;
	}
	
	public void setPurchased(String purchaseData) {
		this.purchaseData = purchaseData;
	}
	
	public boolean isPurchased() {
		return purchaseData != null;
	}
	
	/**
	 * @return If {@link #isPurchased()}, this will return the blob of json that represents the purchase. Otherwise null. 
	 */
	public String getPurchaseData() {
		return purchaseData;
	}
	
	public String getSku() { return skuDetails.getSku(); }
	public String getType() { return skuDetails.getType(); }
	public String getPriceMicros() { return String.valueOf(skuDetails.getPriceAmountMicros()); }
	public String getPriceCurrencyCode() { return skuDetails.getPriceCurrencyCode(); }
	SkuDetails getSkuDetails() { return skuDetails; }
	
	@Override public @NonNull String toString() {
		return skuDetails.getOriginalJson();
	}
	
	// Parcelling
	
	@Override
	public int describeContents() {
		 return 0;
	 }

	@Override
	public void writeToParcel(Parcel out, int flags) {
		 out.writeString(skuDetails.getOriginalJson());
		 out.writeString(purchaseData);
	 }

	public static final Parcelable.Creator<GooglePlayProduct> CREATOR = new Parcelable.Creator<GooglePlayProduct>() {
		@Override
		public GooglePlayProduct createFromParcel(Parcel in) {
			final String json = in.readString();
			try {
				final GooglePlayProduct product = new GooglePlayProduct(new SkuDetails(json));
				product.setPurchased(in.readString());
				return product;
			} catch (JSONException e) {
				throw new AssertionError("Bad data written to parcel: " + json);
			}
		}

		@Override
		public GooglePlayProduct[] newArray(int size) {
			 return new GooglePlayProduct[size];
		 }
	};
}