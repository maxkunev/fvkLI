package com.example.fvkli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fvkli.data.AudioDownloader
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import com.example.fvkli.player.AudioPlayer
import com.example.fvkli.ui.theme.components.MiniPlayer
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import com.example.fvkli.models.Track
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import com.example.fvkli.ui.theme.components.AddLinkDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import com.example.fvkli.ui.theme.components.TrackItem
import com.example.fvkli.ui.theme.screens.PlayerScreen

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Paste a link (YouTube or TikTok)") }
    var isLoading by remember { mutableStateOf(false) }
    var tracks by remember {
        mutableStateOf(AudioDownloader.getSavedTracks(context))
    }
    var editingTrack by remember { mutableStateOf<Track?>(null) }
    val currentTrack = tracks.find { it.id == AudioPlayer.currentTrackId }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPlayerScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF121212),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    shape = CircleShape,
                    containerColor = Color(0xFF1DB954),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add track")
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
                        onNext = { AudioPlayer.next() }
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
                Image(
                    painter = painterResource(id = R.drawable.logo_li_no_bg),
                    contentDescription = "LI Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(36.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isCurrent = AudioPlayer.currentTrackId == track.id,
                            isPlaying = AudioPlayer.isPlaying,
                            onClick = { AudioPlayer.playFromList(tracks, track) },
                            onEditClick = { editingTrack = track },
                            onDeleteClick = {
                                AudioPlayer.removeTrackFromQueue(track.id)
                                AudioDownloader.deleteTrack(context, track)
                                tracks = AudioDownloader.getSavedTracks(context)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showPlayerScreen && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            currentTrack?.let { track ->
                BackHandler { showPlayerScreen = false }
                PlayerScreen(
                    track = track,
                    isPlaying = AudioPlayer.isPlaying,
                    onPrevious = { AudioPlayer.previous() },
                    onPlayPause = { AudioPlayer.togglePlayPause() },
                    onNext = { AudioPlayer.next() },
                    onClose = { showPlayerScreen = false }
                )
            }
        }
    }
    editingTrack?.let { track ->
        var newArtist by remember(track.id) { mutableStateOf(track.artist) }
        var newTitle by remember(track.id) { mutableStateOf(track.title) }

        AlertDialog(
            onDismissRequest = { editingTrack = null },
            title = { Text("Edit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newArtist,
                        onValueChange = { newArtist = it },
                        label = { Text("Author") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    AudioDownloader.updateTrackMeta(context, track.id, newArtist, newTitle)
                    tracks = AudioDownloader.getSavedTracks(context)
                    tracks.find { it.id == track.id }?.let { AudioPlayer.updateTrackInQueue(it) }
                    editingTrack = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingTrack = null }) { Text("Cancel") }
            }
        )
    }
    if (showAddDialog) {
        AddLinkDialog(
            url = url,
            onUrlChange = { url = it },
            statusText = statusText,
            isLoading = isLoading,
            onDismiss = { showAddDialog = false },
            onDownloadClick = {
                if (url.isBlank()) {
                    statusText = "Add link!"
                } else {
                    isLoading = true
                    statusText = "Downloading..."
                    scope.launch {
                        val result = AudioDownloader.downloadAudio(
                            context = context,
                            url = url,
                            onStatus = { msg -> statusText = msg },
                            onProgress = { progress -> statusText = "Downloading: ${progress.toInt()}%" }
                        )
                        statusText = result.fold(
                            onSuccess = { file ->
                                tracks = AudioDownloader.getSavedTracks(context)
                                url = "" // очищаем поле под следующую ссылку
                                val sizeMb = String.format("%.2f", file.length() / (1024.0 * 1024.0))
                                "Ready! (${sizeMb} MB)"
                            },
                            onFailure = { err ->
                                "Error: ${err.localizedMessage}"
                            }
                        )
                        isLoading = false
                    }
                }
            }
        )
    }
}