package com.blackpirateapps.brownpaper

import com.blackpirateapps.brownpaper.core.util.toFtsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryFormatterTest {

    @Test
    fun `formats multi word query for prefix matching`() {
        assertEquals("\"brown\"* AND \"paper\"*", "brown paper".toFtsQuery())
    }

    @Test
    fun `returns null when query has no searchable tokens`() {
        assertNull("!! --".toFtsQuery())
    }
}

