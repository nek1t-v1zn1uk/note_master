package com.example.notemaster

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.notemaster.data.Content
import com.example.notemaster.data.ItemFile
import com.example.notemaster.data.ItemImage
import com.example.notemaster.data.Note
import com.example.notemaster.data.Folder
import com.example.notemaster.data.QuickNote
import com.example.notemaster.data.Tag
import com.example.notemaster.database.ContentItemTypeAdapterFactory
import com.example.notemaster.database.ContentTypeAdapterFactory
import com.example.notemaster.database.LocalDateTimeAdapter
import com.example.notemaster.database.NoteDao
import com.example.notemaster.database.QuickNoteDao
import com.example.notemaster.database.FolderDao
import com.example.notemaster.database.NoteTagCrossRef
import com.example.notemaster.database.UriTypeAdapter
import com.example.notemaster.database.toEntity
import com.example.notemaster.database.toFolder
import com.example.notemaster.database.toFolderEntity
import com.example.notemaster.database.toNote
import com.example.notemaster.database.toQuickNoteEntity
import com.example.notemaster.database.toTag
import com.example.notemaster.database.toTagEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val TAG = "BackupManager"

    private fun buildGson(): Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .registerTypeAdapterFactory(ContentItemTypeAdapterFactory())
        .registerTypeAdapterFactory(ContentTypeAdapterFactory())
        .setPrettyPrinting()
        .create()

    suspend fun exportAllBackup(
        context: Context,
        noteDao: NoteDao,
        quickNoteDao: QuickNoteDao,
        folderDao: FolderDao,
        backupZipUri: Uri
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "exportAllBackup() called, uri=$backupZipUri")
        val gson = buildGson()

        // Load data
        val notes = noteDao.getAllNotesOnce().map { it.toNote() }
        val quickNotes = quickNoteDao.getAllQuickNotes()
        val folders = folderDao.getAllFoldersFlow().first().map { it.toFolder() }
        val tags = noteDao.getAllTagsFlow().first().map { it.toTag() }
        val crossRefs = noteDao.getAllCrossRefs()

        data class Entry(val path: String, val stream: InputStream)
        val fileEntries = mutableListOf<Entry>()

        val notesForJson = notes.map { note ->
            val newItems = note.content.list.map { item ->
                when (item) {
                    is ItemImage -> {
                        val src = item.uri
                        val ext = src.lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"
                        val zipPath = "images/${UUID.randomUUID()}.$ext"
                        context.contentResolver.openInputStream(src)?.let { stream ->
                            fileEntries += Entry(zipPath, stream)
                        }
                        item.copy(uri = Uri.parse(zipPath))
                    }
                    is ItemFile -> {
                        val src = item.uri
                        val zipPath = "files/${UUID.randomUUID()}_${src.lastPathSegment}"
                        context.contentResolver.openInputStream(src)?.let { stream ->
                            fileEntries += Entry(zipPath, stream)
                        }
                        item.copy(uri = Uri.parse(zipPath))
                    }
                    else -> item
                }
            }
            note.copy(content = Content(newItems.toMutableList()))
        }

        val jsons = mapOf(
            "notes.json" to gson.toJson(notesForJson),
            "quicknotes.json" to gson.toJson(quickNotes),
            "folders.json" to gson.toJson(folders),
            "tags.json" to gson.toJson(tags),
            "crossrefs.json" to gson.toJson(crossRefs)
        )

        // Create temp zip
        val tempZip = File(context.cacheDir, "backup_temp.zip").apply { if (exists()) delete() }
        ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZip))).use { zip ->
            jsons.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            fileEntries.forEach { (path, stream) ->
                zip.putNextEntry(ZipEntry(path))
                stream.use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }

        // Copy to destination via SAF
        context.contentResolver.openOutputStream(backupZipUri)?.use { out ->
            FileInputStream(tempZip).use { inp -> inp.copyTo(out) }
        }
        tempZip.delete()
    }

    suspend fun importAllBackup(
        context: Context,
        noteDao: NoteDao,
        quickNoteDao: QuickNoteDao,
        folderDao: FolderDao,
        backupZipUri: Uri
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "importAllBackup() called, uri=$backupZipUri")
        val gson = buildGson()

        noteDao.clearAllNotes()
        noteDao.clearAllTags()
        quickNoteDao.clearAllQuickNotes()
        folderDao.clearAllFolders()
        Log.d(TAG, "Попередні дані видалено з бази даних")

        val tmpDir = File(context.cacheDir, "notes_backup").apply { if (exists()) deleteRecursively(); mkdirs() }
        try {
            context.contentResolver.openInputStream(backupZipUri)?.use { ins ->
                ZipInputStream(BufferedInputStream(ins)).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val outFile = File(tmpDir, entry.name).apply { parentFile?.mkdirs() }
                        try {
                            FileOutputStream(outFile).use { fos -> zip.copyTo(fos) }
                            Log.d(TAG, "Розпаковано: ${entry.name} до ${outFile.absolutePath}")
                        } catch (e: IOException) {
                            Log.e(TAG, "Помилка при розпакуванні ${entry.name}: ${e.message}")
                        } finally {
                            zip.closeEntry()
                        }
                    }
                }
            } ?: run {
                Log.e(TAG, "Не вдалося відкрити потік для читання з URI резервної копії: $backupZipUri")
                tmpDir.deleteRecursively()
                return@withContext
            }

            fun <T> parseList(jsonName: String, typeToken: TypeToken<List<T>>): List<T> {
                val jsonFile = File(tmpDir, jsonName)
                return if (jsonFile.exists()) {
                    val text = jsonFile.readText()
                    try {
                        gson.fromJson(text, typeToken.type) as List<T>
                    } catch (e: Exception) {
                        Log.e(TAG, "Помилка при парсингу $jsonName: ${e.message}, text: $text")
                        emptyList()
                    }
                } else {
                    Log.w(TAG, "JSON файл не знайдено: $jsonName")
                    emptyList()
                }
            }

            val notes: List<Note> = parseList("notes.json", object : TypeToken<List<Note>>() {})
            val quicks: List<QuickNote> = parseList("quicknotes.json", object : TypeToken<List<QuickNote>>() {})
            val foldersList: List<Folder> = parseList("folders.json", object : TypeToken<List<Folder>>() {})
            val tagsList: List<Tag> = parseList("tags.json", object : TypeToken<List<Tag>>() {})
            val crossRefs: List<NoteTagCrossRef> = parseList("crossrefs.json", object : TypeToken<List<NoteTagCrossRef>>() {})

            Log.d(TAG, "Кількість нотаток для імпорту: ${notes.size}")
            Log.d(TAG, "Кількість швидких нотаток для імпорту: ${quicks.size}")
            Log.d(TAG, "Кількість папок для імпорту: ${foldersList.size}")
            Log.d(TAG, "Кількість тегів для імпорту: ${tagsList.size}")
            Log.d(TAG, "Кількість перехресних посилань для імпорту: ${crossRefs.size}")

            val notesWithUris = notes.map { note ->
                val updated = note.content.list.mapNotNull { item ->
                    when (item) {
                        is ItemImage -> {
                            val rel = item.uri?.path
                            if (rel != null) {
                                val src = File(tmpDir, rel)
                                if (src.exists()) {
                                    val dstDir = File(context.filesDir, "images").apply { mkdirs() }
                                    val dst = File(dstDir, src.name)
                                    try {
                                        src.copyTo(dst, overwrite = true)
                                        item.copy(uri = Uri.fromFile(dst))
                                    } catch (e: IOException) {
                                        Log.e(TAG, "Помилка при копіюванні зображення $rel: ${e.message}")
                                        null
                                    }
                                } else {
                                    Log.w(TAG, "Зображення не знайдено: $rel")
                                    null
                                }
                            } else {
                                Log.w(TAG, "Шлях до зображення є null")
                                null
                            }
                        }
                        is ItemFile -> {
                            val rel = item.uri?.path
                            if (rel != null) {
                                val src = File(tmpDir, rel)
                                if (src.exists()) {
                                    val dstDir = File(context.filesDir, "files").apply { mkdirs() }
                                    val dst = File(dstDir, src.name)
                                    try {
                                        src.copyTo(dst, overwrite = true)
                                        item.copy(uri = Uri.fromFile(dst))
                                    } catch (e: IOException) {
                                        Log.e(TAG, "Помилка при копіюванні файлу $rel: ${e.message}")
                                        null
                                    }
                                } else {
                                    Log.w(TAG, "Файл не знайдено: $rel")
                                    null
                                }
                            } else {
                                Log.w(TAG, "Шлях до файлу є null")
                                null
                            }
                        }
                        else -> item
                    }
                }
                note.copy(content = Content(updated.toMutableList()))
            }

            foldersList.forEach { folderDao.insert(it.toFolderEntity()) }
            quicks.forEach { quickNoteDao.insert(it.toQuickNoteEntity()) }
            tagsList.forEach { noteDao.insertTag(it.toTagEntity()) }
            notesWithUris.forEach { noteDao.insert(it.toEntity()) }
            crossRefs.forEach { noteDao.insertCrossRef(it) }

            Log.d(TAG, "Імпорт даних завершено")

        } catch (e: IOException) {
            Log.e(TAG, "Помилка при імпорті резервної копії: ${e.message}")
        } finally {
            tmpDir.deleteRecursively()
            Log.d(TAG, "Тимчасову директорію видалено: ${tmpDir.absolutePath}")
        }
    }
}
