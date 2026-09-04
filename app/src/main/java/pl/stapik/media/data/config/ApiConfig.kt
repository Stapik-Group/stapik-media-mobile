package pl.stapik.media.data.config

/** Stapik Cloud connection details entered on the Connect screen. */
data class ApiConfig(
    val serverUrl: String,
    val apiKey: String,
) {
    companion object {
        const val SLOT_KEY = "media.json"
    }
}
