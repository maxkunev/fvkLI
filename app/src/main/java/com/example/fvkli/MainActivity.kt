package com.example.fvkli

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fvkli.data.AudioDownloader
import com.example.fvkli.models.Track
import com.example.fvkli.player.AudioPlayer
import com.example.fvkli.service.DownloadService
import com.example.fvkli.ui.theme.components.AddLinkDialog
import com.example.fvkli.ui.theme.components.AddSourceMenuOverlay
import com.example.fvkli.ui.theme.components.DeleteTrackDialog
import com.example.fvkli.ui.theme.components.EditTrackDialog
import com.example.fvkli.ui.theme.components.MiniPlayer
import com.example.fvkli.ui.theme.components.SortMenuOverlay
import com.example.fvkli.ui.theme.components.SortOption
import com.example.fvkli.ui.theme.components.TrackAnchor
import com.example.fvkli.ui.theme.components.TrackContextMenuOverlay
import com.example.fvkli.ui.theme.components.TrackItem
import com.example.fvkli.ui.theme.screens.PlayerScreen
import kotlinx.coroutines.launch
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.draw.clip
import com.example.fvkli.ui.theme.components.DeleteSelectedTracksDialog
import android.content.Intent
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.Settings
import com.example.fvkli.ui.theme.screens.SettingsScreen
class MainActivity : ComponentActivity() {
    private var sharedUrlEvent by mutableStateOf<Pair<String, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioPlayer.connect(applicationContext)
        if (savedInstanceState == null) {
            handleShareIntent(intent)
        }
        setContent {
            SimpleWindowScreen(sharedUrlEvent = sharedUrlEvent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val rawText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            if (rawText.isNotBlank()) {
                val cleanUrl = extractUrlFromText(rawText)
                sharedUrlEvent = cleanUrl to System.currentTimeMillis()
            }
        }
    }

    private fun extractUrlFromText(text: String): String {
        val urlRegex = Regex("""https?://[^\s"'<>]+""")
        return urlRegex.find(text)?.value ?: text.trim()
    }
}

