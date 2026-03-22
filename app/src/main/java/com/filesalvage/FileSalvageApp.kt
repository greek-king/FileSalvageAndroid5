package com.filesalvage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class FileSalvageApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val scanChannel = NotificationChannel(
                "filesalvage_scan",
                "File Scan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background scan progress notifications"
                setShowBadge(false)
            }

            val recoveryChannel = NotificationChannel(
                "filesalvage_recovery",
                "File Recovery",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recovery completion notifications"
            }

            mgr.createNotificationChannels(listOf(scanChannel, recoveryChannel))
        }
    }
}
