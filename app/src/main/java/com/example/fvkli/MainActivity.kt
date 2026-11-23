package com.example.fvkli

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fvkli.data.AudioDownloader
import com.example.fvkli.models.Track
import com.example.fvkli.player.AudioPlayer
import com.example.fvkli.ui.theme.components.AddLinkDialog
import com.example.fvkli.ui.theme.components.AddSourceMenuOverlay
import com.example.fvkli.ui.theme.components.DeleteTrackDialog
import com.example.fvkli.ui.theme.components.EditTrackDialog
import com.example.fvkli.ui.theme.components.MiniPlayer
import com.example.fvkli.ui.theme.components.TrackAnchor
import com.example.fvkli.ui.theme.components.TrackContextMenuOverlay
import com.example.fvkli.ui.theme.components.TrackItem
import com.example.fvkli.ui.theme.screens.PlayerScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.SwapVert
import com.example.fvkli.ui.theme.components.SortMenuOverlay
import com.example.fvkli.ui.theme.components.SortOption
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.CircularProgressIndicator
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioPlayer.connect(applicationContext)
        setContent {
            SimpleWindowScreen()
        }
    }
}

@Composable
fun SimpleWindowScreen() {
    var downloadedTracksCount by remember { mutableIntStateOf(0) }
    var totalTracksCount by remember { mutableIntStateOf(0) }
    var fabOffset by remember { mutableStateOf(Offset.Zero) }
    var sortButtonOffset by remember { mutableStateOf(Offset.Zero) }
    var isGridView by remember { mutableStateOf(false) }
    var contextMenuAnchor by remember { mutableStateOf(TrackAnchor()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var tracks by remember { mutableStateOf(AudioDownloader.getSavedTracks(context)) }

    var editingTrack by remember { mutableStateOf<Track?>(null) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var contextMenuTrack by remember { mutableStateOf<Track?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPlayerScreen by remember { mutableStateOf(false) }

    var showAddMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = AudioDownloader.importLocalFile(context, uri)
                result.onSuccess {
                    tracks = AudioDownloader.getSavedTracks(context)
                    Toast.makeText(context, "Track added!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var sortOption by remember { mutableStateOf(SortOption.DATE) }
    var sortAscending by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedTracks = remember(tracks, sortOption, sortAscending) {
        when (sortOption) {
            SortOption.DATE -> {
                if (sortAscending) {
                    tracks.sortedBy { it.id.toLongOrNull() ?: 0L }
                } else {
                    tracks.sortedByDescending { it.id.toLongOrNull() ?: 0L }
                }
            }
            SortOption.NAME -> {
                if (sortAscending) {
                    tracks.sortedBy { it.title.lowercase() }
                } else {
                    tracks.sortedByDescending { it.title.lowercase() }
                }
            }
        }
    }

    val isOverlayOpen = contextMenuTrack != null ||
            editingTrack != null ||
            trackToDelete != null ||
            showAddDialog ||
            showAddMenu ||
            showSortMenu

    val blurRadius by animateDpAsState(
        targetValue = if (isOverlayOpen) 14.dp else 0.dp,
        label = "bgBlur"
    )
    val currentTrack = tracks.find { it.id == AudioPlayer.currentTrackId }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blurRadius),
            containerColor = Color(0xFF121212),
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnimatedVisibility(
                        visible = isLoading && !showAddDialog,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            onClick = { showAddDialog = true },
                            shape = CircleShape,
                            color = Color(0xFF252525),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            shadowElevation = 6.dp
                        ) {
                            Text(
                                text = if (totalTracksCount > 0) "$downloadedTracksCount/$totalTracksCount" else "...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            if (totalTracksCount > 0) {
                                val overallProgress = ((downloadedTracksCount + (downloadProgress ?: 0f)) / totalTracksCount.toFloat())
                                    .coerceIn(0f, 1f)
                                val animatedRing by animateFloatAsState(
                                    targetValue = overallProgress,
                                    animationSpec = tween(durationMillis = 250),
                                    label = "fabRingProgress"
                                )
                                CircularProgressIndicator(
                                    progress = { animatedRing },
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.18f),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.18f),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = { showAddMenu = true },
                            shape = CircleShape,
                            containerColor = Color(0xFF1DB954),
                            contentColor = Color.Black,
                            modifier = Modifier
                                .size(56.dp)
                                .onGloballyPositioned { coords ->
                                    fabOffset = coords.positionInRoot()
                                }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add track")
                        }
                    }
                }
            },
            bottomBar = {
                if (currentTrack != null) {
                    MiniPlayer(
                        track = currentTrack,
                        isPlaying = AudioPlayer.isPlaying,
                        onClick = { showPlayerScreen = true },
                        onPrevious = { AudioPlayer.previous() },
                        onPlayPause = { AudioPlayer.togglePlayPause() },
                        onNext = { AudioPlayer.next() },
                        onCloseClick = { AudioPlayer.stopAndClear() }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.offset(y = (-4).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                                contentDescription = "Toggle grid view",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = "Sort tracks",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .onGloballyPositioned { coords ->
                                        sortButtonOffset = coords.positionInRoot()
                                    }
                                    .graphicsLayer {
                                        alpha = if (showSortMenu) 0f else 1f
                                    }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_li_no_bg),
                            contentDescription = "LI Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(88.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isGridView) 2 else 1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isGridView) 10.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedTracks, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isCurrent = AudioPlayer.currentTrackId == track.id,
                            isPlaying = AudioPlayer.isPlaying,
                            isGrid = isGridView,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(420, easing = FastOutSlowInEasing),
                                placementSpec = tween(480, easing = FastOutSlowInEasing),
                                fadeOutSpec = tween(300, easing = FastOutSlowInEasing)
                            ),
                            onClick = { AudioPlayer.playFromList(sortedTracks, track) },
                            onLongClick = { anchor ->
                                contextMenuAnchor = anchor
                                contextMenuTrack = track
                            }
                        )
                    }
                }
            }
        }

        TrackContextMenuOverlay(
            track = contextMenuTrack,
            anchor = contextMenuAnchor,
            isGrid = isGridView,
            isCurrent = AudioPlayer.currentTrackId == contextMenuTrack?.id,
            isPlaying = AudioPlayer.isPlaying,
            onDismiss = { contextMenuTrack = null },
            onEdit = { editingTrack = it },
            onDelete = { trackToDelete = it }
        )

        AddSourceMenuOverlay(
            visible = showAddMenu,
            fabOffset = fabOffset,
            hasMiniPlayer = currentTrack != null,
            onDismiss = { showAddMenu = false },
            onFromLinkClick = {
                if (!isLoading) {
                    statusText = ""
                    downloadProgress = null
                }
                showAddDialog = true
            },
            onFromDeviceClick = {
                filePickerLauncher.launch(arrayOf("audio/*", "video/*"))
            }
        )
        SortMenuOverlay(
            visible = showSortMenu,
            buttonOffset = sortButtonOffset,
            selectedOption = sortOption,
            isAscending = sortAscending,
            onDismiss = { showSortMenu = false },
            onSelectOption = { clickedOption ->
                if (sortOption == clickedOption) {
                    sortAscending = !sortAscending
                } else {
                    sortOption = clickedOption
                    sortAscending = (clickedOption == SortOption.NAME)
                }
            }
        )


            currentTrack?.let { track ->


                val currentIndex = currentTrack?.let { t -> sortedTracks.indexOfFirst { it.id == t.id } } ?: -1
                val previousTrack = if (currentTrack != null && sortedTracks.size > 1) {
                    sortedTracks.find { it.id == AudioPlayer.getPreviousTrackId() }
                        ?: sortedTracks[(currentIndex - 1 + sortedTracks.size) % sortedTracks.size]
                } else null
                val nextTrack = if (currentTrack != null && sortedTracks.size > 1) {
                    sortedTracks.find { it.id == AudioPlayer.getNextTrackId() }
                        ?: sortedTracks[(currentIndex + 1) % sortedTracks.size]
                } else null

                PlayerScreen(
                    visible = showPlayerScreen && currentTrack != null,
                    track = currentTrack,
                    previousTrack = previousTrack,
                    nextTrack = nextTrack,
                    isPlaying = AudioPlayer.isPlaying,
                    onPrevious = { AudioPlayer.previous() },
                    onPlayPause = { AudioPlayer.togglePlayPause() },
                    onNext = { AudioPlayer.next() },
                    onClose = { showPlayerScreen = false }
                )

        }
    }

    EditTrackDialog(
        track = editingTrack,
        onDismiss = { editingTrack = null },
        onSave = { track, newArtist, newTitle ->
            AudioDownloader.updateTrackMeta(context, track.id, newArtist, newTitle)
            tracks = AudioDownloader.getSavedTracks(context)
            tracks.find { it.id == track.id }?.let { AudioPlayer.updateTrackInQueue(it) }
            editingTrack = null
        }
    )

    DeleteTrackDialog(
        track = trackToDelete,
        onDismiss = { trackToDelete = null },
        onConfirmDelete = { track ->
            AudioPlayer.removeTrackFromQueue(track.id)
            AudioDownloader.deleteTrack(context, track)
            tracks = AudioDownloader.getSavedTracks(context)
            trackToDelete = null
        }
    )

    if (showAddDialog) {
        AddLinkDialog(
            url = url,
            onUrlChange = { url = it },
            statusText = statusText,
            isLoading = isLoading,
            progress = downloadProgress,
            onDismiss = { showAddDialog = false },
            onMinimize = {
                showAddDialog = false
                Toast.makeText(context, "Downloading in background...", Toast.LENGTH_SHORT).show()
            },
            onCancelClick = {
                AudioDownloader.cancelDownload(context)
                downloadJob?.cancel()
                isLoading = false
                downloadProgress = null
                downloadedTracksCount = 0
                totalTracksCount = 0
                statusText = "Cancelled"
            },
            onDownloadClick = {
                isLoading = true
                downloadProgress = null
                downloadedTracksCount = 0
                totalTracksCount = 0
                statusText = "Fetching info..."
                downloadJob = scope.launch {
                    val result = AudioDownloader.downloadAudio(
                        context = context,
                        url = url,
                        onStatus = { msg -> statusText = msg },
                        onProgress = { progress ->
                            downloadProgress = (progress / 100f).coerceIn(0f, 1f)
                        },
                        onTrackDownloaded = {
                            tracks = AudioDownloader.getSavedTracks(context)
                        },
                        onQueueProgress = { downloaded, total ->
                            downloadedTracksCount = downloaded
                            totalTracksCount = total
                        },
                        onRollback = { rolledBackIds ->
                            rolledBackIds.forEach { AudioPlayer.removeTrackFromQueue(it) }
                            tracks = AudioDownloader.getSavedTracks(context)
                        }
                    )
                    val finalMessage = result.fold(
                        onSuccess = { summary ->
                            tracks = AudioDownloader.getSavedTracks(context)
                            url = ""
                            summary
                        },
                        onFailure = { err ->
                            if (err.message == "Cancelled") "Cancelled" else "Error: ${err.localizedMessage}"
                        }
                    )
                    statusText = finalMessage
                    downloadProgress = null
                    downloadedTracksCount = 0
                    totalTracksCount = 0
                    isLoading = false

                    if (!showAddDialog && finalMessage != "Cancelled") {
                        Toast.makeText(context, finalMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

}