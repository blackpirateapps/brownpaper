package com.blackpirateapps.brownpaper.data.wallabag

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.blackpirateapps.brownpaper.core.di.WallabagSessionPreferencesStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Singleton
class WallabagSessionStore @Inject constructor(
    @WallabagSessionPreferencesStore private val dataStore: DataStore<Preferences>,
    private val secretBox: WallabagSecretBox,
) {
    val session: Flow<WallabagSession?> = dataStore.data.map { preferences ->
        preferences[SessionKey]?.let { encrypted ->
            runCatching {
                Json.decodeFromString<StoredWallabagSession>(secretBox.decrypt(encrypted)).toSession()
            }.getOrNull()
        }
    }

    suspend fun readSession(): WallabagSession? = session.first()

    suspend fun saveSession(session: WallabagSession) {
        val encrypted = secretBox.encrypt(Json.encodeToString(StoredWallabagSession.serializer(), session.toStored()))
        dataStore.edit { preferences ->
            preferences[SessionKey] = encrypted
        }
    }

    suspend fun updateSession(transform: (WallabagSession) -> WallabagSession) {
        val current = readSession() ?: return
        saveSession(transform(current))
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(SessionKey)
        }
    }

    private companion object {
        val SessionKey = stringPreferencesKey("wallabag_session")
    }
}
