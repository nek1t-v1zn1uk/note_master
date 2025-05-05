package com.example.notemaster

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class QuickNoteActivity : ComponentActivity() {
    private lateinit var quickNoteDao: QuickNoteDao

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

        quickNoteDao = db.quickNoteDao()


        val quickNoteDao = db.quickNoteDao()

        setContent {
            QuickNoteScreen(onSave = { text ->
                // 2) Вставка в БД в IO-корутині
                if (text.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        quickNoteDao.insert(
                            QuickNote(
                                text = text,
                                lastEdit = LocalDateTime.now()
                            ).toQuickNoteEntity()
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask()
                } else {
                    finish()
                }
                // закриваємо Activity
            }, onCancel = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask()
                } else {
                    finish()
                }

            })
        }
    }
}

@Composable
fun QuickNoteScreen(
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .heightIn(max = 400.dp)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Швидка нотатка",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                color = Color.DarkGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Текст нотатки...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSave(text) },
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text("Зберегти")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text("Скасувати")
                }
            }
        }
    }
}
