package com.example.fvkli.models

import java.io.File

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val file: File,
    val coverFile: File? = null,
    val playlistId: String? = null,
    val sourceUrl: String? = null
)