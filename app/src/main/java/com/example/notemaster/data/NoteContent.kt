package com.example.notemaster.data

import android.net.Uri
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime


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
abstract class ContentItem {
    var id: Int = nextId()
    companion object {
        private var lastId = 0
        fun nextId(): Int = ++lastId
        fun resetLastId(to: Int) { lastId = to }
    }
}

open class ItemText(
    open var text: String = "",
) : ContentItem() {


    fun getSymbolCount(): Int{
        return text.count { !it.isWhitespace() }
    }
}


class ItemImage(
    var uri: Uri
) : ContentItem()

class ItemFile(
    val uri: Uri,
    val fileName: String
) : ContentItem()

class Reminder(
    var date: LocalDateTime,
    var descrition: String = ""
)

