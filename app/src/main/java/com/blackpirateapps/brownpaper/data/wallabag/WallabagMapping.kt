package com.blackpirateapps.brownpaper.data.wallabag

import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import org.jsoup.Jsoup

object WallabagHostNormalizer {
    fun normalize(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) {
            return null
        }

        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return runCatching {
            val uri = URI(withScheme)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()
            if (scheme !in setOf("http", "https") || host.isNullOrBlank()) {
                return null
            }
            uri.normalize().toString().trimEnd('/')
        }.getOrNull()
    }
}

object WallabagContentMapper {
    fun remoteEntryToDomain(entry: WallabagEntryDto): WallabagRemoteEntry {
        val title = entry.title?.trim().orEmpty().ifBlank { entry.url }
        return WallabagRemoteEntry(
            id = entry.id,
            title = title,
            url = entry.url,
            readerText = htmlToReaderText(entry.content.orEmpty()).ifBlank { title },
            previewPicture = entry.previewPicture?.takeIf { it.isNotBlank() },
            isArchived = entry.isArchived == 1,
            isStarred = entry.isStarred == 1,
            tags = entry.tags.mapNotNull { tag ->
                tag.label?.trim()?.takeIf(String::isNotBlank)
                    ?: tag.slug?.trim()?.takeIf(String::isNotBlank)
            }.distinctBy { it.lowercase() },
            createdAtMillis = parseWallabagTimestamp(entry.createdAt),
            updatedAtMillis = parseWallabagTimestamp(entry.updatedAt),
        )
    }

    fun htmlToReaderText(html: String): String {
        if (html.isBlank()) {
            return ""
        }

        val document = Jsoup.parseBodyFragment(html)
        val blocks = document.body().select("p, h1, h2, h3, h4, h5, h6, li, img")
            .mapNotNull { element ->
                if (element.tagName() == "img") {
                    val src = element.absUrl("src").ifBlank { element.attr("src") }
                    src.takeIf(String::isNotBlank)?.let { "![img]($it)" }
                } else {
                    element.text().trim().takeIf(String::isNotBlank)
                }
            }

        return blocks.joinToString("\n\n").ifBlank {
            document.body().text().trim()
        }
    }
}

fun parseWallabagTimestamp(value: String?): Long? {
    val raw = value?.trim()?.takeIf(String::isNotBlank) ?: return null
    raw.toLongOrNull()?.let { timestamp ->
        return if (timestamp < 10_000_000_000L) timestamp * 1_000L else timestamp
    }

    return runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(raw, WallabagCompactOffsetFormatter).toInstant().toEpochMilli() }
        .recoverCatching { Instant.parse(raw).toEpochMilli() }
        .getOrNull()
}

fun sha1Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private val WallabagCompactOffsetFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
    .appendOffset("+HHMM", "+0000")
    .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
    .toFormatter()
