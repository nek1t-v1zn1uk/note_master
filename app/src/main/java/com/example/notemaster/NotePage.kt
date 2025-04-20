package com.example.notemaster

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.trimmedLength
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan

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
@Composable
fun OnKeyboardStartShowing(onStartShowing: () -> Unit) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    var lastHeight by remember { mutableStateOf(imeBottom) }
    var showingTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(imeBottom) {
        // Keyboard is expanding (showing)
        if (imeBottom > lastHeight && lastHeight == 0 && !showingTriggered) {
            showingTriggered = true
            onStartShowing()
        }

        // Reset when keyboard fully shown or starts hiding
        if (imeBottom == 0 || imeBottom < lastHeight) {
            showingTriggered = false
        }

        lastHeight = imeBottom
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePage(note: Note, navController: NavController){
    var isEdit by remember { mutableStateOf(false) }

    var text by remember { mutableStateOf(note.content) }
    var name by remember { mutableStateOf(note.name) }

    var isKeyboard by remember { mutableStateOf(false) }

    var cursorPosition by remember { mutableStateOf(mutableMapOf("indexInList" to 0, "indexInItem" to 0)) }


    val focusManager = LocalFocusManager.current
    OnKeyboardStartShowing {
        isKeyboard = true
    }
    OnKeyboardStartHiding {
        focusManager.clearFocus() // removes focus from TextField
        isKeyboard = false
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
        bottomBar = {
            if(isKeyboard)
                BottomAppBar(
                    modifier = Modifier
                        .height(56.dp)
                ) {
                    Row {
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val item = note.content.list.get(cursorPosition["indexInList"]!!)
                                if(item is ItemText) {
                                    note.content.list.removeAt(cursorPosition["indexInList"]!!)

                                    var firstText = ""
                                    var checkBoxText = ""
                                    var lastText = ""

                                    var indexOfEnterBefore = -1
                                    var i = 0
                                    for (c in item.text){
                                        if(i == cursorPosition["indexInItem"])
                                            break
                                        if(c == '\n')
                                            indexOfEnterBefore = i
                                        i++
                                    }
                                    if(indexOfEnterBefore != -1) {
                                        firstText = item.text.substring(0, indexOfEnterBefore)
                                        checkBoxText = item.text.substring(indexOfEnterBefore, item.text.length)
                                    }
                                    else
                                        firstText = ""


                                    var indexOfEnter = checkBoxText.indexOfFirst { it == '\n' }

                                    checkBoxText = checkBoxText.substring(0, indexOfEnter)

                                    lastText = item.text.substring(indexOfEnter, item.text.length)

                                    note.content.list.add(
                                        cursorPosition["indexInList"]!!,
                                        ItemText(lastText, style = item.style)
                                    )
                                    note.content.list.add(
                                        cursorPosition["indexInList"]!!,
                                        ItemCheckBox(checkBoxText, style = item.style)
                                    )
                                    if(indexOfEnterBefore != -1)
                                        note.content.list.add(
                                            cursorPosition["indexInList"]!!,
                                            ItemText(firstText, style = item.style)
                                        )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.stylus_highlighter),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextIncrease,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextDecrease,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                            )
                        }

                    }
                }
        }
    ) {
        innerPadding ->
        val setCursorPosition = { indexInList: Int, indexInItem: Int ->
            cursorPosition["indexInList"] = indexInList
            cursorPosition["indexInItem"] = indexInItem
        }
        val setNoteText = { indexInList: Int, text: String ->
            val item = note.content.list.get(indexInList)
            if(item is ItemText)
                item.text = text
        }
        NoteContent(note,
            setCursorPosition,
            setNoteText,
            Modifier.padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    } /* { innerPadding ->
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
    }*/
}


