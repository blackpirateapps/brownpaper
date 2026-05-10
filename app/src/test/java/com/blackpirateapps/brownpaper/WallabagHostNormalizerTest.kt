package com.blackpirateapps.brownpaper

import com.blackpirateapps.brownpaper.data.wallabag.WallabagHostNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallabagHostNormalizerTest {
    @Test
    fun `adds https scheme and removes trailing slash`() {
        assertEquals(
            "https://app.wallabag.it",
            WallabagHostNormalizer.normalize("app.wallabag.it/"),
        )
    }

    @Test
    fun `keeps self hosted subpath`() {
        assertEquals(
            "https://example.com/wallabag",
            WallabagHostNormalizer.normalize("https://example.com/wallabag/"),
        )
    }

    @Test
    fun `rejects blank host`() {
        assertNull(WallabagHostNormalizer.normalize("   "))
    }
}
