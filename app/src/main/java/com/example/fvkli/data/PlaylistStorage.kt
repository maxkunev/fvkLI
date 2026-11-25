package com.example.fvkli.data

import android.content.Context
import android.net.Uri
import com.example.fvkli.models.Playlist
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object PlaylistStorage {

    private const val PLAYLISTS_FILE = "playlists.json"
    private const val COVERS_DIR = "playlist_covers"

    private fun playlistsFile(context: Context): File =
        File(context.filesDir, PLAYLISTS_FILE)

    private fun coversDir(context: Context): File =
        File(context.filesDir, COVERS_DIR).apply {
            if (!exists()) mkdirs()
        }

    fun loadPlaylists(context: Context): List<Playlist> {
        val file = playlistsFile(context)
        if (!file.exists()) return emptyList()

        return runCatching {
            val content = file.readText()
            if (content.isBlank()) return emptyList()

            val array = JSONArray(content)
            val dir = coversDir(context)
            buildList {
                for (i in 0 until array.length()) {

                    val obj = array.getJSONObject(i)
                    val hideAutoCover = obj.optBoolean("hideAutoCover", false)
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val coverName = obj.optString("coverFileName").takeIf { it.isNotBlank() }
                    val coverFile = coverName?.let { File(dir, it) }?.takeIf { it.exists() }
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                    add(
                        Playlist(
                            id = id,
                            name = name,
                            coverFile = coverFile,
                            createdAt = createdAt,
                            hideAutoCover = hideAutoCover
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject().apply {
                if (playlist.hideAutoCover) put("hideAutoCover", true)
                put("id", playlist.id)
                put("name", playlist.name)
                put("createdAt", playlist.createdAt)
                playlist.coverFile?.takeIf { it.exists() }?.let {
                    put("coverFileName", it.name)
                }
            }
            array.put(obj)
        }

        val target = playlistsFile(context)
        val tmp = File(context.filesDir, "$PLAYLISTS_FILE.tmp")
        runCatching {
            tmp.writeText(array.toString(2))
            if (!tmp.renameTo(target)) {
                target.writeText(array.toString(2))
                tmp.delete()
            }
        }
    }

    fun isNameTaken(
        playlists: List<Playlist>,
        name: String,
        excludeId: String? = null
    ): Boolean {
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        return playlists.any {
            it.id != excludeId && it.name.trim().equals(normalized, ignoreCase = true)
        }
    }

    fun createPlaylist(
        hideAutoCover: Boolean = false,
        context: Context,
        existing: List<Playlist>,
        name: String,
        coverUri: Uri?
    ): Pair<Playlist, List<Playlist>> {
        val id = UUID.randomUUID().toString()
        val coverFile = coverUri?.let { saveCoverFromUri(context, id, it) }
        val playlist = Playlist(
            id = id,
            name = name.trim(),
            coverFile = coverFile,
            createdAt = System.currentTimeMillis(),
            hideAutoCover = hideAutoCover && coverFile == null
        )
        val updated = existing + playlist
        savePlaylists(context, updated)
        return playlist to updated
    }

    fun updatePlaylist(
        context: Context,
        existing: List<Playlist>,
        playlistId: String,
        newName: String,
        newCoverUri: Uri? = null,
        removeCover: Boolean = false,
        restoreAutoCover: Boolean = false
    ): List<Playlist> {
        val updated = existing.map { playlist ->
            if (playlist.id != playlistId) {
                playlist
            } else {
                val (updatedCover, updatedHideAuto) = when {
                    removeCover -> {
                        playlist.coverFile?.delete()
                        null to true
                    }
                    restoreAutoCover -> {
                        playlist.coverFile?.delete()
                        null to false
                    }
                    newCoverUri != null -> {
                        val saved = saveCoverFromUri(context, playlist.id, newCoverUri)
                        if (saved != null) {
                            playlist.coverFile?.delete()
                            saved to false
                        } else {
                            playlist.coverFile to playlist.hideAutoCover
                        }
                    }
                    else -> playlist.coverFile to playlist.hideAutoCover
                }
                playlist.copy(
                    name = newName.trim(),
                    coverFile = updatedCover,
                    hideAutoCover = updatedHideAuto
                )
            }
        }
        savePlaylists(context, updated)
        return updated
    }

    fun removePlaylistEntries(
        context: Context,
        existing: List<Playlist>,
        playlistIds: Set<String>
    ): List<Playlist> {
        if (playlistIds.isEmpty()) return existing
        existing
            .filter { it.id in playlistIds }
            .forEach { it.coverFile?.delete() }

        val updated = existing.filterNot { it.id in playlistIds }
        savePlaylists(context, updated)
        return updated
    }

    fun pruneEmptyPlaylists(
        context: Context,
        playlists: List<Playlist>,
        nonEmptyPlaylistIds: Set<String>
    ): List<Playlist> {
        val emptyIds = playlists
            .filter { it.id !in nonEmptyPlaylistIds }
            .map { it.id }
            .toSet()

        if (emptyIds.isEmpty()) return playlists
        return removePlaylistEntries(context, playlists, emptyIds)
    }

    private fun saveCoverFromUri(context: Context, playlistId: String, uri: Uri): File? {
        return runCatching {
            val dest = File(coversDir(context), "${playlistId}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            dest
        }.getOrNull()
    }
}