@Composable
fun NoteContent(
    note: Note,
    setCursorPosition: (Int, Int) -> Unit,
    setNoteText: (Int, String) -> (Unit),
    modifier: Modifier){

    val density = LocalDensity.current
    var lazyColumnHeightPx by remember { mutableStateOf(0) }


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                lazyColumnHeightPx = coords.size.height
            }
    ) {
        item {
            NoteContentTop(note.name, note.lastEdit, note.content.getSymbolsCount(),
                    modifier = Modifier.onGloballyPositioned { coords ->
                        lazyColumnHeightPx -= coords.size.height
                    }
                        .padding(bottom = 16.dp)
            )
        }
        var content: MutableList<ContentItem> = note.content.list

        var remainingHeight = with(density) { lazyColumnHeightPx.toDp() }

        items(content.size) { index ->
            val setCursorPositionHere = { indexInItem: Int ->
                setCursorPosition(index, indexInItem)
            }
            val setNoteTextHere = { text: String ->
                setNoteText(index, text)
            }
            var modifierPart: Modifier = Modifier

            val item = content[index]

            if(item is ItemText) {
                if(item is ItemCheckBox){
                    CheckBoxPart(item.text, item.hasCheckBox, setCursorPositionHere)
                }
                else {
                    if(index == content.lastIndex){
                        modifierPart = modifierPart.heightIn(min = remainingHeight)
                    }

                    TextPart(
                        item.text,
                        setCursorPositionHere,
                        setNoteTextHere,
                        modifierPart.fillMaxSize()
                    )
                }
            }
            else if(item is ItemImage){
                TODO()
            }
        }
    }
}

@Composable
fun NoteContentTop(name: String, date: LocalDateTime, symbolCount: Int, modifier: Modifier = Modifier){

    var nameValue = name
    var dateValue = date

    Column(
        modifier = modifier
    ) {
        TextField(
            value = nameValue,
            onValueChange = { nameValue = it },
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
//  note.lastEdit = note.lastEdit.withYear(2024)
        var format = "dd MMMM HH:mm"
        if (dateValue.year != LocalDateTime.now().year)
            format = "dd MMMM yyyy HH:mm"
        Text(
            text = dateValue.format(
                DateTimeFormatter.ofPattern(
                    format,
                    Locale("uk")
                )
            ) + "   |   " + symbolCount + " символ(-ів)",
            //" | ${text.count { !it.isWhitespace() }} символ(-ів)",
            color = Color.Gray,
            modifier = Modifier
                .padding(start = 14.dp)
        )
    }

}

@Composable
fun TextPart(
    value: String,
    setCursorPosition: (Int) -> Unit,
    setNoteText: (String) -> Unit,
    modifier: Modifier = Modifier){
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    TextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue

            setCursorPosition(newValue.selection.start)
            setNoteText(textFieldValue.text)

            /*
            val selection = newValue.selection
            if (!selection.collapsed) {
                val selectedText = newValue.text.substring(selection.start, selection.end)
                Log.d("Selection", "Selected: $selectedText")
            }
            */
        },
        singleLine = false,
        maxLines = Int.MAX_VALUE,
        textStyle = TextStyle(fontSize = 18.sp),
        modifier = modifier
    )
}

@Composable
fun CheckBoxPart(value: String, isChecked: Boolean = false, setCursorPosition: (Int) -> Unit,  modifier: Modifier = Modifier){
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var isCheckedValue by remember { mutableStateOf(isChecked) }
    Row(
        modifier = modifier
    ) {
        Checkbox(
            checked = isCheckedValue,
            onCheckedChange = { isCheckedValue = it }
        )
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue

                setCursorPosition(newValue.selection.start)

                /*
                val selection = newValue.selection
                if (!selection.collapsed) {
                    val selectedText = newValue.text.substring(selection.start, selection.end)
                    Log.d("Selection", "Selected: $selectedText")
                }
                */
            },
            singleLine = false,
            maxLines = Int.MAX_VALUE,
            textStyle = TextStyle(fontSize = 18.sp),
            modifier = modifier
                .fillMaxWidth()
        )
    }
}




@Preview(wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.NONE)
@Composable
fun NotePagePreviw(){
    var content = Content()
    var list: MutableList<ContentItem> = mutableListOf()
    list.add(ItemText("Some shitted Text"))
    list.add(ItemCheckBox("Choose it"))
    list.add(ItemText("Another shitted Text\nSHEESH\nFuck you"))
    content.list = list
    val note = Note("Name", content)
    NotePage(note, rememberNavController())
}