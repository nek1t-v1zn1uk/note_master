package com.example.notemaster.database

import android.net.Uri
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import android.util.Log
import androidx.room.*
import com.example.notemaster.data.Content
import com.example.notemaster.data.ContentItem
import com.example.notemaster.data.Folder
import com.example.notemaster.data.ItemCheckBox
import com.example.notemaster.data.ItemFile
import com.example.notemaster.data.ItemImage
import com.example.notemaster.data.ItemText
import com.example.notemaster.data.Note
import com.example.notemaster.data.QuickNote
import com.example.notemaster.data.Reminder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



@Database(
    entities = [NoteEntity::class, QuickNoteEntity::class, FolderEntity::class, TagEntity::class, NoteTagCrossRef::class],
    version = 9,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun folderDao(): FolderDao
}



