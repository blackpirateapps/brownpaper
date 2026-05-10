package com.blackpirateapps.brownpaper.core.model

enum class ReaderFontFamily {
    SYSTEM,
    SERIF,
    MONO,
    MERRIWEATHER,
    LORA,
    FIRA_SANS,
    INTER,
}

enum class ReaderTheme {
    LIGHT,
    DARK,
    PAPER,
}

enum class ReaderFontWeight {
    LIGHT,
    REGULAR,
    BOLD,
}

data class ReaderPreferences(
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SYSTEM,
    val fontSizeSp: Float = 18f,
    val fontWeight: ReaderFontWeight = ReaderFontWeight.REGULAR,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
)
