package com.blackpirateapps.brownpaper.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val articleDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MMM d, yyyy", Locale.getDefault())

fun Long.toReadableArticleDate(): String = articleDateFormatter.format(
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()),
)

