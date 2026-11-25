package com.example.fvkli.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.fvkli.models.Track
import androidx.media3.common.C

object AudioPlayer {
    private var controller: MediaController? = null

    var currentTrackId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    fun connect(context: Context) {
        if (controller != null) return
        val appContext = context.applicationContext
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()

        future.addListener({
            val ctrl = future.get()
            controller = ctrl
            ctrl.repeatMode = Player.REPEAT_MODE_ALL
            currentTrackId = ctrl.currentMediaItem?.mediaId
            isPlaying = ctrl.isPlaying

            ctrl.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentTrackId = mediaItem?.mediaId
                }
            })
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun Track.toMediaItem(): MediaItem {
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)

        coverFile?.let { file ->
            metaBuilder.setArtworkUri(Uri.fromFile(file))
        }

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.fromFile(file))
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    fun playFromList(tracks: List<Track>, clickedTrack: Track) {
        val ctrl = controller ?: return
        if (currentTrackId == clickedTrack.id) {
            togglePlayPause()
            return
        }
        val startIndex = tracks.indexOfFirst { it.id == clickedTrack.id }.coerceAtLeast(0)
        ctrl.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
        ctrl.repeatMode = Player.REPEAT_MODE_ALL
        ctrl.prepare()
        ctrl.play()
        currentTrackId = clickedTrack.id
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun next() {
        val ctrl = controller ?: return
        if (ctrl.mediaItemCount > 0) ctrl.seekToNextMediaItem()
    }

    fun previous() {
        val ctrl = controller ?: return
        if (ctrl.mediaItemCount > 0) ctrl.seekToPreviousMediaItem()
    }

    fun getCurrentPosition(): Long = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L

    fun getDuration(): Long = controller?.duration?.coerceAtLeast(0L) ?: 0L

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun updateTrackInQueue(track: Track) {
        val ctrl = controller ?: return
        for (i in 0 until ctrl.mediaItemCount) {
            if (ctrl.getMediaItemAt(i).mediaId == track.id) {
                ctrl.replaceMediaItem(i, track.toMediaItem())
                break
            }
        }
    }

    fun removeTrackFromQueue(trackId: String) {
        val ctrl = controller ?: return
        for (i in 0 until ctrl.mediaItemCount) {
            if (ctrl.getMediaItemAt(i).mediaId == trackId) {
                ctrl.removeMediaItem(i)
                break
            }
        }
        if (ctrl.mediaItemCount == 0) {
            currentTrackId = null
            isPlaying = false
        }
    }

    fun updateQueueAroundCurrent(newTracks: List<Track>) {
        val ctrl = controller ?: return
        val curId = currentTrackId ?: return
        val newCurrentIdx = newTracks.indexOfFirst { it.id == curId }
        if (newCurrentIdx < 0) return

        val curIdx = ctrl.currentMediaItemIndex
        if (curIdx == C.INDEX_UNSET || curIdx !in 0 until ctrl.mediaItemCount) return

        if (curIdx + 1 < ctrl.mediaItemCount) {
            ctrl.removeMediaItems(curIdx + 1, ctrl.mediaItemCount)
        }
        if (curIdx > 0) {
            ctrl.removeMediaItems(0, curIdx)
        }

        val before = newTracks.subList(0, newCurrentIdx).map { it.toMediaItem() }
        val after = newTracks.subList(newCurrentIdx + 1, newTracks.size).map { it.toMediaItem() }
        if (before.isNotEmpty()) {
            ctrl.addMediaItems(0, before)
        }
        if (after.isNotEmpty()) {
            ctrl.addMediaItems(ctrl.mediaItemCount, after)
        }
    }
    fun stopAndClear() {
        controller?.stop()
        controller?.clearMediaItems()
        isPlaying = false
        currentTrackId = null
    }
    fun getNextTrackId(): String? {
        val ctrl = controller ?: return null
        if (ctrl.mediaItemCount <= 1) return null
        val idx = ctrl.nextMediaItemIndex
        return if (idx != C.INDEX_UNSET && idx in 0 until ctrl.mediaItemCount) {
            ctrl.getMediaItemAt(idx).mediaId
        } else null
    }

    fun getPreviousTrackId(): String? {
        val ctrl = controller ?: return null
        if (ctrl.mediaItemCount <= 1) return null
        val idx = ctrl.previousMediaItemIndex
        return if (idx != C.INDEX_UNSET && idx in 0 until ctrl.mediaItemCount) {
            ctrl.getMediaItemAt(idx).mediaId
        } else null
    }
}