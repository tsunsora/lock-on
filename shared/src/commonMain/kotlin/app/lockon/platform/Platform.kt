package app.lockon.platform

import androidx.compose.ui.graphics.ImageBitmap

/** Opens and saves user-chosen files through each platform's own file picker. */
interface FileBridge {
    /** Returns false when the person cancelled. */
    suspend fun saveText(suggestedName: String, mimeType: String, content: String): Boolean
    /** Returns false when the person cancelled. */
    suspend fun saveBytes(suggestedName: String, mimeType: String, content: ByteArray): Boolean
    /** Returns null when the person cancelled. */
    suspend fun openText(mimeTypes: List<String>): String?
}

expect fun formatTimestamp(epochMillis: Long): String

expect fun formatDate(epochMillis: Long): String

/** PNG bytes of a rendered image, for saving deck images. */
expect fun encodePng(image: ImageBitmap): ByteArray

/** An opaque image from 0xAARRGGBB [pixels], row by row, for pictures painted in code. */
expect fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap

/** A QR code for [text], as UTF-8 with medium error correction. Both platforms use zxing. */
expect fun encodeShareQr(text: String): app.lockon.sync.QrMatrix
