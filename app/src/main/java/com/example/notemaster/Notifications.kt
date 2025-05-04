package com.example.notemaster

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    const val CHANNEL_ID = "reminder_channel"
    const val CHANNEL_NAME = "Нагадування"
    const val CHANNEL_DESC = "Канал для нагадувань NoteMaster"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context, title: String, text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)  // ваш іконка
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
    }
}

class ReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        // Витягуємо дані з Intent
        val title = intent.getStringExtra("title") ?: "Нагадування"
        val text  = intent.getStringExtra("text")  ?: ""
        // Будуємо та показуємо Notification
        val notification = NotificationHelper.buildNotification(context, title, text)
        NotificationManagerCompat.from(context)
            .notify(intent.getIntExtra("id", 0), notification)
    }
}


fun scheduleNotification(
    context: Context,
    notificationId: Int,
    title: String,
    text: String,
    triggerAtMillis: Long
) {
    NotificationHelper.createChannel(context)

    // 1) Check POST_NOTIFICATIONS (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    // 2) Build the PendingIntent
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("id", notificationId)
        putExtra("title", title)
        putExtra("text", text)
    }
    val pi = PendingIntent.getBroadcast(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 3) Grab the AlarmManager
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 3a) On Android 12+ we **must** check exact-alarm capability:
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!am.canScheduleExactAlarms()) {
            // 3b) Redirect user into the “Exact alarms” settings page
            val req = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(req)
            return
        }
    }

    // 4) Finally schedule the one-off exact alarm
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pi
            )
        } else {
            am.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pi
            )
        }
    } catch (e: SecurityException) {
        Log.e("Reminder", "Failed to schedule exact alarm", e)
    }
}