package com.example.fvkli.data

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
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
import androidx.core.content.FileProvider
import com.example.fvkli.models.Track
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.util.Collections
import java.util.UUID

data class DownloadState(
    val isLoading: Boolean = false,
    val statusText: String = "",
    val progress: Float? = null,
    val currentIndex: Int = 0,
    val downloadedCount: Int = 0,
    val totalCount: Int = 0,
    val activeUrl: String = "",
    val lastRolledBackIds: List<String> = emptyList(),
    val libraryUpdateTick: Int = 0
)

object AudioDownloader {
    private val audioExtensions = setOf("m4a", "mp3", "opus", "webm", "ogg", "aac", "wav", "flac", "mp4", "mkv")
    private val videoExtensions = setOf("mp4", "mov", "mkv", "avi", "3gp")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "image")
    private var isInitialized = false
    private var isUpdated = false

    @Volatile private var currentProcessId: String? = null
    @Volatile private var isCancelled = false

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private data class PlaylistEntry(
        val url: String,
        val title: String
    )

    fun clearStatus() {
        if (!_downloadState.value.isLoading) {
            _downloadState.update { it.copy(statusText = "", progress = null) }
        }
    }

    fun consumeRolledBackIds() {
        _downloadState.update { it.copy(lastRolledBackIds = emptyList()) }
    }

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

    private fun readMetaJson(dir: File, id: String): JSONObject {
        val metaFile = File(dir, "$id.json")
        if (!metaFile.exists()) return JSONObject()
        return runCatching { JSONObject(metaFile.readText()) }.getOrElse { JSONObject() }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun saveTrackMeta(
        dir: File,
        id: String,
        artist: String,
        title: String,
        playlistId: String? = null,
        sourceUrl: String? = null,
        coverFileName: String? = null
    ) {
        val meta = JSONObject().apply {
            put("artist", artist)
            put("title", title)
            playlistId?.takeIf { it.isNotBlank() }?.let { put("playlistId", it) }
            sourceUrl?.takeIf { it.isNotBlank() }?.let { put("sourceUrl", it) }
            coverFileName?.takeIf { it.isNotBlank() }?.let { put("coverFileName", it) }
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

    private fun deleteTrackCovers(dir: File, id: String, excludeFileName: String? = null) {
        dir.listFiles()?.forEach { file ->
            if (file.name == excludeFileName) return@forEach
            val isTrackImage = file.extension.lowercase() in imageExtensions &&
                    (file.nameWithoutExtension == id || file.name.startsWith("$id."))
            if (isTrackImage) {
                file.delete()
            }
        }
    }

    private fun saveTrackCoverFromUri(context: Context, dir: File, id: String, uri: Uri): File? {
        return runCatching {
            val dest = File(dir, "$id.${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            dest
        }.getOrNull()
    }

    fun updateTrackMeta(
        context: Context,
        id: String,
        newArtist: String,
        newTitle: String,
        newCoverUri: Uri? = null,
        removeCover: Boolean = false
    ) {
        val dir = getTracksDir(context)
        val existing = readMetaJson(dir, id)
        val finalCoverFileName = when {
            removeCover -> {
                deleteTrackCovers(dir, id)
                null
            }
            newCoverUri != null -> {
                val saved = saveTrackCoverFromUri(context, dir, id, newCoverUri)
                if (saved != null) {
                    deleteTrackCovers(dir, id, excludeFileName = saved.name)
                    saved.name
                } else {
                    existing.optNullableString("coverFileName")
                }
            }
            else -> existing.optNullableString("coverFileName")
        }

        saveTrackMeta(
            dir = dir,
            id = id,
            artist = newArtist.trim(),
            title = newTitle.trim(),
            playlistId = existing.optNullableString("playlistId"),
            sourceUrl = existing.optNullableString("sourceUrl"),
            coverFileName = finalCoverFileName
        )
    }

    fun assignTracksToPlaylist(
        context: Context,
        trackIds: Collection<String>,
        playlistId: String?
    ) {
        if (trackIds.isEmpty()) return
        val dir = getTracksDir(context)
        val cleanPlaylistId = playlistId?.takeIf { it.isNotBlank() }

        trackIds.forEach { id ->
            val json = readMetaJson(dir, id)
            if (!json.has("title")) json.put("title", id)
            if (!json.has("artist")) json.put("artist", "Unknown")

            if (cleanPlaylistId != null) {
                json.put("playlistId", cleanPlaylistId)
            } else {
                json.remove("playlistId")
            }
            File(dir, "$id.json").writeText(json.toString())
        }
    }

    /**
     * Расформировывает плейлист: снимает playlistId со всех принадлежащих ему треков.
     */
    fun disbandPlaylistTracks(context: Context, playlistId: String) {
        val dir = getTracksDir(context)
        dir.listFiles()?.forEach { file ->
            if (file.extension.equals("json", ignoreCase = true)) {
                runCatching {
                    val json = JSONObject(file.readText())
                    if (json.optNullableString("playlistId") == playlistId) {
                        json.remove("playlistId")
                        file.writeText(json.toString())
                    }
                }
            }
        }
    }

    fun deleteTrack(context: Context, track: Track) {
        getTracksDir(context).listFiles()?.forEach { file ->
            if (file.name == track.file.name || file.name.startsWith("${track.id}.")) {
                file.delete()
            }
        }
    }

    fun shareTracks(context: Context, tracks: List<Track>) {
        if (tracks.isEmpty()) return

        val shareDir = File(context.cacheDir, "shared_tracks").apply {
            if (exists()) listFiles()?.forEach { it.delete() }
            mkdirs()
        }

        val usedNames = mutableSetOf<String>()
        val uris = ArrayList<Uri>()

        for (track in tracks) {
            if (!track.file.exists()) continue

            val ext = track.file.extension.lowercase().ifBlank { "m4a" }
            val rawName = if (track.artist.isNotBlank() && track.artist != "Unknown") {
                "${track.artist} - ${track.title}"
            } else {
                track.title
            }

            val baseName = rawName
                .replace(Regex("""[\\/:*?"<>|]"""), "_")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .ifBlank { track.id }

            var candidate = baseName
            var counter = 2
            while (!usedNames.add(candidate.lowercase())) {
                candidate = "$baseName ($counter)"
                counter++
            }

            val tempFile = File(shareDir, "$candidate.$ext")
            runCatching {
                track.file.copyTo(tempFile, overwrite = true)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                uris.add(uri)
            }
        }

        if (uris.isEmpty()) return

        val clipData = ClipData.newUri(context.contentResolver, "Audio", uris.first()).apply {
            for (i in 1 until uris.size) {
                addItem(ClipData.Item(uris[i]))
            }
        }

        val sendIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }.apply {
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserTitle = if (uris.size == 1) "Share track" else "Share ${uris.size} tracks"
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
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
                var title = id
                var artist = "Unknown"
                var playlistId: String? = null
                var sourceUrl: String? = null
                var coverFileName: String? = null

                if (metaFile.exists()) {
                    runCatching {
                        val json = JSONObject(metaFile.readText())
                        title = json.optString("title", id).ifBlank { id }
                        artist = json.optString("artist", "Unknown").ifBlank { "Unknown" }
                        playlistId = json.optNullableString("playlistId")
                        sourceUrl = json.optNullableString("sourceUrl")
                        coverFileName = json.optNullableString("coverFileName")
                    }
                }
                val coverFile = coverFileName?.let { File(dir, it) }?.takeIf { it.exists() }
                    ?: allFiles.firstOrNull {
                        it.nameWithoutExtension == id && it.extension.lowercase() in imageExtensions
                    }

                Track(
                    id = id,
                    title = title,
                    artist = artist,
                    file = file,
                    coverFile = coverFile,
                    playlistId = playlistId,
                    sourceUrl = sourceUrl
                )
            }
    }

    suspend fun downloadAudio(
        context: Context,
        url: String,
        targetPlaylistId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        isCancelled = false
        val cleanUrl = url.trim()
        val sessionTrackIds = Collections.synchronizedList(mutableListOf<String>())

        _downloadState.update {
            it.copy(
                isLoading = true,
                statusText = "Fetching info...",
                progress = null,
                downloadedCount = 0,
                totalCount = 0,
                activeUrl = cleanUrl,
                lastRolledBackIds = emptyList()
            )
        }

        try {
            ensureInitialized(context) { msg ->
                _downloadState.update { it.copy(statusText = msg) }
            }
            if (isCancelled) throw CancellationException("Cancelled")
            currentCoroutineContext().ensureActive()

            _downloadState.update { it.copy(statusText = "Fetching info...") }
            val entries = extractPlaylistEntries(cleanUrl)

            if (isCancelled) throw CancellationException("Cancelled")
            currentCoroutineContext().ensureActive()

            if (entries.isEmpty()) {
                val msg = "No tracks found"
                _downloadState.update {
                    it.copy(isLoading = false, statusText = "Error: $msg", progress = null, activeUrl = "")
                }
                return@withContext Result.failure(Exception(msg))
            }

            val existingUrlKeys = getSavedTracks(context)
                .mapNotNull { LibraryBackupManager.normalizeSourceUrl(it.sourceUrl) }
                .toMutableSet()

            val isPlaylist = entries.size > 1
            var downloadedCount = 0
            var duplicateCount = 0
            var failedCount = 0
            var lastSavedFile: File? = null

            _downloadState.update {
                it.copy(downloadedCount = 0, totalCount = entries.size)
            }

            for ((index, entry) in entries.withIndex()) {
                if (isCancelled) throw CancellationException("Cancelled")
                currentCoroutineContext().ensureActive()

                val normKey = LibraryBackupManager.normalizeSourceUrl(entry.url)
                if (normKey != null && normKey in existingUrlKeys) {
                    duplicateCount++
                    continue
                }

                val statusPrefix = if (isPlaylist) {
                    "${index + 1}/${entries.size} • ${entry.title}"
                } else {
                    "Downloading..."
                }

                _downloadState.update {
                    it.copy(
                        currentIndex = index + 1,
                        progress = 0f,
                        statusText = statusPrefix
                    )
                }

                val itemResult = downloadSingleItem(
                    context = context,
                    itemUrl = entry.url,
                    fallbackTitle = entry.title,
                    targetPlaylistId = targetPlaylistId,
                    onProgress = { percent ->
                        if (!isCancelled) {
                            _downloadState.update {
                                it.copy(progress = (percent / 100f).coerceIn(0f, 1f))
                            }
                        }
                    }
                )

                if (isCancelled) throw CancellationException("Cancelled")

                itemResult.onSuccess { savedFile ->
                    if (normKey != null) existingUrlKeys.add(normKey)
                    sessionTrackIds.add(savedFile.nameWithoutExtension)
                    downloadedCount++
                    lastSavedFile = savedFile
                    _downloadState.update {
                        it.copy(
                            downloadedCount = downloadedCount,
                            libraryUpdateTick = it.libraryUpdateTick + 1
                        )
                    }
                }.onFailure { err ->
                    if (isCancelled || err is CancellationException) {
                        throw CancellationException("Cancelled")
                    }
                    if (!isPlaylist) {
                        throw err
                    }
                    failedCount++
                }
            }

            if (downloadedCount == 0 && duplicateCount > 0 && failedCount == 0) {
                val summary = if (isPlaylist) "All tracks already in library" else "Already in library"
                _downloadState.update {
                    it.copy(
                        isLoading = false,
                        statusText = summary,
                        progress = null,
                        downloadedCount = 0,
                        totalCount = 0,
                        activeUrl = ""
                    )
                }
                Result.success(summary)
            } else if (downloadedCount == 0) {
                val msg = "Could not download tracks"
                _downloadState.update {
                    it.copy(
                        isLoading = false,
                        statusText = "Error: $msg",
                        progress = null,
                        downloadedCount = 0,
                        totalCount = 0,
                        activeUrl = ""
                    )
                }
                Result.failure(Exception(msg))
            } else {
                val summary = if (!isPlaylist && lastSavedFile != null) {
                    val sizeMb = String.format("%.2f", lastSavedFile!!.length() / (1024.0 * 1024.0))
                    "Ready! ($sizeMb MB)"
                } else {
                    val extras = buildList {
                        if (duplicateCount > 0) add("$duplicateCount already saved")
                        if (failedCount > 0) add("$failedCount failed")
                    }
                    if (extras.isNotEmpty()) {
                        "Added $downloadedCount tracks (${extras.joinToString(", ")})"
                    } else {
                        "Added $downloadedCount tracks"
                    }
                }

                _downloadState.update {
                    it.copy(
                        isLoading = false,
                        statusText = summary,
                        progress = null,
                        downloadedCount = 0,
                        totalCount = 0,
                        activeUrl = "",
                        libraryUpdateTick = it.libraryUpdateTick + 1
                    )
                }
                Result.success(summary)
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val idsToRemove = sessionTrackIds.toList()
                rollbackSessionTracks(context, idsToRemove)
                _downloadState.update {
                    it.copy(
                        isLoading = false,
                        statusText = "Cancelled",
                        progress = null,
                        downloadedCount = 0,
                        totalCount = 0,
                        activeUrl = "",
                        lastRolledBackIds = idsToRemove,
                        libraryUpdateTick = it.libraryUpdateTick + 1
                    )
                }
            }
            Result.failure(Exception("Cancelled"))
        } catch (e: Exception) {
            if (isCancelled) {
                withContext(NonCancellable) {
                    val idsToRemove = sessionTrackIds.toList()
                    rollbackSessionTracks(context, idsToRemove)
                    _downloadState.update {
                        it.copy(
                            isLoading = false,
                            statusText = "Cancelled",
                            progress = null,
                            downloadedCount = 0,
                            totalCount = 0,
                            activeUrl = "",
                            lastRolledBackIds = idsToRemove,
                            libraryUpdateTick = it.libraryUpdateTick + 1
                        )
                    }
                }
                Result.failure(Exception("Cancelled"))
            } else {
                _downloadState.update {
                    it.copy(
                        isLoading = false,
                        statusText = "Error: ${e.localizedMessage}",
                        progress = null,
                        downloadedCount = 0,
                        totalCount = 0,
                        activeUrl = ""
                    )
                }
                Result.failure(e)
            }
        } finally {
            currentProcessId = null
        }
    }

    suspend fun downloadImportBatch(
        context: Context,
        items: List<ImportTrackRequest>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success("Nothing to import")

        isCancelled = false
        val sessionTrackIds = Collections.synchronizedList(mutableListOf<String>())
        val createdPlaylistIds = Collections.synchronizedSet(mutableSetOf<String>())

        _downloadState.update {
            it.copy(
                isLoading = true,
                statusText = "Preparing import...",
                progress = null,
                currentIndex = 0,
                downloadedCount = 0,
                totalCount = items.size,
                activeUrl = "JSON Backup",
                lastRolledBackIds = emptyList()
            )
        }

        try {
            ensureInitialized(context) { msg ->
                _downloadState.update { it.copy(statusText = msg) }
            }
            if (isCancelled) throw CancellationException("Cancelled")
            currentCoroutineContext().ensureActive()

            val existingUrlKeys = getSavedTracks(context)
                .mapNotNull { LibraryBackupManager.normalizeSourceUrl(it.sourceUrl) }
                .toMutableSet()

            var downloadedCount = 0
            var duplicateCount = 0
            var failedCount = 0

            for ((index, item) in items.withIndex()) {
                if (isCancelled) throw CancellationException("Cancelled")
                currentCoroutineContext().ensureActive()

                val normKey = LibraryBackupManager.normalizeSourceUrl(item.url)
                if (normKey != null && normKey in existingUrlKeys) {
                    duplicateCount++
                    continue
                }

                val displayTitle = item.title.ifBlank { "Track ${index + 1}" }
                _downloadState.update {
                    it.copy(
                        currentIndex = index + 1,
                        progress = 0f,
                        statusText = "${index + 1}/${items.size} • $displayTitle"
                    )
                }

                val existingPlaylistId = item.playlistName?.let { plName ->
                    PlaylistStorage.loadPlaylists(context)
                        .firstOrNull { it.name.equals(plName, ignoreCase = true) }
                        ?.id
                }

                val itemResult = downloadSingleItem(
                    context = context,
                    itemUrl = item.url,
                    fallbackTitle = displayTitle,
                    targetPlaylistId = existingPlaylistId,
                    overrideArtist = item.artist,
                    overrideTitle = item.title,
                    onProgress = { percent ->
                        if (!isCancelled) {
                            _downloadState.update {
                                it.copy(progress = (percent / 100f).coerceIn(0f, 1f))
                            }
                        }
                    }
                )

                if (isCancelled) throw CancellationException("Cancelled")

                itemResult.onSuccess { savedFile ->
                    val trackId = savedFile.nameWithoutExtension
                    sessionTrackIds.add(trackId)
                    if (normKey != null) existingUrlKeys.add(normKey)

                    val targetName = item.playlistName
                    if (targetName != null && existingPlaylistId == null) {
                        val latestPlaylists = PlaylistStorage.loadPlaylists(context)
                        val match = latestPlaylists.firstOrNull { it.name.equals(targetName, ignoreCase = true) }
                        val resolvedId = if (match != null) {
                            match.id
                        } else {
                            val (created, _) = PlaylistStorage.createPlaylist(
                                context = context,
                                existing = latestPlaylists,
                                name = targetName,
                                coverUri = null
                            )
                            createdPlaylistIds.add(created.id)
                            created.id
                        }
                        assignTracksToPlaylist(context, listOf(trackId), resolvedId)
                    }

                    downloadedCount++
                    _downloadState.update {
                        it.copy(
                            downloadedCount = downloadedCount,
                            libraryUpdateTick = it.libraryUpdateTick + 1
                        )
                    }
                }.onFailure { err ->
                    if (isCancelled || err is CancellationException) {
                        throw CancellationException("Cancelled")
                    }
                    failedCount++
                }
            }

            val summary = when {
                downloadedCount == 0 && duplicateCount > 0 && failedCount == 0 ->
                    "All tracks already in library"
                downloadedCount == 0 ->
                    "Could not import tracks ($failedCount failed)"
                else -> {
                    val extras = buildList {
                        if (duplicateCount > 0) add("$duplicateCount skipped")
                        if (failedCount > 0) add("$failedCount failed")
                    }
                    if (extras.isNotEmpty()) {
                        "Imported $downloadedCount tracks (${extras.joinToString(", ")})"
                    } else {
                        "Imported $downloadedCount tracks"
                    }
                }
            }

            _downloadState.update {
                it.copy(
                    isLoading = false,
                    statusText = summary,
                    progress = null,
                    downloadedCount = 0,
                    totalCount = 0,
                    activeUrl = "",
                    libraryUpdateTick = it.libraryUpdateTick + 1
                )
            }
            if (downloadedCount == 0 && failedCount > 0) {
                Result.failure(Exception(summary))
            } else {
                Result.success(summary)
            }
        } catch (e: Exception) {
            withContext(NonCancellable) {
                if (isCancelled || e is CancellationException) {
                    val idsToRemove = sessionTrackIds.toList()
                    rollbackSessionTracks(context, idsToRemove)
                    cleanupEmptyCreatedPlaylists(context, createdPlaylistIds)
                    _downloadState.update {
                        it.copy(
                            isLoading = false,
                            statusText = "Import cancelled",
                            progress = null,
                            downloadedCount = 0,
                            totalCount = 0,
                            activeUrl = "",
                            lastRolledBackIds = idsToRemove,
                            libraryUpdateTick = it.libraryUpdateTick + 1
                        )
                    }
                } else {
                    _downloadState.update {
                        it.copy(
                            isLoading = false,
                            statusText = "Error: ${e.localizedMessage}",
                            progress = null,
                            downloadedCount = 0,
                            totalCount = 0,
                            activeUrl = ""
                        )
                    }
                }
            }
            Result.failure(e)
        } finally {
            currentProcessId = null
        }
    }

    private fun cleanupEmptyCreatedPlaylists(context: Context, createdIds: Set<String>) {
        if (createdIds.isEmpty()) return
        val usedIds = getSavedTracks(context).mapNotNull { it.playlistId }.toSet()
        val latest = PlaylistStorage.loadPlaylists(context)
        val cleaned = latest.filterNot { it.id in createdIds && it.id !in usedIds }
        if (cleaned.size != latest.size) {
            PlaylistStorage.savePlaylists(context, cleaned)
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
        targetPlaylistId: String? = null,
        overrideArtist: String? = null,
        overrideTitle: String? = null,
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
            var resolvedSourceUrl = itemUrl
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
                    resolvedSourceUrl = infoJson.optString("webpage_url").ifBlank { itemUrl }
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

            val (parsedArtist, parsedTitle) = parseArtistAndTitle(rawTitle, uploader)
            val artist = overrideArtist?.trim()?.takeIf { it.isNotBlank() } ?: parsedArtist
            val title = overrideTitle?.trim()?.takeIf { it.isNotBlank() } ?: parsedTitle
            saveTrackMeta(
                dir = audioDir,
                id = trackId,
                artist = artist,
                title = title,
                playlistId = targetPlaylistId,
                sourceUrl = resolvedSourceUrl
            )

            Result.success(finalAudioFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            currentProcessId = null
            tempDir.deleteRecursively()
        }
    }

    suspend fun importLocalFile(
        context: Context,
        uri: Uri,
        targetPlaylistId: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
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
            saveTrackMeta(
                dir = audioDir,
                id = trackId,
                artist = artist,
                title = title,
                playlistId = targetPlaylistId
            )

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