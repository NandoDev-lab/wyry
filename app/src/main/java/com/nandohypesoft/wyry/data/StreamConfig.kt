package com.nandohypesoft.wyry.data

import kotlinx.serialization.Serializable

@Serializable
data class StreamConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val host: String = "",
    val port: Int = 8000,
    val user: String = "source",
    val pass: String = "",
    val mountpoint: String = "/stream",
    val bitrate: Int = 128
)
