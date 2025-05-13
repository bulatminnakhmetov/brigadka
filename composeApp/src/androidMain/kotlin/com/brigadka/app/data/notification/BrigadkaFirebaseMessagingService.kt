package com.brigadka.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.brigadka.app.MainActivity
import com.brigadka.app.data.api.push.PushTokenRegistrator
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class BrigadkaFirebaseMessagingService : FirebaseMessagingService() {

    private val pushTokenRegistrator: PushTokenRegistrator by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            // Handle notification message
            sendNotification(
                title = notification.title ?: "Notification",
                body = notification.body ?: "",
                imageUrl = notification.imageUrl?.toString(),
                data = remoteMessage.data
            )
        } ?: run {
            // Handle data message if no notification payload
            if (remoteMessage.data.isNotEmpty()) {
                val title = remoteMessage.data["title"] ?: "Notification"
                val body = remoteMessage.data["body"] ?: ""
                val imageUrl = remoteMessage.data["image"]

                sendNotification(title, body, imageUrl, remoteMessage.data)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        pushTokenRegistrator.registerPushToken(token)
    }

    private fun sendNotification(
        title: String,
        body: String,
        imageUrl: String? = null,
        data: Map<String, String> = emptyMap()
    ) {
        val channelId = "brigadka_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Create intent to open app when notification is clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Add any extra data if needed
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntentFlag =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, pendingIntentFlag
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // You could add image support with Glide if needed
        // For a simple implementation, we're omitting image loading

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel for Android Oreo and above
        val channel = NotificationChannel(
            channelId,
            "Brigadka Channel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for Brigadka notifications"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        // Show notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}