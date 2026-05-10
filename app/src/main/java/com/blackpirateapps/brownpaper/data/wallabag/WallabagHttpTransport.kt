package com.blackpirateapps.brownpaper.data.wallabag

import com.blackpirateapps.brownpaper.core.model.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

data class WallabagHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val form: Map<String, String> = emptyMap(),
    val jsonBody: String? = null,
)

data class WallabagHttpResponse(
    val code: Int,
    val body: String,
) {
    val isSuccessful: Boolean = code in 200..299
}

interface WallabagHttpTransport {
    suspend fun execute(request: WallabagHttpRequest): WallabagHttpResponse
}

@Singleton
class OkHttpWallabagTransport @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val dispatchers: AppDispatchers,
) : WallabagHttpTransport {
    override suspend fun execute(request: WallabagHttpRequest): WallabagHttpResponse = withContext(dispatchers.io) {
        val requestBody = request.body()

        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (key, value) -> builder.header(key, value) }

        when (request.method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: emptyBody())
            "PUT" -> builder.put(requestBody ?: emptyBody())
            "PATCH" -> builder.patch(requestBody ?: emptyBody())
            "DELETE" -> builder.delete(requestBody)
            else -> error("Unsupported method: ${request.method}")
        }

        okHttpClient.newCall(builder.build()).execute().use { response ->
            WallabagHttpResponse(
                code = response.code,
                body = response.body?.string().orEmpty(),
            )
        }
    }

    private fun WallabagHttpRequest.body(): RequestBody? {
        jsonBody?.let { json ->
            return json.toRequestBody("application/json; charset=utf-8".toMediaType())
        }
        if (form.isEmpty()) {
            return null
        }
        return FormBody.Builder().apply {
            form.forEach { (key, value) -> add(key, value) }
        }.build()
    }

    private fun emptyBody(): RequestBody = ByteArray(0).toRequestBody(null)
}
