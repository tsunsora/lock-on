package app.lockon.ui.components

import coil3.decode.Decoder

// Coil's BitmapFactory decoder already resamples with filtering on Android.
internal actual fun artworkDecoder(): Decoder.Factory? = null
