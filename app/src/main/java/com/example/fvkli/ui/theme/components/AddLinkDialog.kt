package com.example.fvkli.ui.theme.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun AddLinkDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    statusText: String,
    isLoading: Boolean,
    progress: Float? = null,
    onDismiss: () -> Unit,
    onMinimize: () -> Unit = {},
    onCancelClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val dialogBgColor = Color(0xFF1E1E1E)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = Color.White.copy(alpha = 0.5f),
        cursorColor = Color(0xFF1DB954),
        focusedBorderColor = Color(0xFF1DB954),
        unfocusedBorderColor = Color(0xFF444444),
        disabledBorderColor = Color(0xFF333333)
    )

    val displayStatus = statusText.ifBlank { "Waiting" }
    val statusColor = when {
        statusText.isBlank() -> Color(0xFF777777)
        statusText.startsWith("Error", ignoreCase = true) -> Color(0xFFFF5252)
        statusText.startsWith("Ready", ignoreCase = true) ||
                statusText.startsWith("Added", ignoreCase = true) -> Color(0xFF1DB954)
        else -> Color(0xFFB3B3B3)
    }

    Dialog(
        onDismissRequest = { if (isLoading) onMinimize() else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Выступающий язычок со стрелкой вниз появляется только во время скачивания
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(tween(200)) + expandVertically(
                    expandFrom = Alignment.Bottom,
                    animationSpec = tween(220)
                ),
                exit = fadeOut(tween(150)) + shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                    animationSpec = tween(180)
                )
            ) {
                MinimizeTab(
                    color = dialogBgColor,
                    onClick = onMinimize,
                    modifier = Modifier
                        .padding(end = 22.dp)
                        .offset(y = 1.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = dialogBgColor,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Add from link",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        enabled = !isLoading,
                        placeholder = {
                            Text(
                                text = "YouTube or TikTok link",
                                color = Color(0xFF777777),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            if (!isLoading) {
                                if (url.isNotEmpty()) {
                                    IconButton(onClick = { onUrlChange("") }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF9E9E9E),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.getText()?.text?.trim()?.let { pasted ->
                                                if (pasted.isNotEmpty()) onUrlChange(pasted)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = Color(0xFF9E9E9E),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = displayStatus,
                            color = statusColor,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLoading && progress != null) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = Color(0xFFB3B3B3),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    BlockProgressBar(
                        isActive = isLoading,
                        progress = progress
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { if (isLoading) onCancelClick() else onDismiss() }
                        ) {
                            Text(
                                text = "Cancel",
                                color = if (isLoading) Color(0xFFFF5252) else Color(0xFFB3B3B3)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val downloadEnabled = !isLoading && url.isNotBlank()
                        TextButton(
                            onClick = onDownloadClick,
                            enabled = downloadEnabled
                        ) {
                            Text(
                                text = "Download",
                                color = if (downloadEnabled) Color(0xFF1DB954) else Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimizeTab(
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
            val topRadius = 10.dp.toPx()

            val path = Path().apply {
                moveTo(0f, h)
                cubicTo(
                    flare * 0.65f, h,
                    flare, h - flare * 0.35f,
                    flare + 2.dp.toPx(), topRadius
                )
                quadraticTo(
                    flare + 3.dp.toPx(), 0f,
                    flare + 3.dp.toPx() + topRadius, 0f
                )
                lineTo(w - flare - 3.dp.toPx() - topRadius, 0f)
                quadraticTo(
                    w - flare - 3.dp.toPx(), 0f,
                    w - flare - 2.dp.toPx(), topRadius
                )
                cubicTo(
                    w - flare, h - flare * 0.35f,
                    w - flare * 0.65f, h,
                    w, h
                )
                close()
            }
            drawPath(path = path, color = color)
        }

        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Minimize",
            tint = Color(0xFFB3B3B3),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
        )
    }
}

@Composable
private fun BlockProgressBar(
    isActive: Boolean,
    progress: Float?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val activeAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "barActiveAlpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = if (isActive && progress != null) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "barProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "preparingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val blockAlpha = if (isActive && progress == null) {
        activeAlpha * pulseAlpha
    } else {
        activeAlpha
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val trackHeight = 10.dp
        val trackShape = RoundedCornerShape(5.dp)
        val blockWidthDp = 10.dp
        val blockWidthPx = with(density) { blockWidthDp.toPx() }
        val travelPx = (totalWidthPx - blockWidthPx).coerceAtLeast(0f)
        val blockOffsetPx = travelPx * animatedProgress
        val fillWidthDp = with(density) { (blockOffsetPx + blockWidthPx / 2f).toDp() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(trackShape)
                .background(Color.White.copy(alpha = 0.10f))
        )

        if (activeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .width(fillWidthDp)
                    .height(trackHeight)
                    .clip(trackShape)
                    .background(Color(0xFF1DB954).copy(alpha = activeAlpha))
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(blockOffsetPx.roundToInt(), 0) }
                    .width(blockWidthDp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = blockAlpha))
            )
        }
    }
}