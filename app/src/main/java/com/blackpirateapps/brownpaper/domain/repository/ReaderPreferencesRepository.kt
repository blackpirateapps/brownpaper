package com.blackpirateapps.brownpaper.domain.repository

import com.blackpirateapps.brownpaper.core.model.ReaderFontFamily
import com.blackpirateapps.brownpaper.core.model.ReaderFontWeight
import com.blackpirateapps.brownpaper.core.model.ReaderContentWidth
import com.blackpirateapps.brownpaper.core.model.ReaderPreferences
import com.blackpirateapps.brownpaper.core.model.ReaderTheme
import kotlinx.coroutines.flow.Flow

interface ReaderPreferencesRepository {
    val readerPreferences: Flow<ReaderPreferences>
    suspend fun updateFontFamily(fontFamily: ReaderFontFamily)
    suspend fun updateFontSize(fontSizeSp: Float)
    suspend fun updateFontWeight(fontWeight: ReaderFontWeight)
    suspend fun updateTheme(theme: ReaderTheme)
    suspend fun updateContentWidth(contentWidth: ReaderContentWidth)
}
