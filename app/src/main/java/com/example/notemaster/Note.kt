package com.example.notemaster

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime

class Note(
    name: String = "NaMe",
    content: String = "CoNtEnT",
    lastEdit: LocalDateTime = LocalDateTime.now()
){
    var name: String = ""
    var content: String = ""
    var lastEdit: LocalDateTime = LocalDateTime.of(1111, 11, 11, 11, 11)

    init {
        this.name = name
        this.content = content
        this.lastEdit = lastEdit
    }

}