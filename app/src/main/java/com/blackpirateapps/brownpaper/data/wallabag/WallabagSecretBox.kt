package com.blackpirateapps.brownpaper.data.wallabag

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

interface WallabagSecretBox {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}

@Singleton
class AndroidKeystoreWallabagSecretBox @Inject constructor() : WallabagSecretBox {
    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return listOf(
            Version,
            Base64.getEncoder().encodeToString(cipher.iv),
            Base64.getEncoder().encodeToString(encrypted),
        ).joinToString(":")
    }

    override fun decrypt(cipherText: String): String {
        val parts = cipherText.split(":")
        require(parts.size == 3 && parts[0] == Version) { "Unsupported encrypted session format" }

        val iv = Base64.getDecoder().decode(parts[1])
        val encrypted = Base64.getDecoder().decode(parts[2])
        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (keyStore.getEntry(KeyAlias, null) as? KeyStore.SecretKeyEntry)?.let { entry ->
            return entry.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        val spec = KeyGenParameterSpec.Builder(
            KeyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val AndroidKeyStore = "AndroidKeyStore"
        const val KeyAlias = "brownpaper_wallabag_session"
        const val Transformation = "AES/GCM/NoPadding"
        const val Version = "v1"
    }
}
