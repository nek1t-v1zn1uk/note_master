package com.example.notemaster

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.trimmedLength
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OnKeyboardStartHiding(onStartHiding: () -> Unit) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    var lastHeight by remember { mutableStateOf(imeBottom) }
    var hidingTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(imeBottom) {
        // Keyboard is shrinking (hiding)
        if (imeBottom < lastHeight && imeBottom > 0 && !hidingTriggered) {
            hidingTriggered = true
            onStartHiding()
        }

        // Reset when keyboard fully hidden or re-opened
        if (imeBottom == 0 || imeBottom > lastHeight) {
            hidingTriggered = false
        }

        lastHeight = imeBottom
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePage(note: Note, navController: NavController){
    var isEdit by remember { mutableStateOf(false) }

    var text by remember { mutableStateOf(note.content) }
    var name by remember { mutableStateOf(note.name) }


    val focusManager = LocalFocusManager.current

    OnKeyboardStartHiding {
        focusManager.clearFocus() // removes focus from TextField
    }

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon (
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {}
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Назва", fontSize = 24.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 24.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedPlaceholderColor = Color.LightGray,
                    unfocusedPlaceholderColor = Color.LightGray,
                )
            )
//            note.lastEdit = note.lastEdit.withYear(2024)
            var format = "dd MMMM HH:mm"
            if(note.lastEdit.year != LocalDateTime.now().year)
                format = "dd MMMM yyyy HH:mm"
            Text(
                text = note.lastEdit.format(DateTimeFormatter.ofPattern(format, Locale("uk"))) +
                        " | ${text.count { !it.isWhitespace() }} символ(-ів)",
                color = Color.Gray,
                modifier = Modifier
                    .padding(start = 14.dp)
            )

            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Type here...") },
                singleLine = false,
                textStyle = TextStyle(fontSize = 18.sp),
                maxLines = Int.MAX_VALUE,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    errorContainerColor = Color.Red,
                    focusedPlaceholderColor = Color.LightGray,
                    unfocusedPlaceholderColor = Color.LightGray,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .heightIn(min = 0.dp)
            )
        }
    }

}

@Preview
@Composable
fun NotePagePreviw(){
    val note = Note("Name", "Some shit")
    NotePage(note, rememberNavController())
}