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
import android.os.PowerManager
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
        // спочатку переконаємося, що маємо дозвіл
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return

        val title = intent.getStringExtra("title") ?: "Нагадування"
        val text  = intent.getStringExtra("text")  ?: ""
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

    // 1) Створюємо Intent і PendingIntent
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

    // 2) Отримуємо AlarmManager
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 3) (Android S+) перевіряємо і, за необхідності, відкриваємо діалог Exact Alarms
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
        )
        // але НЕ робимо return — даємо користувачу можливість самотужки ввімкнути…
    }

    // 4) Ставимо будильник навіть якщо немає POST_NOTIFICATIONS
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
}

fun cancelNotification(context: Context, notificationId: Int) {
    // Відтворюємо той же PendingIntent, який використовували для schedule
    val intent = Intent(context, ReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) ?: return  // якщо PendingIntent не знайдено — нічого не скасовуємо

    // Отримуємо AlarmManager і скасовуємо
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.cancel(pi)

    // Також відмічаємо сам PendingIntent як скасований
    pi.cancel()
}


// 2) Функція для запиту оптимізацій батареї
fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .apply { data = Uri.parse("package:${context.packageName}") }
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

// 3) Функція для запиту точних будильників (Android 12+)
fun requestExactAlarmsPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (!am.canScheduleExactAlarms()) {
            context.startActivity(
                Intent("android.app.action.REQUEST_SCHEDULE_EXACT_ALARM")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}