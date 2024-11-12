package com.pocket.sdk.api;

import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.PocketThings;
import com.pocket.sync.space.Holder;
import com.pocket.sync.thing.Thing;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Keeps various Send to Friend related {@link Thing}s persisted and synced and provides some tools for working with that feature.
 * <p>
 * Note: We're in the process of deprecating and removing Send to Friend.
 * In phase 1 we removed it is a sharing option (from the share sheet and from save overlay).
 * It is still possible to receive new items through Send to Friend and to view previously 
 * received items.
 */
@Singleton
public class SendToFriend {

	@Inject
	public SendToFriend(Pocket pocket, AppSync appsync) {
		pocket.setup(() -> {
			Holder holder = Holder.persistent("stf");
			PocketThings t = pocket.spec().things();
			pocket.remember(holder, t.friends().build());
			pocket.remember(holder, t.recentFriends().build());
			pocket.remember(holder, t.autoCompleteEmails().build());
			
			pocket.initialize(t.friends().build());
			pocket.initialize(t.recentFriends().build());
			pocket.initialize(t.autoCompleteEmails().build());
		});
		
		appsync.addInitialFlags(g -> g.forcemails(1));
		appsync.addFlags(g -> g.shares(1));
	}
}
