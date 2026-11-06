package com.drivepark.app.drive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.drivepark.app.DriveParkApp
import com.drivepark.app.R

/**
 * 주행 상태를 감시하는 포그라운드 서비스. 순정 내비가 앞에 있어도 앱 프로세스가 얼지 않게 한다.
 * (2026-11-05 주행 테스트: 서비스 없이 백그라운드로 가면 8초 만에 isFrozen=true)
 */
class DriveWatchService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "드라이브 대기", NotificationManager.IMPORTANCE_MIN),
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText("Waveby가 다음 드라이브를 기다리고 있어요")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        val app = application as DriveParkApp
        app.container.driveSession.start(app.applicationScope)
        if (popupStarted.compareAndSet(false, true)) app.container.drivePopup.start(app.applicationScope)
        Log.d(TAG, "drive watch started")
        return START_STICKY
    }

    companion object {
        private val popupStarted = java.util.concurrent.atomic.AtomicBoolean(false)
        private const val TAG = "DrivePark/Service"
        private const val CHANNEL_ID = "drive_watch"
        private const val NOTIFICATION_ID = 2

        /** 서비스를 시작한다. 실패하면 false(호출자가 앱 범위 실행으로 대체). */
        fun start(context: Context): Boolean = runCatching {
            ContextCompat.startForegroundService(context, Intent(context, DriveWatchService::class.java))
        }.onFailure { Log.w(TAG, "could not start service", it) }.isSuccess
    }
}
