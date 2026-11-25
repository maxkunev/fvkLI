package com.example.fvkli.ui.theme.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Unarchive
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fvkli.data.PlaylistStorage
import com.example.fvkli.models.Playlist
import com.example.fvkli.models.Track
import java.io.File
import kotlin.math.roundToInt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
data class PlaylistAnchor(
    val x: Float = 0f,
    val y: Float = 0f,
    val widthPx: Int = 0
)

@Composable
fun PlaylistCover(
    playlist: Playlist,
    playlistTracks: List<Track>,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 14,
    showCountBadge: Boolean = true
) {
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    val customCover = playlist.coverFile?.takeIf { it.exists() }
    val trackCovers = remember(playlistTracks, playlist.hideAutoCover) {
        if (playlist.hideAutoCover) emptyList()
        else playlistTracks.mapNotNull { it.coverFile?.takeIf(File::exists) }.distinct().take(4)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(Color(0xFF1F1F1F))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            customCover != null -> {
                AsyncImage(
                    model = customCover,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            trackCovers.size >= 4 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = trackCovers[0],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = trackCovers[1],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = trackCovers[2],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = trackCovers[3],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            trackCovers.isNotEmpty() -> {
                AsyncImage(
                    model = trackCovers.first(),
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        if (showCountBadge && playlistTracks.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${playlistTracks.size}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistTile(
    playlist: Playlist,
    playlistTracks: List<Track>,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isElevated: Boolean = false,
    enabled: Boolean = true,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: ((PlaylistAnchor) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var anchor by remember { mutableStateOf(PlaylistAnchor()) }
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                anchor = PlaylistAnchor(pos.x, pos.y, coords.size.width)
            }
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = if (onLongClick != null) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick(anchor)
                    }
                } else null
            )
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isElevated) Modifier.shadow(14.dp, shape) else Modifier
                )
        ) {
            val borderColor = if (isCurrent) Color(0xFF1DB954).copy(alpha = 0.85f) else null

            PlaylistCover(
                playlist = playlist,
                playlistTracks = playlistTracks,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (borderColor != null) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = borderColor,
                                shape = shape
                            )
                        } else Modifier
                    )
            )

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(vertical = 2.dp)
                ) {
                    PlaylistPlayingWave(isPlaying = isPlaying)
                }
            }

            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF1DB954) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = playlist.name,
            color = if (isSelected || isCurrent) Color(0xFF1DB954) else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isCurrent) Color(0xFF1DB954) else Color.Transparent)
        )
    }
}

