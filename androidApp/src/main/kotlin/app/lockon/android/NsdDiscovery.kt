package app.lockon.android

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import app.lockon.sync.Discovery
import app.lockon.sync.LocalIdentity
import app.lockon.sync.NearbyDevice
import app.lockon.sync.SyncProtocol
import java.net.Inet4Address
import java.util.concurrent.Executors

/**
 * Finds Lock On desktops on the local network with Android's built-in DNS-SD. Runs only while the
 * app is in the foreground. Results are hints for where a desktop is; trust comes from pairing.
 */
class NsdDiscovery(
    context: Context,
    private val onFound: (serviceName: String, desktop: NearbyDevice) -> Unit,
    private val onLost: (serviceName: String) -> Unit,
) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val executor = Executors.newSingleThreadExecutor()
    private var listener: NsdManager.DiscoveryListener? = null
    private val infoCallbacks = mutableListOf<NsdManager.ServiceInfoCallback>()
    private var registration: NsdManager.RegistrationListener? = null

    /** Discovers devices and, like LocalSend, announces this phone so desktops can invite it. */
    fun start(identity: LocalIdentity) {
        announce(identity)
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

    private fun announce(identity: LocalIdentity) {
        if (registration != null) return
        val info = NsdServiceInfo().apply {
            serviceName = identity.deviceName.take(60)
            serviceType = "${SyncProtocol.SERVICE_TYPE}."
            // Phones don't accept connections; the port is required by DNS-SD but unused.
            port = SyncProtocol.DEFAULT_PORT
            Discovery.txt(identity, fingerprint = null, role = SyncProtocol.ROLE_PHONE).forEach { (k, v) -> setAttribute(k, v) }
        }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) { registration = null }
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
        }
        registration = l
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l) }.onFailure { registration = null }
    }

    fun stop() {
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
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
