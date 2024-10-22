package com.pocket.sdk.premium.billing;

import com.pocket.sdk.util.ErrorReport;

/**
 * A simplified error. There are only a few {@link Type}s.
 */
public class PremiumBillingError {
	
	public enum Type {
		/** Failed because Google Play Billing is not available. Further attempts will likely also fail */
		FATAL,
		/** User cancelled, likely can ignore */
		CANCEL,
		/** A Retry may fix */
		TEMPORARY,
		/** It looks like the user already owns this product */
		ALREADY_PURCHASED
	}
	
	private final Type mType;
	private final ErrorReport mReportableError; // TODO i think we can remove this, all cases end up being null?
	
	/**
	 * 
	 * @param type
	 * @param reportableError Optional error that might be passed onto support via a Get Help button. While these messages won't be shown directly to a user, they <b>may see them</b> in the email we create on their behalf.
	 */
	public PremiumBillingError(Type type, ErrorReport reportableError) {
		mType = type;
		mReportableError = reportableError;
	}
	
	public PremiumBillingError(Type type) {
		this(type, null);
	}
	
	public Type getType() {
		return mType;
	}
	
	public ErrorReport getReportableError() {
		return mReportableError;
	}
	
	// Note, this class may be flushed out to offer more specific messages
	
}