package app.lockon.desktop

import app.lockon.sync.QrMatrix
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object AppDirs {
    /** Per-user data folder: %APPDATA%\LockOn, ~/Library/Application Support/LockOn, or ~/.local/share/lock-on. */
    /** Set the `lockon.dataDir` system property to run a second, separate profile. */
    val data: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        val dir = System.getProperty("lockon.dataDir")?.let(::File) ?: when {
            "win" in os -> File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", "LockOn")
            "mac" in os -> File(home, "Library/Application Support/LockOn")
            else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "lock-on")
        }
        dir.apply { mkdirs() }
    }
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

private val random = SecureRandom()

fun randomToken(bytes: Int = 32): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(bytes).also(random::nextBytes))

/** Pairing codes avoid look-alike characters (0/O, 1/I) so they are easy to type. */
fun randomPairingCode(length: Int = 6): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..length).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
}

fun constantTimeEquals(a: String, b: String): Boolean =
    MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

fun encodeQr(text: String): QrMatrix {
    val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    val size = matrix.width
    return QrMatrix(size, BooleanArray(size * size) { matrix[it % size, it / size] })
}

/**
 * LAN IPv4 addresses a phone could reach, physical adapters first. Virtual adapters (VPNs,
 * Hyper-V, Docker, WSL) are listed last because phones usually cannot reach them.
 */
fun lanAddresses(): List<String> {
    val virtualHints = listOf("virtual", "vethernet", "vmware", "vbox", "hyper-v", "docker", "wsl", "tailscale", "zerotier", "loopback")
    return NetworkInterface.getNetworkInterfaces().toList()
        .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
        .flatMap { nic ->
            val virtual = nic.isVirtual || virtualHints.any { nic.displayName.lowercase().contains(it) || nic.name.lowercase().contains(it) }
            nic.inetAddresses.toList().filterIsInstance<Inet4Address>().filter { it.isSiteLocalAddress }.map { it.hostAddress to virtual }
        }
        .sortedBy { it.second }
        .map { it.first }
        .distinct()
}

/** Logs how long after the process started each stage of launching was reached, for reports of slow starts. */
object BootLog {
    private val started = runCatching { java.lang.management.ManagementFactory.getRuntimeMXBean().startTime }.getOrDefault(System.currentTimeMillis())

    fun mark(stage: String) {
        org.slf4j.LoggerFactory.getLogger("LockOn").info("Launch: $stage at ${System.currentTimeMillis() - started} ms")
    }
}
