package com.example.notemaster

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.util.Dictionary

class Note(
    var id: Int = 0,
    var name: String = "NaMe",
    var content: Content = Content(),
    var lastEdit: LocalDateTime = LocalDateTime.now(),
    initialReminder: Reminder? = null,
    var isSecret: Boolean = false
){
    var reminder: Reminder? = initialReminder
        set(value){
            field = value
            lastEdit = LocalDateTime.now()
        }
    fun hasReminder(): Boolean{
        return reminder != null
    }
}

class Content(
    var list: MutableList<ContentItem> = mutableListOf()
) {
    fun addComponent(index: Int, item: ContentItem) {
        list.add(index, item)
        if (list.last() !is ItemText)
            list.add(ItemText())
    }

    fun ensureTrailingText() {
        if (list.isEmpty() || list.last() !is ItemText) {
            addComponent(list.size, ItemText())
        }
    }

    fun getSymbolsCount(): Int {
        return list.sumOf { if (it is ItemText) it.getSymbolCount() else 0 }
    }
}
/*
abstract class ContentItem() {
    var id: Int = 0
    companion object{
        var lastId = 0
    }
    init {
        id = lastId + 1
        lastId = id
    }
}
*/
abstract class ContentItem {
    var id: Int = nextId()
    companion object {
        private var lastId = 0
        fun nextId(): Int = ++lastId
        fun resetLastId(to: Int) { lastId = to }
    }
}
/*
data class TextStyle(
    val size: Float = 18f,
    val bold: Boolean = false,
    val highlight: Boolean = false
)
 */
open class ItemText(
    open var text: String = "",
) : ContentItem() {


    fun getSymbolCount(): Int{
        return text.count { !it.isWhitespace() }
    }
}

class ItemCheckBox(
    text: String = "",
    style: MutableMap<String, Any> = mutableMapOf(
        "size" to 18.dp,
        "bold" to false,
        "highlight" to false
    ),
    hasCheckBox: Boolean = false) : ItemText(text,){

    var hasCheckBox: Boolean = false

    init {
        this.hasCheckBox = hasCheckBox
    }
}

class ItemImage(
    var uri: Uri
) : ContentItem() {

}

class ItemFile(
    val uri: Uri,
    val fileName: String
) : ContentItem(){

}

class Reminder(
    var date: LocalDateTime,
    var descrition: String = ""
)