package com.example.fvkli.ui.theme.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fvkli.models.Track
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.Check

data class TrackAnchor(
    val x: Float = 0f,
    val y: Float = 0f,
    val widthPx: Int = 0
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGrid: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: ((TrackAnchor) -> Unit)? = null,
    isElevated: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var anchor by remember { mutableStateOf(TrackAnchor()) }

    val bgColor = when {
        isElevated -> Color(0xFF2A2A2A)
        isCurrent -> Color(0xFF1E1E1E)
        isGrid -> Color(0xFF181818)
        else -> Color.Transparent
    }
    val badgeBorderColor = when {
        isElevated -> Color(0xFF2A2A2A)
        isCurrent -> Color(0xFF1E1E1E)
        isGrid -> Color(0xFF181818)
        else -> Color(0xFF121212)
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        shadowElevation = if (isElevated) 14.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                anchor = TrackAnchor(
                    x = pos.x,
                    y = pos.y,
                    widthPx = coords.size.width
                )
            }
    ) {
        val clickModifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick?.let { callback ->
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    callback(anchor)
                }
            }
        )

        if (isGrid) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(clickModifier)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF242424)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.coverFile != null) {
                            AsyncImage(
                                model = track.coverFile,
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        if (isCurrent && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.72f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    SelectionCheckBadge(
                        visible = isSelected,
                        borderColor = badgeBorderColor,
                        sizeDp = 26,
                        iconSizeDp = 16,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = track.title,
                    color = if (isCurrent) Color(0xFF1DB954) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(clickModifier)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrent) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )
                }

                Box(modifier = Modifier.size(46.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF242424)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.coverFile != null) {
                            AsyncImage(
                                model = track.coverFile,
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.Gray)
                        }
                    }

                    SelectionCheckBadge(
                        visible = isSelected,
                        borderColor = badgeBorderColor,
                        sizeDp = 21,
                        iconSizeDp = 13,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = if (isCurrent) Color(0xFF1DB954) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = Color(0xFFB3B3B3),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionCheckBadge(
    visible: Boolean,
    borderColor: Color,
    sizeDp: Int,
    iconSizeDp: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(tween(150), initialScale = 0.5f) + fadeIn(tween(150)),
        exit = scaleOut(tween(120), targetScale = 0.5f) + fadeOut(tween(120)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(Color(0xFF1DB954))
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(iconSizeDp.dp)
            )
        }
    }
}