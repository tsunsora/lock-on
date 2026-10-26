package app.lockon.desktop

import app.lockon.sync.Discovery
import app.lockon.sync.LocalIdentity
import app.lockon.sync.SyncProtocol
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

/**
 * Advertises this desktop as `_lockon._tcp` on the local network so phones can find it without
 * typing an address. Advertising grants nothing: phones still pair and pin the certificate.
 */
class LanAdvertiser(private val identity: LocalIdentity, private val fingerprint: String) {
    private var jmdns: JmDNS? = null
    private var advertised: Pair<String, Int>? = null

    /** (Re)advertises on [address]. Safe to call again when the address or port changes. */
    @Synchronized
    fun advertise(address: String, port: Int) {
        if (advertised == address to port) return
        stop()
        runCatching {
            val dns = JmDNS.create(InetAddress.getByName(address), identity.deviceName)
            val info = ServiceInfo.create(
                "${SyncProtocol.SERVICE_TYPE}.local.", identity.deviceName.take(60), port, 0, 0,
                Discovery.txt(identity, fingerprint),
            )
            dns.registerService(info)
            jmdns = dns
            advertised = address to port
        }
    }

    @Synchronized
    fun stop() {
        jmdns?.let { runCatching { it.unregisterAllServices(); it.close() } }
        jmdns = null
        advertised = null
    }
}
