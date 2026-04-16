package com.blackpirateapps.brownpaper.core.util

import java.net.URI

fun String.normalizeUrl(): String? {
    val candidate = trim().removePrefix("android-app://").ifBlank { return null }
    val normalized = if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
        candidate
    } else {
        "https://$candidate"
    }

    return runCatching {
        URI(normalized).toURL()
        normalized
    }.getOrNull()
}