@Composable
private fun PlaylistPlayingWave(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playlistWave")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveBar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveBar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveBar3"
    )

    val heights = if (isPlaying) listOf(bar1, bar2, bar3) else listOf(0.42f, 0.72f, 0.42f)

    Row(
        modifier = modifier
            .height(14.dp)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight(fraction.coerceIn(0.2f, 1f))
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF1DB954))
            )
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistShelf(
    playlists: List<Playlist>,
    tracksByPlaylist: Map<String, List<Track>>,
    isPlaylistSelectionMode: Boolean,
    selectedPlaylistIds: Set<String>,
    dimmed: Boolean,
    playingPlaylistId: String? = null,
    isPlaying: Boolean = false,
    onPlaylistClick: (Playlist) -> Unit,
    onPlaylistLongClick: (Playlist, PlaylistAnchor) -> Unit,
    onExpandGrid: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlists.isEmpty()) return

    val pageCount = ((playlists.size + 2) / 3).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val hasOverflow = playlists.size > 3

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (dimmed) 0.45f else 1f },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp
        ) { page ->
            val startIndex = page * 3
            val pageItems = if (startIndex < playlists.size) {
                playlists.subList(startIndex, minOf(startIndex + 3, playlists.size))
            } else {
                emptyList()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (slot in 0 until 3) {
                    val playlist = pageItems.getOrNull(slot)
                    if (playlist != null) {
                        PlaylistTile(
                            playlist = playlist,
                            isCurrent = playlist.id == playingPlaylistId, isPlaying = isPlaying,
                            playlistTracks = tracksByPlaylist[playlist.id].orEmpty(),
                            isSelectionMode = isPlaylistSelectionMode,
                            isSelected = playlist.id in selectedPlaylistIds,
                            onClick = { onPlaylistClick(playlist) },
                            onLongClick = { anchor -> onPlaylistLongClick(playlist, anchor) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (hasOverflow) {
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .padding(horizontal = 4.dp)
            ) {
                // Точки пагинации строго по центру
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (active) 6.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) Color(0xFF1DB954) else Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }


                Surface(
                    onClick = onExpandGrid,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF222222),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Expand playlists",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.06f),
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
        )
    }
}

@Composable
fun PlaylistExpandedGridOverlay(
    visible: Boolean,
    playlists: List<Playlist>,
    tracksByPlaylist: Map<String, List<Track>>,
    isPlaylistSelectionMode: Boolean,
    selectedPlaylistIds: Set<String>,
    playingPlaylistId: String? = null,
    isPlaying: Boolean = false,
    onDismiss: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onPlaylistLongClick: (Playlist, PlaylistAnchor) -> Unit,
    onCancelContextMenu: () -> Unit,
    onReorderPlaylists: (List<Playlist>) -> Unit
) {
    AnimatedVisibility(
        visible = visible && playlists.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BackHandler(onBack = onDismiss)

        val haptic = LocalHapticFeedback.current
        val density = LocalDensity.current
        val maxGridHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
        val dragSlopPx = with(density) { 10.dp.toPx() }
        val hysteresisPx = with(density) { 10.dp.toPx() }
        val hGap = 12.dp
        val vGap = 14.dp

        val scrollState = rememberScrollState()
        var pressedId by remember { mutableStateOf<String?>(null) }
        var workingPlaylists by remember { mutableStateOf(playlists) }
        LaunchedEffect(playlists) {
            if (pressedId == null) {
                workingPlaylists = playlists
            }
        }
        var isDragging by remember { mutableStateOf(false) }
        var cumulativeDrag by remember { mutableStateOf(Offset.Zero) }
        var dragStartSlotOffset by remember { mutableStateOf(Offset.Zero) }
        var dragTileWidthDp by remember { mutableStateOf(0.dp) }
        var dragTileHeightDp by remember { mutableStateOf(0.dp) }

        var overlayRootOffset by remember { mutableStateOf(Offset.Zero) }
        var contentRootOffset by remember { mutableStateOf(Offset.Zero) }

        val latestOnClick by rememberUpdatedState(onPlaylistClick)
        val latestOnLongClick by rememberUpdatedState(onPlaylistLongClick)
        val latestOnCancelMenu by rememberUpdatedState(onCancelContextMenu)
        val latestOnReorder by rememberUpdatedState(onReorderPlaylists)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    overlayRootOffset = coords.positionInRoot()
                }
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 12.dp, end = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1A1A1A),
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Playlists (${workingPlaylists.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxGridHeight)
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .verticalScroll(scrollState, enabled = pressedId == null)
                        ) {
                            val tileWidth = (maxWidth - hGap * 2) / 3f
                            val tileHeight = tileWidth + 30.dp

                            val tileWidthPx = with(density) { tileWidth.toPx() }
                            val tileHeightPx = with(density) { tileHeight.toPx() }
                            val hGapPx = with(density) { hGap.toPx() }
                            val vGapPx = with(density) { vGap.toPx() }
                            val stepXPx = tileWidthPx + hGapPx
                            val stepYPx = tileHeightPx + vGapPx

                            val rows = ((workingPlaylists.size + 2) / 3).coerceAtLeast(1)
                            val totalHeight = tileHeight * rows + vGap * (rows - 1).coerceAtLeast(0)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(totalHeight)
                                    .onGloballyPositioned { coords ->
                                        contentRootOffset = coords.positionInRoot()
                                    }
                                    .pointerInput(
                                        tileWidthPx,
                                        tileHeightPx,
                                        isPlaylistSelectionMode
                                    ) {
                                        val touchSlop = viewConfiguration.touchSlop
                                        val longPressTimeout =
                                            viewConfiguration.longPressTimeoutMillis

                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val hitCol = (down.position.x / stepXPx).toInt()
                                            val hitRow = (down.position.y / stepYPx).toInt()
                                            val inCol =
                                                hitCol in 0..2 && (down.position.x - hitCol * stepXPx) <= tileWidthPx
                                            val inRow =
                                                hitRow >= 0 && (down.position.y - hitRow * stepYPx) <= tileHeightPx
                                            val hitIndex = hitRow * 3 + hitCol
                                            if (!inCol || !inRow || hitIndex !in workingPlaylists.indices) {
                                                return@awaitEachGesture
                                            }

                                            val hitPlaylist = workingPlaylists[hitIndex]
                                            var liftedBeforeTimeout = false
                                            var movedBeforeTimeout = false
                                            var lastPos = down.position

                                            val longPressed = withTimeoutOrNull(longPressTimeout) {
                                                while (true) {
                                                    val event =
                                                        awaitPointerEvent(PointerEventPass.Main)
                                                    val change =
                                                        event.changes.firstOrNull { it.id == down.id }
                                                    if (change == null || !change.pressed) {
                                                        liftedBeforeTimeout = true
                                                        break
                                                    }
                                                    lastPos = change.position
                                                    if ((change.position - down.position).getDistance() > touchSlop) {
                                                        movedBeforeTimeout = true
                                                        break
                                                    }
                                                }
                                                false
                                            } == null

                                            if (liftedBeforeTimeout && !movedBeforeTimeout) {
                                                latestOnClick(hitPlaylist)
                                                return@awaitEachGesture
                                            }
                                            if (!longPressed || isPlaylistSelectionMode) {
                                                return@awaitEachGesture
                                            }

                                            // Долгое нажатие сработало — сначала открываем меню
                                            val startSlotX = hitCol * stepXPx
                                            val startSlotY = hitRow * stepYPx
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            pressedId = hitPlaylist.id
                                            isDragging = false
                                            cumulativeDrag = Offset.Zero
                                            dragStartSlotOffset = Offset(startSlotX, startSlotY)
                                            dragTileWidthDp = tileWidth
                                            dragTileHeightDp = tileHeight

                                            latestOnLongClick(
                                                hitPlaylist,
                                                PlaylistAnchor(
                                                    x = contentRootOffset.x + startSlotX,
                                                    y = contentRootOffset.y + startSlotY,
                                                    widthPx = tileWidthPx.roundToInt()
                                                )
                                            )

                                            val dragOriginPos = lastPos
                                            var didReorder = false
                                            var lastSwapFromIndex = -1
                                            var lastSwapPos = Offset.Zero

                                            try {
                                                while (true) {
                                                    val event =
                                                        awaitPointerEvent(PointerEventPass.Initial)
                                                    val change =
                                                        event.changes.firstOrNull { it.id == down.id }
                                                    if (change == null || !change.pressed) {
                                                        if (change != null && isDragging) {
                                                            change.consume()
                                                        }
                                                        break
                                                    }


                                                    change.consume()
                                                    val delta = change.position - dragOriginPos

                                                    if (!isDragging) {
                                                        if (delta.getDistance() < dragSlopPx) continue
                                                        isDragging = true
                                                        latestOnCancelMenu()
                                                    }

                                                    cumulativeDrag = delta

                                                    val currentIndex =
                                                        workingPlaylists.indexOfFirst { it.id == hitPlaylist.id }
                                                    if (currentIndex < 0) continue

                                                    val centerX =
                                                        startSlotX + tileWidthPx / 2f + delta.x
                                                    val centerY =
                                                        startSlotY + tileHeightPx / 2f + delta.y

                                                    val totalRows =
                                                        ((workingPlaylists.size + 2) / 3).coerceAtLeast(
                                                            1
                                                        )
                                                    val targetCol =
                                                        ((centerX + hGapPx / 2f) / stepXPx)
                                                            .toInt()
                                                            .coerceIn(0, 2)
                                                    val targetRow =
                                                        ((centerY + vGapPx / 2f) / stepYPx)
                                                            .toInt()
                                                            .coerceIn(0, totalRows - 1)
                                                    val targetIndex = (targetRow * 3 + targetCol)
                                                        .coerceIn(0, workingPlaylists.lastIndex)

                                                    if (targetIndex != currentIndex) {
                                                        val currentCenter = Offset(centerX, centerY)
                                                        val bouncingBack =
                                                            targetIndex == lastSwapFromIndex &&
                                                                    (currentCenter - lastSwapPos).getDistance() < hysteresisPx
                                                        if (!bouncingBack) {
                                                            lastSwapFromIndex = currentIndex
                                                            lastSwapPos = currentCenter
                                                            val mutable =
                                                                workingPlaylists.toMutableList()
                                                            val movedItem =
                                                                mutable.removeAt(currentIndex)
                                                            mutable.add(targetIndex, movedItem)
                                                            workingPlaylists = mutable
                                                            didReorder = true
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.TextHandleMove
                                                            )
                                                        }
                                                    }
                                                }
                                            } finally {
                                                pressedId = null
                                                isDragging = false
                                                cumulativeDrag = Offset.Zero
                                                if (didReorder) {
                                                    latestOnReorder(workingPlaylists)
                                                }
                                            }
                                        }
                                    }
                            ) {
                                workingPlaylists.forEachIndexed { index, playlist ->
                                    key(playlist.id) {
                                        val row = index / 3
                                        val col = index % 3
                                        val targetOffset = IntOffset(
                                            (col * stepXPx).roundToInt(),
                                            (row * stepYPx).roundToInt()
                                        )
                                        val animatedOffset by animateIntOffsetAsState(
                                            targetValue = targetOffset,
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = 500f
                                            ),
                                            label = "playlistTilePos"
                                        )
                                        val isHiddenSlot = isDragging && playlist.id == pressedId

                                        Box(
                                            modifier = Modifier
                                                .offset { animatedOffset }
                                                .width(tileWidth)
                                                .height(tileHeight)
                                                .graphicsLayer {
                                                    alpha = if (isHiddenSlot) 0f else 1f
                                                }
                                        ) {
                                            PlaylistTile(
                                                playlist = playlist,
                                                isCurrent = playlist.id == playingPlaylistId,
                                                isPlaying = isPlaying,
                                                playlistTracks = tracksByPlaylist[playlist.id].orEmpty(),
                                                isSelectionMode = isPlaylistSelectionMode,
                                                isSelected = playlist.id in selectedPlaylistIds,
                                                enabled = false,
                                                onClick = {},
                                                onLongClick = null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                PlaylistCollapseTab(
                    color = Color(0xFF1A1A1A),
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 24.dp)
                        .offset(y = (-1).dp)
                )
            }

            if (isDragging && pressedId != null) {
                val draggedPlaylist = workingPlaylists.find { it.id == pressedId }
                if (draggedPlaylist != null) {
                    val originX = contentRootOffset.x - overlayRootOffset.x + dragStartSlotOffset.x
                    val originY = contentRootOffset.y - overlayRootOffset.y + dragStartSlotOffset.y

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (originX + cumulativeDrag.x).roundToInt(),
                                    (originY + cumulativeDrag.y).roundToInt()
                                )
                            }
                            .width(dragTileWidthDp)
                            .height(dragTileHeightDp)
                            .graphicsLayer {
                                scaleX = 1.06f
                                scaleY = 1.06f
                            }
                    ) {
                        PlaylistTile(
                            playlist = draggedPlaylist,
                            playlistTracks = tracksByPlaylist[draggedPlaylist.id].orEmpty(),
                            isCurrent = draggedPlaylist.id == playingPlaylistId,
                            isPlaying = isPlaying,
                            isElevated = true,
                            enabled = false,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PlaylistContextMenuOverlay(
    playlist: Playlist?,
    playlistTracks: List<Track>,
    anchor: PlaylistAnchor,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    onDismiss: () -> Unit,
    onSelect: (Playlist) -> Unit,
    onEdit: (Playlist) -> Unit,
    onDisband: (Playlist) -> Unit,
    onShare: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit
) {
    AnimatedVisibility(
        visible = playlist != null,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(100))
    ) {
        playlist?.let { selected ->
            BackHandler(onBack = onDismiss)

            val density = LocalDensity.current
            val config = LocalConfiguration.current
            val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

            val minTopPx = with(density) { 48.dp.toPx() }
            val bottomReservePx = with(density) { 410.dp.toPx() }
            val maxTopPx = (screenHeightPx - bottomReservePx).coerceAtLeast(minTopPx)
            val clampedY = anchor.y.coerceIn(minTopPx, maxTopPx).roundToInt()

            val cardWidthDp = if (anchor.widthPx > 0) {
                with(density) { anchor.widthPx.toDp() }
            } else {
                104.dp
            }
            val menuWidthDp = 160.dp
            val menuWidthPx = with(density) { menuWidthDp.toPx() }
            val edgePadPx = with(density) { 12.dp.toPx() }

            val cardCenterX = anchor.x + anchor.widthPx / 2f
            val rawMenuOffsetX = when {
                cardCenterX < screenWidthPx / 3f -> 0
                cardCenterX > screenWidthPx * 2f / 3f -> (anchor.widthPx - menuWidthPx).roundToInt()
                else -> ((anchor.widthPx - menuWidthPx) / 2f).roundToInt()
            }
            val minOffsetX = (-anchor.x + edgePadPx).roundToInt()
            val maxOffsetX = (screenWidthPx - anchor.x - menuWidthPx - edgePadPx).roundToInt()
            val menuOffsetX = rawMenuOffsetX.coerceIn(minOffsetX, maxOffsetX.coerceAtLeast(minOffsetX))

            val liftScale by animateFloatAsState(
                targetValue = if (playlist != null) 1.04f else 1f,
                label = "playlistCardLift"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
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
                        PlaylistTile(
                            isCurrent = isCurrent, isPlaying = isPlaying,
                            playlist = selected,
                            playlistTracks = playlistTracks,
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
                            PlaylistMenuRow("Select", Icons.Rounded.CheckCircleOutline) {
                                onDismiss()
                                onSelect(selected)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            PlaylistMenuRow("Edit", Icons.Filled.Edit) {
                                onDismiss()
                                onEdit(selected)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            PlaylistMenuRow("Disband", Icons.Rounded.Unarchive) {
                                onDismiss()
                                onDisband(selected)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            PlaylistMenuRow("Share", Icons.AutoMirrored.Rounded.Shortcut) {
                                onDismiss()
                                onShare(selected)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            PlaylistMenuRow("Delete", Icons.Filled.Delete, Color(0xFFFF5252)) {
                                onDismiss()
                                onDelete(selected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistMenuRow(
    text: String,
    icon: ImageVector,
    color: Color = Color.White,
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
        Text(text, color = color, fontSize = 15.sp)
        Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(19.dp))
    }
}

/**
 * Диалог выбора существующего плейлиста или создания нового для добавления/переноса треков.
 */
@Composable
fun PlaylistPickerDialog(
    visible: Boolean,
    title: String,
    playlists: List<Playlist>,
    tracksByPlaylist: Map<String, List<Track>>,
    excludePlaylistId: String? = null,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNewClick: () -> Unit
) {
    if (!visible) return

    val available = remember(playlists, excludePlaylistId) {
        playlists.filter { it.id != excludePlaylistId }
    }

    DarkDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = "Cancel",
        confirmColor = Color(0xFFB3B3B3),
        dismissText = null,
        onConfirm = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1DB954).copy(alpha = 0.14f))
                    .clickable(onClick = onCreateNewClick)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1DB954)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "New playlist",
                    color = Color(0xFF1DB954),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (available.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(available, key = { it.id }) { playlist ->
                        val pTracks = tracksByPlaylist[playlist.id].orEmpty()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectPlaylist(playlist) }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlaylistCover(
                                playlist = playlist,
                                playlistTracks = pTracks,
                                cornerRadiusDp = 8,
                                showCountBadge = false,
                                modifier = Modifier.size(42.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pTracks.size} ${if (pTracks.size == 1) "track" else "tracks"}",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 12.sp
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
fun CreateOrEditPlaylistDialog(
    visible: Boolean,
    existingPlaylists: List<Playlist>,
    initialPlaylist: Playlist? = null,
    initialTracks: List<Track> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, coverUri: Uri?, removeCover: Boolean, restoreAutoCover: Boolean) -> Unit
) {
    if (!visible) return

    var name by remember(initialPlaylist?.id) { mutableStateOf(initialPlaylist?.name.orEmpty()) }
    val hasTrackCovers = remember(initialTracks) {
        initialTracks.any { it.coverFile?.exists() == true }
    }
    var pickedCoverUri by remember(initialPlaylist?.id) { mutableStateOf<Uri?>(null) }
    var coverRemoved by remember(initialPlaylist?.id) {
        mutableStateOf(initialPlaylist?.hideAutoCover == true)
    }
    var clearedExistingCustom by remember(initialPlaylist?.id) { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedCoverUri = uri
            coverRemoved = false
            clearedExistingCustom = false
        }
    }

    val trimmed = name.trim()
    val nameTaken = PlaylistStorage.isNameTaken(
        playlists = existingPlaylists,
        name = trimmed,
        excludeId = initialPlaylist?.id
    )
    val canConfirm = trimmed.isNotEmpty() && !nameTaken

    val showingCustom = pickedCoverUri != null ||
            (!coverRemoved && !clearedExistingCustom && initialPlaylist?.coverFile?.exists() == true)
    val showingAuto = !showingCustom && !coverRemoved && hasTrackCovers
    val hasVisibleCover = showingCustom || showingAuto

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        errorTextColor = Color.White,
        cursorColor = Color(0xFF1DB954),
        errorCursorColor = Color(0xFFFF5252),
        focusedBorderColor = Color(0xFF1DB954),
        unfocusedBorderColor = Color(0xFF444444),
        errorBorderColor = Color(0xFFFF5252),
        focusedLabelColor = Color(0xFF1DB954),
        unfocusedLabelColor = Color(0xFFB3B3B3),
        errorLabelColor = Color(0xFFFF5252)
    )

    DarkDialog(
        onDismissRequest = onDismiss,
        title = if (initialPlaylist == null) "New playlist" else "Edit playlist",
        confirmText = if (initialPlaylist == null) "Create" else "Save",
        confirmEnabled = canConfirm,
        onConfirm = {
            onConfirm(trimmed, pickedCoverUri, coverRemoved, !coverRemoved && clearedExistingCustom)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    showingCustom && initialPlaylist?.coverFile != null -> {
                        AsyncImage(
                            model = initialPlaylist.coverFile,
                            contentDescription = "Playlist cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    showingAuto -> {
                        PlaylistCover(
                            playlist = (initialPlaylist ?: Playlist("", "")).copy(
                                coverFile = null,
                                hideAutoCover = false
                            ),
                            playlistTracks = initialTracks,
                            showCountBadge = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
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

            if (hasVisibleCover) {
                TextButton(
                    onClick = {
                        pickedCoverUri = null
                        coverRemoved = true
                        clearedExistingCustom = true
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Remove cover",
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp
                    )
                }
            } else if (hasTrackCovers) {
                TextButton(
                    onClick = {
                        pickedCoverUri = null
                        coverRemoved = false
                        clearedExistingCustom = true
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Use auto cover",
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                isError = nameTaken,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            if (nameTaken) {
                Text(
                    text = "A playlist with this name already exists",
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DisbandPlaylistDialog(
    playlist: Playlist?,
    trackCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Playlist) -> Unit
) {
    playlist?.let { target ->
        val trackWord = if (trackCount == 1) "track" else "tracks"
        DarkDialog(
            onDismissRequest = onDismiss,
            title = "Disband playlist?",
            confirmText = "Disband",
            onConfirm = { onConfirm(target) }
        ) {
            Text(
                "Remove playlist \"${target.name}\" and return $trackCount $trackWord back to the main list? Audio files will not be deleted."
            )
        }
    }
}

@Composable
fun DeletePlaylistsDialog(
    playlists: List<Playlist>,
    totalTrackCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (playlists.isEmpty()) return
    val trackWord = if (totalTrackCount == 1) "audio file" else "audio files"
    val message = if (playlists.size == 1) {
        "Delete playlist \"${playlists.first().name}\" and permanently delete $totalTrackCount $trackWord from your device?"
    } else {
        "Delete ${playlists.size} selected playlists and permanently delete $totalTrackCount $trackWord from your device?"
    }

    DarkDialog(
        onDismissRequest = onDismiss,
        title = if (playlists.size == 1) "Delete playlist?" else "Delete playlists?",
        confirmText = "Delete all",
        confirmColor = Color(0xFFFF5252),
        onConfirm = onConfirm
    ) {
        Text(message)
    }
}

@Composable
fun PlaylistDestinationSelector(
    playlists: List<Playlist>,
    selectedPlaylistId: String?,
    enabled: Boolean = true,
    onSelect: (String?) -> Unit
) {
    if (playlists.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val selectedName = playlists.find { it.id == selectedPlaylistId }?.name ?: "No playlist"

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF282828))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .then(if (enabled) Modifier.clickable { expanded = true } else Modifier)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = if (selectedPlaylistId != null) Color(0xFF1DB954) else Color(0xFF9E9E9E),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text("Save to", color = Color(0xFF9E9E9E), fontSize = 11.sp)
                    Text(
                        text = selectedName,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF252525)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "No playlist",
                        color = if (selectedPlaylistId == null) Color(0xFF1DB954) else Color.White
                    )
                },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            playlists.forEach { playlist ->
                DropdownMenuItem(
                    text = {
                        Text(
                            playlist.name,
                            color = if (selectedPlaylistId == playlist.id) Color(0xFF1DB954) else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(playlist.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCollapseTab(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(56.dp)
            .height(26.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val flare = 8.dp.toPx()
            val bottomRadius = 10.dp.toPx()

            val path = Path().apply {
                moveTo(0f, 0f)
                cubicTo(
                    flare * 0.65f, 0f,
                    flare, flare * 0.35f,
                    flare + 2.dp.toPx(), h - bottomRadius
                )
                quadraticTo(
                    flare + 3.dp.toPx(), h,
                    flare + 3.dp.toPx() + bottomRadius, h
                )
                lineTo(w - flare - 3.dp.toPx() - bottomRadius, h)
                quadraticTo(
                    w - flare - 3.dp.toPx(), h,
                    w - flare - 2.dp.toPx(), h - bottomRadius
                )
                cubicTo(
                    w - flare, flare * 0.35f,
                    w - flare * 0.65f, 0f,
                    w, 0f
                )
                close()
            }
            drawPath(path = path, color = color)
        }

        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = "Collapse playlists",
            tint = Color(0xFFB3B3B3),
            modifier = Modifier
                .padding(bottom = 2.dp)
                .size(22.dp)
        )
    }
}