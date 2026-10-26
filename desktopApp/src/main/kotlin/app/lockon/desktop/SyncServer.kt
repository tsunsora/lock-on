package app.lockon.desktop

import app.lockon.sync.DeviceRepository
import app.lockon.sync.DeviceRole
import app.lockon.sync.ErrorResponse
import app.lockon.sync.HelloResponse
import app.lockon.sync.LocalIdentity
import app.lockon.sync.PairRequest
import app.lockon.sync.PairedDevice
import app.lockon.sync.PushRequest
import app.lockon.sync.SyncHost
import app.lockon.sync.SyncProtocol
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

/** HTTP routes for pairing and sync. Kept separate from the server so tests can mount them. */
fun Application.lockOnSync(host: SyncHost, devices: DeviceRepository, pairing: PairingManager, identity: LocalIdentity) {
    install(ContentNegotiation) { json(SyncProtocol.json) }

    suspend fun ApplicationCall.client(): PairedDevice? {
        val token = request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
        if (token.isNullOrEmpty()) return null
        return devices.byTokenHash(sha256Hex(token.toByteArray()))?.takeIf { it.role == DeviceRole.CLIENT }
    }

    suspend fun ApplicationCall.unauthorized() =
        respond(HttpStatusCode.Unauthorized, ErrorResponse("The desktop no longer recognizes this device. Pair again."))

    suspend fun ApplicationCall.guard(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(e.message ?: "The request could not be applied."))
        } catch (e: Exception) {
            respond(HttpStatusCode.BadRequest, ErrorResponse("The desktop could not read the request."))
        }
    }

    routing {
        get("/v1/hello") {
            call.respond(HelloResponse(deviceId = identity.deviceId, deviceName = identity.deviceName))
        }
        post("/v1/pair") {
            call.guard {
                val request = call.receive<PairRequest>()
                if (request.protocolVersion < SyncProtocol.MIN_PROTOCOL_VERSION) {
                    call.respond(HttpStatusCode.UpgradeRequired, ErrorResponse("Update Lock On on your phone to pair."))
                    return@guard
                }
                when (val outcome = pairing.handle(request)) {
                    is PairingManager.Outcome.Accepted -> call.respond(outcome.response)
                    is PairingManager.Outcome.Rejected -> call.respond(HttpStatusCode.Forbidden, outcome.response)
                    is PairingManager.Outcome.Invalid -> call.respond(HttpStatusCode.Unauthorized, ErrorResponse(outcome.message))
                    is PairingManager.Outcome.Busy -> call.respond(HttpStatusCode.Conflict, ErrorResponse(outcome.message))
                }
            }
        }
        post("/v1/sync/push") {
            val client = call.client() ?: return@post call.unauthorized()
            call.guard { call.respond(host.push(client.deviceId, call.receive<PushRequest>())) }
        }
        get("/v1/sync/pull") {
            val client = call.client() ?: return@get call.unauthorized()
            call.guard {
                val after = call.request.queryParameters["after"]?.toLongOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 200
                call.respond(host.pull(client.deviceId, after, limit))
            }
        }
    }
}

/** Starts HTTPS on the first free port from [preferredPort]. Plain HTTP is never served. */
fun startSyncServer(
    tls: TlsIdentity,
    preferredPort: Int,
    module: Application.() -> Unit,
): Pair<EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>, Int> {
    var lastError: Throwable? = null
    for (port in preferredPort until preferredPort + 10) {
        val server = embeddedServer(
            Netty,
            applicationEnvironment { },
            configure = {
                sslConnector(
                    keyStore = tls.keyStore,
                    keyAlias = TlsIdentity.ALIAS,
                    keyStorePassword = { tls.password.toCharArray() },
                    privateKeyPassword = { tls.password.toCharArray() },
                ) {
                    this.port = port
                    this.host = "0.0.0.0"
                }
            },
            module = module,
        )
        try {
            server.start(wait = false)
            return server to port
        } catch (e: Exception) {
            lastError = e
            runCatching { server.stop(0, 0) }
        }
    }
    throw IllegalStateException("No free port for sync near $preferredPort.", lastError)
}
