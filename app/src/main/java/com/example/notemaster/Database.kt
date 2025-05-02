package com.example.notemaster

import android.net.Uri
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import android.util.Log
import androidx.room.*
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


// ----------------------------
// ENTITY
// ----------------------------
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: Content, // Stored via TypeConverter
    val lastEdit: LocalDateTime // Stored via TypeConverter
)

// ----------------------------
// TYPE CONVERTERS
// ----------------------------
class Converters {

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .registerTypeAdapterFactory(ContentItemTypeAdapterFactory())
        .registerTypeAdapterFactory(ContentTypeAdapterFactory())
        .create()

    // Content <-> String
    @TypeConverter
    fun fromContent(content: Content): String {
        val json = gson.toJson(content)
        Log.d("TypeConverter", "Saving content: $json")
        return gson.toJson(content)
    }

    @TypeConverter
    fun toContent(data: String): Content {
        Log.d("TypeConverter", "Loading content: $data")
        val type = object : TypeToken<Content>() {}.type
        return gson.fromJson(data, type)
    }

    // LocalDateTime <-> String
    @TypeConverter
    fun fromLocalDateTime(time: LocalDateTime): String {
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @TypeConverter
    fun toLocalDateTime(time: String): LocalDateTime {
        return LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    // Uri <-> String (used in ItemImage)
    @TypeConverter
    fun fromUri(uri: Uri): String = uri.toString()

    @TypeConverter
    fun toUri(uriString: String): Uri = Uri.parse(uriString)
}

// ----------------------------
// DAO
// ----------------------------
@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes ORDER BY lastEdit DESC")
    suspend fun getAllNotesOnce(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Int): NoteEntity?

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)

}

// ----------------------------
// DATABASE
// ----------------------------
@Database(entities = [NoteEntity::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}





// Convert NoteEntity -> Note
fun NoteEntity.toNote(): Note {
    return Note(
        id = this.id,
        name = this.name,
        content = this.content,
        lastEdit = this.lastEdit
    )
}

// Convert Note -> NoteEntity
fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        name = this.name,
        content = this.content,
        lastEdit = this.lastEdit
    )
}

class ContentItemTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != ContentItem::class.java) return null

        val itemTextAdapter = gson.getDelegateAdapter(this, TypeToken.get(ItemText::class.java))
        val itemCheckBoxAdapter = gson.getDelegateAdapter(this, TypeToken.get(ItemCheckBox::class.java))
        val itemImageAdapter = gson.getDelegateAdapter(this, TypeToken.get(ItemImage::class.java))
        val itemFileAdapter = gson.getDelegateAdapter(this, TypeToken.get(ItemFile::class.java))

        @Suppress("UNCHECKED_CAST")
        val contentItemAdapter = object : TypeAdapter<ContentItem>() {

            @Throws(IOException::class)
            override fun write(out: JsonWriter?, value: ContentItem?) {
                if (out == null || value == null) return

                out.beginObject()
                val actualType = when (value) {
                    is ItemText -> "ItemText"
                    is ItemCheckBox -> "ItemCheckBox"
                    is ItemImage -> "ItemImage"
                    is ItemFile -> "ItemFile"
                    else -> throw IllegalArgumentException("Unknown type: ${value.javaClass}")
                }
                out.name("type").value(actualType)
                out.name("data")
                when (value) {
                    is ItemText -> itemTextAdapter.write(out, value)
                    is ItemCheckBox -> itemCheckBoxAdapter.write(out, value)
                    is ItemImage -> itemImageAdapter.write(out, value)
                    is ItemFile -> itemFileAdapter.write(out, value)
                }
                out.endObject()
            }

            @Throws(IOException::class)
            override fun read(reader: JsonReader?): ContentItem? {
                if (reader == null) return null

                var type: String? = null
                var element: JsonElement? = null

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "type" -> type = reader.nextString()
                        "data" -> element = JsonParser.parseReader(reader)
                    }
                }
                reader.endObject()

                val actualAdapter = when (type) {
                    "ItemText" -> itemTextAdapter
                    "ItemCheckBox" -> itemCheckBoxAdapter
                    "ItemImage" -> itemImageAdapter
                    "ItemFile" -> itemFileAdapter
                    else -> throw IllegalArgumentException("Unknown type: $type")
                }

                return actualAdapter.fromJsonTree(element!!)
            }
        }

        return contentItemAdapter as TypeAdapter<T>
    }
}

class ContentTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != Content::class.java) return null

        val contentItemAdapter = gson.getAdapter(ContentItem::class.java)

        @Suppress("UNCHECKED_CAST")
        return object : TypeAdapter<Content>() {
            override fun write(out: JsonWriter, value: Content) {
                out.beginObject()
                out.name("list")
                out.beginArray()
                value.list.forEach { item ->
                    contentItemAdapter.write(out, item)
                }
                out.endArray()
                out.endObject()
            }

            override fun read(reader: JsonReader): Content {
                val list = mutableListOf<ContentItem>()

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "list" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val item = contentItemAdapter.read(reader)
                                list.add(item)
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                return Content(list)
            }
        } as TypeAdapter<T>
    }
}

class UriTypeAdapter : TypeAdapter<Uri>() {
    override fun write(out: JsonWriter, value: Uri?) {
        out.value(value?.toString())
    }
    override fun read(reader: JsonReader): Uri {
        val s = reader.nextString()
        return Uri.parse(s)
    }
}


