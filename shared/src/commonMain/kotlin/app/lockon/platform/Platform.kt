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

/** The local calendar day [epochMillis] falls on, as a day count: only differences between two mean anything. */
expect fun localDay(epochMillis: Long): Long

/** "Oct 28" in the device's locale. */
expect fun formatMonthDay(epochMillis: Long): String

/** "21:44" or "9:44 PM", as the device tells time. */
expect fun formatClock(epochMillis: Long): String

private fun nowMillis() = kotlin.time.Clock.System.now().toEpochMilliseconds()

/** "Today", "Yesterday" or "Oct 28". */
fun dayLabel(epochMillis: Long): String = when (localDay(nowMillis()) - localDay(epochMillis)) {
    0L -> "Today"
    1L -> "Yesterday"
    else -> formatMonthDay(epochMillis)
}

/** "Today 13:10", "Yesterday 21:44" or "Oct 28 18:02". */
fun dayAndTimeLabel(epochMillis: Long): String = "${dayLabel(epochMillis)} ${formatClock(epochMillis)}"

/** How long ago, shortest first: "4 min", "2 h", "Yesterday" or "Oct 28". */
fun ageLabel(epochMillis: Long): String {
    val minutes = (nowMillis() - epochMillis) / 60_000
    return when {
        minutes < 1 -> "Now"
        minutes < 60 -> "$minutes min"
        localDay(nowMillis()) == localDay(epochMillis) -> "${minutes / 60} h"
        else -> dayLabel(epochMillis)
    }
}

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
