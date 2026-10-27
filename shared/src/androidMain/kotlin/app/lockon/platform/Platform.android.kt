package app.lockon.platform

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
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

actual fun encodePng(image: ImageBitmap): ByteArray {
    // Hardware bitmaps can't be compressed directly.
    val bitmap = image.asAndroidBitmap().let { if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it }
    return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
}

actual fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

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
    androidx.compose.ui.input.pointer.PointerIcon(android.view.PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW)
