package com.blackpirateapps.brownpaper.data.wallabag

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallabagAuthenticator @Inject constructor(
    private val apiClient: WallabagApiClient,
    private val sessionStore: WallabagSessionStore,
) {
    suspend fun authorizedSession(nowMillis: Long = System.currentTimeMillis()): WallabagSession? {
        val session = sessionStore.readSession() ?: return null
        if (session.expiresAtMillis - RefreshSkewMillis > nowMillis) {
            return session
        }

        val token = apiClient.refreshToken(session)
        val refreshed = session.copy(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken ?: session.refreshToken,
            expiresAtMillis = nowMillis + token.expiresIn * 1_000L,
        )
        sessionStore.saveSession(refreshed)
        return refreshed
    }

    private companion object {
        const val RefreshSkewMillis = 60_000L
    }
}
