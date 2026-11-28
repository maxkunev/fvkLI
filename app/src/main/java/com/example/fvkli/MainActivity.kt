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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.ui.text.style.TextOverflow
import com.example.fvkli.data.PlaylistStorage
import com.example.fvkli.models.Playlist
import com.example.fvkli.ui.theme.components.CreateOrEditPlaylistDialog
import com.example.fvkli.ui.theme.components.DeletePlaylistsDialog
import com.example.fvkli.ui.theme.components.DisbandPlaylistDialog
import com.example.fvkli.ui.theme.components.PlaylistAnchor
import com.example.fvkli.ui.theme.components.PlaylistContextMenuOverlay
import com.example.fvkli.ui.theme.components.PlaylistExpandedGridOverlay
import com.example.fvkli.ui.theme.components.PlaylistPickerDialog
import com.example.fvkli.ui.theme.components.PlaylistShelf
import com.example.fvkli.ui.theme.components.DarkDialog
import androidx.compose.foundation.lazy.grid.GridItemSpan
import android.net.Uri
import com.example.fvkli.ui.theme.components.PlaylistShelfAnchor
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

private data class PendingEmptyMove(
    val trackIds: List<String>,
    val sourcePlaylistName: String,
    val targetPlaylist: Playlist? = null,
    val newPlaylistName: String? = null,
    val newPlaylistCoverUri: Uri? = null,
    val newPlaylistHideAutoCover: Boolean = false
)
@Composable
fun SimpleWindowScreen(
    sharedUrlEvent: Pair<String, Long>? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fvkli_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var tracks by remember { mutableStateOf(AudioDownloader.getSavedTracks(context)) }
    var playlists by remember {
        mutableStateOf(
            PlaylistStorage.pruneEmptyPlaylists(
                context = context,
                playlists = PlaylistStorage.loadPlaylists(context),
                nonEmptyPlaylistIds = tracks.mapNotNull { it.playlistId }.toSet()
            )
        )
    }

    var activePlaylistId by remember { mutableStateOf<String?>(null) }
    val activePlaylist = remember(playlists, activePlaylistId) {
        playlists.find { it.id == activePlaylistId }
    }

    var selectedTrackIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedPlaylistIds by remember { mutableStateOf(emptySet<String>()) }
    val isTrackSelectionMode = selectedTrackIds.isNotEmpty()
    val isPlaylistSelectionMode = selectedPlaylistIds.isNotEmpty()
    val isSelectionMode = isTrackSelectionMode || isPlaylistSelectionMode

    var showSettingsScreen by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showExpandedPlaylists by remember { mutableStateOf(false) }
    var expandedPlaylistsAnchor by remember { mutableStateOf(PlaylistShelfAnchor()) }
    var contextMenuPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playlistMenuAnchor by remember { mutableStateOf(PlaylistAnchor()) }
    var trackIdsPendingPlaylist by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDisband by remember { mutableStateOf<Playlist?>(null) }
    var playlistsToDelete by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var trackToRemoveFromPlaylist by remember { mutableStateOf<Track?>(null) }

    var pendingEmptyMove by remember { mutableStateOf<PendingEmptyMove?>(null) }

    fun findEmptiedSourcePlaylist(trackIds: List<String>): Playlist? {
        if (trackIds.isEmpty()) return null
        val sourceIds = tracks
            .filter { it.id in trackIds }
            .mapNotNull { it.playlistId }
            .distinct()
        if (sourceIds.size != 1) return null
        val sourceId = sourceIds.first()
        val remaining = tracks.count { it.playlistId == sourceId && it.id !in trackIds }
        return if (remaining == 0) playlists.find { it.id == sourceId } else null
    }

    var downloadTargetPlaylistId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = activePlaylistId != null && !isSelectionMode) {
        activePlaylistId = null
    }
    BackHandler(enabled = isSelectionMode) {
        selectedTrackIds = emptySet()
        selectedPlaylistIds = emptySet()
    }

    var fabOffset by remember { mutableStateOf(Offset.Zero) }
    var sortButtonOffset by remember { mutableStateOf(Offset.Zero) }
    var contextMenuAnchor by remember { mutableStateOf(TrackAnchor()) }

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

    fun sortTrackList(list: List<Track>): List<Track> = when (sortOption) {
        SortOption.DATE -> if (sortAscending) {
            list.sortedBy { it.id.toLongOrNull() ?: 0L }
        } else {
            list.sortedByDescending { it.id.toLongOrNull() ?: 0L }
        }
        SortOption.NAME -> if (sortAscending) {
            list.sortedBy { it.title.lowercase() }
        } else {
            list.sortedByDescending { it.title.lowercase() }
        }
    }

    fun refreshLibrary() {
        val freshTracks = AudioDownloader.getSavedTracks(context)
        val nonEmptyIds = freshTracks.mapNotNull { it.playlistId }.toSet()
        val freshPlaylists = PlaylistStorage.pruneEmptyPlaylists(
            context = context,
            playlists = PlaylistStorage.loadPlaylists(context),
            nonEmptyPlaylistIds = nonEmptyIds
        )
        tracks = freshTracks
        playlists = freshPlaylists
        if (activePlaylistId != null && freshPlaylists.none { it.id == activePlaylistId }) {
            activePlaylistId = null
        }
        if (freshPlaylists.size <= 3) {
            showExpandedPlaylists = false
        }
        selectedTrackIds = selectedTrackIds.filter { id -> freshTracks.any { it.id == id } }.toSet()
        selectedPlaylistIds = selectedPlaylistIds.filter { id -> freshPlaylists.any { it.id == id } }.toSet()

        val playing = freshTracks.find { it.id == AudioPlayer.currentTrackId }
        if (playing != null) {
            val isolatedScope = sortTrackList(freshTracks.filter { it.playlistId == playing.playlistId })
            AudioPlayer.updateQueueAroundCurrent(isolatedScope)
        }
    }

    fun handleRemoveFromPlaylist(track: Track) {
        val pId = track.playlistId ?: return
        val countInPlaylist = tracks.count { it.playlistId == pId }
        if (countInPlaylist <= 1) {
            trackToRemoveFromPlaylist = track
        } else {
            AudioDownloader.assignTracksToPlaylist(context, listOf(track.id), null)
            refreshLibrary()
            Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show()
        }
    }

    var url by remember { mutableStateOf("") }
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
            refreshLibrary()
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
        showExpandedPlaylists = false
        if (!isLoading) {
            AudioDownloader.clearStatus()
            url = sharedUrl
            downloadTargetPlaylistId = null
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
                val result = AudioDownloader.importLocalFile(context, uri, targetPlaylistId = activePlaylistId)
                result.onSuccess {
                    refreshLibrary()
                    Toast.makeText(context, "Track added!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }

    val tracksByPlaylist = remember(tracks) {
        tracks.filter { it.playlistId != null }.groupBy { it.playlistId!! }
    }

    val sortedTracks = remember(tracks, activePlaylistId, sortOption, sortAscending) {
        sortTrackList(tracks.filter { it.playlistId == activePlaylistId })
    }

    val isOverlayOpen = contextMenuTrack != null ||
            contextMenuPlaylist != null ||
            pendingEmptyMove != null ||
            showExpandedPlaylists ||
            editingTrack != null ||
            trackToDelete != null ||
            trackToRemoveFromPlaylist != null ||
            showBatchDeleteDialog ||
            trackIdsPendingPlaylist.isNotEmpty() ||
            showCreatePlaylistDialog ||
            editingPlaylist != null ||
            playlistToDisband != null ||
            playlistsToDelete.isNotEmpty() ||
            showAddDialog

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
                                modifier = Modifier.defaultMinSize(
                                    minWidth = 60.dp,
                                    minHeight = 32.dp
                                )
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
                                val completedBeforeCurrent =
                                    (downloadState.currentIndex - 1).coerceAtLeast(0)
                                val overallProgress =
                                    ((completedBeforeCurrent + currentFraction) / downloadState.totalCount.toFloat())
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    // Левая часть
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        when {
                            isSelectionMode -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            selectedTrackIds = emptySet()
                                            selectedPlaylistIds = emptySet()
                                        },
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
                                        text = "${if (isPlaylistSelectionMode) selectedPlaylistIds.size else selectedTrackIds.size}",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            activePlaylist != null -> {
                                IconButton(
                                    onClick = { activePlaylistId = null },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back to library",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            else -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            isGridView = !isGridView
                                            prefs.edit().putBoolean("is_grid_view", isGridView)
                                                .apply()
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
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = if (isTrackSelectionMode) 118.dp else 92.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activePlaylist != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = activePlaylist.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${sortedTracks.size} ${if (sortedTracks.size == 1) "track" else "tracks"}",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.logo_li_no_bg),
                                contentDescription = "LI Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        when {
                            isPlaylistSelectionMode -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val toShare =
                                                tracks.filter { it.playlistId in selectedPlaylistIds }
                                            AudioDownloader.shareTracks(context, toShare)
                                            selectedPlaylistIds = emptySet()
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Shortcut,
                                            contentDescription = "Share playlists",
                                            tint = Color.White,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            playlistsToDelete =
                                                playlists.filter { it.id in selectedPlaylistIds }
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete playlists",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            isTrackSelectionMode -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            trackIdsPendingPlaylist = selectedTrackIds.toList()
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (activePlaylist != null) Icons.Rounded.DriveFileMove else Icons.AutoMirrored.Rounded.PlaylistAdd,
                                            contentDescription = "Add or move to playlist",
                                            tint = Color(0xFF1DB954),
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val toShare =
                                                sortedTracks.filter { it.id in selectedTrackIds }
                                            AudioDownloader.shareTracks(context, toShare)
                                            selectedTrackIds = emptySet()
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Shortcut,
                                            contentDescription = "Share selected tracks",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showBatchDeleteDialog = true },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete selected tracks",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                }
                            }

                            activePlaylist != null -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            isGridView = !isGridView
                                            prefs.edit().putBoolean("is_grid_view", isGridView)
                                                .apply()
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

                            else -> {
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

                Spacer(modifier = Modifier.height(12.dp))


                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isGridView) 2 else 1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = if (currentTrack != null) 144.dp else 88.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isGridView) 10.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activePlaylist == null && playlists.isNotEmpty()) {
                        item(
                            key = "playlist_shelf",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            PlaylistShelf(
                                playlists = playlists,
                                playingPlaylistId = currentTrack?.playlistId,
                                isPlaying = AudioPlayer.isPlaying,
                                tracksByPlaylist = tracksByPlaylist,
                                isPlaylistSelectionMode = isPlaylistSelectionMode,
                                selectedPlaylistIds = selectedPlaylistIds,
                                dimmed = isTrackSelectionMode,
                                onPlaylistClick = { playlist ->
                                    if (isPlaylistSelectionMode) {
                                        selectedPlaylistIds =
                                            if (playlist.id in selectedPlaylistIds) {
                                                selectedPlaylistIds - playlist.id
                                            } else {
                                                selectedPlaylistIds + playlist.id
                                            }
                                    } else if (!isTrackSelectionMode) {
                                        activePlaylistId = playlist.id
                                    }
                                },
                                onPlaylistLongClick = { playlist, anchor ->
                                    if (!isSelectionMode) {
                                        playlistMenuAnchor = anchor
                                        contextMenuPlaylist = playlist
                                    }
                                },
                                onExpandGrid = { anchor ->
                                    if (!isTrackSelectionMode) {
                                        expandedPlaylistsAnchor = anchor
                                        showExpandedPlaylists = true
                                    }
                                }
                            )
                        }
                    }
                    items(sortedTracks, key = { it.id }) { track ->
                        val isSelected = track.id in selectedTrackIds

                        Box(
                            modifier = Modifier
                                .graphicsLayer { alpha = if (isPlaylistSelectionMode) 0.4f else 1f }
                                .animateItem(
                                    fadeInSpec = tween(420, easing = FastOutSlowInEasing),
                                    placementSpec = tween(480, easing = FastOutSlowInEasing),
                                    fadeOutSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                        ) {
                            TrackItem(
                                track = track,
                                isCurrent = AudioPlayer.currentTrackId == track.id,
                                isPlaying = AudioPlayer.isPlaying,
                                isGrid = isGridView,
                                isSelected = isSelected,
                                onClick = {
                                    if (isPlaylistSelectionMode) return@TrackItem
                                    if (isTrackSelectionMode) {
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
            onAddOrMoveToPlaylist = { track ->
                trackIdsPendingPlaylist = listOf(track.id)
            },
            onRemoveFromPlaylist = { track ->
                handleRemoveFromPlaylist(track)
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
                downloadTargetPlaylistId = null
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
        PlaylistExpandedGridOverlay(
            visible = showExpandedPlaylists && activePlaylist == null,
            playlists = playlists,
            playingPlaylistId = currentTrack?.playlistId,
            isPlaying = AudioPlayer.isPlaying,
            tracksByPlaylist = tracksByPlaylist,
            anchor = expandedPlaylistsAnchor,
            isPlaylistSelectionMode = isPlaylistSelectionMode,
            selectedPlaylistIds = selectedPlaylistIds,
            onDismiss = { showExpandedPlaylists = false },
            onPlaylistClick = { playlist ->
                if (isPlaylistSelectionMode) {
                    selectedPlaylistIds = if (playlist.id in selectedPlaylistIds) {
                        selectedPlaylistIds - playlist.id
                    } else {
                        selectedPlaylistIds + playlist.id
                    }
                } else {
                    showExpandedPlaylists = false
                    activePlaylistId = playlist.id
                }
            },
            onPlaylistLongClick = { playlist, anchor ->
                if (!isSelectionMode) {
                    playlistMenuAnchor = anchor
                    contextMenuPlaylist = playlist
                }
            },
            onCancelContextMenu = {
                contextMenuPlaylist = null
            },
            onReorderPlaylists = { reordered ->
                playlists = reordered
                PlaylistStorage.savePlaylists(context, reordered)
            }
        )

        PlaylistContextMenuOverlay(
            playlist = contextMenuPlaylist,
            isCurrent = contextMenuPlaylist != null && currentTrack?.playlistId == contextMenuPlaylist?.id,
            isPlaying = AudioPlayer.isPlaying,
            playlistTracks = contextMenuPlaylist?.let { tracksByPlaylist[it.id].orEmpty() }
                .orEmpty(),
            anchor = playlistMenuAnchor,
            onDismiss = { contextMenuPlaylist = null },
            onSelect = { playlist ->
                selectedPlaylistIds = setOf(playlist.id)
            },
            onEdit = { playlist ->
                editingPlaylist = playlist
            },
            onDisband = { playlist ->
                playlistToDisband = playlist
            },
            onShare = { playlist ->
                val pTracks = tracksByPlaylist[playlist.id].orEmpty()
                AudioDownloader.shareTracks(context, pTracks)
            },
            onDelete = { playlist ->
                playlistsToDelete = listOf(playlist)
            }
        )
        currentTrack?.let { track ->
            val previousTrack = AudioPlayer.getPreviousTrackId()?.let { id ->
                tracks.find { it.id == id }
            }
            val nextTrack = AudioPlayer.getNextTrackId()?.let { id ->
                tracks.find { it.id == id }
            }

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
                onAddOrMoveToPlaylist = { trackIdsPendingPlaylist = listOf(it.id) },
                onRemoveFromPlaylist = { handleRemoveFromPlaylist(it) },
                onDelete = { trackToDelete = it },
                onClose = { showPlayerScreen = false }
            )
        }

        SettingsScreen(
            visible = showSettingsScreen,
            tracks = tracks,
            playlists = playlists,
            onImportStarted = { showSettingsScreen = false },
            onClose = { showSettingsScreen = false }
        )
        val pendingSourcePlaylistId = remember(trackIdsPendingPlaylist, tracks) {
            val ids = tracks
                .filter { it.id in trackIdsPendingPlaylist }
                .map { it.playlistId }
                .distinct()
            if (ids.size == 1) ids.first() else null
        }

        PlaylistPickerDialog(
            visible = trackIdsPendingPlaylist.isNotEmpty() && !showCreatePlaylistDialog,
            title = if (pendingSourcePlaylistId != null) "Move to playlist" else "Add to playlist",
            playlists = playlists,
            tracksByPlaylist = tracksByPlaylist,
            excludePlaylistId = pendingSourcePlaylistId,
            onDismiss = { trackIdsPendingPlaylist = emptyList() },
            onSelectPlaylist = { target ->
                val emptiedSource = findEmptiedSourcePlaylist(trackIdsPendingPlaylist)
                if (emptiedSource != null) {
                    pendingEmptyMove = PendingEmptyMove(
                        trackIds = trackIdsPendingPlaylist,
                        sourcePlaylistName = emptiedSource.name,
                        targetPlaylist = target
                    )
                    trackIdsPendingPlaylist = emptyList()
                } else {
                    AudioDownloader.assignTracksToPlaylist(context, trackIdsPendingPlaylist, target.id)
                    trackIdsPendingPlaylist = emptyList()
                    selectedTrackIds = emptySet()
                    refreshLibrary()
                    Toast.makeText(context, "Added to \"${target.name}\"", Toast.LENGTH_SHORT).show()
                }
            },
            onCreateNewClick = {
                showCreatePlaylistDialog = true
            }
        )

        CreateOrEditPlaylistDialog(
            visible = showCreatePlaylistDialog,
            existingPlaylists = playlists,
            initialTracks = tracks.filter { it.id in trackIdsPendingPlaylist },
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, coverUri, removeCover, _ ->
                val emptiedSource = findEmptiedSourcePlaylist(trackIdsPendingPlaylist)
                if (emptiedSource != null) {
                    pendingEmptyMove = PendingEmptyMove(
                        trackIds = trackIdsPendingPlaylist,
                        sourcePlaylistName = emptiedSource.name,
                        newPlaylistName = name,
                        newPlaylistCoverUri = coverUri,
                        newPlaylistHideAutoCover = removeCover
                    )
                    showCreatePlaylistDialog = false
                    trackIdsPendingPlaylist = emptyList()
                } else {
                    val (created, _) = PlaylistStorage.createPlaylist(
                        context = context,
                        existing = playlists,
                        name = name,
                        coverUri = coverUri,
                        hideAutoCover = removeCover
                    )
                    AudioDownloader.assignTracksToPlaylist(context, trackIdsPendingPlaylist, created.id)
                    showCreatePlaylistDialog = false
                    trackIdsPendingPlaylist = emptyList()
                    selectedTrackIds = emptySet()
                    refreshLibrary()
                    Toast.makeText(context, "Created \"${created.name}\"", Toast.LENGTH_SHORT).show()
                }
            }
        )

        pendingEmptyMove?.let { move ->
            val destName = move.targetPlaylist?.name ?: move.newPlaylistName.orEmpty()
            val trackLabel = if (move.trackIds.size == 1) "the last track" else "all tracks"
            DarkDialog(
                onDismissRequest = { pendingEmptyMove = null },
                title = "Remove empty playlist?",
                confirmText = "Move",
                onConfirm = {
                    val targetId = if (move.targetPlaylist != null) {
                        move.targetPlaylist.id
                    } else {
                        val (created, _) = PlaylistStorage.createPlaylist(
                            context = context,
                            existing = playlists,
                            name = move.newPlaylistName.orEmpty(),
                            coverUri = move.newPlaylistCoverUri,
                            hideAutoCover = move.newPlaylistHideAutoCover
                        )
                        created.id
                    }
                    AudioDownloader.assignTracksToPlaylist(context, move.trackIds, targetId)
                    pendingEmptyMove = null
                    selectedTrackIds = emptySet()
                    refreshLibrary()
                    Toast.makeText(context, "Moved to \"$destName\"", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Moving $trackLabel to \"$destName\" will leave \"${move.sourcePlaylistName}\" empty and remove it. Continue?")
            }
        }

        editingPlaylist?.let { target ->
            CreateOrEditPlaylistDialog(
                visible = true,
                existingPlaylists = playlists,
                initialPlaylist = target,
                initialTracks = tracksByPlaylist[target.id].orEmpty(),
                onDismiss = { editingPlaylist = null },
                onConfirm = { newName, newCoverUri, removeCover, restoreAutoCover ->
                    playlists = PlaylistStorage.updatePlaylist(
                        context = context,
                        existing = playlists,
                        playlistId = target.id,
                        newName = newName,
                        newCoverUri = newCoverUri,
                        removeCover = removeCover,
                        restoreAutoCover = restoreAutoCover
                    )
                    editingPlaylist = null
                }
            )
        }

        DisbandPlaylistDialog(
            playlist = playlistToDisband,
            trackCount = playlistToDisband?.let { tracksByPlaylist[it.id]?.size ?: 0 } ?: 0,
            onDismiss = { playlistToDisband = null },
            onConfirm = { target ->
                AudioDownloader.disbandPlaylistTracks(context, target.id)
                PlaylistStorage.removePlaylistEntries(context, playlists, setOf(target.id))
                playlistToDisband = null
                refreshLibrary()
                Toast.makeText(context, "Playlist disbanded", Toast.LENGTH_SHORT).show()
            }
        )

        DeletePlaylistsDialog(
            playlists = playlistsToDelete,
            totalTrackCount = playlistsToDelete.sumOf { tracksByPlaylist[it.id]?.size ?: 0 },
            onDismiss = { playlistsToDelete = emptyList() },
            onConfirm = {
                val idsToDelete = playlistsToDelete.map { it.id }.toSet()
                val tracksInTarget = tracks.filter { it.playlistId in idsToDelete }
                tracksInTarget.forEach { t ->
                    AudioPlayer.removeTrackFromQueue(t.id)
                    AudioDownloader.deleteTrack(context, t)
                }
                PlaylistStorage.removePlaylistEntries(context, playlists, idsToDelete)
                playlistsToDelete = emptyList()
                selectedPlaylistIds = emptySet()
                refreshLibrary()
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            }
        )

        trackToRemoveFromPlaylist?.let { target ->
            val plName = playlists.find { it.id == target.playlistId }?.name ?: "this playlist"
            DarkDialog(
                onDismissRequest = { trackToRemoveFromPlaylist = null },
                title = "Remove last track?",
                confirmText = "Remove",
                onConfirm = {
                    AudioDownloader.assignTracksToPlaylist(context, listOf(target.id), null)
                    trackToRemoveFromPlaylist = null
                    refreshLibrary()
                    Toast.makeText(
                        context,
                        "Removed and empty playlist deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text("This is the last track in \"$plName\". Removing it will return the track to the main list and remove the empty playlist.")
            }
        }

        EditTrackDialog(
            track = editingTrack,
            onDismiss = { editingTrack = null },
            onSave = { track, newArtist, newTitle, newCoverUri, removeCover ->
                AudioDownloader.updateTrackMeta(
                    context = context,
                    id = track.id,
                    newArtist = newArtist,
                    newTitle = newTitle,
                    newCoverUri = newCoverUri,
                    removeCover = removeCover
                )
                refreshLibrary()
                tracks.find { it.id == track.id }?.let { AudioPlayer.updateTrackInQueue(it) }
                editingTrack = null
            }
        )

        val deleteTrackWarning = trackToDelete?.let { t ->
            t.playlistId?.let { pId ->
                if (tracks.count { it.playlistId == pId && it.id != t.id } == 0) {
                    val plName = playlists.find { it.id == pId }?.name ?: "this playlist"
                    "This is the last track in \"$plName\". The playlist will also be removed."
                } else null
            }
        }

        DeleteTrackDialog(
            track = trackToDelete,
            warningNote = deleteTrackWarning,
            onDismiss = { trackToDelete = null },
            onConfirmDelete = { track ->
                AudioPlayer.removeTrackFromQueue(track.id)
                AudioDownloader.deleteTrack(context, track)
                refreshLibrary()
                if (AudioPlayer.currentTrackId == null) {
                    showPlayerScreen = false
                }
                trackToDelete = null
            }
        )

        val batchDeleteWarning = if (
            showBatchDeleteDialog &&
            activePlaylist != null &&
            sortedTracks.isNotEmpty() &&
            sortedTracks.none { it.id !in selectedTrackIds }
        ) {
            "All tracks in \"${activePlaylist.name}\" will be deleted, so the playlist will also be removed."
        } else null

        DeleteSelectedTracksDialog(
            count = if (showBatchDeleteDialog) selectedTrackIds.size else 0,
            warningNote = batchDeleteWarning,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirmDelete = {
                val toDelete = tracks.filter { it.id in selectedTrackIds }
                toDelete.forEach { track ->
                    AudioPlayer.removeTrackFromQueue(track.id)
                    AudioDownloader.deleteTrack(context, track)
                }
                selectedTrackIds = emptySet()
                showBatchDeleteDialog = false
                refreshLibrary()
                if (AudioPlayer.currentTrackId == null) {
                    showPlayerScreen = false
                }
            }
        )
    }


    if (showAddDialog) {
        AddLinkDialog(
            url = if (isLoading && url.isBlank()) downloadState.activeUrl else url,
            onUrlChange = { url = it },
            statusText = downloadState.statusText,
            playlists = playlists,
            selectedPlaylistId = downloadTargetPlaylistId,
            onSelectPlaylistId = { downloadTargetPlaylistId = it },
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
                DownloadService.start(context, url, downloadTargetPlaylistId)
            }
        )
    }
}