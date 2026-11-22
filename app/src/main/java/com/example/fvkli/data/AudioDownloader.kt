package com.example.fvkli.data

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AudioDownloader {

    private var isInitialized = false
    private var isUpdated = false

    private fun ensureInitialized(
        context: Context,
        onStatus: (String) -> Unit
    ) {
        val appContext = context.applicationContext
        if (!isInitialized) {
            onStatus("Unpacking core...")
            YoutubeDL.getInstance().init(appContext)
            FFmpeg.getInstance().init(appContext)
            isInitialized = true
        }
        if (!isUpdated) {
            onStatus("Updating yt-dlp, pls wait...")
            // Подтягивает свежий yt-dlp напрямую с GitHub (можно также передать второй аргумент UpdateChannel._NIGHTLY)
            val status = YoutubeDL.getInstance().updateYoutubeDL(appContext)
            Log.d("AudioDownloader", "Update status: $status")
            isUpdated = true
        }
    }

    suspend fun downloadAudio(
        context: Context,
        url: String,
        onStatus: (String) -> Unit = {},
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInitialized(context, onStatus)
            onStatus("Запуск загрузки...")

            val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
            val trackId = System.currentTimeMillis().toString()

            val request = YoutubeDLRequest(url.trim()).apply {
                addOption("-o", "${audioDir.absolutePath}/$trackId.%(ext)s")
                addOption("-x")
                addOption("--audio-format", "m4a")
                addOption("--no-playlist")
            }

            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, line ->
                Log.d("AudioDownloader", "[$progress% | ETA ${etaInSeconds}s] $line")
                if (progress >= 0f) {
                    onProgress(progress)
                }
            }

            val outputFile = File(audioDir, "$trackId.m4a")
            require(outputFile.exists()) { "File not found: ${outputFile.absolutePath}" }
            outputFile
        }
    }
}