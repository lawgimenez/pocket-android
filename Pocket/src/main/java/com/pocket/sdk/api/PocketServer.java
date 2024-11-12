package com.pocket.sdk.api;

import com.ideashower.readitlater.BuildConfig;
import com.pocket.app.AppMode;
import com.pocket.sdk.api.generated.thing.ArticleView;
import com.pocket.util.prefs.IntPreference;
import com.pocket.util.prefs.Preferences;
import com.pocket.util.prefs.StringPreference;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Pocket API constants such as endpoint urls, params, response fields etc.
 * Helps decide which Pocket API should be used.
 */
@Singleton
public class PocketServer {
	
	public static final String API_PRODUCTION = "https://api.getpocket.com";
	
	public static final String ARTICLE_VIEW = ArticleView.REMOTE.address;
	public static final String PERM_LIBRARY = "https://text.getpocket.com/v3beta/loadWebCache";
	
	private static final String SNOWPLOW_PROD_COLLECTOR = "https://getpocket.com";
	private static final String SNOWPLOW_POST_PATH_PROD = "t/e";

	private static final String SNOWPLOW_DEV_COLLECTOR = "https://com-getpocket-prod1.mini.snplow.net";

	private static final String SNOWPLOW_POST_PATH_DEV = "com.snowplowanalytics.snowplow/tp2";

	private final String api;
	private final String articleView;
	private final String snowplowCollector;

	private final String snowplowPostPath;

	private final DevConfig devConfig;

	@Inject
	public PocketServer(AppMode mode, Preferences prefs) {
		devConfig = new DevConfig(prefs);
		
		if (mode.isForInternalCompanyOnly()) {
			api = devConfig.api();
			articleView = devConfig.articleView();
			snowplowCollector = devConfig.snowplowCollector();
			snowplowPostPath = devConfig.snowplowPostPath();
		} else {
			api = API_PRODUCTION;
			articleView = ARTICLE_VIEW;
			snowplowCollector = SNOWPLOW_PROD_COLLECTOR;
			snowplowPostPath = SNOWPLOW_POST_PATH_PROD;
		}
	}
	
	public String api() { return api;}
	
	public String articleView() { return articleView;}
	
	public String snowplowCollector() { return snowplowCollector; }

	public String snowplowPostPath() { return snowplowPostPath; }

	public DevConfig devConfig() { return devConfig; }
	
	public static class DevConfig {
		public final IntPreference api;
		public final StringPreference devServerPrefix;
		
		public final IntPreference parserApi;
		public final StringPreference customParserApi;
		
		public final IntPreference snowplow;
		public final StringPreference snowplowMicro;
		
		public DevConfig(Preferences prefs) {
			Preferences dcfig = prefs.group("dcfig_");
			api = dcfig.forApp("a", 0);
			devServerPrefix = dcfig.forApp("dspref", (String) null);
			
			parserApi = dcfig.forApp("atp", 0);
			customParserApi = dcfig.forApp("catp", PocketServer.ARTICLE_VIEW);
			
			snowplow = dcfig.forApp("snwplwclctr", 0);
			snowplowMicro = dcfig.forApp("snwplwmcr", "192.168.1.?");
		}
		
		private String api() {
			switch (api.get()) {
				case 0: return API_PRODUCTION;
				case 1: return "https://" + devServerPrefix.get() + BuildConfig.API_DEV_SUFFIX;
				default:
					// Unknown / Invalid - reset to a default
					return API_PRODUCTION;
			}
		}
		
		private String articleView() {
			switch (parserApi.get()) {
				case 0: return ARTICLE_VIEW;
				case 1: return customParserApi.get();
				default:
					// Unknown / Invalid - reset to a default
					return ARTICLE_VIEW;
			}
		}
		
		private String snowplowCollector() {
			switch (snowplow.get()) {
				case 0: return SNOWPLOW_PROD_COLLECTOR;
				case 1: return SNOWPLOW_DEV_COLLECTOR;
				case 2: return "http://" + snowplowMicro.get();
				default:
					// Unknown / Invalid - reset to a default
					return SNOWPLOW_PROD_COLLECTOR;
			}
		}

		private String snowplowPostPath() {
			switch (snowplow.get()) {
				case 0: return SNOWPLOW_POST_PATH_PROD;
				case 1:
				case 2: return SNOWPLOW_POST_PATH_DEV;
				default:
					// Unknown / Invalid - reset to a default
					return SNOWPLOW_POST_PATH_PROD;
			}
		}
	}
}
