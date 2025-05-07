package com.example.notemaster.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notemaster.data.Folder
import com.example.notemaster.data.Tag
import com.example.notemaster.viewmodels.NoteListViewModel

@Composable
fun TagPage(lvm: NoteListViewModel, onDismissRequest: () -> Unit){
    val tags by lvm.tags.collectAsState()
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .size(500.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp)
        ){
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxSize()
            ){
                item {
                    var getName by remember { mutableStateOf(false) }
                    if (getName) {
                        Dialog(onDismissRequest = { getName = false }) {
                            Box(
                                modifier = Modifier
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(20.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    var nameValue by remember { mutableStateOf("") }
                                    TextField(
                                        value = nameValue,
                                        onValueChange = { nameValue = it },
                                        placeholder = {
                                            Text(
                                                "Нова мітка...",
                                                color = Color.Black
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color(
                                                red = 100,
                                                green = 100,
                                                blue = 255,
                                                alpha = 120
                                            ),
                                            unfocusedContainerColor = Color(
                                                red = 100,
                                                green = 100,
                                                blue = 255,
                                                alpha = 80
                                            ),
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedPlaceholderColor = Color.LightGray,
                                            unfocusedPlaceholderColor = Color.LightGray,
                                            cursorColor = Color(red = 100, green = 100, blue = 255),
                                            selectionColors = TextSelectionColors(
                                                handleColor = Color(
                                                    red = 100,
                                                    green = 100,
                                                    blue = 255
                                                ),
                                                backgroundColor = Color(
                                                    red = 100,
                                                    green = 100,
                                                    blue = 255,
                                                    alpha = 100
                                                ),
                                            ),
                                            disabledIndicatorColor = Color(
                                                red = 100,
                                                green = 100,
                                                blue = 255
                                            ),
                                            errorIndicatorColor = Color(
                                                red = 100,
                                                green = 100,
                                                blue = 255
                                            ),

                                            ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            lvm.addTag(Tag(name = nameValue))
                                            getName = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                red = 100,
                                                green = 100,
                                                blue = 255
                                            ),
                                            contentColor = Color.Black,
                                            disabledContainerColor = Color(
                                                red = 100,
                                                green = 100,
                                                blue = 255
                                            ),
                                            disabledContentColor = Color(
                                                red = 0,
                                                green = 100,
                                                blue = 255
                                            ),
                                        )
                                    ) {
                                        Text("Добавити")
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Color(
                                    red = 100,
                                    green = 100,
                                    blue = 255,
                                    alpha = 40
                                )
                            )
                            .padding(8.dp)
                            .padding(start = 12.dp)
                            .clickable {
                                getName = true
                            }
                    ) {
                        Text(
                            text = "Створити нову мітку",
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(red = 100, green = 100, blue = 255, alpha = 200),
                            modifier = Modifier
                                .size(50.dp)
                                .padding(4.dp)
                        )
                    }
                    /*
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Color(
                                red = 100,
                                green = 100,
                                blue = 255,
                                alpha = 40
                            )
                        )
                        .padding(8.dp)
                        .padding(start = 12.dp)
                        .clickable {
                            viewModel.addSelectedToFolder(null)
                        }
                ){
                    Text(
                        text = "Видалити всі мітки",
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.FolderOff,
                        contentDescription = null,
                        tint = Color(red = 100, green = 100, blue = 255, alpha = 200),
                        modifier = Modifier
                            .size(50.dp)
                            .padding(4.dp)
                    )
                }
                */
                }
                items(
                    count = tags.size,
                    key = { tags[it].tagId }
                ){ index ->
                    var tag = tags[index]
                    var hasSelected by remember { mutableStateOf(lvm.selectedHasTag(tag.tagId)) }
                    var hasAllSelected by remember { mutableStateOf(lvm.allSelectedHasTag(tag.tagId)) }

                    Row {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Color(
                                        red = 100,
                                        green = 100,
                                        blue = 255,
                                        alpha = 40
                                    )
                                )
                                .padding(8.dp)
                                .padding(start = 12.dp)
                                .clickable {
                                    hasAllSelected = !hasAllSelected
                                    hasSelected = hasAllSelected
                                    if (hasAllSelected)
                                        lvm.addTagToSelected(tag.tagId)
                                    else
                                        lvm.removeTagToSelected(tag.tagId)
                                }
                        ) {
                            Text(
                                text = tag.name,
                                color = Color.Black
                            )
                            if (hasSelected || hasAllSelected) {
                                Icon(
                                    imageVector = if (hasAllSelected) Icons.Default.Check else Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = Color(red = 100, green = 100, blue = 255, alpha = 200),
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(4.dp)
                                )
                            }
                        }
                        IconButton(onClick = {
                            lvm.deleteTag(tag.tagId)
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}