package com.example.fvkli.ui.theme.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fvkli.models.Track
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.PlaylistRemove
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
@Composable
fun TrackContextMenuOverlay(
    track: Track?,
    anchor: TrackAnchor,
    isGrid: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Track) -> Unit = {},
    onAddOrMoveToPlaylist: (Track) -> Unit = {},
    onRemoveFromPlaylist: (Track) -> Unit = {},
    onShare: (Track) -> Unit = {},
    onEdit: (Track) -> Unit,
    onDelete: (Track) -> Unit
) {
    AnimatedVisibility(
        visible = track != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        track?.let { selectedTrack ->
            BackHandler(onBack = onDismiss)

            val density = LocalDensity.current
            val config = LocalConfiguration.current
            val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

            val inPlaylist = selectedTrack.playlistId != null
            val minTopPx = with(density) { 48.dp.toPx() }
            val bottomReservePx = with(density) {
                if (isGrid) 380.dp.toPx() else if (inPlaylist) 340.dp.toPx() else 290.dp.toPx()
            }
            val maxTopPx = (screenHeightPx - bottomReservePx).coerceAtLeast(minTopPx)
            val clampedY = anchor.y.coerceIn(minTopPx, maxTopPx).roundToInt()

            val cardWidthDp = if (anchor.widthPx > 0) {
                with(density) { anchor.widthPx.toDp() }
            } else {
                config.screenWidthDp.dp - 16.dp
            }
            val menuWidthDp = if (inPlaylist) 210.dp else 178.dp
            val menuWidthPx = with(density) { menuWidthDp.toPx() }
            val isLeftColumn = isGrid && (anchor.x + anchor.widthPx / 2f) < (screenWidthPx / 2f)
            val rawMenuOffsetX = if (isLeftColumn) {
                0
            } else {
                (anchor.widthPx - menuWidthPx).roundToInt()
            }
            val edgePadPx = with(density) { 12.dp.toPx() }
            val minOffsetX = (-anchor.x + edgePadPx).roundToInt()
            val maxOffsetX = (screenWidthPx - anchor.x - menuWidthPx - edgePadPx).roundToInt()
            val menuOffsetX = rawMenuOffsetX.coerceIn(minOffsetX, maxOffsetX.coerceAtLeast(minOffsetX))

            val liftScale by animateFloatAsState(
                targetValue = if (track != null) 1.03f else 1f,
                label = "cardLift"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            ) {
                Column(
                    modifier = Modifier
                        .offset { IntOffset(anchor.x.roundToInt(), clampedY) }
                        .width(cardWidthDp)
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = liftScale
                            scaleY = liftScale
                        }
                    ) {
                        TrackItem(
                            track = selectedTrack,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            isGrid = isGrid,
                            isElevated = true,
                            onClick = onDismiss
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF252525),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .offset { IntOffset(menuOffsetX, 0) }
                            .width(menuWidthDp)
                    ) {
                        Column {
                            ContextMenuRow("Select", Icons.Rounded.CheckCircleOutline) {
                                onDismiss()
                                onSelect(selectedTrack)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            ContextMenuRow(
                                text = if (inPlaylist) "Move to playlist" else "Add to playlist",
                                icon = if (inPlaylist) Icons.Rounded.DriveFileMove else Icons.AutoMirrored.Rounded.PlaylistAdd
                            ) {
                                onDismiss()
                                onAddOrMoveToPlaylist(selectedTrack)
                            }
                            if (inPlaylist) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                ContextMenuRow("Remove from playlist", Icons.Rounded.PlaylistRemove) {
                                    onDismiss()
                                    onRemoveFromPlaylist(selectedTrack)
                                }
                            }
                            ContextMenuRow("Share", Icons.AutoMirrored.Rounded.Shortcut) {
                                onDismiss()
                                onShare(selectedTrack)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            ContextMenuRow("Edit", Icons.Filled.Edit) {
                                onDismiss()
                                onEdit(selectedTrack)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            ContextMenuRow("Delete", Icons.Filled.Delete, Color(0xFFFF5252)) {
                                onDismiss()
                                onDelete(selectedTrack)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    text: String,
    icon: ImageVector,
    color: Color = Color.White,
    iconFirst: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (iconFirst) Arrangement.spacedBy(12.dp) else Arrangement.SpaceBetween
    ) {
        if (iconFirst) {
            Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(19.dp))
            Text(text, color = color, fontSize = 15.sp)
        } else {
            Text(text, color = color, fontSize = 15.sp)
            Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
fun DeleteSelectedTracksDialog(
    count: Int,
    warningNote: String? = null,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    if (count > 0) {
        val label = if (count == 1) "1 selected track" else "$count selected tracks"
        DarkDialog(
            onDismissRequest = onDismiss,
            title = "Delete tracks?",
            confirmText = "Delete",
            confirmColor = Color(0xFFFF5252),
            onConfirm = onConfirmDelete
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delete $label from your device?")
                if (!warningNote.isNullOrBlank()) {
                    Text(warningNote, color = Color(0xFFFF8A80), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun EditTrackDialog(
    track: Track?,
    onDismiss: () -> Unit,
    onSave: (track: Track, newArtist: String, newTitle: String, newCoverUri: Uri?, removeCover: Boolean) -> Unit
) {
    track?.let { selectedTrack ->
        var newArtist by remember(selectedTrack.id) { mutableStateOf(selectedTrack.artist) }
        var newTitle by remember(selectedTrack.id) { mutableStateOf(selectedTrack.title) }
        var pickedCoverUri by remember(selectedTrack.id) { mutableStateOf<Uri?>(null) }
        var removeCover by remember(selectedTrack.id) { mutableStateOf(false) }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                pickedCoverUri = uri
                removeCover = false
            }
        }

        val hasCover = pickedCoverUri != null || (!removeCover && selectedTrack.coverFile?.exists() == true)

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF1DB954),
            focusedBorderColor = Color(0xFF1DB954),
            unfocusedBorderColor = Color(0xFF444444),
            focusedLabelColor = Color(0xFF1DB954),
            unfocusedLabelColor = Color(0xFFB3B3B3)
        )

        DarkDialog(
            onDismissRequest = onDismiss,
            title = "Edit track",
            confirmText = "Save",
            onConfirm = { onSave(selectedTrack, newArtist, newTitle, pickedCoverUri, removeCover) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF282828))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        pickedCoverUri != null -> {
                            AsyncImage(
                                model = pickedCoverUri,
                                contentDescription = "Selected cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        !removeCover && selectedTrack.coverFile?.exists() == true -> {
                            AsyncImage(
                                model = selectedTrack.coverFile,
                                contentDescription = "Track cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = "Pick cover",
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Change cover",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (hasCover) {
                    TextButton(
                        onClick = {
                            pickedCoverUri = null
                            removeCover = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Remove cover",
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = newArtist,
                    onValueChange = { newArtist = it },
                    label = { Text("Author") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
fun DeleteTrackDialog(
    track: Track?,
    warningNote: String? = null,
    onDismiss: () -> Unit,
    onConfirmDelete: (Track) -> Unit
) {
    track?.let { selectedTrack ->
        DarkDialog(
            onDismissRequest = onDismiss,
            title = "Delete track?",
            confirmText = "Delete",
            confirmColor = Color(0xFFFF5252),
            onConfirm = { onConfirmDelete(selectedTrack) }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delete \"${selectedTrack.title}\" from your device?")
                if (!warningNote.isNullOrBlank()) {
                    Text(warningNote, color = Color(0xFFFF8A80), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun DarkDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmColor: Color = Color(0xFF1DB954),
    confirmEnabled: Boolean = true,
    dismissText: String? = "Cancel",
    dismissColor: Color = Color(0xFFB3B3B3),
    onDismissButtonClick: (() -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFB3B3B3),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                titleTrailing?.invoke()
            }
        },
        text = content,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(
                    text = confirmText,
                    color = if (confirmEnabled) confirmColor else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            if (dismissText != null) {
                TextButton(onClick = onDismissButtonClick ?: onDismissRequest) {
                    Text(dismissText, color = dismissColor)
                }
            }
        }
    )
}

@Composable
fun AddSourceMenuOverlay(
    visible: Boolean,
    fabOffset: Offset,
    hasMiniPlayer: Boolean = false,
    onDismiss: () -> Unit,
    onFromLinkClick: () -> Unit,
    onFromDeviceClick: () -> Unit
) {
    val iconRotation by animateFloatAsState(
        targetValue = if (visible) 45f else 0f,
        label = "fabRotation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BackHandler(onBack = onDismiss)

        val density = LocalDensity.current
        val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val gapPx = with(density) { 12.dp.toPx() }
        val menuBottomPadding = if (fabOffset.y > 0f) {
            with(density) { (screenHeightPx - fabOffset.y + gapPx).coerceAtLeast(0f).toDp() }
        } else {
            if (hasMiniPlayer) 180.dp else 104.dp
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF252525),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = menuBottomPadding)
                    .width(100.dp)
            ) {
                Column {
                    ContextMenuRow("Link", Icons.Rounded.Link, iconFirst = true) {
                        onDismiss()
                        onFromLinkClick()
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    ContextMenuRow("Local", Icons.Rounded.Smartphone, iconFirst = true) {
                        onDismiss()
                        onFromDeviceClick()
                    }
                }
            }

            FloatingActionButton(
                onClick = onDismiss,
                shape = CircleShape,
                containerColor = Color(0xFF1DB954),
                contentColor = Color.Black,
                modifier = Modifier.offset {
                    IntOffset(fabOffset.x.roundToInt(), fabOffset.y.roundToInt())
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Close menu",
                    modifier = Modifier.rotate(iconRotation)
                )
            }
        }
    }
}

enum class SortOption {
    DATE,
    NAME
}

@Composable
fun SortMenuOverlay(
    visible: Boolean,
    buttonOffset: Offset,
    selectedOption: SortOption,
    isAscending: Boolean,
    onDismiss: () -> Unit,
    onSelectOption: (SortOption) -> Unit
) {
    val overlayAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "sortMenuAlpha"
    )

    if (!visible && overlayAlpha == 0f) return

    BackHandler(enabled = visible, onBack = onDismiss)

    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val menuWidthDp = 135.dp
    val menuWidthPx = with(density) { menuWidthDp.roundToPx() }
    val edgePadPx = with(density) { 12.dp.roundToPx() }
    val circlePaddingPx = with(density) { 8.dp.roundToPx() }
    val menuTopPx = buttonOffset.y.roundToInt() + with(density) { 38.dp.roundToPx() }
    val menuX = (buttonOffset.x.roundToInt() - circlePaddingPx)
        .coerceIn(edgePadPx, (screenWidthPx - menuWidthPx - edgePadPx).coerceAtLeast(edgePadPx))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * overlayAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // Точно на координатах иконки 24.dp, а кружок 40.dp центрируется поверх неё
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(buttonOffset.x.roundToInt(), buttonOffset.y.roundToInt())
                }
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(40.dp)
                    .background(Color.White.copy(alpha = 0.12f * overlayAlpha), CircleShape)
            )
            Icon(
                imageVector = Icons.Rounded.SwapVert,
                contentDescription = "Sort tracks",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF252525),
            shadowElevation = 12.dp,
            modifier = Modifier
                .offset { IntOffset(menuX, menuTopPx) }
                .graphicsLayer { alpha = overlayAlpha }
                .width(menuWidthDp)
        ) {
            Column {
                SortMenuRow(
                    text = "By date",
                    selected = selectedOption == SortOption.DATE,
                    isAscending = isAscending,
                    onClick = { onSelectOption(SortOption.DATE) }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SortMenuRow(
                    text = "By name",
                    selected = selectedOption == SortOption.NAME,
                    isAscending = isAscending,
                    onClick = { onSelectOption(SortOption.NAME) }
                )
            }
        }
    }
}

@Composable
private fun SortMenuRow(
    text: String,
    selected: Boolean,
    isAscending: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp
        )
        if (selected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(17.dp)
                )
                Icon(
                    imageVector = if (isAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = if (isAscending) "Ascending" else "Descending",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}