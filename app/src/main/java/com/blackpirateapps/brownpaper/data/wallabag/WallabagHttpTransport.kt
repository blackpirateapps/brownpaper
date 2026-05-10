package com.blackpirateapps.brownpaper.data.wallabag

import com.blackpirateapps.brownpaper.core.model.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

data class WallabagHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val form: Map<String, String> = emptyMap(),
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
        val formBody = FormBody.Builder().apply {
            request.form.forEach { (key, value) -> add(key, value) }
        }.build()

        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (key, value) -> builder.header(key, value) }

        when (request.method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post(formBody)
            "PATCH" -> builder.patch(formBody)
            "DELETE" -> builder.delete(if (request.form.isEmpty()) null else formBody)
            else -> error("Unsupported method: ${request.method}")
        }

        okHttpClient.newCall(builder.build()).execute().use { response ->
            WallabagHttpResponse(
                code = response.code,
                body = response.body?.string().orEmpty(),
            )
        }
    }
}
