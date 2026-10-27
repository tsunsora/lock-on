package app.lockon.update

import app.lockon.catalog.downloadClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One installer in the update feed. [file] is relative to the feed's address, or a full https:// link. */
@Serializable
data class UpdateFile(val file: String, val sha256: String, val size: Long = 0)

/**
 * The small file each release publishes next to its installers (`lock-on-update.json`), written by
 * `./gradlew releaseArtifacts`. Checking for updates reads only this.
 */
@Serializable
data class UpdateManifest(
    val version: String,
    val notes: String? = null,
    val windows: UpdateFile? = null,
    val android: UpdateFile? = null,
) {
    fun fileFor(platform: UpdatePlatform): UpdateFile? = when (platform) {
        UpdatePlatform.WINDOWS -> windows
        UpdatePlatform.ANDROID -> android
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): UpdateManifest = json.decodeFromString(serializer(), text.trim().removePrefix("﻿"))
    }
}

class UpdateException(message: String) : Exception(message)

/**
 * Compares dotted versions such as `1.2.10` and `1.2.9` part by part, as numbers. Anything after a
 * part's digits (`1.1.0-beta`) is ignored.
 */
fun compareVersions(a: String, b: String): Int {
    fun parts(v: String) = v.trim().removePrefix("v").split('.').map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    val pa = parts(a)
    val pb = parts(b)
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
        if (c != 0) return c
    }
    return 0
}

/** Where [file] lives: a full link as is, otherwise next to the feed. */
fun resolveUpdateUrl(feedUrl: String, file: String): String =
    if (file.startsWith("https://")) file else feedUrl.substringBeforeLast('/') + "/" + file

/** SHA-256 of a file, as lowercase hex. */
expect fun sha256OfFile(path: String): String

internal suspend fun httpGetText(url: String): String {
    val client = downloadClient()
    try {
        val response = client.get(url)
        if (!response.status.isSuccess()) throw UpdateException("The update server answered with an error (${response.status.value}).")
        return response.bodyAsText()
    } finally {
        client.close()
    }
}

/**
 * Streams [url] into [dest] and checks its SHA-256. A partial or mismatched file is deleted, so
 * only a verified installer is ever left at [dest].
 */
internal suspend fun downloadVerified(url: String, dest: String, sha256: String, onProgress: (Float?) -> Unit) {
    val target = Path(dest)
    val partial = Path("$dest.part")
    target.parent?.let { SystemFileSystem.createDirectories(it) }
    val client = downloadClient()
    try {
        client.prepareGet(url) {
            // Installers are large; only a stalled connection should fail, not a slow one.
            timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS; socketTimeoutMillis = 30_000 }
        }.execute { response ->
            if (!response.status.isSuccess()) throw UpdateException("The update server answered with an error (${response.status.value}).")
            val total = response.contentLength()?.takeIf { it > 0 }
            val channel = response.bodyAsChannel()
            SystemFileSystem.sink(partial).buffered().use { sink ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val n = channel.readAvailable(buffer, 0, buffer.size)
                    if (n == -1) break
                    sink.write(buffer, 0, n)
                    done += n
                    onProgress(total?.let { (done.toFloat() / it).coerceIn(0f, 1f) })
                }
            }
        }
        if (!sha256OfFile(partial.toString()).equals(sha256, ignoreCase = true)) {
            throw UpdateException("The update didn't download correctly. Try again.")
        }
        SystemFileSystem.delete(target, mustExist = false)
        SystemFileSystem.atomicMove(partial, target)
    } finally {
        client.close()
        SystemFileSystem.delete(partial, mustExist = false)
    }
}
