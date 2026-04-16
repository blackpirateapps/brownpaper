package com.blackpirateapps.brownpaper.core.util

private val ftsTokenSanitizer = Regex("[^\\p{L}\\p{N}]")
private val whitespaceRegex = Regex("\\s+")

fun String.toFtsQuery(): String? {
    val tokens = trim()
        .split(whitespaceRegex)
        .mapNotNull { token ->
            val cleaned = token.replace(ftsTokenSanitizer, "")
            cleaned.takeIf { it.isNotBlank() }?.let { "\"$it\"*" }
        }

    return tokens.takeIf { it.isNotEmpty() }?.joinToString(separator = " AND ")
}

