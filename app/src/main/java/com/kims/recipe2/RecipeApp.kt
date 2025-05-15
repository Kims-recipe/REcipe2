package com.kims.recipe2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class RecipeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Android 8 이상에서 알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default",
                "일반 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
