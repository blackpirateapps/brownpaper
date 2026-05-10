package com.blackpirateapps.brownpaper.data.wallabag

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class WallabagApiClient @Inject constructor(
    private val transport: WallabagHttpTransport,
) {
    suspend fun getInfo(host: String): WallabagInfoDto =
        executeJson(WallabagHttpRequest(method = "GET", url = buildUrl(host, "/api/info")))

    suspend fun login(
        host: String,
        clientId: String,
        clientSecret: String,
        username: String,
        password: String,
    ): WallabagTokenResponse = executeJson(
        WallabagHttpRequest(
            method = "POST",
            url = buildUrl(host, "/oauth/v2/token"),
            form = mapOf(
                "grant_type" to "password",
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "username" to username,
                "password" to password,
            ),
        ),
    )

    suspend fun refreshToken(session: WallabagSession): WallabagTokenResponse {
        val refreshToken = requireNotNull(session.refreshToken) { "Missing refresh token" }
        return executeJson(
            WallabagHttpRequest(
                method = "POST",
                url = buildUrl(session.host, "/oauth/v2/token"),
                form = mapOf(
                    "grant_type" to "refresh_token",
                    "client_id" to session.clientId,
                    "client_secret" to session.clientSecret,
                    "refresh_token" to refreshToken,
                ),
            ),
        )
    }

    suspend fun getUser(session: WallabagSession): WallabagUserDto =
        executeJson(authorizedRequest(session, method = "GET", path = "/api/user"))

    suspend fun getEntries(
        session: WallabagSession,
        page: Int,
        sinceEpochSeconds: Long?,
    ): WallabagEntriesResponse {
        val query = buildMap {
            put("detail", "full")
            put("sort", "updated")
            put("order", "asc")
            put("perPage", "50")
            put("page", page.toString())
            if (sinceEpochSeconds != null && sinceEpochSeconds > 0) {
                put("since", sinceEpochSeconds.toString())
            }
        }
        return executeJson(authorizedRequest(session, method = "GET", path = "/api/entries", query = query))
    }

    suspend fun getEntry(session: WallabagSession, entryId: Long): WallabagEntryDto =
        executeJson(authorizedRequest(session, method = "GET", path = "/api/entries/$entryId"))

    suspend fun findEntryId(session: WallabagSession, url: String): Long? {
        val response: JsonElement = executeJson(
            authorizedRequest(
                session = session,
                method = "GET",
                path = "/api/entries/exists",
                query = mapOf(
                    "return_id" to "1",
                    "url" to url,
                    "hashed_url" to sha1Hex(url),
                ),
            ),
        )
        return parseEntryExistsResponse(response)
    }

    suspend fun createEntry(
        session: WallabagSession,
        article: LocalWallabagArticle,
    ): WallabagEntryDto = executeJson(
        authorizedRequest(
            session = session,
            method = "POST",
            path = "/api/entries",
            form = article.asWallabagForm(),
        ),
    )

    suspend fun patchEntry(
        session: WallabagSession,
        entryId: Long,
        article: LocalWallabagArticle,
    ): WallabagEntryDto = executeJson(
        authorizedRequest(
            session = session,
            method = "PATCH",
            path = "/api/entries/$entryId",
            form = article.asWallabagForm(includeContent = false),
        ),
    )

    suspend fun deleteEntry(session: WallabagSession, entryId: Long) {
        val response = transport.execute(
            authorizedRequest(
                session = session,
                method = "DELETE",
                path = "/api/entries/$entryId",
                query = mapOf("expect" to "id"),
            ),
        )
        if (!response.isSuccessful) {
            throw WallabagApiException(response.code, response.body.ifBlank { "wallabag request failed" })
        }
    }

    suspend fun addEntryTags(
        session: WallabagSession,
        entryId: Long,
        tags: List<String>,
    ): WallabagEntryDto = executeJson(
        authorizedRequest(
            session = session,
            method = "POST",
            path = "/api/entries/$entryId/tags",
            form = mapOf("tags" to tags.joinToString(",")),
        ),
    )

    suspend fun deleteEntryTag(session: WallabagSession, entryId: Long, tagId: Long) {
        val response = transport.execute(
            authorizedRequest(
                session = session,
                method = "DELETE",
                path = "/api/entries/$entryId/tags/$tagId",
            ),
        )
        if (!response.isSuccessful) {
            throw WallabagApiException(response.code, response.body.ifBlank { "wallabag request failed" })
        }
    }

    suspend fun getAnnotations(session: WallabagSession, entryId: Long): List<WallabagAnnotationDto> {
        val response: JsonElement = executeJson(
            authorizedRequest(
                session = session,
                method = "GET",
                path = "/api/annotations/$entryId",
            ),
        )
        return parseAnnotationsResponse(response)
    }

    suspend fun createAnnotation(
        session: WallabagSession,
        entryId: Long,
        annotation: LocalWallabagAnnotation,
    ): WallabagAnnotationDto = executeJson(
        authorizedRequest(
            session = session,
            method = "POST",
            path = "/api/annotations/$entryId",
            jsonBody = annotation.asWallabagAnnotationJson(),
        ),
    )

    suspend fun updateAnnotation(
        session: WallabagSession,
        annotationId: String,
        annotation: LocalWallabagAnnotation,
    ): WallabagAnnotationDto = executeJson(
        authorizedRequest(
            session = session,
            method = "PUT",
            path = "/api/annotations/$annotationId",
            jsonBody = annotation.asWallabagAnnotationJson(),
        ),
    )

    suspend fun deleteAnnotation(session: WallabagSession, annotationId: String) {
        val response = transport.execute(
            authorizedRequest(
                session = session,
                method = "DELETE",
                path = "/api/annotations/$annotationId",
            ),
        )
        if (!response.isSuccessful) {
            throw WallabagApiException(response.code, response.body.ifBlank { "wallabag request failed" })
        }
    }

    private fun authorizedRequest(
        session: WallabagSession,
        method: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        form: Map<String, String> = emptyMap(),
        jsonBody: String? = null,
    ): WallabagHttpRequest = WallabagHttpRequest(
        method = method,
        url = buildUrl(session.host, path, query),
        headers = mapOf("Authorization" to "Bearer ${session.accessToken}"),
        form = form,
        jsonBody = jsonBody,
    )

    private suspend inline fun <reified T> executeJson(request: WallabagHttpRequest): T {
        val response = transport.execute(request)
        if (!response.isSuccessful) {
            throw WallabagApiException(response.code, response.body.ifBlank { "wallabag request failed" })
        }
        return try {
            WallabagJson.decodeFromString(response.body)
        } catch (exception: SerializationException) {
            throw WallabagApiException(response.code, "Unable to read wallabag response", exception)
        } catch (exception: IllegalArgumentException) {
            throw WallabagApiException(response.code, "Unable to read wallabag response", exception)
        }
    }

    private fun buildUrl(host: String, path: String, query: Map<String, String> = emptyMap()): String {
        val base = host.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid wallabag host")
        val builder = base.newBuilder()
            .addPathSegments(path.trimStart('/'))
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private fun parseEntryExistsResponse(response: JsonElement): Long? {
        val primitive = response.jsonPrimitiveOrNull()
        primitive?.longOrNull?.let { return it }
        primitive?.booleanOrNull?.let { exists -> if (!exists) return null }
        primitive?.contentOrNull?.toLongOrNull()?.let { return it }

        val obj = response.jsonObjectOrNull() ?: return null
        obj["id"]?.jsonPrimitiveOrNull()?.longOrNull?.let { return it }
        obj["entry_id"]?.jsonPrimitiveOrNull()?.longOrNull?.let { return it }
        obj["entry"]?.jsonPrimitiveOrNull()?.longOrNull?.let { return it }
        return null
    }

    private fun parseAnnotationsResponse(response: JsonElement): List<WallabagAnnotationDto> {
        response.jsonArrayOrNull()?.let { array ->
            return array.map { WallabagJson.decodeFromJsonElementCompat(it) }
        }

        val obj = response.jsonObjectOrNull() ?: return emptyList()
        val array = obj["annotations"]?.jsonArrayOrNull()
            ?: obj["items"]?.jsonArrayOrNull()
            ?: obj["rows"]?.jsonArrayOrNull()
            ?: obj["_embedded"]?.jsonObjectOrNull()?.get("annotations")?.jsonArrayOrNull()
            ?: obj["_embedded"]?.jsonObjectOrNull()?.get("items")?.jsonArrayOrNull()
            ?: return emptyList()

        return array.map { WallabagJson.decodeFromJsonElementCompat(it) }
    }
}

private val WallabagJson = Json {
    ignoreUnknownKeys = true
}

data class LocalWallabagArticle(
    val url: String,
    val title: String,
    val content: String,
    val previewPicture: String?,
    val isArchived: Boolean,
    val isStarred: Boolean,
    val tags: List<String>,
)

private fun LocalWallabagArticle.asWallabagForm(includeContent: Boolean = true): Map<String, String> = buildMap {
    put("url", url)
    put("title", title)
    put("archive", if (isArchived) "1" else "0")
    put("starred", if (isStarred) "1" else "0")
    if (tags.isNotEmpty()) {
        put("tags", tags.joinToString(","))
    }
    if (includeContent && content.isNotBlank()) {
        put("content", content)
    }
    previewPicture?.takeIf(String::isNotBlank)?.let { put("preview_picture", it) }
}

private fun LocalWallabagAnnotation.asWallabagAnnotationJson(): String {
    val ranges = runCatching { WallabagJson.parseToJsonElement(rangesJson) }
        .getOrElse { JsonArray(emptyList()) }
    val body = JsonObject(
        mapOf(
            "ranges" to ranges,
            "quote" to JsonArray(listOf(JsonPrimitive(quote))),
            "text" to JsonArray(listOf(JsonPrimitive(text))),
        ),
    )
    return body.toString()
}

class WallabagApiException(
    val code: Int,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)

private fun JsonElement.jsonPrimitiveOrNull() = runCatching { jsonPrimitive }.getOrNull()

private fun JsonElement.jsonObjectOrNull() = runCatching { jsonObject }.getOrNull()

private fun JsonElement.jsonArrayOrNull() = runCatching { jsonArray }.getOrNull()

private inline fun <reified T> Json.decodeFromJsonElementCompat(element: JsonElement): T =
    decodeFromString(element.toString())
