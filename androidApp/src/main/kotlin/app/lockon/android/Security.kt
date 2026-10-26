package app.lockon.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.lockon.sync.lockOnDefaults
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Sync tokens, encrypted with a hardware-backed Android Keystore key. Only the encrypted form
 * reaches app storage, and it cannot be decrypted on another device.
 */
class SecretStore(context: Context) {
    private val prefs = context.getSharedPreferences("secrets", Context.MODE_PRIVATE)

    fun put(name: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(value.toByteArray())
        prefs.edit().putString(name, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    fun get(name: String): String? {
        val sealed = prefs.getString(name, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, IV_BYTES))
            String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES))
        }.getOrNull()
    }

    fun remove(name: String) = prefs.edit().remove(name).apply()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val ALIAS = "lock-on-sync"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/**
 * Trusts exactly one certificate: the desktop's self-signed one, identified by its SHA-256
 * fingerprint. With no fingerprint yet (manual pairing), it accepts the first certificate and
 * reports it so people can compare verification codes on both screens before trusting it.
 */
class PinnedTrustManager(private val expected: String?, private val onSeen: (String) -> Unit = {}) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("The desktop sent no certificate.")
        val seen = sha256Hex(leaf.encoded)
        onSeen(seen)
        if (expected != null && !seen.equals(expected, ignoreCase = true)) {
            throw CertificateException("The desktop's certificate does not match the paired one.")
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
        throw CertificateException("Client certificates are not used.")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

fun pinnedHttpClient(expectedFingerprint: String?, onSeen: (String) -> Unit = {}): HttpClient {
    val trust = PinnedTrustManager(expectedFingerprint, onSeen)
    val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
    return HttpClient(OkHttp) {
        engine {
            config {
                sslSocketFactory(ssl.socketFactory, trust)
                // The pinned fingerprint identifies the desktop, so the IP address need not match a name.
                hostnameVerifier { _, _ -> true }
            }
        }
        lockOnDefaults()
    }
}
