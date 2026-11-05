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

/** The pointer shown over a divider that resizes side by side areas. */
expect val resizeHorizontalPointer: androidx.compose.ui.input.pointer.PointerIcon

/** Whether Lock On runs on a phone or tablet, for settings that only apply there. */
expect val isMobile: Boolean

/**
 * A dialog that fills the window. On phones it reaches under the status and navigation bars, so the
 * dim it paints darkens them too; the system adds no dim of its own there, and the dialog's content
 * keeps clear of the bars with window insets.
 */
expect fun fullWindowDialogProperties(): androidx.compose.ui.window.DialogProperties

/** Inside a [fullWindowDialogProperties] dialog: the bars' clock and icons stay light over its dark dim, in either theme. */
@androidx.compose.runtime.Composable
expect fun LightSystemBarIcons()
