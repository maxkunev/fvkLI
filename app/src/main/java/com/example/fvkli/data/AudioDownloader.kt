package com.example.fvkli.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.fvkli.models.Track
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.NonCancellable
import java.util.Collections

object AudioDownloader {
    private val audioExtensions = setOf("m4a", "mp3", "opus", "webm", "ogg", "aac", "wav", "flac", "mp4", "mkv")
    private val videoExtensions = setOf("mp4", "mov", "mkv", "avi", "3gp")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "image")
    private var isInitialized = false
    private var isUpdated = false

    @Volatile private var currentProcessId: String? = null
    @Volatile private var isCancelled = false

    private data class PlaylistEntry(
        val url: String,
        val title: String
    )

    fun cancelDownload(context: Context) {
        isCancelled = true
        currentProcessId?.let { id ->
            runCatching { YoutubeDL.getInstance().destroyProcessById(id) }
        }
    }

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

    private fun downloadFallbackThumbnail(thumbUrl: String, targetFile: File) {
        if (thumbUrl.isBlank()) return
        runCatching {
            val conn = java.net.URL(thumbUrl).openConnection().apply {
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            conn.getInputStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }.onFailure {
            Log.e("AudioDownloader", "Fallback thumbnail failed: ${it.message}")
            targetFile.delete()
        }
    }

    fun updateTrackMeta(context: Context, id: String, newArtist: String, newTitle: String) {
        saveTrackMeta(getTracksDir(context), id, newArtist.trim(), newTitle.trim())
    }

    fun deleteTrack(context: Context, track: Track) {
        getTracksDir(context).listFiles()?.forEach { file ->
            if (file.name == track.file.name || file.name.startsWith("${track.id}.")) {
                file.delete()
            }
        }
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
        onStatus: (String) -> Unit,
        onProgress: (Float) -> Unit,
        onTrackDownloaded: (File) -> Unit = {},
        onQueueProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> },
        onRollback: (List<String>) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        isCancelled = false
        val cleanUrl = url.trim()
        val sessionTrackIds = Collections.synchronizedList(mutableListOf<String>())

        try {
            ensureInitialized(context, onStatus)
            if (isCancelled) throw CancellationException("Cancelled")
            currentCoroutineContext().ensureActive()

            withContext(Dispatchers.Main) { onStatus("Fetching info...") }
            val entries = extractPlaylistEntries(cleanUrl)

            if (isCancelled) throw CancellationException("Cancelled")
            currentCoroutineContext().ensureActive()

            if (entries.isEmpty()) {
                return@withContext Result.failure(Exception("No tracks found"))
            }

            val isPlaylist = entries.size > 1
            var downloadedCount = 0
            var skippedCount = 0
            var lastSavedFile: File? = null

            withContext(Dispatchers.Main) {
                onQueueProgress(0, entries.size)
            }

            for ((index, entry) in entries.withIndex()) {
                if (isCancelled) throw CancellationException("Cancelled")
                currentCoroutineContext().ensureActive()

                val statusPrefix = if (isPlaylist) {
                    "${index + 1}/${entries.size} • ${entry.title}"
                } else {
                    "Downloading..."
                }

                withContext(Dispatchers.Main) {
                    onProgress(0f)
                    onStatus(statusPrefix)
                }

                val itemResult = downloadSingleItem(
                    context = context,
                    itemUrl = entry.url,
                    fallbackTitle = entry.title,
                    onProgress = { percent ->
                        if (!isCancelled) {
                            onProgress(percent)
                        }
                    }
                )

                if (isCancelled) throw CancellationException("Cancelled")

                itemResult.onSuccess { savedFile ->
                    sessionTrackIds.add(savedFile.nameWithoutExtension)
                    downloadedCount++
                    lastSavedFile = savedFile
                    withContext(Dispatchers.Main) {
                        onTrackDownloaded(savedFile)
                        onQueueProgress(downloadedCount, entries.size)
                    }
                }.onFailure { err ->
                    if (isCancelled || err is CancellationException) {
                        throw CancellationException("Cancelled")
                    }
                    if (!isPlaylist) {
                        throw err
                    }
                    skippedCount++
                }
            }

            if (downloadedCount == 0) {
                Result.failure(Exception("Could not download tracks"))
            } else if (!isPlaylist && lastSavedFile != null) {
                val sizeMb = String.format("%.2f", lastSavedFile!!.length() / (1024.0 * 1024.0))
                Result.success("Ready! ($sizeMb MB)")
            } else {
                val summary = if (skippedCount > 0) {
                    "Added $downloadedCount tracks ($skippedCount skipped)"
                } else {
                    "Added $downloadedCount tracks"
                }
                Result.success(summary)
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val idsToRemove = sessionTrackIds.toList()
                rollbackSessionTracks(context, idsToRemove)
                withContext(Dispatchers.Main) {
                    onRollback(idsToRemove)
                }
            }
            Result.failure(Exception("Cancelled"))
        } catch (e: Exception) {
            if (isCancelled) {
                withContext(NonCancellable) {
                    val idsToRemove = sessionTrackIds.toList()
                    rollbackSessionTracks(context, idsToRemove)
                    withContext(Dispatchers.Main) {
                        onRollback(idsToRemove)
                    }
                }
                Result.failure(Exception("Cancelled"))
            } else {
                Result.failure(e)
            }
        } finally {
            currentProcessId = null
        }
    }

    private fun rollbackSessionTracks(context: Context, trackIds: List<String>) {
        if (trackIds.isEmpty()) return
        val dir = getTracksDir(context)
        val idSet = trackIds.toSet()
        dir.listFiles()?.forEach { file ->
            if (file.nameWithoutExtension in idSet) {
                file.delete()
            }
        }
    }

    private fun extractPlaylistEntries(url: String): List<PlaylistEntry> {
        val processId = "extract_${UUID.randomUUID()}"
        currentProcessId = processId

        return try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--flat-playlist")
                addOption("--dump-single-json")
                addOption("--no-warnings")
            }

            val response = YoutubeDL.getInstance().execute(request, processId)
            val rawOut = response.out.trim()
            val jsonStart = rawOut.indexOf('{')
            if (jsonStart < 0) {
                return if (isCancelled) emptyList() else listOf(PlaylistEntry(url, "Track"))
            }

            val json = JSONObject(rawOut.substring(jsonStart))
            val entriesArray = json.optJSONArray("entries")

            if (entriesArray != null && entriesArray.length() > 0) {
                val result = mutableListOf<PlaylistEntry>()
                for (i in 0 until entriesArray.length()) {
                    val item = entriesArray.optJSONObject(i) ?: continue
                    val rawTitle = item.optString("title").trim()
                    if (rawTitle == "[Deleted video]" || rawTitle == "[Private video]") continue

                    val title = rawTitle.ifBlank { "Track ${i + 1}" }
                    var itemUrl = item.optString("webpage_url").ifEmpty { item.optString("url") }

                    if (!itemUrl.startsWith("http")) {
                        val id = item.optString("id").ifEmpty { itemUrl }
                        val ieKey = item.optString("ie_key")
                        if (id.isNotBlank() && (ieKey.equals("Youtube", ignoreCase = true) || url.contains("youtu", ignoreCase = true))) {
                            itemUrl = "https://www.youtube.com/watch?v=$id"
                        }
                    }

                    if (itemUrl.startsWith("http")) {
                        result.add(PlaylistEntry(itemUrl, title))
                    }
                }
                result
            } else {
                val singleTitle = json.optString("title").ifBlank { "Track" }
                val singleUrl = json.optString("webpage_url").ifEmpty { url }
                listOf(PlaylistEntry(singleUrl, singleTitle))
            }
        } catch (e: Exception) {
            if (isCancelled) emptyList() else listOf(PlaylistEntry(url, "Track"))
        } finally {
            currentProcessId = null
        }
    }

    private fun downloadSingleItem(
        context: Context,
        itemUrl: String,
        fallbackTitle: String,
        onProgress: (Float) -> Unit
    ): Result<File> {
        val trackId = System.currentTimeMillis().toString()
        val tempDir = File(context.cacheDir, "dl_tmp_$trackId").apply { mkdirs() }
        val processId = "dl_$trackId"
        currentProcessId = processId

        return try {
            val request = YoutubeDLRequest(itemUrl).apply {
                addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
                addOption("-o", "${tempDir.absolutePath}/audio.%(ext)s")
                addOption("--no-playlist")
                addOption("--write-thumbnail")
                addOption("--write-info-json")
                addOption("--no-mtime")
                addOption("--no-warnings")
            }

            YoutubeDL.getInstance().execute(request, processId) { progress, _, _ ->
                if (!isCancelled && progress >= 0f) {
                    onProgress(progress)
                }
            }

            if (isCancelled) throw CancellationException("Cancelled")

            val files = tempDir.listFiles()?.toList().orEmpty()
            val audioFile = files.firstOrNull { file ->
                file.isFile &&
                        file.extension.lowercase() in audioExtensions &&
                        !file.name.endsWith(".part")
            } ?: error("Audio file not found")

            var rawTitle = fallbackTitle
            var uploader = "Unknown"
            var thumbUrl = ""
            val infoFile = files.firstOrNull { it.name.endsWith(".info.json") }
            if (infoFile != null) {
                runCatching {
                    val infoJson = JSONObject(infoFile.readText())
                    rawTitle = infoJson.optString("title").ifBlank { fallbackTitle }
                    uploader = infoJson.optString("artist")
                        .ifBlank { infoJson.optString("creator") }
                        .ifBlank { infoJson.optString("uploader") }
                        .ifBlank { infoJson.optString("channel") }
                        .ifBlank { "Unknown" }
                    thumbUrl = infoJson.optString("thumbnail")
                }
            }

            val audioDir = getTracksDir(context)
            val finalExt = audioFile.extension.lowercase().ifBlank { "m4a" }
            val finalAudioFile = File(audioDir, "$trackId.$finalExt")
            audioFile.copyTo(finalAudioFile, overwrite = true)

            val targetCover = File(audioDir, "$trackId.jpg")
            val thumbFile = files.firstOrNull { it.isFile && it.extension.lowercase() in imageExtensions }
                ?: if (thumbUrl.isNotBlank()) {
                    File(tempDir, "fallback_thumb").also { downloadFallbackThumbnail(thumbUrl, it) }
                } else null

            if (thumbFile != null && thumbFile.exists()) {
                runCatching {
                    val bitmap = BitmapFactory.decodeFile(thumbFile.absolutePath)
                    if (bitmap != null) {
                        targetCover.outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        bitmap.recycle()
                    }
                }
            }

            val (artist, title) = parseArtistAndTitle(rawTitle, uploader)
            saveTrackMeta(audioDir, trackId, artist, title)

            Result.success(finalAudioFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            currentProcessId = null
            tempDir.deleteRecursively()
        }
    }

    suspend fun importLocalFile(context: Context, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val audioDir = getTracksDir(context)
            val trackId = System.currentTimeMillis().toString()

            val displayName = getFileName(context, uri)
            val ext = displayName.substringAfterLast('.', "m4a").lowercase()
            val rawName = displayName.substringBeforeLast('.')

            val targetFile = if (ext in videoExtensions) {
                val m4aFile = File(audioDir, "$trackId.m4a")
                if (extractAudioFromVideo(context, uri, m4aFile)) {
                    m4aFile
                } else {
                    copyUriToFile(context, uri, File(audioDir, "$trackId.$ext"))
                }
            } else {
                val safeExt = if (ext in audioExtensions) ext else "m4a"
                copyUriToFile(context, uri, File(audioDir, "$trackId.$safeExt"))
            }

            extractEmbeddedCover(context, uri, File(audioDir, "$trackId.jpg"))

            val (artist, title) = parseArtistAndTitle(rawName, "Unknown")
            saveTrackMeta(audioDir, trackId, artist, title)

            targetFile
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown Track.m4a"
    }

    private fun copyUriToFile(context: Context, uri: Uri, dest: File): File {
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open file")
        return dest
    }

    private fun extractAudioFromVideo(context: Context, uri: Uri, outFile: File): Boolean {
        return runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            var audioIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioIndex < 0 || audioFormat == null) {
                extractor.release()
                return false
            }
            extractor.selectTrack(audioIndex)
            val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                info.offset = 0
                info.size = sampleSize
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrack, buffer, info)
                extractor.advance()
            }
            muxer.stop()
            muxer.release()
            extractor.release()
            true
        }.getOrElse {
            outFile.delete()
            false
        }
    }

    private fun extractEmbeddedCover(context: Context, uri: Uri, targetCover: File) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture
            retriever.release()
            if (bytes != null && BitmapFactory.decodeByteArray(bytes, 0, bytes.size) != null) {
                targetCover.writeBytes(bytes)
            }
        }
    }
}