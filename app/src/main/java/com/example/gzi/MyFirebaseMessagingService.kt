package com.example.gzi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Вызывается при получении нового push-уведомления
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Получаем данные из полезной нагрузки (Data payload)
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "ПСГиИ"
        val message = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
        val type = remoteMessage.data["type"] ?: "chat" // тип: chat или update

        sendNotification(title, message, type)
    }

    // Вызывается при первом запуске приложения для генерации уникального токена устройства
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // В будущем этот токен можно сохранить в Firestore профиля сотрудника,
        // чтобы отправлять уведомления лично ему.
    }

    private fun sendNotification(title: String, messageBody: String, type: String) {
        // Определяем, куда перейдет пользователь при клике на push
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("navigate_to", type) // Передаем тип экрана (chat или settings)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Разделяем каналы уведомлений (требование Android 8.0+)
        val channelId = if (type == "update") "gzi_updates_channel" else "gzi_chat_channel"
        val channelName = if (type == "update") "Обновления системы" else "Сообщения чата"
        val importance = if (type == "update") NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Ваша иконка приложения
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(if (type == "update") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(channel)
        }

        // Генерируем уникальный ID, чтобы уведомления не затирали друг друга
        val notificationId = if (type == "update") 999 else System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
