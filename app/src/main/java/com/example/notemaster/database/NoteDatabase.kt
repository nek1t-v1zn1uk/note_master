package com.example.notemaster.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.example.notemaster.data.Content
import com.example.notemaster.data.Note
import com.example.notemaster.data.Reminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime


@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: Content,           // TypeConverter
    val lastEdit: LocalDateTime,    // TypeConverter
    val reminder: Reminder? = null,
    val isSecret: Boolean,
    val folderId: Int? = null
)


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

    @Query("SELECT * FROM notes ORDER BY lastEdit DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Int): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteByIdFlow(noteId: Int): Flow<NoteEntity?>

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY lastEdit DESC")
    fun getNotesByFolderFlow(folderId: Int): Flow<List<NoteEntity>>
}


// Convert NoteEntity -> Note
fun NoteEntity.toNote(): Note {
    return Note(
        id = this.id,
        name = this.name,
        content = this.content,
        lastEdit = this.lastEdit,
        initialReminder = this.reminder,
        isSecret = this.isSecret,
        folderId = this.folderId
    )
}

// Convert Note -> NoteEntity
fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        name = this.name,
        content = this.content,
        lastEdit = this.lastEdit,
        reminder = this.reminder,
        isSecret = this.isSecret,
        folderId = this.folderId
    )
}
