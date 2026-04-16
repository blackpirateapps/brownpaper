package com.blackpirateapps.brownpaper.core.model

enum class ReaderFontFamily {
    SYSTEM,
    SERIF,
    MONO,
}

enum class ReaderTheme {
    LIGHT,
    DARK,
    PAPER,
}

data class ReaderPreferences(
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SYSTEM,
    val fontSizeSp: Float = 18f,
    val useEmphasizedWeight: Boolean = false,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
)

