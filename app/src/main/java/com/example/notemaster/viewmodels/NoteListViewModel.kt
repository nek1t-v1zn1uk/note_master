package com.example.notemaster.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notemaster.data.Content
import com.example.notemaster.data.Folder
import com.example.notemaster.data.ItemText
import com.example.notemaster.data.Note
import com.example.notemaster.database.NoteDao
import com.example.notemaster.data.QuickNote
import com.example.notemaster.database.FolderDao
import com.example.notemaster.database.QuickNoteDao
import com.example.notemaster.repositories.NotificationRepository
import com.example.notemaster.database.toEntity
import com.example.notemaster.database.toFolder
import com.example.notemaster.database.toFolderEntity
import com.example.notemaster.database.toNote
import com.example.notemaster.database.toQuickNote
import com.example.notemaster.database.toQuickNoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class NoteListViewModel(
    application: Application,
    private val noteDao: NoteDao,
    private val quickNoteDao: QuickNoteDao,
    private val folderDao: FolderDao
) : AndroidViewModel(application) {

    private val notificationRepo = NotificationRepository(application)


    // перетворимо Flow<Entity → Flow<DomainModel>
    val allNotes: StateFlow<List<Note>> =
        noteDao.getAllNotesFlow()
            .map { list -> list.map { it.toNote() } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val quickNotes: StateFlow<List<QuickNote>> =
        quickNoteDao.getAllQuickNotesFlow()
            .map { list -> list.map { it.toQuickNote() } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val folders: StateFlow<List<Folder>> =
        folderDao.getAllFoldersFlow()
            .map { list -> list.map { it.toFolder() } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // UI-стан флагів
    private val _isQuickNotes = MutableStateFlow(false)
    val isQuickNotes: StateFlow<Boolean> = _isQuickNotes.asStateFlow()

    private val _isSecret = MutableStateFlow(false)
    val isSecret: StateFlow<Boolean> = _isSecret.asStateFlow()

    private val _isCheckMode = MutableStateFlow(false)
    val isCheckMode: StateFlow<Boolean> = _isCheckMode.asStateFlow()

    private val _selectedItemsIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedItemsIds: StateFlow<Set<Int>> = _selectedItemsIds.asStateFlow()

    private val _selectedFoldersIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedFoldersIds: StateFlow<Set<Int>> = _selectedFoldersIds.asStateFlow()

    // Методи для керування станом
    fun getQuickNoteById(id: Int): QuickNote? { return quickNotes.value.find { it.id == id } }
    fun toggleQuickNotes() = _isQuickNotes.update { !it }
    fun moveToQuickNotes() { _isQuickNotes.value = true }
    fun moveToNotes() { _isQuickNotes.value = false }
    fun toggleSecret()      = _isSecret.update    { !it }
    fun enterSecretMode() { _isSecret.value = true }
    fun exitSecretMode() { _isSecret.value = false}
    fun enterCheckMode() { _isCheckMode.value = true }
    fun exitCheckMode()     {
        _isCheckMode.value = false
        _selectedItemsIds.value = emptySet()
        _selectedFoldersIds.value = emptySet()
    }
    fun selectItem(id: Int)     = _selectedItemsIds.update { it + id }
    fun deselectItem(id: Int)   = _selectedItemsIds.update { it - id }
    fun selectFolder(id: Int)     = _selectedFoldersIds.update { it + id }
    fun deselectFolder(id: Int)   = _selectedFoldersIds.update { it - id }

    fun convertQuickNotesToNotes() {
        viewModelScope.launch {
            for (id in _selectedItemsIds.value) {
                val item = getQuickNoteById(id)!!
                addNote(
                    Note(
                        name = "Швидка нотатка",
                        content = Content(mutableListOf(ItemText(item.text))),
                        lastEdit = LocalDateTime.now()
                    )
                )
                deleteQuickNoteById(id)
            }
            exitCheckMode()
            moveToNotes()
        }
    }
    fun deleteSelected(){
        viewModelScope.launch {
            if(!_isQuickNotes.value){
                for (id in _selectedItemsIds.value) {
                    deleteNoteById(id)
                    notificationRepo.cancel(id.hashCode())
                }
                for (id in _selectedFoldersIds.value) {
                    deleteFolder(id)
                }
            }
            else{
                for (id in _selectedItemsIds.value)
                    deleteQuickNoteById(id)
            }
            exitCheckMode()
        }
    }
    fun addSelectedToFolder(fId: Int?){
        viewModelScope.launch {
            for (id in _selectedItemsIds.value) {
                allNotes.value.find{ it.id == id }!!.folderId = fId
                updateNote(allNotes.value.find{ it.id == id }!!)
            }
            exitCheckMode()
        }
    }

    //CRUD
    fun addNote(note: Note) {
        viewModelScope.launch {
            noteDao.insert(note.toEntity())
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.update(note.toEntity())
        }
    }

    fun deleteNoteById(id: Int) {
        viewModelScope.launch {
            noteDao.deleteNoteById(id)
        }
    }

    fun addQuickNote(qnote: QuickNote) {
        viewModelScope.launch {
            quickNoteDao.insert(qnote.toQuickNoteEntity())
        }
    }
    fun deleteQuickNoteById(id: Int) {
        viewModelScope.launch {
            quickNoteDao.deleteNoteById(id)
        }
    }

    fun addFolder(f: Folder){
        viewModelScope.launch {
            folderDao.insert(f.toFolderEntity())
        }
    }
    fun deleteFolder(fId: Int){
        viewModelScope.launch {
            folderDao.deleteFolderById(fId)
        }
    }

}


class NoteListViewModelFactory(
    private val application: Application,
    private val noteDao: NoteDao,
    private val quickNoteDao: QuickNoteDao,
    private val folderDao: FolderDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteListViewModel(application, noteDao, quickNoteDao, folderDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
