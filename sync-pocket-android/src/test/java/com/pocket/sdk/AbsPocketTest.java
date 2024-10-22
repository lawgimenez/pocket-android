package com.pocket.sdk;

import com.pocket.sdk.api.endpoint.AppInfo;
import com.pocket.sdk.api.endpoint.DeviceInfo;
import com.pocket.sdk.api.source.LoggingPocket;
import com.pocket.sdk.api.source.PocketRemoteSource;
import com.pocket.sdk.network.EclecticOkHttpClient;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sync.source.result.SyncException;
import com.pocket.util.java.Logs;

import org.junit.BeforeClass;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * TODO Documentation
 */
public abstract class AbsPocketTest {
	
	public PocketInstance newPocket() throws SyncException {
		PocketInstance p = new PocketInstance();
		p.testAccount();
		return p;
	}
	
	private static boolean isSetup;
	
	@BeforeClass
	public static void setup() throws Exception {
		if (!isSetup) {
			Logs.logger(Logs.SOUT);
			isSetup = true;
		}
	}
	
	public class PocketInstance {
		
		public final Pocket pocket;
		public final EclecticHttp http;
		
		public PocketInstance() {
			AppInfo app = new AppInfo(
					"5513-8646141fb5902c766272e74d",
					"Pocket",
					"Free",
					"6.7.0.0",
					"play",
					"play");
			
			DeviceInfo device = new DeviceInfo("Android", "9.0", "Test", "Test", "Test", "handset", "en-us", null);

			http = new EclecticOkHttpClient(
					new OkHttpClient.Builder()
							.connectTimeout(5, TimeUnit.SECONDS)
							.readTimeout(60, TimeUnit.SECONDS)
							.build()
			);

			String name = UUID.randomUUID().toString();
			
			Pocket.Config config = new Pocket.Config.Builder(name, app, device)
					.remote(new PocketRemoteSource(http))
					.build();
			
			pocket = LoggingPocket.debug(config, System.out::println);
		}
		
		public void testAccount() throws SyncException {
			pocket.user().login("test+a3828@readitlater.com", "abcdef", new Pocket.AuthenticationExtras(null, null, null));
		}
		
		public void setNetworkEnabled(boolean enabled) {
			http.setEnabled(enabled);
		}
		
	}
	
}