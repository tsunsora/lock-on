package app.lockon.desktop

import app.lockon.update.UpdateInstaller
import app.lockon.update.UpdatePlatform
import java.io.File
import java.util.Base64

/**
 * Installs a downloaded MSI over this copy of Lock On. A hidden PowerShell helper waits for Lock On
 * to close, runs the installer with a progress bar and no questions, then optionally reopens Lock
 * On. The MSI installs per user, so no administrator prompt appears.
 */
class WindowsUpdateInstaller(private val requestExit: () -> Unit) : UpdateInstaller {
    /** Set by the jpackage launcher; absent when running from Gradle. */
    private val launcher: File? = System.getProperty("jpackage.app-path")?.let(::File)?.takeIf { it.isFile }

    override val platform = UpdatePlatform.WINDOWS
    override val canInstall: Boolean = launcher != null && "win" in System.getProperty("os.name").lowercase()
    override val downloadDir: String = File(AppDirs.data, "updates").absolutePath

    override suspend fun install(path: String, reopen: Boolean) {
        val exe = checkNotNull(launcher) { "Not an installed copy" }
        // The installer doesn't remember a folder chosen at first install, so name it again.
        val installDir = exe.parentFile.absolutePath
        val log = File(downloadDir, "install.log").absolutePath
        val script = buildString {
            appendLine("Wait-Process -Id ${ProcessHandle.current().pid()} -ErrorAction SilentlyContinue")
            // Updates never make a desktop shortcut. The upgrade removes the one the first install made,
            // so if it is still there it is set aside and put back afterwards, as it was.
            appendLine("\$shortcut = Join-Path ([Environment]::GetFolderPath('DesktopDirectory')) 'Lock On.lnk'")
            appendLine("\$kept = ${ps(File(downloadDir, "Lock On.lnk").absolutePath)}")
            appendLine("if (Test-Path -LiteralPath \$shortcut) { Copy-Item -LiteralPath \$shortcut -Destination \$kept -Force }")
            appendLine(
                "\$p = Start-Process -FilePath msiexec.exe -Wait -PassThru -ArgumentList " +
                    ps("/i \"$path\" /passive /norestart /l* \"$log\" INSTALLDIR=\"$installDir\" JP_INSTALL_DESKTOP_SHORTCUT=\"\""),
            )
            appendLine(
                "if (Test-Path -LiteralPath \$kept) { if (-not (Test-Path -LiteralPath \$shortcut)) " +
                    "{ Move-Item -LiteralPath \$kept -Destination \$shortcut } else { Remove-Item -LiteralPath \$kept } }",
            )
            appendLine("Add-Content -LiteralPath ${ps("$log.txt")} -Value \"\$(Get-Date -Format s) msiexec exit \$(\$p.ExitCode)\"")
            // Reopen even if the install failed: the MSI rolls back, so the old version still works.
            if (reopen) appendLine("Start-Process -FilePath ${ps(exe.absolutePath)}")
        }
        // An encoded command needs no script file and no quoting through cmd.exe.
        val encoded = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
        ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-EncodedCommand", encoded)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        if (reopen) requestExit()
    }

    /** A PowerShell single-quoted string literal. */
    private fun ps(value: String) = "'" + value.replace("'", "''") + "'"
}
