package app.lockon.desktop

import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import java.io.File
import java.security.KeyStore

/**
 * The desktop's long-lived self-signed certificate. Phones pin its fingerprint at pairing, so it
 * must survive restarts; deleting the files forces every phone to pair again.
 */
class TlsIdentity private constructor(val keyStore: KeyStore, val password: String) {
    val fingerprint: String = sha256Hex(keyStore.getCertificate(ALIAS).encoded)

    companion object {
        const val ALIAS = "lock-on"

        fun loadOrCreate(dir: File): TlsIdentity {
            val storeFile = File(dir, "tls.p12")
            val passwordFile = File(dir, "tls.key")
            if (storeFile.exists() && passwordFile.exists()) {
                val password = passwordFile.readText().trim()
                runCatching {
                    val store = KeyStore.getInstance("PKCS12")
                    storeFile.inputStream().use { store.load(it, password.toCharArray()) }
                    if (store.containsAlias(ALIAS)) return TlsIdentity(store, password)
                }
            }
            val password = randomToken(24)
            val store = buildKeyStore {
                certificate(ALIAS) {
                    this.password = password
                    domains = listOf("localhost")
                    daysValid = 3650
                }
            }
            store.saveToFile(storeFile, password)
            passwordFile.writeText(password)
            // Reload so the in-memory type matches what later launches read back.
            val reloaded = KeyStore.getInstance("PKCS12")
            storeFile.inputStream().use { reloaded.load(it, password.toCharArray()) }
            return TlsIdentity(reloaded, password)
        }
    }
}
