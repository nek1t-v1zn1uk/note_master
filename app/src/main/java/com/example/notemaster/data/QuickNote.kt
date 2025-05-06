package com.example.notemaster.data

import java.time.LocalDateTime

class QuickNote(
    var id: Int = 0,
    var text: String = "",
    var lastEdit: LocalDateTime = LocalDateTime.now(),
)