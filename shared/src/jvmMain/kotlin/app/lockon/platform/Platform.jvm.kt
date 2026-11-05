package app.lockon.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

actual fun formatTimestamp(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

actual fun formatDate(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

actual fun encodePng(image: ImageBitmap): ByteArray =
    Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
        ?: error("Could not encode the image.")

actual fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    // Skia wants bytes; BGRA on little-endian is the int's bytes in memory order.
    val bytes = ByteArray(pixels.size * 4)
    pixels.forEachIndexed { i, p ->
        bytes[i * 4] = p.toByte(); bytes[i * 4 + 1] = (p shr 8).toByte()
        bytes[i * 4 + 2] = (p shr 16).toByte(); bytes[i * 4 + 3] = (p ushr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
    return Image.makeRaster(info, bytes, width * 4).toComposeImageBitmap()
}

actual fun encodeShareQr(text: String): app.lockon.sync.QrMatrix {
    val hints = mapOf(
        com.google.zxing.EncodeHintType.MARGIN to 0,
        com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8",
        com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,
    )
    val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, hints)
    val size = matrix.width
    return app.lockon.sync.QrMatrix(size, BooleanArray(size * size) { matrix[it % size, it / size] })
}

actual val resizeHorizontalPointer: androidx.compose.ui.input.pointer.PointerIcon =
    androidx.compose.ui.input.pointer.PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR))

actual val isMobile: Boolean = false

actual fun localDay(epochMillis: Long): Long =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()

actual fun formatMonthDay(epochMillis: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("MMM d").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(epochMillis))

actual fun formatClock(epochMillis: Long): String =
    java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(epochMillis))

actual fun fullWindowDialogProperties() = plainDialogProperties(usePlatformDefaultWidth = false)

actual fun plainDialogProperties(usePlatformDefaultWidth: Boolean, dismissible: Boolean) = androidx.compose.ui.window.DialogProperties(
    dismissOnBackPress = dismissible,
    dismissOnClickOutside = dismissible,
    usePlatformDefaultWidth = usePlatformDefaultWidth,
    scrimColor = androidx.compose.ui.graphics.Color.Transparent,
)

@androidx.compose.runtime.Composable
actual fun LightSystemBarIcons() = Unit
