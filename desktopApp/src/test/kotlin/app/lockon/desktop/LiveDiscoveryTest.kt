package app.lockon.desktop

import app.lockon.sync.Discovery
import app.lockon.sync.LocalIdentity
import app.lockon.sync.SyncProtocol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * On the real network: a phone-style announcement must show up in the desktop's nearby list. Needs
 * multicast on the LAN, so it only runs with LOCKON_LIVE=1 and LOCKON_LAN_IP set to this PC's address.
 */
class LiveDiscoveryTest {
    @Test fun desktopSeesAnnouncingPhones() = runBlocking<Unit> {
        if (System.getenv("LOCKON_LIVE") != "1") return@runBlocking
        val address = System.getenv("LOCKON_LAN_IP") ?: lanAddresses().first()
        val desktop = LanAdvertiser(LocalIdentity("live-desk", "Live Desk"), "ab".repeat(32))
        desktop.advertise(address, 53999)
        val phoneDns = JmDNS.create(InetAddress.getByName(address), "live-phone")
        try {
            val phone = LocalIdentity("live-phone-id", "Live Test Phone")
            phoneDns.registerService(
                ServiceInfo.create("${SyncProtocol.SERVICE_TYPE}.local.", phone.deviceName, SyncProtocol.DEFAULT_PORT, 0, 0,
                    Discovery.txt(phone, null, SyncProtocol.ROLE_PHONE)),
            )
            val seen = withTimeoutOrNull(20_000) { desktop.nearby.first { map -> map.values.any { it.deviceId == "live-phone-id" } } }
            assertNotNull(seen, "the desktop should notice the phone within 20 s")
            val found = seen.values.first { it.deviceId == "live-phone-id" }
            assertTrue(!found.isDesktop)
            println("Desktop saw: ${found.name} (${found.role}) at ${found.host}")
        } finally {
            phoneDns.unregisterAllServices(); phoneDns.close()
            desktop.stop()
        }
    }
}
