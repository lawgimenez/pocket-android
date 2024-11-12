package com.pocket.sdk.http;

/**
 * Information about the device's internet connection.
 */
public interface NetworkStatus {
	/**
	 * @return true if the device has an active internet connection.
	 * 	Implementations will attempt their best to have this mean that there is an actual available connection to the internet, not just connected to a network.
	 * 	However some device's may or may not support that level of correctness. At the very least this indicates connected to some network.
	 */
	boolean isOnline();
	
	/**
	 * @return true if the connection has been online for at least the X milliseconds with no known disconnections or loss in service.
	 * 			If the provided duration is longer than the total known time, it will use the total known time instead of your provided duration.
	 */
	boolean isStable(long duration);
	
	/**
	 * @return true if the active network is a wifi network.
	 */
	boolean isWifi();
	
	/**
	 * @return true if the active network is unmetered.
	 */
	boolean isUnmetered();
	
	void addListener(Listener listener);
	void removeListener(Listener listener);
	
	interface Listener {
		/** Some property of the network connection has changed (online, wifi or unmetered). Requery as needed. Warning: This could be invoked from a background thread. */
		void onStatusChanged(NetworkStatus status);
	}
	
}
