package com.example.notemaster.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.example.notemaster.data.QuickNote
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime


@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val lastEdit: LocalDateTime
)

@Dao
interface QuickNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: QuickNoteEntity)

    @Update
    suspend fun update(note: QuickNoteEntity)

    @Delete
    suspend fun delete(note: QuickNoteEntity)

    @Query("SELECT * FROM quick_notes ORDER BY lastEdit DESC")
    suspend fun getAllQuickNotes(): List<QuickNoteEntity>

    @Query("SELECT * FROM quick_notes ORDER BY lastEdit DESC")
    fun getAllQuickNotesFlow(): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_notes WHERE id = :id LIMIT 1")
    suspend fun getQuickNoteById(id: Int): QuickNoteEntity?


    @Query("DELETE FROM quick_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)
}

fun QuickNoteEntity.toQuickNote(): QuickNote =
    QuickNote(
        id = this.id,
        text = this.text,
        lastEdit = this.lastEdit
    )

fun QuickNote.toQuickNoteEntity(): QuickNoteEntity =
    QuickNoteEntity(
        id = this.id,
        text = this.text,
        lastEdit = this.lastEdit
    )
