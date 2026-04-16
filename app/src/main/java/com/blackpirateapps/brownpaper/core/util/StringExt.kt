package com.blackpirateapps.brownpaper.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

fun String.highlightMatches(
    query: String,
    highlightColor: Color,
): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(this)
    }

    val lowerText = lowercase()
    val lowerQuery = query.lowercase()

    return buildAnnotatedString {
        var startIndex = 0
        while (startIndex < length) {
            val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
            if (matchIndex < 0) {
                append(substring(startIndex))
                break
            }

            if (matchIndex > startIndex) {
                append(substring(startIndex, matchIndex))
            }

            pushStyle(SpanStyle(background = highlightColor))
            append(substring(matchIndex, matchIndex + lowerQuery.length))
            pop()
            startIndex = matchIndex + lowerQuery.length
        }
    }
}
