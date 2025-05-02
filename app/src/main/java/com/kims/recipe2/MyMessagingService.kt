package com.kims.recipe2

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.ktx.Firebase
import android.Manifest
import com.kims.recipe2.R

class MyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "New token: $token")
        Firebase.auth.currentUser?.uid?.let { uid ->
            Firebase.firestore.collection("users")
                .document(uid).update("fcmToken", token)
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        msg.notification?.let {
            // ------ 권한 체크 ------
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한이 없으면 알림 생성을 건너뜀
                Log.w("FCM", "Notification skipped – permission not granted")
                return
            }

            val notification = NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(it.title)
                .setContentText(it.body)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(this).notify(0, notification)
        }
    }
}
