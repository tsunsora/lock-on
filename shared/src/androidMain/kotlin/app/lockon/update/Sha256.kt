package app.lockon.update

import java.io.File
import java.security.MessageDigest

actual fun sha256OfFile(path: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    File(path).inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n == -1) break
            digest.update(buffer, 0, n)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
