package com.example.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.util.NotificationHelper
import java.io.File

class BuildForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_UPDATE = "com.example.service.action.UPDATE"
        const val ACTION_STOP = "com.example.service.action.STOP"

        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_PROJECT_NAME = "extra_project_name"
        const val EXTRA_IS_DONE = "extra_is_done"
        const val EXTRA_IS_SUCCESS = "extra_is_success"
        const val EXTRA_APK_PATH = "extra_apk_path"

        fun startService(context: Context, statusMessage: String, projectName: String = "Projeto Android") {
            NotificationHelper.setupChannels(context)
            val intent = Intent(context, BuildForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STATUS, statusMessage)
                putExtra(EXTRA_PROJECT_NAME, projectName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateStatus(context: Context, statusMessage: String) {
            val intent = Intent(context, BuildForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_STATUS, statusMessage)
            }
            context.startService(intent)
        }

        fun finishService(
            context: Context,
            isSuccess: Boolean,
            projectName: String,
            message: String,
            apkFile: File? = null
        ) {
            val intent = Intent(context, BuildForegroundService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_STATUS, message)
                putExtra(EXTRA_PROJECT_NAME, projectName)
                putExtra(EXTRA_IS_DONE, true)
                putExtra(EXTRA_IS_SUCCESS, isSuccess)
                apkFile?.let { putExtra(EXTRA_APK_PATH, it.absolutePath) }
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.setupChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val statusMessage = intent?.getStringExtra(EXTRA_STATUS) ?: "Compilando no GitHub Actions..."
        val projectName = intent?.getStringExtra(EXTRA_PROJECT_NAME) ?: "Projeto Android"
        val isDone = intent?.getBooleanExtra(EXTRA_IS_DONE, false) ?: false
        val isSuccess = intent?.getBooleanExtra(EXTRA_IS_SUCCESS, false) ?: false
        val apkPath = intent?.getStringExtra(EXTRA_APK_PATH)
        val apkFile = apkPath?.let { File(it) }

        when (action) {
            ACTION_START -> {
                val notification = buildProgressNotification(statusMessage, projectName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NotificationHelper.PROGRESS_NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NotificationHelper.PROGRESS_NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE -> {
                val notification = buildProgressNotification(statusMessage, projectName)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.notify(NotificationHelper.PROGRESS_NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                if (isDone) {
                    NotificationHelper.showCompletionAlert(
                        context = this,
                        isSuccess = isSuccess,
                        projectName = projectName,
                        message = statusMessage,
                        apkFile = apkFile
                    )
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildProgressNotification(
        message: String,
        projectName: String
    ): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.PROGRESS_CHANNEL_ID)
            .setContentTitle("Compilando: $projectName")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
