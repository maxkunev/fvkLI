package com.example.fvkli.ui.theme.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fvkli.models.Track
import com.example.fvkli.ui.theme.components.DarkDialog
import java.io.File
import java.util.Locale

private enum class LegalDoc(val title: String, val body: String) {
    PRIVACY(
        title = "Privacy Policy",
        body = "fvkLI does not collect, store, or transmit personal data, usage analytics, or telemetry.\n\n" +
                "All downloaded audio files, cover art, and track metadata are stored locally on your device. " +
                "Network requests are made only directly to the media URLs you provide."
    ),
    TERMS(
        title = "Terms of Use",
        body = "fvkLI is provided as a personal offline audio player and media utility.\n\n" +
                "You are solely responsible for ensuring that you have the right to download and store any content, " +
                "and for complying with copyright laws and the terms of service of the source platforms."
    ),
    LICENSES(
        title = "Open Source Licenses",
        body = "fvkLI uses the following open-source software:\n\n" +
                "• yt-dlp — The Unlicense\n" +
                "• FFmpeg — LGPL v2.1+ / GPL v2+\n" +
                "• youtubedl-android — GPL v3.0\n" +
                "• AndroidX Media3 (ExoPlayer) — Apache License 2.0\n" +
                "• Jetpack Compose — Apache License 2.0\n" +
                "• Coil — Apache License 2.0"
    )
}

@Composable
fun SettingsScreen(
    visible: Boolean,
    tracks: List<Track>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var activeLegalDoc by remember { mutableStateOf<LegalDoc?>(null) }
    var cacheBytes by remember { mutableLongStateOf(0L) }

    val libraryBytes = remember(tracks) {
        tracks.sumOf { track ->
            val audioSize = if (track.file.exists()) track.file.length() else 0L
            val coverSize = if (track.coverFile?.exists() == true) track.coverFile.length() else 0L
            audioSize + coverSize
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            cacheBytes = calculateDirSize(context.cacheDir)
        } else {
            activeLegalDoc = null
        }
    }

    val appVersion = remember(context) { getAppVersionLabel(context) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 180))
    ) {
        BackHandler(enabled = activeLegalDoc == null, onBack = onClose)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .systemBarsPadding()
        ) {
            // Шапка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Секция Storage
                SettingsSectionTitle("Storage")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Storage,
                        title = "Downloaded library",
                        subtitle = "${tracks.size} tracks on device",
                        trailingText = formatBytes(libraryBytes),
                        showChevron = false,
                        onClick = null
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsRow(
                        icon = Icons.Rounded.CleaningServices,
                        title = "Clear temporary cache",
                        subtitle = "Remove shared copies and temp files",
                        trailingText = formatBytes(cacheBytes),
                        showChevron = false,
                        onClick = {
                            clearCacheDir(context.cacheDir)
                            cacheBytes = calculateDirSize(context.cacheDir)
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Секция Legal
                SettingsSectionTitle("Legal")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "Privacy Policy",
                        onClick = { activeLegalDoc = LegalDoc.PRIVACY }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsRow(
                        icon = Icons.Rounded.Description,
                        title = "Terms of Use",
                        onClick = { activeLegalDoc = LegalDoc.TERMS }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsRow(
                        icon = Icons.Rounded.Gavel,
                        title = "Open Source Licenses",
                        onClick = { activeLegalDoc = LegalDoc.LICENSES }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Секция Support
                SettingsSectionTitle("Support")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = "Source Code",
                        subtitle = "github.com/maxkunev/fvkLI",
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = {
                            openUrl(context, "https://github.com/maxkunev/fvkLI")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsRow(
                        icon = Icons.Rounded.BugReport,
                        title = "Report a Bug",
                        subtitle = "Open an issue on GitHub",
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = {
                            openUrl(context, "https://github.com/maxkunev/fvkLI/issues")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingsRow(
                        icon = Icons.Rounded.ContentCopy,
                        title = "Copy Device Info",
                        subtitle = "Useful when reporting an issue",
                        showChevron = false,
                        onClick = {
                            val info = "fvkLI $appVersion • Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) • ${Build.MANUFACTURER} ${Build.MODEL}"
                            clipboardManager.setText(AnnotatedString(info))
                            Toast.makeText(context, "Device info copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(28.dp))

                // Динамический номер версии внизу
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "fvkLI • $appVersion",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    activeLegalDoc?.let { doc ->
        DarkDialog(
            onDismissRequest = { activeLegalDoc = null },
            title = doc.title,
            confirmText = "Close",
            dismissText = null,
            onConfirm = { activeLegalDoc = null }
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = doc.body,
                    color = Color(0xFFD0D0D0),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = Color(0xFF1DB954),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E1E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
            }
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                color = Color(0xFFB3B3B3),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = Color(0xFF8E8E8E),
                modifier = Modifier.size(18.dp)
            )
        } else if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF8E8E8E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getAppVersionLabel(context: Context): String {
    return runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionName = packageInfo.versionName ?: "0.1.0"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        "v$versionName ($versionCode)"
    }.getOrDefault("v0.2.1")
}

private fun calculateDirSize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var total = 0L
    dir.listFiles()?.forEach { file ->
        total += if (file.isDirectory) calculateDirSize(file) else file.length()
    }
    return total
}

private fun clearCacheDir(dir: File?) {
    if (dir == null || !dir.exists()) return
    dir.listFiles()?.forEach { child ->
        child.deleteRecursively()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 0.1 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
    }
}