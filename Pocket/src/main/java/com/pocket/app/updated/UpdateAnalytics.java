package com.pocket.app.updated;

import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.app.VersionUtil;
import com.pocket.app.build.Versioning;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtPage;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.value.Timestamp;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Tracks when the app is upgraded and from what versions.
 * Useful for making reports to see how far back upgrade paths might need to realistically support.
 * https://docs.google.com/spreadsheets/d/1My7HyOBBiC4_RSg9oBUQxtT7CWxCAM_tzDs_N7LGj3s/edit#gid=0
 */
@Singleton
public class UpdateAnalytics {

	@Inject
	public UpdateAnalytics(Pocket pocket, Versioning versioning, ErrorHandler errorHandler) {
		if (versioning.isUpgrade()) {
			try {
				int from = versioning.from();
				int to = versioning.to();
				// It isn't safe to invoke Interaction during this constructor since the components aren't all setup.  So just make an action without cxt data. Probably isn't important for this event and posting to a handler might risk the event not firing if the process is killed quickly in the background.
				pocket.sync(null, pocket.spec().actions().pv_wt()
						.time(Timestamp.now())
						.view(CxtView.MOBILE)
						.section(CxtSection.APP_UPGRADE)
						.page(CxtPage.create(VersionUtil.toVersionName(from)))
						.action_identifier(CxtEvent.create(VersionUtil.toVersionName(to)))
						.build());
			} catch (Throwable t) {
				// Don't crash the app for this analytic.
				errorHandler.reportError(t);
			}
		}
	}
}
