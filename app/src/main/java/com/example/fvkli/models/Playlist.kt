package com.example.fvkli.models

import java.io.File

data class Playlist(
    val id: String,
    val name: String,
    val coverFile: File? = null,
    val hideAutoCover: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)