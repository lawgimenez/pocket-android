package com.pocket.sdk.http.sentry

import java.util.regex.Pattern
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExcludedTargetsTest {
    private val regex = ExcludedTargets()
        .apply {
            exclude("https://api.getpocket.com/graphql", ExcludedTargets.Mode.Exact)
            exclude("https://text.getpocket.com/v3beta/mobile", ExcludedTargets.Mode.Exact)
            exclude("https://api.getpocket.com/v3/send", ExcludedTargets.Mode.Exact)
            exclude("https://api.getpocket.com/v3/get", ExcludedTargets.Mode.Exact)
            exclude("https://api.getpocket.com/v3/fetch", ExcludedTargets.Mode.Exact)
            exclude("https://pocket-image-cache.com", ExcludedTargets.Mode.Prefix)
        }
        .toRegex()


    @Test fun includesExample() {
        assertTrue(Pattern.matches(regex, "https://example.com"))
    }

    @Test fun excludesGraph() {
        assertFalse(Pattern.matches(regex, "https://api.getpocket.com/graphql"))
    }

    @Test fun excludesSend() {
        assertFalse(Pattern.matches(regex, "https://api.getpocket.com/v3/send"))
    }

    @Test fun excludesFetch() {
        assertFalse(Pattern.matches(regex, "https://api.getpocket.com/v3/fetch"))
    }

    @Test fun excludesGet() {
        assertFalse(Pattern.matches(regex, "https://api.getpocket.com/v3/get"))
    }

    @Test fun includesGetItemAudio() {
        assertTrue(Pattern.matches(regex, "https://api.getpocket.com/v3/getItemAudio"))
    }

    @Test fun excludesImageCache() {
        assertFalse(Pattern.matches(regex,"https://pocket-image-cache.com/filters:format(jpeg):quality(60):no_upscale():strip_exif()/https:%2F%2Fwww.washingtonpost.com%2Fwp-apps%2Fimrs.php%3Fsrc=https:%2F%2Farc-anglerfish-washpost-prod-washpost.s3.amazonaws.com%2Fpublic%2FA6IIWGCR6JASVF7QBOUIUCBR6E.jpg&w=1440"))
    }
}
