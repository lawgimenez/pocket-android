package com.pocket.sync.value.binary;

import com.pocket.sdk.api.generated.thing.Features;
import com.pocket.sync.ThingMock;
import com.pocket.sync.thing.Thing;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class CompressionTest {
	
	@Test
	public void compression() throws Exception {
		List<Thing> tests = Arrays.asList(
				ThingMock.thing().feed(),
				ThingMock.thing().getNotifications(),
				ThingMock.thing().getProfileFeed(),
				new Features.Builder()
						.show_ios_premium_upsells(true)
						.show_list_counts(true)
						.show_recs(true)
						.show_premium_icon(true)
						.build()
		);
		
		for (Thing t : tests) {
			ByteWriter b = new ByteWriter();
			t.compress(b);
			byte[] bs = b.readByteArray();
			
			ByteReader br = new ByteReader(bs);
			Thing restored = (Thing) t.getByteCreator().create(br);
			
			Assert.assertTrue(restored.equals(Thing.Equality.STATE, t));
		}
	}
	
}