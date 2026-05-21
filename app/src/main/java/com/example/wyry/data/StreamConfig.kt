package com.example.wyry.data

data class StreamConfig(
    val host: String = "",
    val port: Int = 8000,
    val user: String = "source",
    val pass: String = "",
    val mountpoint: String = "/stream",
    val bitrate: Int = 128
)
