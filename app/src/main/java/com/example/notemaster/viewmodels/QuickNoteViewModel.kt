package com.example.notemaster.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notemaster.data.QuickNote
import com.example.notemaster.database.QuickNoteDao
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.example.notemaster.database.toQuickNoteEntity

class QuickNoteViewModel(
    application: Application,
    private val quickNoteDao: QuickNoteDao
) : AndroidViewModel(application) {

    fun addQuickNote(text: String) {
        viewModelScope.launch {
            quickNoteDao.insert(
                QuickNote(text = text, lastEdit = LocalDateTime.now()).toQuickNoteEntity()
            )
        }
    }
}

class QuickNotesViewModelFactory(
    private val application: Application,
    private val quickNoteDao: QuickNoteDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickNoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuickNoteViewModel(application, quickNoteDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
