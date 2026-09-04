package pl.stapik.media.data.config

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM encryption backed by a key held in the Android Keystore, so the
 * server URL and API key are never written to disk in plaintext.
 * `setUnlockedDeviceRequired` is applied from API 30, matching the calendar
 * companion app's config storage.
 */
class CryptoManager(
    private val keyAlias: String = "stapikmedia_config_key",
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val specBuilder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specBuilder.setUnlockedDeviceRequired(true)
        }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(specBuilder.build()) }
            .generateKey()
    }

    /** Encrypts [plaintext], returning base64(iv) + ":" + base64(ciphertext). */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        return "${b64(iv)}:${b64(ciphertext)}"
    }

    /** Reverses [encrypt]. */
    fun decrypt(payload: String): String {
        val (ivPart, cipherPart) = payload.split(":", limit = 2)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, unb64(ivPart)))
        }
        return String(cipher.doFinal(unb64(cipherPart)), Charsets.UTF_8)
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(str: String) = Base64.decode(str, Base64.NO_WRAP)

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
