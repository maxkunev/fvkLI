package com.example.fvkli.data

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.example.fvkli.models.Track
import org.json.JSONObject
object AudioDownloader {
    private val audioExtensions = setOf("m4a", "mp3", "opus", "webm", "ogg", "aac")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
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
            val status = YoutubeDL.getInstance().updateYoutubeDL(appContext)
            Log.d("AudioDownloader", "Update status: $status")
            isUpdated = true
        }
    }

    fun getTracksDir(context: Context): File =
        File(context.filesDir, "audio").apply { mkdirs() }

    private fun parseArtistAndTitle(rawTitle: String, fallbackArtist: String): Pair<String, String> {
        val clean = rawTitle.trim()
        val separators = listOf(" - ", " — ", " – ", " | ", ": ")
        for (sep in separators) {
            if (clean.contains(sep)) {
                val parts = clean.split(sep, limit = 2)
                return parts[0].trim() to parts[1].trim()
            }
        }
        // Если разделителя нет — берём первое слово как автора, остальное как название
        val words = clean.split(" ", limit = 2)
        return if (words.size == 2) {
            words[0].trim() to words[1].trim()
        } else {
            fallbackArtist.ifBlank { "Unknown" } to clean.ifBlank { "Untitled" }
        }
    }

    private fun saveTrackMeta(dir: File, id: String, artist: String, title: String) {
        val meta = JSONObject().apply {
            put("artist", artist)
            put("title", title)
        }
        File(dir, "$id.json").writeText(meta.toString())
    }

    fun updateTrackMeta(context: Context, id: String, newArtist: String, newTitle: String) {
        saveTrackMeta(getTracksDir(context), id, newArtist.trim(), newTitle.trim())
    }

    fun deleteTrack(context: Context, track: Track) {
        track.file.delete()
        track.coverFile?.delete()
        File(getTracksDir(context), "${track.id}.json").delete()

    }

    fun getSavedTracks(context: Context): List<Track> {
        val dir = getTracksDir(context)
        val allFiles = dir.listFiles() ?: return emptyList()

        return allFiles
            .filter { it.extension.lowercase() in audioExtensions }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val id = file.nameWithoutExtension
                val metaFile = File(dir, "$id.json")
                val coverFile = allFiles.firstOrNull {
                    it.nameWithoutExtension == id && it.extension.lowercase() in imageExtensions
                }
                var title = id
                var artist = "Unknown"
                if (metaFile.exists()) {
                    runCatching {
                        val json = JSONObject(metaFile.readText())
                        title = json.optString("title", id)
                        artist = json.optString("artist", "Unknown")
                    }
                }
                Track(id = id, title = title, artist = artist, file = file, coverFile = coverFile)
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
            onStatus("Start downloading...")

            val audioDir = getTracksDir(context)
            val trackId = System.currentTimeMillis().toString()

            val request = YoutubeDLRequest(url.trim()).apply {
                addOption("-o", "${audioDir.absolutePath}/$trackId.%(ext)s")
                addOption("-f", "bestaudio/best")
                addOption("-x")
                addOption("--no-playlist")
                addOption("--write-info-json")
                addOption("--write-thumbnail")
            }

            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, line ->
                Log.d("AudioDownloader", "[$progress% | ETA ${etaInSeconds}s] $line")
                if (progress >= 0f) {
                    onProgress(progress)
                }
            }

            val outputFile = audioDir.listFiles()?.firstOrNull {
                it.nameWithoutExtension == trackId && it.extension.lowercase() in audioExtensions
            } ?: error("File not found!")
            val infoFile = File(audioDir, "$trackId.info.json")
            var rawTitle = trackId
            var fallbackArtist = "Unknown"
            if (infoFile.exists()) {
                val json = JSONObject(infoFile.readText())
                rawTitle = json.optString("title", trackId)
                fallbackArtist = json.optString("uploader", "Unknown")
                infoFile.delete()
            }

            val (artist, title) = parseArtistAndTitle(rawTitle, fallbackArtist)
            saveTrackMeta(audioDir, trackId, artist, title)
            outputFile
            outputFile
        }
    }
}