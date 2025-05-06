package com.example.notemaster.data

import android.net.Uri
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime



class Note(
    var id: Int = 0,
    var name: String = "Нотатка",
    var content: Content = Content(),
    var lastEdit: LocalDateTime = LocalDateTime.now(),
    initialReminder: Reminder? = null,
    var isSecret: Boolean = false,
    var folderId: Int? = null
){
    var reminder: Reminder? = initialReminder
        set(value){
            field = value
            lastEdit = LocalDateTime.now()
        }
    fun hasReminder(): Boolean{
        return reminder != null
    }

    fun copy(
        id: Int = this.id,
        name: String = this.name,
        content: Content = this.content,
        lastEdit: LocalDateTime = this.lastEdit,
        reminder: Reminder? = this.reminder,
        isSecret: Boolean = this.isSecret,
        folderId: Int? = this.folderId
    ) = Note(id, name, content, lastEdit, reminder, isSecret, folderId)
}
