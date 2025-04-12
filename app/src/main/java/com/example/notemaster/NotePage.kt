package com.example.notemaster

import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun NotePage(note: Note){
    var text by remember { mutableStateOf(note.content) }

    TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Enter something") },
        placeholder = { Text("Type here...") },
        singleLine = false
    )

}

@Preview
@Composable
fun NotePagePreviw(){
    val note = Note("Name", "Some shit")
    NotePage(note)
}