package com.pocket.sdk.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import com.pocket.util.android.ApiLevel;
import com.pocket.util.java.Safe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static android.net.NetworkCapabilities.NET_CAPABILITY_FOREGROUND;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;

/**
 * A {@link NetworkStatus} implementation using Android's {@link ConnectivityManager}'s listeners.
 * There is slightly more fine tuned accuracy on api levels 23+ and 24+ as some other apis become available.
 */
public class AndroidNetworkStatus implements NetworkStatus {
	
	private final ConnectivityManager manager;
	private final Set<Network> online = new HashSet<>();
	private final Set<Network> wifi = new HashSet<>();
	private final Set<Listener> listeners = new HashSet<>();
	private boolean isOnline;
	private boolean isWifi;
	private boolean isUnmetered;
	private Network active;
	private long lastDisconnect;
	
	public AndroidNetworkStatus(Context context) {
		manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (manager == null) throw new AssertionError("Couldn't get ConnectivityManager");
		
		for (Network n : manager.getAllNetworks()) {
			NetworkCapabilities c = manager.getNetworkCapabilities(n);
			if (c == null) continue;
			if (c.hasCapability(NET_CAPABILITY_INTERNET)
					&& (ApiLevel.isPreP() || c.hasCapability(NET_CAPABILITY_FOREGROUND))
					&& (c.hasCapability(NET_CAPABILITY_VALIDATED))) {
				online(n);
			}
		}
		updateStatus();
			
		NetworkRequest.Builder b = new NetworkRequest.Builder();
		b.addCapability(NET_CAPABILITY_INTERNET);
		b.addCapability(NET_CAPABILITY_VALIDATED);
		manager.registerNetworkCallback(b.build(), new ConnectivityManager.NetworkCallback() {
			@Override
			public void onAvailable(@NonNull Network network) {
				online(network);
				updateStatus();
			}
			
			@Override
			public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
				updateCapabilities(network, networkCapabilities);
				updateStatus();
			}
			
			@Override
			public void onLost(@NonNull Network network) {
				lost(network);
				updateStatus();
			}
		});
		if (ApiLevel.isNougatOrGreater()) {
			manager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
				
				@Override
				public void onAvailable(@NonNull Network network) {
					active = network;
					updateStatus();
				}
				
				@Override
				public void onLost(@NonNull Network network) {
					if (network.equals(active)) active = null;
					updateStatus();
				}
			});
		}
	}
	
	private synchronized void online(Network network) {
		online.add(network);
		updateCapabilities(network, manager.getNetworkCapabilities(network));
	}
	
	private synchronized void updateCapabilities(Network network, NetworkCapabilities nc) {
		wifi.remove(network);
		if (nc == null) return;
		if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
			wifi.add(network);
		}
	}
	
	private synchronized void lost(Network network) {
		online.remove(network);
		wifi.remove(network);
		if (online.isEmpty()) {
			lastDisconnect = System.currentTimeMillis();
		}
	}
	
	private synchronized void updateStatus() {
		boolean newOnline = !online.isEmpty();
		boolean newWifi;
		boolean newUnmetered;
		if (newOnline) {
			if (active != null) {
				newWifi = wifi.contains(active);
			} else {
				// We don't know which one is active, so have to fall back to the older method
				newWifi = Safe.getBoolean(() -> manager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI);
			}
			newUnmetered = !manager.isActiveNetworkMetered();
		} else {
			newWifi = false;
			newUnmetered = false;
		}
		
		boolean changed =
				newOnline != isOnline ||
				newWifi != isWifi ||
				newUnmetered != isUnmetered;

		isOnline = newOnline;
		isWifi = newWifi;
		isUnmetered = newUnmetered;
		
		if (changed) {
			for (Listener listener : new ArrayList<>(listeners)) {
				listener.onStatusChanged(this);
			}
		}
	}
	
	@Override
	public synchronized boolean isOnline() {
		return isOnline;
	}
	
	@Override
	public synchronized boolean isStable(long millis) {
		if (!isOnline()) return false;
		return lastDisconnect == 0 || lastDisconnect < System.currentTimeMillis() - millis;
	}
	
	@Override
	public synchronized boolean isWifi() {
		return isWifi;
	}
	
	@Override
	public synchronized boolean isUnmetered() {
		return isUnmetered;
	}
	
	@Override
	public synchronized void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	@Override
	public synchronized void removeListener(Listener listener) {
		listeners.remove(listener);
	}
}
