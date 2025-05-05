package com.example.notemaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickNoteActivity : ComponentActivity() {
    private lateinit var noteDao: NoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Побудова БД і отримання DAO
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "notes_database"
        )
            .fallbackToDestructiveMigration()
            .build()
        noteDao = db.noteDao()

        setContent {
            QuickNoteScreen(onSave = { text ->
                // 2) Вставка в БД в IO-корутині
                if (text.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        noteDao.insert(
                            Note(
                                content = Content(mutableListOf(ItemText(text)))
                            ).toEntity()
                        )
                    }
                }
                finish()   // закриваємо Activity
            }, onCancel = {
                finish()
            })
        }
    }
}

@Composable
fun QuickNoteScreen(
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Текст нотатки...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(text) }) {
                    Text("Зберегти")
                }
                OutlinedButton(onClick = onCancel) {
                    Text("Скасувати")
                }
            }
        }
    }
}
