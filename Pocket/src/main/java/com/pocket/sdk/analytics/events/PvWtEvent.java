package com.pocket.sdk.analytics.events;

import com.pocket.app.App;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.action.PvWt;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtPage;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtSource;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk2.analytics.context.Interaction;

/**
 * Helper for working with {@link com.pocket.sdk.api.generated.action.PvWt} tracking events.
 * Create an instance with your tracking details and then invoke one of the various send methods
 * to fire the event. Can be reused and fired any number of times by invoking the send method.
 */
public class PvWtEvent {
	
	private final PvWt event;
	
	public PvWtEvent(int typeId, CxtSection section, CxtPage page, CxtEvent action) {
		event = App.getApp().pocket().spec().actions().pv_wt()
				.view(CxtView.MOBILE)
				.type_id(typeId)
				.section(section)
				.page(page)
				.action_identifier(action)
				.build();
	}
	
	public void send() {
		send(null, null, null);
	}
	
	/**
	 * Creates a new stat of this type and commits it asynchronously.
	 */
	public void send(String pageParams) {
		send(pageParams, null, null);
	}
	
	/**
	 * Creates a new stat of this type and commits it asynchronously.
	 */
	public void send(String pageParams, CxtSource source) {
		send(pageParams, source, null);
	}

	public void send(String pageParams, CxtSource source, CxtSection section) {
		Pocket pocket = App.getApp().pocket();
		Interaction it = Interaction.on(App.getApp());
		pocket.sync(null, event.builder()
				.section(section != null ? section : event.section)
				.page_params(pageParams)
				.source(source)
				.time(it.time)
				.context(it.context)
				.build());
	}
	
}
