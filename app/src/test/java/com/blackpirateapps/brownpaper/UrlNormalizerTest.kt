package com.blackpirateapps.brownpaper

import com.blackpirateapps.brownpaper.core.util.normalizeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun `adds https when scheme is missing`() {
        assertEquals(
            "https://example.com/article",
            "example.com/article".normalizeUrl(),
        )
    }

    @Test
    fun `rejects blank urls`() {
        assertNull("   ".normalizeUrl())
    }
}
