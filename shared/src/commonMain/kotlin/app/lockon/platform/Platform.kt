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
