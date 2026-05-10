package com.blackpirateapps.brownpaper

import com.blackpirateapps.brownpaper.data.wallabag.WallabagApiClient
import com.blackpirateapps.brownpaper.data.wallabag.WallabagHttpRequest
import com.blackpirateapps.brownpaper.data.wallabag.WallabagHttpResponse
import com.blackpirateapps.brownpaper.data.wallabag.WallabagHttpTransport
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WallabagApiClientTest {
    @Test
    fun `login parses token response and sends password grant form`() = runBlocking {
        val transport = RecordingTransport(
            WallabagHttpResponse(
                code = 200,
                body = """
                    {
                      "access_token": "access",
                      "expires_in": 3600,
                      "refresh_token": "refresh",
                      "token_type": "bearer"
                    }
                """.trimIndent(),
            ),
        )
        val client = WallabagApiClient(transport)

        val token = client.login(
            host = "https://example.com/wallabag",
            clientId = "client-id",
            clientSecret = "client-secret",
            username = "reader",
            password = "password",
        )

        assertEquals("access", token.accessToken)
        assertEquals("refresh", token.refreshToken)
        assertEquals("https://example.com/wallabag/oauth/v2/token", transport.requests.single().url)
        assertEquals("password", transport.requests.single().form["grant_type"])
        assertEquals("client-id", transport.requests.single().form["client_id"])
    }

    @Test
    fun `authorized requests include bearer token`() = runBlocking {
        val transport = RecordingTransport(
            WallabagHttpResponse(
                code = 200,
                body = """{"id":1,"username":"reader"}""",
            ),
        )
        val client = WallabagApiClient(transport)

        client.getUser(
            WallabagSession(
                host = "https://app.wallabag.it",
                username = "reader",
                clientId = "client-id",
                clientSecret = "client-secret",
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresAtMillis = Long.MAX_VALUE,
                lastSyncAtMillis = 0,
            ),
        )

        assertEquals("Bearer access-token", transport.requests.single().headers["Authorization"])
        assertEquals("https://app.wallabag.it/api/user", transport.requests.single().url)
    }

    private class RecordingTransport(
        private val response: WallabagHttpResponse,
    ) : WallabagHttpTransport {
        val requests = mutableListOf<WallabagHttpRequest>()

        override suspend fun execute(request: WallabagHttpRequest): WallabagHttpResponse {
            requests += request
            return response
        }
    }
}
