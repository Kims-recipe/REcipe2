package com.kims.recipe2

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.ktx.Firebase

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
            val n = NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.ic_notification) // drawable 준비
                .setContentTitle(it.title)
                .setContentText(it.body)
                .setAutoCancel(true)
                .build()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            NotificationManagerCompat.from(this).notify(0, n)
        }
    }
}

