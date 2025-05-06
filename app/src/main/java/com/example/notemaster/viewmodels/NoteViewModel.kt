package com.example.notemaster.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notemaster.data.Note
import com.example.notemaster.database.NoteDao
import com.example.notemaster.data.ContentItem
import com.example.notemaster.data.ItemText
import com.example.notemaster.data.Reminder
import com.example.notemaster.database.NoteEntity
import com.example.notemaster.repositories.NotificationRepository
import com.example.notemaster.database.toEntity
import com.example.notemaster.database.toNote
import com.example.notemaster.pages.evaluateExpression
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.math.min
import kotlin.text.forEachIndexed
import kotlin.text.take

sealed interface NoteUiState {
    object Loading : NoteUiState
    class Success() : NoteUiState
    class Error(val message: String)    : NoteUiState
}

class NotePageViewModel(
    application: Application,
    private val noteDao: NoteDao,
    private val noteId: Int
) : AndroidViewModel(application) {

    private val notificationRepo = NotificationRepository(application)

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()

    private val _itemIndexInList = MutableStateFlow<Int>(0)
    val itemIndexInList: StateFlow<Int> = _itemIndexInList.asStateFlow()

    private val _indexInItem = MutableStateFlow<Int>(0)
    val indexInItem: StateFlow<Int> = _indexInItem.asStateFlow()


    private val _canCalculate = MutableStateFlow(false)
    val canCalculate: StateFlow<Boolean> = _canCalculate.asStateFlow()

    private val _changeNeeded = MutableStateFlow(false)
    val changeNeeded: StateFlow<Boolean> = _changeNeeded.asStateFlow()

    //private val _cursorStart = MutableStateFlow<Int>(0)
    //val cursorStart: StateFlow<Int> = _cursorStart.asStateFlow()

    var makeCalculations: () -> Unit  = {}

    private val _uiState = MutableStateFlow<NoteUiState>(NoteUiState.Loading)
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            noteDao.getNoteByIdFlow(noteId)
                .map<NoteEntity?, NoteUiState> { entity ->
                    entity?.toNote()
                        ?.let {
                            _note.value = it
                            NoteUiState.Success() }
                        ?: NoteUiState.Error("Нотатку не знайдено")
                }
                .onStart {
                    emit(NoteUiState.Loading)
                }
                .catch { e ->
                    emit(NoteUiState.Error(e.localizedMessage ?: "Невідома помилка"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }


    fun updateLastEdit(){
        _note.value = _note.value!!.copy(lastEdit = LocalDateTime.now())
    }
    fun addItem(index: Int, item: ContentItem) {
        val old = _note.value!!
        val updated = old.copy()
        updated.content.addComponent(index, item)
        _note.value = updated
        updateLastEdit()
        updateNote()
    }
    fun removeItemAt(index: Int) {
        val list = _note.value!!.content.list
        list.removeAt(index)
        if(index > 0 && list[index - 1] is ItemText && list[index] is ItemText){
            (list[index - 1] as ItemText).text += "\n" + (list[index] as ItemText).text
            list.removeAt(index)
        }
        _note.value!!.content.list = list
        updateLastEdit()
        updateNote()
    }
    fun setName(newName: String) {
        val old = _note.value!!
        val updated = old.copy(
            name = newName
        )
        _note.value = updated
        updateLastEdit()
        refreshNotification()
        updateNote()
    }
    fun getContentListSize(): Int{
        return note.value!!.content.list.size
    }
    fun setReminder(r: Reminder?) {
        val old = _note.value!!
        val updated = old.copy(
            reminder = r
        )
        _note.value = updated
        refreshNotification()
        updateNote()
    }
    fun toggleSecret(){
        val old = _note.value!!
        val updated = old.copy(
            isSecret = !old.isSecret
        )
        _note.value = updated
        updateNote()
    }

    fun refreshNotification() {
        notificationRepo.cancel(_note.value!!.id.hashCode())
        if(_note.value!!.reminder != null) {
            notificationRepo.schedule(
                _note.value!!.id.hashCode(),
                "Нагадування: ${_note.value!!.name}",
                if (_note.value!!.reminder!!.descrition.isBlank()) "Перегляньте нотатку." else _note.value!!.reminder!!.descrition,
                _note.value!!.reminder!!.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        }
    }

    fun getFocusedItem(): ContentItem {
        return note.value!!.content.list[_itemIndexInList.value]
    }
    fun setItemIndexInList(i: Int){
        _itemIndexInList.value = i
    }
    fun setIndexInItem(i: Int){
        _indexInItem.value = i
    }
    fun setCanCalculate(b: Boolean){
        _canCalculate.value = b
    }
    fun setChangeNeeded(b: Boolean){
        _changeNeeded.value = b
    }

    fun checkCalculate() {
        var startOfLine = 0
        val item = _note.value!!.content.list[_itemIndexInList.value] as ItemText
        val selectionStart = _indexInItem.value
        item.text
            .take(selectionStart)
            .forEachIndexed { index, c ->
                if (c == '\n')
                    startOfLine = index + 1
            }
        var checkableTextLine = item.text.substring(startOfLine, max(0, min(selectionStart, item.text.length)))

        //тіко підходящі символи
        var startOfExpressionInLine = 0
        var checkableText = checkableTextLine
        for((index, c) in checkableTextLine.reversed().withIndex()){
            if(!(c.isDigit() || c=='+' || c=='-' || c=='*' || c=='/' || c=='(' || c==')' || c==' ' || c=='.' || c==',')) {
                checkableText = checkableTextLine.substring(checkableTextLine.length - index, checkableTextLine.length)
                startOfExpressionInLine = checkableTextLine.length - index
                break
            }
        }
        if(startOfExpressionInLine != 0) {
            if (checkableText.indexOf(' ') == -1) {
                checkableText = "no"
            } else {
                checkableText = checkableText.substring(checkableText.indexOf(' '), checkableText.length)
            }
        }


        try {
            var result = evaluateExpression(checkableText)
            setCanCalculate(true)
            val start = selectionStart
            val suffix = " = ${result} "
            makeCalculations = {
                val newText = buildString {
                    append(item.text.substring(0, start))
                    append(suffix)
                    append(item.text.substring(start))
                }
                setIndexInItem(start + suffix.length)

                //?
                //FocusedItem.updateValue(newText)
                setChangeNeeded(true)
                item.text = newText
            }
            setCanCalculate(true)
            //?
            //FocusedItem.updateCalculator(true)

        } catch (e: Exception){
            setCanCalculate(false)
            //?
            //FocusedItem.updateCalculator(false)
        }
    }



    fun saveNoteOnKeyboardHide() {
        _note.value?.let { saveNote(it) }
    }

    fun updateNote(n: Note = note.value!!){
        saveNote(n)
    }

    private fun saveNote(n: Note) {
        viewModelScope.launch {
            noteDao.update(n.toEntity())
        }
    }

    fun deleteReminder() {
        _note.value?.let { n ->
            val cleared = n.copy(reminder = null)
            _note.value = cleared
            saveNote(cleared)
            notificationRepo.cancel(noteId.hashCode())
        }
    }

    fun scheduleReminder(triggerAtMillis: Long, descr: String) {
        _note.value?.let { n ->
            val withRem = n.copy(
                reminder  = Reminder(
                    date = LocalDateTime.ofEpochSecond(triggerAtMillis / 1000, 0, ZoneOffset.UTC),
                    descrition = descr
                )
            )
            _note.value = withRem
            saveNote(withRem)
            notificationRepo.schedule(
                noteId.hashCode(),
                "Нагадування: ${withRem.name}",
                descr.ifBlank { "Перегляньте нотатку." },
                triggerAtMillis
            )
        }
    }



}


class NotePageViewModelFactory(
    private val application: Application,
    private val noteDao: NoteDao,
    private val noteId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotePageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotePageViewModel(application, noteDao, noteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}