@Composable
fun SimpleWindowScreen(
    sharedUrlEvent: Pair<String, Long>? = null
) {
    var selectedTrackIds by remember { mutableStateOf(emptySet<String>()) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    val isSelectionMode = selectedTrackIds.isNotEmpty()
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        selectedTrackIds = emptySet()
    }
    var fabOffset by remember { mutableStateOf(Offset.Zero) }
    var sortButtonOffset by remember { mutableStateOf(Offset.Zero) }
    var contextMenuAnchor by remember { mutableStateOf(TrackAnchor()) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fvkli_prefs", Context.MODE_PRIVATE) }

    var isGridView by remember {
        mutableStateOf(prefs.getBoolean("is_grid_view", false))
    }
    var sortOption by remember {
        mutableStateOf(
            runCatching {
                SortOption.valueOf(
                    prefs.getString("sort_option", SortOption.DATE.name) ?: SortOption.DATE.name
                )
            }.getOrDefault(SortOption.DATE)
        )
    }
    var sortAscending by remember {
        mutableStateOf(prefs.getBoolean("sort_ascending", false))
    }
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf(AudioDownloader.getSavedTracks(context)) }
    var showAddDialog by remember { mutableStateOf(false) }

    val downloadState by AudioDownloader.downloadState.collectAsState()
    val isLoading = downloadState.isLoading



    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(downloadState.libraryUpdateTick) {
        if (downloadState.libraryUpdateTick > 0) {
            if (downloadState.lastRolledBackIds.isNotEmpty()) {
                downloadState.lastRolledBackIds.forEach { AudioPlayer.removeTrackFromQueue(it) }
                AudioDownloader.consumeRolledBackIds()
            }
            tracks = AudioDownloader.getSavedTracks(context)
        }
    }

    var wasLoading by remember { mutableStateOf(false) }
    LaunchedEffect(downloadState.isLoading) {
        if (wasLoading && !downloadState.isLoading) {
            val msg = downloadState.statusText
            if (msg.startsWith("Ready") || msg.startsWith("Added")) {
                url = ""
            }
            if (!showAddDialog && msg.isNotBlank() && msg != "Cancelled") {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
        wasLoading = downloadState.isLoading
    }

    var editingTrack by remember { mutableStateOf<Track?>(null) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var contextMenuTrack by remember { mutableStateOf<Track?>(null) }
    var showPlayerScreen by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    LaunchedEffect(sharedUrlEvent) {
        showSettingsScreen = false
        val sharedUrl = sharedUrlEvent?.first ?: return@LaunchedEffect
        showAddMenu = false
        showPlayerScreen = false
        if (!isLoading) {
            AudioDownloader.clearStatus()
            url = sharedUrl
            showAddDialog = true
        } else {
            showAddDialog = true
            Toast.makeText(context, "Wait for current download to finish", Toast.LENGTH_SHORT).show()
        }
    }

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
            showBatchDeleteDialog ||
            showAddDialog ||
            showAddMenu ||
            showSortMenu

    val blurRadius by animateDpAsState(
        targetValue = if (isOverlayOpen) 14.dp else 0.dp,
        label = "bgBlur"
    )
    val currentTrack = tracks.find { it.id == AudioPlayer.currentTrackId }
    val fabBottomPadding by animateDpAsState(
        targetValue = if (currentTrack != null) 74.dp else 0.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "fabBottomPadding"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(

            modifier = Modifier.blur(blurRadius),
            containerColor = Color(0xFF121212),
            floatingActionButton = {
                val canExpandFromBadge = isLoading && !showAddDialog

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = fabBottomPadding)
                        .then(
                            if (canExpandFromBadge) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showAddDialog = true }
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    AnimatedVisibility(
                        visible = isLoading && !showAddDialog && !isSelectionMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF252525),
                                border = BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.65f)),
                                shadowElevation = 8.dp,
                                modifier = Modifier.defaultMinSize(minWidth = 60.dp, minHeight = 32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (downloadState.totalCount > 0) {
                                            "${downloadState.currentIndex}/${downloadState.totalCount}"
                                        } else {
                                            "..."
                                        },
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            if (downloadState.totalCount > 0) {
                                val currentFraction = downloadState.progress ?: 0f
                                val completedBeforeCurrent = (downloadState.currentIndex - 1).coerceAtLeast(0)
                                val overallProgress = ((completedBeforeCurrent + currentFraction) / downloadState.totalCount.toFloat())
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
                    // Левая часть: сетка и сортировка (или крестик и счётчик при выделении)
                    Crossfade(
                        targetState = isSelectionMode,
                        modifier = Modifier
                            .width(88.dp)
                            .offset(y = (-4).dp),
                        label = "topLeftControls"
                    ) { selecting ->
                        if (selecting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedTrackIds = emptySet() },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Cancel selection",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "${selectedTrackIds.size}",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        isGridView = !isGridView
                                        prefs.edit().putBoolean("is_grid_view", isGridView).apply()
                                    },
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
                        }
                    }

                    // Центр: логотип
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

                    // Правая часть: настройки (или отправка и удаление при выделении)
                    Crossfade(
                        targetState = isSelectionMode,
                        modifier = Modifier
                            .width(88.dp)
                            .offset(y = (-4).dp),
                        label = "topRightControls"
                    ) { selecting ->
                        if (selecting) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val toShare = sortedTracks.filter { it.id in selectedTrackIds }
                                        AudioDownloader.shareTracks(context, toShare)
                                        selectedTrackIds = emptySet()
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Shortcut,
                                        contentDescription = "Share selected tracks",
                                        tint = Color.White,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showBatchDeleteDialog = true },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete selected tracks",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                IconButton(
                                    onClick = { showSettingsScreen = true },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isGridView) 2 else 1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = if (currentTrack != null) 144.dp else 88.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isGridView) 10.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedTracks, key = { it.id }) { track ->
                        val isSelected = track.id in selectedTrackIds

                        Box(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = tween(420, easing = FastOutSlowInEasing),
                                    placementSpec = tween(480, easing = FastOutSlowInEasing),
                                    fadeOutSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = Color(0xFF1DB954),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            TrackItem(
                                track = track,
                                isCurrent = AudioPlayer.currentTrackId == track.id,
                                isPlaying = AudioPlayer.isPlaying,
                                isGrid = isGridView,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedTrackIds = if (isSelected) {
                                            selectedTrackIds - track.id
                                        } else {
                                            selectedTrackIds + track.id
                                        }
                                    } else {
                                        AudioPlayer.playFromList(sortedTracks, track)
                                    }
                                },
                                onLongClick = { anchor ->
                                    if (!isSelectionMode) {
                                        contextMenuAnchor = anchor
                                        contextMenuTrack = track
                                    }
                                }
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color(0xFF1DB954).copy(alpha = 0.12f))
                                )
                            }

                            if (isSelectionMode) {
                                Box(
                                    modifier = Modifier
                                        .align(if (isGridView) Alignment.TopEnd else Alignment.CenterEnd)
                                        .padding(if (isGridView) 8.dp else 14.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF1DB954)
                                            else Color.Black.copy(alpha = 0.45f)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) Color(0xFF1DB954) else Color.White.copy(
                                                alpha = 0.7f
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .blur(blurRadius)
        ) {
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
        TrackContextMenuOverlay(
            track = contextMenuTrack,
            anchor = contextMenuAnchor,
            isGrid = isGridView,
            isCurrent = AudioPlayer.currentTrackId == contextMenuTrack?.id,
            isPlaying = AudioPlayer.isPlaying,
            onDismiss = { contextMenuTrack = null },
            onSelect = { track ->
                selectedTrackIds = setOf(track.id)
            },
            onShare = { track ->
                AudioDownloader.shareTracks(context, listOf(track))
            },
            onEdit = { editingTrack = it },
            onDelete = { trackToDelete = it }
        )

        AddSourceMenuOverlay(
            visible = showAddMenu,
            fabOffset = fabOffset,
            hasMiniPlayer = currentTrack != null,
            onDismiss = { showAddMenu = false },
            onFromLinkClick = {
                AudioDownloader.clearStatus()
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
                prefs.edit()
                    .putString("sort_option", sortOption.name)
                    .putBoolean("sort_ascending", sortAscending)
                    .apply()
            }
        )

        currentTrack?.let { track ->
            val currentIndex = sortedTracks.indexOfFirst { it.id == track.id }
            val previousTrack = if (sortedTracks.size > 1 && currentIndex >= 0) {
                sortedTracks.find { it.id == AudioPlayer.getPreviousTrackId() }
                    ?: sortedTracks[(currentIndex - 1 + sortedTracks.size) % sortedTracks.size]
            } else null
            val nextTrack = if (sortedTracks.size > 1 && currentIndex >= 0) {
                sortedTracks.find { it.id == AudioPlayer.getNextTrackId() }
                    ?: sortedTracks[(currentIndex + 1) % sortedTracks.size]
            } else null

            PlayerScreen(
                visible = showPlayerScreen,
                track = track,
                previousTrack = previousTrack,
                nextTrack = nextTrack,
                isPlaying = AudioPlayer.isPlaying,
                onPrevious = { AudioPlayer.previous() },
                onPlayPause = { AudioPlayer.togglePlayPause() },
                onNext = { AudioPlayer.next() },
                onEdit = { editingTrack = it },
                onDelete = { trackToDelete = it },
                onClose = { showPlayerScreen = false }
            )
        }
    }

    SettingsScreen(
        visible = showSettingsScreen,
        tracks = tracks,
        onClose = { showSettingsScreen = false }
    )

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
            if (AudioPlayer.currentTrackId == null) {
                showPlayerScreen = false
            }
            trackToDelete = null
        }
    )
            DeleteSelectedTracksDialog(
                count = if (showBatchDeleteDialog) selectedTrackIds.size else 0,
                onDismiss = { showBatchDeleteDialog = false },
                onConfirmDelete = {
                    val toDelete = tracks.filter { it.id in selectedTrackIds }
                    toDelete.forEach { track ->
                        AudioPlayer.removeTrackFromQueue(track.id)
                        AudioDownloader.deleteTrack(context, track)
                    }
                    tracks = AudioDownloader.getSavedTracks(context)
                    selectedTrackIds = emptySet()
                    showBatchDeleteDialog = false
                }
            )

    if (showAddDialog) {
        AddLinkDialog(
            url = if (isLoading && url.isBlank()) downloadState.activeUrl else url,
            onUrlChange = { url = it },
            statusText = downloadState.statusText,
            isLoading = isLoading,
            progress = downloadState.progress,
            onDismiss = { showAddDialog = false },
            onMinimize = {
                showAddDialog = false
                Toast.makeText(context, "Downloading in background...", Toast.LENGTH_SHORT).show()
            },
            onCancelClick = {
                DownloadService.cancel(context)
            },
            onDownloadClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                DownloadService.start(context, url)
            }
        )
    }
}