package com.pocket.sdk.util

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class `PocketUrlUtil should` {
    @Test fun `recognize explore url in getpocket domain`() {
        assertTrue {
            PocketUrlUtil.isExploreUrl("https://getpocket.com/explore/item/how-britney-spears-changed-pop-with-baby-one-more-time")
        }
    }

    @Test fun `recognize explore url in dev domain`() {
        assertTrue {
            PocketUrlUtil.isExploreUrl("https://example.getpocket.dev/explore/item/how-britney-spears-changed-pop-with-baby-one-more-time")
        }
    }
    
    @Test fun `not recognize a topic url in getpocket domain as an explore url`() {
        assertFalse {
            PocketUrlUtil.isExploreUrl("https://getpocket.com/explore/science")
        }
    }

    @Test fun `not recognize a topic url in dev domain as an explore url`() {
        assertFalse {
            PocketUrlUtil.isExploreUrl("https://example.getpocket.dev/explore/science")
        }
    }
}
