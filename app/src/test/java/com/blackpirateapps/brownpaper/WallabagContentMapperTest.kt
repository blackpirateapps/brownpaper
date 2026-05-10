package com.blackpirateapps.brownpaper

import com.blackpirateapps.brownpaper.data.wallabag.WallabagContentMapper
import com.blackpirateapps.brownpaper.data.wallabag.WallabagEntryDto
import com.blackpirateapps.brownpaper.data.wallabag.WallabagTagDto
import com.blackpirateapps.brownpaper.data.wallabag.parseWallabagTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallabagContentMapperTest {
    @Test
    fun `converts wallabag html into reader blocks`() {
        val html = """
            <article>
                <h1>Heading</h1>
                <p>First paragraph.</p>
                <ul><li>List item</li></ul>
                <img src="https://example.com/image.jpg">
            </article>
        """.trimIndent()

        assertEquals(
            "Heading\n\nFirst paragraph.\n\nList item\n\n![img](https://example.com/image.jpg)",
            WallabagContentMapper.htmlToReaderText(html),
        )
    }

    @Test
    fun `maps remote entry fields into sync model`() {
        val remote = WallabagContentMapper.remoteEntryToDomain(
            WallabagEntryDto(
                id = 42,
                title = "Saved title",
                url = "https://example.com/story",
                content = "<p>Body</p>",
                previewPicture = "https://example.com/hero.jpg",
                isArchived = 1,
                isStarred = 1,
                tags = listOf(WallabagTagDto(label = "Read Later")),
                createdAt = "2026-05-10T10:00:00+0000",
                updatedAt = "2026-05-10T11:00:00+0000",
            ),
        )

        assertEquals(42, remote.id)
        assertEquals("Saved title", remote.title)
        assertEquals("Body", remote.readerText)
        assertTrue(remote.isArchived)
        assertTrue(remote.isStarred)
        assertEquals(listOf("Read Later"), remote.tags)
        assertEquals(1778407200000L, remote.createdAtMillis)
        assertEquals(1778410800000L, remote.updatedAtMillis)
    }

    @Test
    fun `parses wallabag compact offset timestamp`() {
        assertEquals(1778407200000L, parseWallabagTimestamp("2026-05-10T10:00:00+0000"))
    }
}
