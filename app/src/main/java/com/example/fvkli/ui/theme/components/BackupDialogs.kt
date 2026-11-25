package com.example.fvkli.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fvkli.data.ExportPreview
import com.example.fvkli.data.ExportPreviewPlaylist
import com.example.fvkli.data.ImportPreview

@Composable
fun ExportLibraryDialog(
    preview: ExportPreview,
    onDismiss: () -> Unit,
    onSaveToFile: (selectedPlaylists: List<ExportPreviewPlaylist>, includeStandalone: Boolean) -> Unit,
    onShare: (selectedPlaylists: List<ExportPreviewPlaylist>, includeStandalone: Boolean) -> Unit
) {
    val selectablePlaylistIds = remember(preview) {
        preview.playlists.filter { it.exportableTracks.isNotEmpty() }.map { it.playlist.id }.toSet()
    }
    val hasStandaloneSelectable = preview.standaloneExportable.isNotEmpty()

    var selectedPlaylistIds by remember(preview) { mutableStateOf(selectablePlaylistIds) }
    var includeStandalone by remember(preview) { mutableStateOf(hasStandaloneSelectable) }

    val allSelected = selectedPlaylistIds.size == selectablePlaylistIds.size &&
            (!hasStandaloneSelectable || includeStandalone) &&
            (selectablePlaylistIds.isNotEmpty() || hasStandaloneSelectable)

    val selectedTracksCount = preview.playlists
        .filter { it.playlist.id in selectedPlaylistIds }
        .sumOf { it.exportableTracks.size } +
            if (includeStandalone) preview.standaloneExportable.size else 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFD0D0D0),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedTracksCount > 0) "Export ($selectedTracksCount)" else "Export library",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (preview.totalExportable > 0) {
                    Text(
                        text = if (allSelected) "Deselect all" else "Select all",
                        color = Color(0xFF1DB954),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (allSelected) {
                                    selectedPlaylistIds = emptySet()
                                    includeStandalone = false
                                } else {
                                    selectedPlaylistIds = selectablePlaylistIds
                                    includeStandalone = hasStandaloneSelectable
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (preview.totalExportable == 0) {
                    Text(
                        text = "No tracks with source links found. Local files added from device cannot be exported to JSON.",
                        color = Color(0xFFB3B3B3),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    if (preview.playlists.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 230.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            preview.playlists.forEach { item ->
                                val enabled = item.exportableTracks.isNotEmpty()
                                val checked = item.playlist.id in selectedPlaylistIds
                                val trackWord = if (item.exportableTracks.size == 1) "track" else "tracks"
                                val subtitle = when {
                                    !enabled -> "Local files only (${item.localSkippedCount})"
                                    item.localSkippedCount > 0 ->
                                        "${item.exportableTracks.size} $trackWord • ${item.localSkippedCount} local skipped"
                                    else ->
                                        "${item.exportableTracks.size} $trackWord"
                                }

                                BackupSelectableRow(
                                    icon = Icons.Rounded.QueueMusic,
                                    title = item.playlist.name,
                                    subtitle = subtitle,
                                    checked = checked && enabled,
                                    enabled = enabled,
                                    onToggle = {
                                        selectedPlaylistIds = if (checked) {
                                            selectedPlaylistIds - item.playlist.id
                                        } else {
                                            selectedPlaylistIds + item.playlist.id
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (preview.standaloneExportable.isNotEmpty() || preview.standaloneLocalSkipped > 0) {
                        if (preview.playlists.isNotEmpty()) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        val standaloneWord = if (preview.standaloneExportable.size == 1) "track" else "tracks"
                        val subtitle = when {
                            !hasStandaloneSelectable ->
                                "Local files only (${preview.standaloneLocalSkipped})"
                            preview.standaloneLocalSkipped > 0 ->
                                "${preview.standaloneExportable.size} $standaloneWord • ${preview.standaloneLocalSkipped} local skipped"
                            else ->
                                "${preview.standaloneExportable.size} $standaloneWord"
                        }
                        BackupSelectableRow(
                            icon = Icons.Rounded.MusicNote,
                            title = "Tracks outside playlists",
                            subtitle = subtitle,
                            checked = includeStandalone && hasStandaloneSelectable,
                            enabled = hasStandaloneSelectable,
                            onToggle = { includeStandalone = !includeStandalone }
                        )
                    }

                    if (preview.totalLocalSkipped > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val fileWord = if (preview.totalLocalSkipped == 1) "file" else "files"
                        BackupInfoNote(
                            text = "${preview.totalLocalSkipped} local $fileWord without links won't be included in JSON."
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB3B3B3))
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(
                    enabled = selectedTracksCount > 0,
                    onClick = {
                        val chosenPlaylists = preview.playlists.filter { it.playlist.id in selectedPlaylistIds }
                        onSaveToFile(chosenPlaylists, includeStandalone)
                    }
                ) {
                    Text(
                        text = "Save",
                        color = if (selectedTracksCount > 0) Color.White else Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(
                    enabled = selectedTracksCount > 0,
                    onClick = {
                        val chosenPlaylists = preview.playlists.filter { it.playlist.id in selectedPlaylistIds }
                        onShare(chosenPlaylists, includeStandalone)
                    }
                ) {
                    Text(
                        text = "Share",
                        color = if (selectedTracksCount > 0) Color(0xFF1DB954) else Color(0xFF666666),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    )
}

@Composable
fun ImportLibraryDialog(
    preview: ImportPreview,
    onDismiss: () -> Unit,
    onConfirm: (selectedPlaylistNames: Set<String>, includeStandalone: Boolean) -> Unit
) {
    val selectablePlaylistNames = remember(preview) {
        preview.playlists.filter { it.newTracks.isNotEmpty() }.map { it.name }.toSet()
    }
    val hasStandaloneSelectable = preview.standaloneNewTracks.isNotEmpty()

    var selectedPlaylistNames by remember(preview) { mutableStateOf(selectablePlaylistNames) }
    var includeStandalone by remember(preview) { mutableStateOf(hasStandaloneSelectable) }

    val allSelected = selectedPlaylistNames.size == selectablePlaylistNames.size &&
            (!hasStandaloneSelectable || includeStandalone) &&
            (selectablePlaylistNames.isNotEmpty() || hasStandaloneSelectable)

    val selectedCount = preview.playlists
        .filter { it.name in selectedPlaylistNames }
        .sumOf { it.newTracks.size } +
            if (includeStandalone) preview.standaloneNewTracks.size else 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFD0D0D0),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Import library",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (preview.totalNewTracks > 0) {
                    Text(
                        text = if (allSelected) "Deselect all" else "Select all",
                        color = Color(0xFF1DB954),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (allSelected) {
                                    selectedPlaylistNames = emptySet()
                                    includeStandalone = false
                                } else {
                                    selectedPlaylistNames = selectablePlaylistNames
                                    includeStandalone = hasStandaloneSelectable
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (preview.playlists.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 230.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        preview.playlists.forEach { pl ->
                            val enabled = pl.newTracks.isNotEmpty()
                            val checked = pl.name in selectedPlaylistNames
                            val subtitle = buildString {
                                if (!enabled) {
                                    append("All ${pl.totalCount} already in library")
                                } else {
                                    append("${pl.newTracks.size} new")
                                    if (pl.duplicateCount > 0) {
                                        append(" • ${pl.duplicateCount} already saved")
                                    }
                                    if (pl.mergesIntoExisting) {
                                        append(" • merges")
                                    }
                                }
                            }

                            BackupSelectableRow(
                                icon = Icons.Rounded.QueueMusic,
                                title = pl.name,
                                subtitle = subtitle,
                                checked = checked && enabled,
                                enabled = enabled,
                                onToggle = {
                                    selectedPlaylistNames = if (checked) {
                                        selectedPlaylistNames - pl.name
                                    } else {
                                        selectedPlaylistNames + pl.name
                                    }
                                }
                            )
                        }
                    }
                }

                if (preview.standaloneTotal > 0) {
                    if (preview.playlists.isNotEmpty()) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    val subtitle = when {
                        !hasStandaloneSelectable ->
                            "All ${preview.standaloneTotal} already in library"
                        preview.standaloneDuplicates > 0 ->
                            "${preview.standaloneNewTracks.size} new • ${preview.standaloneDuplicates} already saved"
                        else ->
                            "${preview.standaloneNewTracks.size} new"
                    }

                    BackupSelectableRow(
                        icon = Icons.Rounded.MusicNote,
                        title = "Tracks outside playlists",
                        subtitle = subtitle,
                        checked = includeStandalone && hasStandaloneSelectable,
                        enabled = hasStandaloneSelectable,
                        onToggle = { includeStandalone = !includeStandalone }
                    )
                }

                if (preview.totalNewTracks == 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    BackupInfoNote(text = "Every track in this backup is already in your library.")
                } else if (preview.totalDuplicates > 0 || preview.invalidEntriesSkipped > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val note = buildList {
                        if (preview.totalDuplicates > 0) add("${preview.totalDuplicates} duplicates skipped")
                        if (preview.invalidEntriesSkipped > 0) add("${preview.invalidEntriesSkipped} invalid entries ignored")
                    }.joinToString(" • ")
                    BackupInfoNote(text = note)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB3B3B3))
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedCount > 0,
                onClick = { onConfirm(selectedPlaylistNames, includeStandalone) }
            ) {
                Text(
                    text = if (selectedCount > 0) "Import ($selectedCount)" else "Import",
                    color = if (selectedCount > 0) Color(0xFF1DB954) else Color(0xFF666666),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun BackupSelectableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) Color(0xFF252E28) else Color(0xFF262626),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onToggle) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f * contentAlpha),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = contentAlpha),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF9E9E9E).copy(alpha = contentAlpha),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !enabled -> Color.Transparent
                            checked -> Color(0xFF1DB954)
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            !enabled -> Color.White.copy(alpha = 0.2f)
                            checked -> Color(0xFF1DB954)
                            else -> Color.White.copy(alpha = 0.45f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked && enabled) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupInfoNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF252525))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = Color(0xFF9E9E9E),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color(0xFFB3B3B3),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}