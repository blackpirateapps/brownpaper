package com.blackpirateapps.brownpaper.data.wallabag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class WallabagSession(
    val host: String,
    val username: String,
    val clientId: String,
    val clientSecret: String,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtMillis: Long,
    val lastSyncAtMillis: Long,
)

@Serializable
data class StoredWallabagSession(
    val host: String,
    val username: String,
    val clientId: String,
    val clientSecret: String,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMillis: Long = 0,
    val lastSyncAtMillis: Long = 0,
) {
    fun toSession(): WallabagSession = WallabagSession(
        host = host,
        username = username,
        clientId = clientId,
        clientSecret = clientSecret,
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtMillis = expiresAtMillis,
        lastSyncAtMillis = lastSyncAtMillis,
    )
}

fun WallabagSession.toStored(): StoredWallabagSession = StoredWallabagSession(
    host = host,
    username = username,
    clientId = clientId,
    clientSecret = clientSecret,
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAtMillis = expiresAtMillis,
    lastSyncAtMillis = lastSyncAtMillis,
)

@Serializable
data class WallabagTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String = "bearer",
)

@Serializable
data class WallabagInfoDto(
    val appname: String? = null,
    val version: String? = null,
    @SerialName("allowed_registration") val allowedRegistration: Boolean? = null,
)

@Serializable
data class WallabagUserDto(
    val id: Long? = null,
    val username: String? = null,
    val email: String? = null,
    val name: String? = null,
)

@Serializable
data class WallabagEntriesResponse(
    @SerialName("_embedded") val embedded: WallabagEmbeddedEntries = WallabagEmbeddedEntries(),
    val page: Int = 1,
    val pages: Int = 1,
    val total: Int = 0,
)

@Serializable
data class WallabagEmbeddedEntries(
    val items: List<WallabagEntryDto> = emptyList(),
)

@Serializable
data class WallabagEntryDto(
    val id: Long,
    val title: String? = null,
    val url: String,
    val content: String? = null,
    @SerialName("preview_picture") val previewPicture: String? = null,
    @SerialName("is_archived") val isArchived: Int = 0,
    @SerialName("is_starred") val isStarred: Int = 0,
    val tags: List<WallabagTagDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class WallabagTagDto(
    val id: Long? = null,
    val label: String? = null,
    val slug: String? = null,
)

data class WallabagRemoteEntry(
    val id: Long,
    val title: String,
    val url: String,
    val readerText: String,
    val previewPicture: String?,
    val isArchived: Boolean,
    val isStarred: Boolean,
    val tags: List<String>,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
)

sealed interface WallabagLoginResult {
    data object Success : WallabagLoginResult
    data object MissingClientCredentials : WallabagLoginResult
    data class InvalidHost(val message: String) : WallabagLoginResult
    data class Failure(val message: String) : WallabagLoginResult
}

sealed interface WallabagSyncResult {
    data class Success(val pulled: Int, val pushed: Int) : WallabagSyncResult
    data object NotConnected : WallabagSyncResult
    data class Failure(val message: String) : WallabagSyncResult
}

enum class WallabagSyncOperationType {
    UPSERT_ENTRY,
    UPDATE_ENTRY,
}
