package com.example.fvkli.ui.theme.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fvkli.R
import com.example.fvkli.models.Track
import com.example.fvkli.player.AudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

@Composable
fun PlayerScreen(
    visible: Boolean,
    track: Track?,
    previousTrack: Track? = null,
    nextTrack: Track? = null,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onEdit: (Track) -> Unit = {},
    onDelete: (Track) -> Unit = {},
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val expandProgress = remember { Animatable(0f) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var displayedTrack by remember { mutableStateOf(track) }
    if (track != null) {
        displayedTrack = track
    }

    LaunchedEffect(visible) {
        showMoreMenu = false
        if (visible) {
            expandProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 380f)
            )
        } else {
            expandProgress.snapTo(0f)
            if (track == null) displayedTrack = null
        }
    }

    val activeTrack = displayedTrack
    if ((!visible && expandProgress.value == 0f) || activeTrack == null) return

    fun triggerClose() {
        showMoreMenu = false
        scope.launch {
            expandProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
            onClose()
        }
    }

    if (visible && !showMoreMenu) {
        BackHandler(onBack = ::triggerClose)
    }

    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val miniPlayerHeightPx = with(density) { 78.dp.toPx() }
    val maxTravelPx = (screenHeightPx - miniPlayerHeightPx).coerceAtLeast(1f)

    val p = expandProgress.value.coerceIn(0f, 1f)
    val sheetCornerRadius = 20.dp * (1f - p)
    val sheetAlpha = (p / 0.35f).coerceIn(0f, 1f)
    val innerScale = 0.92f + 0.08f * p

    val offsetX = remember { Animatable(0f) }
    val swipeThresholdPx = with(density) { 96.dp.toPx() }

    val bgEntryAlpha = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            bgEntryAlpha.snapTo(0f)
            bgEntryAlpha.animateTo(1f, tween(durationMillis = 900))
        }
    }

    var settledTrackId by remember { mutableStateOf(activeTrack.id) }
    val activeOffsetX = if (activeTrack.id != settledTrackId) 0f else offsetX.value

    LaunchedEffect(activeTrack.id) {
        showMoreMenu = false
        offsetX.snapTo(0f)
        settledTrackId = activeTrack.id
    }

    var positionMs by remember(activeTrack.id) { mutableLongStateOf(AudioPlayer.getCurrentPosition()) }
    var durationMs by remember(activeTrack.id) { mutableLongStateOf(AudioPlayer.getDuration()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(activeTrack.id, isPlaying) {
        while (true) {
            if (!isDragging) {
                positionMs = AudioPlayer.getCurrentPosition()
                durationMs = AudioPlayer.getDuration()
            }
            delay(300L)
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
    val sliderValue = if (isDragging) {
        dragPosition
    } else {
        positionMs.toFloat().coerceIn(0f, safeDuration)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * p))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = (1f - p) * maxTravelPx
                    alpha = sheetAlpha
                }
                .clip(RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius))
                .background(Color(0xFF121212))
                .pointerInput(Unit) {}
                .pointerInput(maxTravelPx) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (expandProgress.value < 0.78f) {
                                triggerClose()
                            } else {
                                scope.launch {
                                    expandProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { expandProgress.animateTo(1f, spring()) }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val currentDrag = (1f - expandProgress.value) * maxTravelPx
                            val nextDrag = (currentDrag + dragAmount).coerceIn(0f, maxTravelPx)
                            scope.launch {
                                expandProgress.snapTo(1f - (nextDrag / maxTravelPx))
                            }
                        }
                    )
                }
        ) {
            Crossfade(
                targetState = activeTrack.coverFile,
                animationSpec = tween(durationMillis = 800),
                label = "bgCrossfade",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = bgEntryAlpha.value }
            ) { cover ->
                if (cover != null) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.35f
                                scaleY = 1.35f
                            }
                            .blur(55.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1E1E).copy(alpha = 0.62f),
                                Color(0xFF121212).copy(alpha = 0.86f),
                                Color(0xFF0C0C0C)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = innerScale
                        scaleY = innerScale
                    }
                    .systemBarsPadding()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(
                        onClick = ::triggerClose,
                        modifier = Modifier
                            .size(48.dp)
                            .offset(y = (-6).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
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

                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .offset(y = (-6).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Track options",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.55f))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cardSize = maxWidth - 56.dp
                    val gap = 34.dp
                    val stepPx = with(density) { (cardSize + gap).toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(activeTrack.id, nextTrack != null, previousTrack != null) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        scope.launch {
                                            when {
                                                offsetX.value < -swipeThresholdPx && nextTrack != null -> {
                                                    offsetX.animateTo(-stepPx, tween(200))
                                                    onNext()
                                                    delay(400L)
                                                    if (offsetX.value != 0f) offsetX.animateTo(0f, spring())
                                                }
                                                offsetX.value > swipeThresholdPx && previousTrack != null -> {
                                                    offsetX.animateTo(stepPx, tween(200))
                                                    onPrevious()
                                                    delay(400L)
                                                    if (offsetX.value != 0f) offsetX.animateTo(0f, spring())
                                                }
                                                else -> {
                                                    offsetX.animateTo(0f, spring())
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch { offsetX.animateTo(0f, spring()) }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        val canDrag = (dragAmount < 0 && nextTrack != null) ||
                                                (dragAmount > 0 && previousTrack != null)
                                        val factor = if (canDrag) 1f else 0.25f
                                        scope.launch {
                                            offsetX.snapTo(offsetX.value + dragAmount * factor)
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (previousTrack != null) {
                            key("prev_${previousTrack.id}") {
                                CoverCard(
                                    track = previousTrack,
                                    modifier = Modifier
                                        .size(cardSize)
                                        .offset { IntOffset((-stepPx + activeOffsetX).roundToInt(), 0) }
                                )
                            }
                        }

                        CoverCard(
                            track = activeTrack,
                            modifier = Modifier
                                .size(cardSize)
                                .offset { IntOffset(activeOffsetX.roundToInt(), 0) }
                        )

                        if (nextTrack != null) {
                            key("next_${nextTrack.id}") {
                                CoverCard(
                                    track = nextTrack,
                                    modifier = Modifier
                                        .size(cardSize)
                                        .offset { IntOffset((stepPx + activeOffsetX).roundToInt(), 0) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(34.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Text(
                        text = activeTrack.title,
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeTrack.artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            isDragging = true
                            dragPosition = it
                        },
                        onValueChangeFinished = {
                            AudioPlayer.seekTo(dragPosition.toLong())
                            positionMs = dragPosition.toLong()
                            isDragging = false
                        },
                        valueRange = 0f..safeDuration,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF1DB954),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(sliderValue.toLong()),
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .size(70.dp)
                                .shadow(12.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            AnimatedVisibility(
                visible = showMoreMenu,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                BackHandler { showMoreMenu = false }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showMoreMenu = false }
                        )
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF252525),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .systemBarsPadding()
                            .padding(top = 52.dp, end = 16.dp)
                            .width(150.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMoreMenu = false
                                        onEdit(activeTrack)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Edit", color = Color.White, fontSize = 15.sp)
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMoreMenu = false
                                        onDelete(activeTrack)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Delete", color = Color(0xFFFF5252), fontSize = 15.sp)
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverCard(
    track: Track,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(20.dp)
    val imageRequest = remember(track.coverFile) {
        ImageRequest.Builder(context)
            .data(track.coverFile)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .shadow(elevation = 20.dp, shape = cardShape)
            .clip(cardShape)
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color.White.copy(alpha = 0.08f), cardShape),
        contentAlignment = Alignment.Center
    ) {
        if (track.coverFile != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "Track Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}