package app.lockon.desktop

import app.lockon.sync.Discovery
import app.lockon.sync.LocalIdentity
import app.lockon.sync.NearbyDevice
import app.lockon.sync.SyncProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

/**
 * Makes this desktop visible on the local network (`_lockon._tcp`) and, like LocalSend, watches for
 * other Lock On devices: phones with the app open announce themselves too. Seeing a device grants
 * nothing; connecting still needs a confirmed pairing.
 */
class LanAdvertiser(private val identity: LocalIdentity, private val fingerprint: String) {
    private var jmdns: JmDNS? = null
    private var advertised: Pair<String, Int>? = null
    private val _nearby = MutableStateFlow<Map<String, NearbyDevice>>(emptyMap())
    /** Other Lock On devices on the network, keyed by service name. */
    val nearby: StateFlow<Map<String, NearbyDevice>> = _nearby.asStateFlow()

    private val type = "${SyncProtocol.SERVICE_TYPE}.local."

    private val listener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) { event.dns.requestServiceInfo(event.type, event.name, 3_000) }
        override fun serviceRemoved(event: ServiceEvent) { _nearby.value = _nearby.value - event.name }
        override fun serviceResolved(event: ServiceEvent) {
            val info = event.info
            val txt = info.propertyNames.toList().associateWith { info.getPropertyString(it).orEmpty() }
            val host = info.inet4Addresses.firstOrNull()?.hostAddress ?: info.hostAddresses.firstOrNull() ?: return
            val device = Discovery.parse(txt, host, info.port, event.name) ?: return
            if (device.deviceId == identity.deviceId) return
            _nearby.value = _nearby.value + (event.name to device)
        }
    }

    /** (Re)advertises on [address]. Safe to call again when the address or port changes. */
    @Synchronized
    fun advertise(address: String, port: Int) {
        if (advertised == address to port) return
        stop()
        runCatching {
            val dns = JmDNS.create(InetAddress.getByName(address), identity.deviceName)
            val info = ServiceInfo.create(type, identity.deviceName.take(60), port, 0, 0, Discovery.txt(identity, fingerprint))
            dns.registerService(info)
            dns.addServiceListener(type, listener)
            jmdns = dns
            advertised = address to port
        }
    }

    @Synchronized
    fun stop() {
        jmdns?.let { runCatching { it.removeServiceListener(type, listener); it.unregisterAllServices(); it.close() } }
        jmdns = null
        advertised = null
        _nearby.value = emptyMap()
    }
}
