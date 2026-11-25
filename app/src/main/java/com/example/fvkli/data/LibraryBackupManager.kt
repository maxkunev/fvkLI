package com.example.fvkli.data

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.fvkli.models.Playlist
import com.example.fvkli.models.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupTrackItem(
    val url: String,
    val artist: String,
    val title: String
)

data class ExportPreviewPlaylist(
    val playlist: Playlist,
    val exportableTracks: List<Track>,
    val localSkippedCount: Int
)

data class ExportPreview(
    val playlists: List<ExportPreviewPlaylist>,
    val standaloneExportable: List<Track>,
    val standaloneLocalSkipped: Int
) {
    val totalExportable: Int
        get() = playlists.sumOf { it.exportableTracks.size } + standaloneExportable.size

    val totalLocalSkipped: Int
        get() = playlists.sumOf { it.localSkippedCount } + standaloneLocalSkipped
}

data class ImportPreviewPlaylist(
    val name: String,
    val totalCount: Int,
    val duplicateCount: Int,
    val newTracks: List<BackupTrackItem>,
    val mergesIntoExisting: Boolean
)

data class ImportPreview(
    val playlists: List<ImportPreviewPlaylist>,
    val standaloneTotal: Int,
    val standaloneDuplicates: Int,
    val standaloneNewTracks: List<BackupTrackItem>,
    val invalidEntriesSkipped: Int
) {
    val totalNewTracks: Int
        get() = playlists.sumOf { it.newTracks.size } + standaloneNewTracks.size

    val totalDuplicates: Int
        get() = playlists.sumOf { it.duplicateCount } + standaloneDuplicates
}

data class ImportTrackRequest(
    val url: String,
    val artist: String,
    val title: String,
    val playlistName: String?
)

object LibraryBackupManager {
    private const val FORMAT_VERSION = 1
    private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024 // 5 MB

    private val trackingParams = setOf(
        "si", "feature", "pp", "t", "fbclid", "igshid",
        "is_from_webapp", "sender_device", "_r", "_t", "share_id", "list", "index"
    )

    fun normalizeSourceUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return trimmed.lowercase()
        val rawHost = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")?.removePrefix("m.")?.removePrefix("mobile.")
            ?: return trimmed.lowercase()

        // YouTube / YouTube Music / Shorts
        if (rawHost == "youtu.be") {
            val id = uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
            if (id != null) return "yt:$id"
        } else if (rawHost == "youtube.com" || rawHost == "music.youtube.com") {
            val vParam = uri.getQueryParameter("v")?.takeIf { it.isNotBlank() }
            if (vParam != null) return "yt:$vParam"

            val segments = uri.pathSegments
            if (segments.size >= 2 && segments[0] in setOf("shorts", "embed", "live", "v")) {
                val id = segments[1].takeIf { it.isNotBlank() }
                if (id != null) return "yt:$id"
            }
        }

        // TikTok
        if (rawHost.endsWith("tiktok.com")) {
            val segments = uri.pathSegments
            val mediaIdx = segments.indexOfFirst { it == "video" || it == "photo" }
            if (mediaIdx >= 0 && mediaIdx + 1 < segments.size) {
                val id = segments[mediaIdx + 1].takeIf { it.isNotBlank() }
                if (id != null) return "tt:$id"
            }
            val cleanPath = uri.path?.trimEnd('/').orEmpty()
            return "tt:$rawHost$cleanPath"
        }

        // Остальные ссылки: убираем трекинговые query-параметры и хвостовой слеш
        val cleanPath = uri.path?.trimEnd('/').orEmpty()
        val keptParams = runCatching {
            uri.queryParameterNames
                .filterNot { key ->
                    val lower = key.lowercase(Locale.ROOT)
                    lower in trackingParams || lower.startsWith("utm_")
                }
                .sorted()
                .mapNotNull { key ->
                    val value = uri.getQueryParameter(key) ?: return@mapNotNull null
                    "$key=$value"
                }
                .joinToString("&")
        }.getOrDefault("")

