package com.adoradorespreparados.studio.data

data class StreamingConfig(
    val host: String = "",
    val port: Int = 8000,
    val user: String = "source",
    val pass: String = "",
    val mountpoint: String = "/live",
    val bitrate: Int = 128,
    val isIcecast: Boolean = true
) {
    fun getUrl(): String {
        return if (isIcecast) {
            "icecast://$user:$pass@$host:$port$mountpoint"
        } else {
            "shoutcast://$host:$port"
        }
    }
}
