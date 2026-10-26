package app.lockon.android

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import app.lockon.sync.Discovery
import app.lockon.sync.NearbyDesktop
import app.lockon.sync.SyncProtocol
import java.net.Inet4Address
import java.util.concurrent.Executors

/**
 * Finds Lock On desktops on the local network with Android's built-in DNS-SD. Runs only while the
 * app is in the foreground. Results are hints for where a desktop is; trust comes from pairing.
 */
class NsdDiscovery(
    context: Context,
    private val onFound: (serviceName: String, desktop: NearbyDesktop) -> Unit,
    private val onLost: (serviceName: String) -> Unit,
) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val executor = Executors.newSingleThreadExecutor()
    private var listener: NsdManager.DiscoveryListener? = null
    private val infoCallbacks = mutableListOf<NsdManager.ServiceInfoCallback>()

    fun start() {
        if (listener != null) return
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { listener = null }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceFound(service: NsdServiceInfo) = resolve(service)
            override fun onServiceLost(service: NsdServiceInfo) = onLost(service.serviceName)
        }
        listener = l
        runCatching { nsd.discoverServices("${SyncProtocol.SERVICE_TYPE}.", NsdManager.PROTOCOL_DNS_SD, l) }
            .onFailure { listener = null }
    }

    fun stop() {
        listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        listener = null
        if (Build.VERSION.SDK_INT >= 34) synchronized(infoCallbacks) {
            infoCallbacks.forEach { runCatching { nsd.unregisterServiceInfoCallback(it) } }
            infoCallbacks.clear()
        }
    }

    private fun resolve(service: NsdServiceInfo) {
        if (Build.VERSION.SDK_INT >= 34) {
            val callback = object : NsdManager.ServiceInfoCallback {
                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) = Unit
                override fun onServiceUpdated(info: NsdServiceInfo) = report(info)
                override fun onServiceLost() = onLost(service.serviceName)
                override fun onServiceInfoCallbackUnregistered() = Unit
            }
            synchronized(infoCallbacks) { infoCallbacks += callback }
            runCatching { nsd.registerServiceInfoCallback(service, executor, callback) }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                nsd.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(info: NsdServiceInfo) = report(info)
                })
            }
        }
    }

    private fun report(info: NsdServiceInfo) {
        val address = if (Build.VERSION.SDK_INT >= 34) {
            info.hostAddresses.firstOrNull { it is Inet4Address } ?: info.hostAddresses.firstOrNull()
        } else {
            @Suppress("DEPRECATION") info.host
        } ?: return
        val txt = info.attributes.mapValues { (_, v) -> v?.decodeToString().orEmpty() }
        val desktop = Discovery.parse(txt, address.hostAddress ?: return, info.port, info.serviceName) ?: return
        onFound(info.serviceName, desktop)
    }
}
