package app.lockon.catalog

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

actual fun downloadClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) { requestTimeoutMillis = 60_000; connectTimeoutMillis = 15_000 }
}
