package app.lockon.sync

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json

/** JSON and timeout setup every Lock On client needs, applied inside a platform's `HttpClient(engine) { }`. */
fun HttpClientConfig<*>.lockOnDefaults() {
    install(ContentNegotiation) { json(SyncProtocol.json) }
    install(HttpTimeout) {
        connectTimeoutMillis = 8_000
        requestTimeoutMillis = 30_000
    }
    expectSuccess = false
}

fun baseUrl(host: String, port: Int): String {
    val bracketed = if (':' in host && !host.startsWith("[")) "[$host]" else host
    return "https://$bracketed:$port"
}

class HttpSyncTransport(
    private val client: HttpClient,
    private val baseUrl: String,
    private val authToken: String,
) : SyncTransport {
    override suspend fun hello(): HelloResponse = client.get("$baseUrl/v1/hello").read()

    override suspend fun push(request: PushRequest): PushResponse =
        client.post("$baseUrl/v1/sync/push") { authed(); json(request) }.read()

    override suspend fun pull(after: Long, limit: Int): PullResponse =
        client.get("$baseUrl/v1/sync/pull") {
            authed()
            parameter("after", after)
            parameter("limit", limit)
        }.read()

    override suspend fun waitForChanges(after: Long): WaitResponse =
        client.get("$baseUrl/v1/sync/wait") {
            authed()
            parameter("after", after)
            timeout { requestTimeoutMillis = (SyncProtocol.MAX_WAIT_SECONDS + 10) * 1000L }
        }.read()

    private fun HttpRequestBuilder.authed() = bearerAuth(authToken)
}

/**
 * Waits up to [SyncProtocol.MAX_WAIT_SECONDS] for the desktop to invite this phone. Returns null
 * when it doesn't. Needs no pairing: an invitation only starts the normal, confirmed pairing.
 */
suspend fun awaitInvite(client: HttpClient, baseUrl: String, deviceId: String): InviteOffer? {
    val response = client.get("$baseUrl/v1/invites") {
        parameter("device", deviceId)
        timeout { requestTimeoutMillis = (SyncProtocol.MAX_WAIT_SECONDS + 10) * 1000L }
    }
    return if (response.status.value == 200) response.body() else null
}

/** Asks the desktop to pair. Waits while someone confirms on the desktop screen. */
suspend fun requestPairing(client: HttpClient, baseUrl: String, request: PairRequest): PairResponse {
    val hello: HelloResponse = client.get("$baseUrl/v1/hello").read()
    SyncProtocol.incompatibility(hello)?.let { throw SyncException(it) }
    val response = client.post("$baseUrl/v1/pair") {
        json(request)
        timeout { requestTimeoutMillis = 130_000 }
    }
    return if (response.status.isSuccess() || response.status.value == 403) response.body()
    else throw SyncException(response.errorMessage())
}

private inline fun <reified T> HttpRequestBuilder.json(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

private suspend inline fun <reified T> HttpResponse.read(): T {
    if (!status.isSuccess()) throw SyncException(errorMessage())
    return body()
}

private suspend fun HttpResponse.errorMessage(): String {
    val text = runCatching { bodyAsText() }.getOrDefault("")
    val message = runCatching { SyncProtocol.json.decodeFromString(ErrorResponse.serializer(), text).message }.getOrNull()
    return when {
        message != null -> message
        status.value == 401 -> "The desktop no longer recognizes this device. Pair again."
        else -> "The desktop answered with an error (${status.value})."
    }
}
