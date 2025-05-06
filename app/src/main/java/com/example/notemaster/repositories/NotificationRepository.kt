package com.example.notemaster.repositories

import android.content.Context
import com.example.notemaster.*

class NotificationRepository(private val context: Context) {
    fun cancel(id: Int) {
        cancelNotification(context, id)
    }

    fun schedule(id: Int, title: String, text: String, at: Long) {
        scheduleNotification(context, id, title, text, at)
    }
}
