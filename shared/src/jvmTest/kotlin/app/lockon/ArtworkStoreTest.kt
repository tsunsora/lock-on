package app.lockon

import app.lockon.data.ArtworkStore
import app.lockon.model.Printing
import app.lockon.ui.components.artworkHttpClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArtworkStoreTest {
    private val hits = AtomicInteger()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            hits.incrementAndGet()
            val body = exchange.requestURI.path.encodeToByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        start()
    }
    private val dir = Files.createTempDirectory("artwork").toFile()

    @AfterTest
    fun cleanUp() {
        server.stop(0)
        dir.deleteRecursively()
    }

    private fun url(name: String) = "http://127.0.0.1:${server.address.port}/$name"
    private fun store() = ArtworkStore(dir.absolutePath, ::artworkHttpClient)
    private fun printing(name: String, alternate: Boolean = false) =
        Printing(name, name, "SET", "C", isAlternateArt = alternate, artworkUrl = url(name))

    @Test
    fun eachImageDownloadsOnceEvenWhenAskedForTogether() = runBlocking {
        val store = store()
        val files = List(5) { async { store.fetch(url("a.webp")) } }.awaitAll()
        store.fetch(url("a.webp"))
        assertEquals(1, hits.get())
        assertContentEquals("/a.webp".encodeToByteArray(), File(files.first().toString()).readBytes())
    }

    @Test
    fun savedImagesStillLoadWithTheServerGone() = runBlocking {
        store().downloadAll(listOf(printing("a"), printing("b", alternate = true)))
        server.stop(0)
        val reopened = store()
        assertTrue(reopened.has(url("a")) && reopened.has(url("b")))
        assertContentEquals("/b".encodeToByteArray(), File(reopened.fetch(url("b")).toString()).readBytes())
    }

    @Test
    fun downloadAllReportsProgressAndDropsImagesNoLongerLinked() = runBlocking {
        val store = store()
        store.downloadAll(listOf(printing("old"), printing("a")))
        store.downloadAll(listOf(printing("a"), printing("b")))
        val status = store.status.value
        assertEquals(2, status.total)
        assertTrue(status.complete)
        assertFalse(status.downloading)
        assertFalse(store.has(url("old")))
        assertEquals(3, hits.get())
        assertEquals(2, dir.list()!!.size)
    }
}
