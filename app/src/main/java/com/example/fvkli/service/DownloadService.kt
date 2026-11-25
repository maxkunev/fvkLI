package com.example.fvkli.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.fvkli.MainActivity
import com.example.fvkli.data.AudioDownloader
import com.example.fvkli.data.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.example.fvkli.data.ImportTrackRequest

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var downloadJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                AudioDownloader.cancelDownload(this)
                return START_NOT_STICKY
            }
            ACTION_START_IMPORT -> {
                val items = pendingImportItems
                pendingImportItems = emptyList()
                if (items.isEmpty()) {
                    if (!AudioDownloader.downloadState.value.isLoading) stopSelf()
                    return START_NOT_STICKY
                }
                if (downloadJob?.isActive == true) {
                    return START_NOT_STICKY
                }

                startForegroundWithNotification(
                    AudioDownloader.downloadState.value.copy(
                        isLoading = true,
                        totalCount = items.size,
                        statusText = "Preparing import..."
                    )
                )
                observeDownloadState()

                downloadJob = serviceScope.launch {
                    val result = AudioDownloader.downloadImportBatch(this@DownloadService, items)
                    observerJob?.cancel()
                    ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)

                    result.onSuccess { summary ->
                        showCompletionNotification(summary)
                    }
                    stopSelf()
                }
            }
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL)?.trim().orEmpty()
                val targetPlaylistId = intent.getStringExtra(EXTRA_PLAYLIST_ID)?.takeIf { it.isNotBlank() }
                if (url.isBlank()) {
                    if (!AudioDownloader.downloadState.value.isLoading) stopSelf()
                    return START_NOT_STICKY
                }
                if (downloadJob?.isActive == true) {
                    return START_NOT_STICKY
                }

                startForegroundWithNotification(
                    AudioDownloader.downloadState.value.copy(
                        isLoading = true,
                        statusText = "Fetching info..."
                    )
                )
                observeDownloadState()

                downloadJob = serviceScope.launch {
                    val result = AudioDownloader.downloadAudio(this@DownloadService, url, targetPlaylistId)
                    observerJob?.cancel()
                    ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)

                    result.onSuccess { summary ->
                        showCompletionNotification(summary)
                    }
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun observeDownloadState() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            var lastNotifyTime = 0L
            var lastStatus = ""
            var lastCount = -1

            AudioDownloader.downloadState.collect { state ->
                if (!state.isLoading) return@collect
                val now = SystemClock.elapsedRealtime()
                val importantChange = state.statusText != lastStatus || state.downloadedCount != lastCount

                if (importantChange || now - lastNotifyTime >= 400L) {
                    lastNotifyTime = now
                    lastStatus = state.statusText
                    lastCount = state.downloadedCount
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(NOTIFICATION_ID, buildProgressNotification(state))
                }
            }
        }
    }

    private fun startForegroundWithNotification(state: DownloadState) {
        val notification = buildProgressNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildProgressNotification(state: DownloadState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (state.totalCount > 1) {
            "Downloading (${state.currentIndex}/${state.totalCount})"
        } else {
            "Downloading audio"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(state.statusText.ifBlank { "Preparing..." })
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Cancel", cancelPendingIntent)

        val p = state.progress
        if (p == null) {
            builder.setProgress(100, 0, true)
        } else {
            builder.setProgress(100, (p * 100).toInt().coerceIn(0, 100), false)
        }

        return builder.build()
    }

    private fun showCompletionNotification(summary: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            2,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText(summary)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background audio and playlist downloads"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "fvkli_downloads"
        private const val NOTIFICATION_ID = 2001
        private const val COMPLETION_NOTIFICATION_ID = 2002

        const val ACTION_START = "com.example.fvkli.action.START_DOWNLOAD"
        const val ACTION_CANCEL = "com.example.fvkli.action.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
        const val ACTION_START_IMPORT = "com.example.fvkli.action.START_IMPORT"

        @Volatile
        private var pendingImportItems: List<ImportTrackRequest> = emptyList()

        fun startImport(context: Context, items: List<ImportTrackRequest>) {
            if (items.isEmpty()) return
            pendingImportItems = items
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_IMPORT
            }
            ContextCompat.startForegroundService(context, intent)
        }
        fun start(context: Context, url: String, targetPlaylistId: String? = null) {
            val cleanUrl = url.trim()
            if (cleanUrl.isBlank()) return
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, cleanUrl)
                targetPlaylistId?.takeIf { it.isNotBlank() }?.let {
                    putExtra(EXTRA_PLAYLIST_ID, it)
                }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}