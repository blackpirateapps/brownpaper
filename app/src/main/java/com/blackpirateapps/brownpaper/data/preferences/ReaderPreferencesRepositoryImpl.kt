package com.blackpirateapps.brownpaper.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.blackpirateapps.brownpaper.core.di.ReaderPreferencesStore
import com.blackpirateapps.brownpaper.core.model.ReaderFontFamily
import com.blackpirateapps.brownpaper.core.model.ReaderPreferences
import com.blackpirateapps.brownpaper.core.model.ReaderTheme
import com.blackpirateapps.brownpaper.domain.repository.ReaderPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReaderPreferencesRepositoryImpl @Inject constructor(
    @ReaderPreferencesStore
    private val dataStore: DataStore<Preferences>,
) : ReaderPreferencesRepository {

    override val readerPreferences: Flow<ReaderPreferences> = dataStore.data.map { preferences ->
        ReaderPreferences(
            fontFamily = preferences[FontFamilyKey]?.let { ReaderFontFamily.valueOf(it) }
                ?: ReaderFontFamily.SYSTEM,
            fontSizeSp = preferences[FontSizeKey] ?: 18f,
            useEmphasizedWeight = preferences[UseEmphasizedWeightKey] ?: false,
            theme = preferences[ThemeKey]?.let { ReaderTheme.valueOf(it) } ?: ReaderTheme.LIGHT,
        )
    }

    override suspend fun updateFontFamily(fontFamily: ReaderFontFamily) {
        dataStore.edit { it[FontFamilyKey] = fontFamily.name }
    }

    override suspend fun updateFontSize(fontSizeSp: Float) {
        dataStore.edit { it[FontSizeKey] = fontSizeSp }
    }

    override suspend fun updateEmphasizedWeight(enabled: Boolean) {
        dataStore.edit { it[UseEmphasizedWeightKey] = enabled }
    }

    override suspend fun updateTheme(theme: ReaderTheme) {
        dataStore.edit { it[ThemeKey] = theme.name }
    }

    private companion object {
        val FontFamilyKey = stringPreferencesKey("font_family")
        val FontSizeKey = floatPreferencesKey("font_size_sp")
        val UseEmphasizedWeightKey = booleanPreferencesKey("use_emphasized_weight")
        val ThemeKey = stringPreferencesKey("theme")
    }
}
