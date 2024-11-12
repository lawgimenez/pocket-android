package com.pocket.sdk.premium.billing;


public abstract class PremiumProduct {
		
	private final String mPrice;
	private final String mTitle;
	private final String mDescription;
	
    public PremiumProduct(String price, String title, String description) {
        mPrice = price;
        mTitle = title;
        mDescription = description;
    }
	
    public String getPrice() { return mPrice; }
    public String getTitle() { return mTitle; }
    public String getDescription() { return mDescription; }
    
    @Override
    public String toString() {
    	return getTitle() + "," + getPrice() + "," + getDescription();
    }
	
}