        return if (keptParams.isNotEmpty()) {
            "$rawHost$cleanPath?$keptParams"
        } else {
            "$rawHost$cleanPath"
        }
    }

    fun isValidHttpUrl(rawUrl: String?): Boolean {
        val trimmed = rawUrl?.trim().orEmpty()
        return trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
    }

    fun buildExportPreview(playlists: List<Playlist>, tracks: List<Track>): ExportPreview {
        val validPlaylistIds = playlists.map { it.id }.toSet()
        val tracksByPlaylist = tracks
            .filter { it.playlistId != null && it.playlistId in validPlaylistIds }
            .groupBy { it.playlistId!! }

        val previewPlaylists = playlists.map { playlist ->
            val plTracks = tracksByPlaylist[playlist.id].orEmpty()
            val exportable = plTracks.filter { isValidHttpUrl(it.sourceUrl) }
            ExportPreviewPlaylist(
                playlist = playlist,
                exportableTracks = exportable,
                localSkippedCount = plTracks.size - exportable.size
            )
        }

        val standaloneAll = tracks.filter { it.playlistId == null || it.playlistId !in validPlaylistIds }
        val standaloneExportable = standaloneAll.filter { isValidHttpUrl(it.sourceUrl) }

        return ExportPreview(
            playlists = previewPlaylists,
            standaloneExportable = standaloneExportable,
            standaloneLocalSkipped = standaloneAll.size - standaloneExportable.size
        )
    }

    fun buildBackupJson(
        selectedPlaylists: List<ExportPreviewPlaylist>,
        includeStandalone: Boolean,
        standaloneTracks: List<Track>
    ): String {
        val root = JSONObject().apply {
            put("app", "fvkLI")
            put("version", FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())

            val playlistsArray = JSONArray()
            selectedPlaylists.forEach { item ->
                if (item.exportableTracks.isEmpty()) return@forEach
                val tracksArray = JSONArray()
                item.exportableTracks.forEach { track ->
                    val url = track.sourceUrl?.trim() ?: return@forEach
                    tracksArray.put(
                        JSONObject().apply {
                            put("url", url)
                            put("artist", track.artist)
                            put("title", track.title)
                        }
                    )
                }
                playlistsArray.put(
                    JSONObject().apply {
                        put("name", item.playlist.name)
                        put("tracks", tracksArray)
                    }
                )
            }
            put("playlists", playlistsArray)

            val standaloneArray = JSONArray()
            if (includeStandalone) {
                standaloneTracks.forEach { track ->
                    val url = track.sourceUrl?.trim() ?: return@forEach
                    standaloneArray.put(
                        JSONObject().apply {
                            put("url", url)
                            put("artist", track.artist)
                            put("title", track.title)
                        }
                    )
                }
            }
            put("standaloneTracks", standaloneArray)
        }
        return root.toString(2)
    }

    fun defaultBackupFileName(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
        return "fvkLI_backup_$stamp.json"
    }

    fun writeBackupToUri(context: Context, uri: Uri, jsonContent: String): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jsonContent.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: error("Cannot open output stream")
            true
        }.onFailure {
            android.util.Log.e("LibraryBackup", "Failed to save backup", it)
        }.getOrDefault(false)
    }
    fun shareBackupJson(context: Context, jsonContent: String): Boolean {
        return runCatching {
            val shareDir = File(context.cacheDir, "shared_backups").apply {
                if (exists()) listFiles()?.forEach { it.delete() }
                mkdirs()
            }
            val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val outFile = File(shareDir, defaultBackupFileName())
            outFile.writeText(jsonContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "fvkLI library backup")
                clipData = ClipData.newUri(context.contentResolver, "Library Backup", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Export library JSON").apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    fun parseImportJson(
        context: Context,
        uri: Uri,
        existingPlaylists: List<Playlist>,
        existingTracks: List<Track>
    ): Result<ImportPreview> = runCatching {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            if (bytes.size > MAX_IMPORT_BYTES) {
                error("File is too large (max 5 MB)")
            }
            bytes
        } ?: error("Cannot read selected file")

        val text = rawBytes.toString(Charsets.UTF_8).trim()
        if (text.isBlank()) error("File is empty")

        val root = JSONObject(text)
        if (!root.has("playlists") && !root.has("standaloneTracks")) {
            error("Not a valid fvkLI backup file")
        }

        val existingUrlKeys = existingTracks
            .mapNotNull { normalizeSourceUrl(it.sourceUrl) }
            .toMutableSet()
        val existingPlaylistNamesLower = existingPlaylists
            .map { it.name.trim().lowercase(Locale.ROOT) }
            .toSet()

        var invalidEntries = 0
        val seenInFileKeys = mutableSetOf<String>()

        fun parseTrackArray(array: JSONArray?): Triple<Int, Int, List<BackupTrackItem>> {
            if (array == null) return Triple(0, 0, emptyList())
            var totalValid = 0
            var duplicates = 0
            val newItems = mutableListOf<BackupTrackItem>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj == null) {
                    invalidEntries++
                    continue
                }
                val url = obj.optString("url").trim()
                if (!isValidHttpUrl(url)) {
                    invalidEntries++
                    continue
                }
                val normKey = normalizeSourceUrl(url)
                if (normKey == null) {
                    invalidEntries++
                    continue
                }

                totalValid++
                if (normKey in existingUrlKeys || !seenInFileKeys.add(normKey)) {
                    duplicates++
                } else {
                    newItems.add(
                        BackupTrackItem(
                            url = url,
                            artist = obj.optString("artist").trim(),
                            title = obj.optString("title").trim()
                        )
                    )
                }
            }
            return Triple(totalValid, duplicates, newItems)
        }

        // Группируем плейлисты с одинаковым именем (на случай ручной правки JSON)
        val playlistMap = LinkedHashMap<String, ImportPreviewPlaylist>()
        val playlistsArray = root.optJSONArray("playlists")
        if (playlistsArray != null) {
            for (i in 0 until playlistsArray.length()) {
                val plObj = playlistsArray.optJSONObject(i)
                if (plObj == null) {
                    invalidEntries++
                    continue
                }
                val name = plObj.optString("name").trim()
                if (name.isBlank()) {
                    invalidEntries++
                    continue
                }
                val (total, dups, newTracks) = parseTrackArray(plObj.optJSONArray("tracks"))
                if (total == 0) continue

                val key = name.lowercase(Locale.ROOT)
                val prev = playlistMap[key]
                if (prev == null) {
                    playlistMap[key] = ImportPreviewPlaylist(
                        name = name,
                        totalCount = total,
                        duplicateCount = dups,
                        newTracks = newTracks,
                        mergesIntoExisting = key in existingPlaylistNamesLower
                    )
                } else {
                    playlistMap[key] = prev.copy(
                        totalCount = prev.totalCount + total,
                        duplicateCount = prev.duplicateCount + dups,
                        newTracks = prev.newTracks + newTracks
                    )
                }
            }
        }

        val (standaloneTotal, standaloneDups, standaloneNew) =
            parseTrackArray(root.optJSONArray("standaloneTracks"))

        if (playlistMap.isEmpty() && standaloneTotal == 0) {
            error("Backup contains no valid track links")
        }

        ImportPreview(
            playlists = playlistMap.values.toList(),
            standaloneTotal = standaloneTotal,
            standaloneDuplicates = standaloneDups,
            standaloneNewTracks = standaloneNew,
            invalidEntriesSkipped = invalidEntries
        )
    }

    fun buildImportQueue(
        preview: ImportPreview,
        selectedPlaylistNames: Set<String>,
        includeStandalone: Boolean
    ): List<ImportTrackRequest> {
        val queue = mutableListOf<ImportTrackRequest>()

        preview.playlists.forEach { pl ->
            if (pl.name in selectedPlaylistNames) {
                pl.newTracks.forEach { track ->
                    queue.add(
                        ImportTrackRequest(
                            url = track.url,
                            artist = track.artist,
                            title = track.title,
                            playlistName = pl.name
                        )
                    )
                }
            }
        }

        if (includeStandalone) {
            preview.standaloneNewTracks.forEach { track ->
                queue.add(
                    ImportTrackRequest(
                        url = track.url,
                        artist = track.artist,
                        title = track.title,
                        playlistName = null
                    )
                )
            }
        }

        return queue
    }
}