package app.lockon.desktop

import androidx.compose.ui.graphics.Color
import app.lockon.ui.theme.SystemAccent
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.util.concurrent.TimeUnit

/**
 * Reads the system's accent colour for Material You colours (UX rule V3a): Windows' accent colour,
 * macOS's accent colour or GNOME's. Elsewhere, or if the system won't say, there is none and Lock On's
 * own Sky and Twilight show. Read at launch and whenever the window comes forward, since the accent may
 * have changed while Lock On was in the background.
 */
internal object SystemAccentReader {
    private val os = System.getProperty("os.name").orEmpty()

    /** Reads it now on Windows, where it is a quick registry read, and in the background elsewhere. */
    fun refresh() {
        if (os.startsWith("Windows")) SystemAccent.color.value = windows()
        else kotlin.concurrent.thread(isDaemon = true, name = "accent") { SystemAccent.color.value = read() }
    }

    private fun read(): Color? = when {
        os.startsWith("Mac") -> mac()
        os.startsWith("Linux") -> gnome()
        else -> null
    }

    /** Windows keeps the accent as 0xAABBGGRR under DWM. */
    private fun windows(): Color? = runCatching {
        val key = "Software\\Microsoft\\Windows\\DWM"
        if (!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, key, "AccentColor")) return null
        val abgr = Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, key, "AccentColor")
        Color(red = abgr and 0xFF, green = abgr shr 8 and 0xFF, blue = abgr shr 16 and 0xFF)
    }.getOrNull()

    /** macOS names its accent by number; with none set ("Multicolor") it is blue. */
    private fun mac(): Color? {
        val out = command("defaults", "read", "-g", "AppleAccentColor")
        return when (out?.toIntOrNull()) {
            -1 -> Color(0xFF8C8C8C)
            0 -> Color(0xFFFF5257)
            1 -> Color(0xFFF7821B)
            2 -> Color(0xFFFFC600)
            3 -> Color(0xFF62BA46)
            5 -> Color(0xFFA550A7)
            6 -> Color(0xFFF74F9E)
            else -> Color(0xFF007AFF)
        }
    }

    /** GNOME 47 and later name one of nine accents; older versions have none. */
    private fun gnome(): Color? = when (command("gsettings", "get", "org.gnome.desktop.interface", "accent-color")?.trim('\'')) {
        "blue" -> Color(0xFF3584E4)
        "teal" -> Color(0xFF2190A4)
        "green" -> Color(0xFF3A944A)
        "yellow" -> Color(0xFFC88800)
        "orange" -> Color(0xFFED5B00)
        "red" -> Color(0xFFE62D42)
        "pink" -> Color(0xFFD56199)
        "purple" -> Color(0xFF9141AC)
        "slate" -> Color(0xFF6F8396)
        else -> null
    }

    /** A command's trimmed output, or null if it fails or takes more than two seconds. */
    private fun command(vararg args: String): String? = runCatching {
        val process = ProcessBuilder(*args).redirectErrorStream(true).start()
        if (!process.waitFor(2, TimeUnit.SECONDS)) { process.destroy(); return null }
        val text = process.inputStream.bufferedReader().readText().trim()
        text.takeIf { process.exitValue() == 0 }
    }.getOrNull()
}
