package app.lockon.ui.components

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Dispatcher

actual fun artworkHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine { config { dispatcher(Dispatcher().apply { maxRequestsPerHost = ARTWORK_DOWNLOADS }) } }
}
