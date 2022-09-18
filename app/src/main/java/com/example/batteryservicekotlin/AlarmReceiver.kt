package com.example.batteryservicekotlin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

private const val CHANNEL_ID = "channelId"

class AlarmReceiver : BroadcastReceiver() {

    private val LOG_TAG = "myLogs"

    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onReceive(context: Context, intent: Intent?) {
        notificationManagerCompat = NotificationManagerCompat.from(context)
        createNotificationChannel(context)

        Log.d(LOG_TAG, "onReceive")
        Log.d(LOG_TAG, "action = ${intent?.action}")
        Log.d(LOG_TAG, "extra = ${intent?.getStringExtra("extra")}")

        sendNotification(1, context)
    }

    private fun createNotificationChannel(context: Context?) {
        val name = "myChannel"
        val descriptionText = "descriptionText"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        //val notificationManager: NotificationManager = getSystemService(context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationManager: NotificationManager? = context?.getSystemService()
        notificationManager?.createNotificationChannel(channel)
    }

    private fun sendNotification(id: Int, context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Title $id")
            .setContentText("Content $id")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        with(notificationManagerCompat) {
            notify(id, notificationBuilder.build())
        }
    }
}