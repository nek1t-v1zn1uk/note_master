package com.example.notemaster

import android.util.Log
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.util.Dictionary

class Note(
    name: String = "NaMe",
    content: Content = Content(),
    lastEdit: LocalDateTime = LocalDateTime.now()
){
    var name: String = ""
    var content: Content = Content()
    var lastEdit: LocalDateTime = LocalDateTime.of(1111, 11, 11, 11, 11)

    init {
        this.name = name
        this.content = content
        this.lastEdit = lastEdit
    }

}

class Content(list: MutableList<ContentItem> = mutableListOf()) {

    private val _list: MutableList<ContentItem> = mutableListOf()

    var list: MutableList<ContentItem>
        get() {
            if (_list.isEmpty() || _list.last() !is ItemText) {
                _list.add(ItemText())
            }
            return _list
        }
        set(value) {
            _list.clear()
            _list.addAll(value)
        }


    init{
        this.list = list
    }

    fun getSymbolsCount(): Int{
        var count = 0
        list.forEach { item ->
            if(item is ItemText)
                count += item.getSymbolCount()
        }
        return count
    }
}

abstract class ContentItem() {

}

open class ItemText(
    open var text: String = "",
    open var style: MutableMap<String, Any> = mutableMapOf(
        "size" to 18.dp,
        "bold" to false,
        "highlight" to false
    )
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

class ItemImage : ContentItem() {

}