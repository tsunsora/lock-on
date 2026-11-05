package app.lockon.desktop

import com.sun.jna.platform.win32.KnownFolders
import com.sun.jna.platform.win32.Shell32Util
import com.sun.jna.platform.win32.Win32Exception
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/** Windows asks the shell, as people can move their Downloads folder to another drive. */
internal fun downloadsFolder(): Path {
    val home = Path.of(System.getProperty("user.home"), "Downloads")
    if (!System.getProperty("os.name").startsWith("Windows")) return home
    return try {
        Path.of(Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_Downloads))
    } catch (_: Win32Exception) {
        home
    }
}

/** Writes [name] into [folder], or "name (1)", "name (2)" and so on when that's taken, and never over a file already there. */
internal fun writeBesideOthers(folder: Path, name: String, content: ByteArray): Path {
    Files.createDirectories(folder)
    val stem = name.substringBeforeLast('.')
    val extension = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
    var copy = 0
    while (true) {
        val file = folder.resolve(if (copy == 0) name else "$stem ($copy)$extension")
        try {
            return Files.write(file, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        } catch (_: FileAlreadyExistsException) {
            copy++
        }
    }
}
