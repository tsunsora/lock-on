package app.lockon.platform

/** Opens and saves user-chosen files through each platform's own file picker. */
interface FileBridge {
    /** Returns false when the person cancelled. */
    suspend fun saveText(suggestedName: String, mimeType: String, content: String): Boolean
    /** Returns null when the person cancelled. */
    suspend fun openText(mimeTypes: List<String>): String?
}

expect fun formatTimestamp(epochMillis: Long): String
