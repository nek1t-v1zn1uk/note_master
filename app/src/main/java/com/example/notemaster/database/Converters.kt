package com.example.notemaster.database

import android.net.Uri
import android.util.Log
import androidx.room.TypeConverter
import com.example.notemaster.data.Content
import com.example.notemaster.data.ContentItem
import com.example.notemaster.data.ItemFile
import com.example.notemaster.data.ItemImage
import com.example.notemaster.data.ItemText
import com.example.notemaster.data.Reminder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    // Reminder <-> String
    @TypeConverter
    fun fromReminder(reminder: Reminder?): String? =
        reminder?.let { gson.toJson(it) }

    @TypeConverter
    fun toReminder(data: String?): Reminder? =
        data?.let { gson.fromJson(it, Reminder::class.java) }
}

class ContentItemTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != ContentItem::class.java) return null

        val itemTextAdapter = gson.getDelegateAdapter(this, TypeToken.get(ItemText::class.java))
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
                    is ItemImage -> "ItemImage"
                    is ItemFile -> "ItemFile"
                    else -> throw IllegalArgumentException("Unknown type: ${value.javaClass}")
                }
                out.name("type").value(actualType)
                out.name("data")
                when (value) {
                    is ItemText -> itemTextAdapter.write(out, value)
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
