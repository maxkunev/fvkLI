package com.example.fvkli.ui.theme.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fvkli.models.Track

@Composable
fun MiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCloseClick: () -> Unit
) {
    var displayedTrack by remember { mutableStateOf(track) }
    if (track != null) {
        displayedTrack = track
    }

    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 180))
    ) {
        val activeTrack = displayedTrack ?: return@AnimatedVisibility

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .width(69.dp)
                    .height(26.dp)
                    .clip(TrapezoidTabShape)
                    .background(Color(0xFF282828))
                    .clickable { onCloseClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close player",
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF282828))
                    .clickable { onClick() }
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeTrack.coverFile != null) {
                        AsyncImage(
                            model = activeTrack.coverFile,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTrack.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeTrack.artist,
                        color = Color(0xFFB3B3B3),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Prev",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF1DB954)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White)
                }
            }
        }
    }
}

private val TrapezoidTabShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val flare = w * 0.16f
    val topRadius = h * 0.44f

    moveTo(0f, h)
    cubicTo(
        x1 = flare * 0.6f, y1 = h,
        x2 = flare * 0.9f, y2 = h - flare * 0.4f,
        x3 = flare, y3 = topRadius
    )
    quadraticTo(
        x1 = flare, y1 = 0f,
        x2 = flare + topRadius, y2 = 0f
    )
    lineTo(w - flare - topRadius, 0f)
    quadraticTo(
        x1 = w - flare, y1 = 0f,
        x2 = w - flare, y2 = topRadius
    )
    cubicTo(
        x1 = w - flare * 0.9f, y1 = h - flare * 0.4f,
        x2 = w - flare * 0.6f, y2 = h,
        x3 = w, y3 = h
    )
    close()
}