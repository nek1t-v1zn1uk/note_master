package com.example.notemaster

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notemaster.ui.theme.NoteMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteMasterTheme {
                Surface {
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(55.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteList(list: Array<Note>, modifier: Modifier = Modifier){
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .padding(16.dp)
    ) {
        for(item: Note in list) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = true
                    )
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    fontSize = 24.sp,
                    text = if (item.name.isEmpty()) "Empty" else item.name,
                    modifier = modifier
                        .padding(start = 16.dp)
                )
                var isContextMenu by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        isContextMenu = true
                              },
                    modifier = modifier
                        .align(Alignment.CenterEnd)
                        //.background(Color.Red)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "!2#More options",
                        modifier = modifier
                            .size(50.dp)
                            .graphicsLayer(scaleX = 0.6f, scaleY = 0.6f)
                    )
                DropdownMenu(
                    expanded = isContextMenu,
                    onDismissRequest = { isContextMenu = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            isContextMenu = false
                        },
                        text = {
                        Text(
                            text = "Action 1"
                        )
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            isContextMenu = false
                        },
                        text = {
                            Text(
                                text = "Action 2"
                            )
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            isContextMenu = false
                        },
                        text = {
                            Text(
                                text = "Action 3"
                            )
                        }
                    )
                }

                }
            }
        }
    }
}

@Preview(showBackground = true,
    device = "spec:width=393dp,height=873dp", name = "MyXiaomi", apiLevel = 35,
    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
)
@Composable
fun NoteListPreview(){
    val list = arrayOf<Note>(
        Note("Лабораторні", "no content"),
        Note("Курсовий проект", "no content"),
        Note("Днюхи", "no content"),
        Note("Велосипед", "no content"),
        Note("Закупки", "no content")
    )
    NoteMasterTheme {
        NoteList(list)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NoteMasterTheme {
        Greeting("Android")
    }
}