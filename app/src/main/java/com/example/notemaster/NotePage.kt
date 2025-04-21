package com.example.notemaster

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.trimmedLength
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
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

class FocusedItem {
    companion object {
        var indexInList: Int = -1
        var indexInItem: Int = 0
        var updateValue: (String) -> Unit = {}
        var changeNeeded: Boolean = false
        var cursorStart: Int = 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePage(note: Note, navController: NavController){
    var isKeyboard by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    OnKeyboardStartShowing {
        isKeyboard = true
    }
    OnKeyboardStartHiding {
        focusManager.clearFocus() // removes focus from TextField
        isKeyboard = false
    }

    var imageUri = remember { mutableStateOf<Uri?>(null) }
    val waitingForImage = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            imageUri.value = null
            imageUri.value = uri
            waitingForImage.value = false // Done waiting
        }
    )
    fun openGallery() {
        waitingForImage.value = true
        launcher.launch("image/*")
    }
    //starts when image is pressed
    LaunchedEffect(imageUri.value) {
        imageUri.value?.let { uri ->
            // This block is only triggered after image is selected!
            val newImageItem = ItemImage(uri)

            var item = note.content.list[FocusedItem.indexInList]
            if(item is ItemText) {
                var indexInListOfNewImage = -1

                if (FocusedItem.indexInItem == 0) {
                    indexInListOfNewImage = 0
                } else if (FocusedItem.indexInItem == item.text.length) {
                    indexInListOfNewImage = FocusedItem.indexInList + 1
                    if (FocusedItem.indexInList == note.content.list.size - 1)
                        note.content.addComponent(
                            FocusedItem.indexInList + 1,
                            ItemText("", style = item.style)
                        )
                } else {
                    indexInListOfNewImage = FocusedItem.indexInList + 1
                    var firstText = item.text.substring(0, FocusedItem.indexInItem)
                    var secondText =
                        item.text.substring(FocusedItem.indexInItem, item.text.length)
                    Log.d("Shit", "item.text:${item.text};  firstText:${firstText}; secondText:${secondText};")
                    item.text = firstText
                    note.content.addComponent(
                        FocusedItem.indexInList + 1,
                        ItemText(secondText, style = item.style)
                    )
                    FocusedItem.updateValue(firstText)
                }

                note.content.addComponent(
                    indexInListOfNewImage,
                    newImageItem
                )
                imageUri.value = null
            }
        }
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
                            onClick = {
                                openGallery()
                            },
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
                                val item = note.content.list.get(FocusedItem.indexInList)
                                if(item is ItemText) {

                                    var firstText = ""
                                    var checkBoxText = ""
                                    var lastText = ""

                                    var i = FocusedItem.indexInItem
                                    var indexBefore = i - 1
                                    var indexAfter = i
                                    while(indexBefore != 0 && item.text[indexBefore] != '\n'){
                                        indexBefore--
                                    }
                                    while(indexAfter != item.text.length && item.text[indexAfter] != '\n'){
                                        indexAfter++
                                    }


                                    lastText = item.text.substring(indexAfter, item.text.length)
                                    checkBoxText = item.text.substring(indexBefore + 1, indexAfter)
                                    if(indexBefore != 0)
                                        firstText = item.text.substring(0, indexBefore)

                                    val tempIndexInList = FocusedItem.indexInList
                                    FocusedItem.changeNeeded = true
                                    FocusedItem.indexInList += 1
                                    FocusedItem.cursorStart = FocusedItem.indexInItem - firstText.length

                                    note.content.addComponent(
                                        tempIndexInList,
                                        ItemCheckBox(checkBoxText, style = item.style)
                                    )
                                    if(indexBefore != 0)
                                        note.content.addComponent(
                                            tempIndexInList,
                                            ItemText(firstText, style = item.style)
                                        )

                                    item.text = lastText
                                    FocusedItem.updateValue(firstText)
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

        NoteContent(note,
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
        val content: MutableList<ContentItem> = note.content.list

        val remainingHeight = with(density) { lazyColumnHeightPx.toDp() }
        Log.d("Shit", "123")
        items(
            count = content.size,
            key = { content[it].id } // 👈 This ensures Compose tracks each item correctly
        ) { index ->
            Log.d("Shit", "123")
            val focusRequester = remember { FocusRequester() }
            var modifierPart: Modifier = Modifier
            val item = content[index]

            when (item) {
                is ItemCheckBox -> {
                    CheckBoxPart(
                        item.text,
                        item.hasCheckBox,
                        index,
                        focusRequester
                    )
                }

                is ItemText -> {
                    if (index == content.lastIndex) {
                        modifierPart = modifierPart.heightIn(min = remainingHeight)
                    }
                    TextPart(
                        item.text,
                        index,
                        focusRequester,
                        modifierPart.fillMaxSize()
                    )
                }

                is ItemImage -> {
                    Image(
                        painter = rememberAsyncImagePainter(item.uri),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp)
                    )
                }
            }
        }


        /*
        var content: MutableList<ContentItem> = note.content.list

        var remainingHeight = with(density) { lazyColumnHeightPx.toDp() }

        items(content.size) { index ->
            val focusRequester = remember { FocusRequester() }
            var modifierPart: Modifier = Modifier

            val item = content[index]

            if(item is ItemText) {
                if(item is ItemCheckBox){
                    CheckBoxPart(
                        item.text,
                        item.hasCheckBox,
                        index,
                        focusRequester
                    )
                }
                else {
                    if(index == content.lastIndex){
                        modifierPart = modifierPart.heightIn(min = remainingHeight)
                    }

                    TextPart(
                        item.text,
                        index,
                        focusRequester,
                        modifierPart.fillMaxSize()
                    )
                }
            }
            else if(item is ItemImage){
                Image(
                    painter = rememberAsyncImagePainter(item.uri),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
            }
        }*/
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
    indexInList: Int,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier){
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }

    LaunchedEffect(Unit) {
        if (indexInList == FocusedItem.indexInList && FocusedItem.changeNeeded) {
            Log.d("Shit", "TB - indexInList:${indexInList}; ")
            focusRequester.requestFocus()
            textFieldValue = textFieldValue.copy(selection = TextRange(FocusedItem.cursorStart))
            FocusedItem.changeNeeded = false
        }
    }
    TextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue

            FocusedItem.indexInList = indexInList
            FocusedItem.indexInItem = newValue.selection.start
            FocusedItem.updateValue = { value: String ->
                textFieldValue = textFieldValue.copy(text = value)
            }

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
            .focusRequester(focusRequester)
            .focusable()
    )
}

@Composable
fun CheckBoxPart(
    value: String,
    isChecked: Boolean = false,
    indexInList: Int,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier){
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var isCheckedValue by remember { mutableStateOf(isChecked) }

    LaunchedEffect(Unit) {
        if(indexInList == FocusedItem.indexInList && FocusedItem.changeNeeded){
            Log.d("Shit", "CB - indexInList:${indexInList}; ")
            focusRequester.requestFocus()
            textFieldValue = textFieldValue.copy(selection = TextRange(FocusedItem.cursorStart))
            FocusedItem.changeNeeded = false
        }
    }
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
                .focusRequester(focusRequester)
                .focusable()
        )
    }
}




@Preview(wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.NONE)
@Composable
fun NotePagePreviw(){
    //SHIIIITTTTT
    var content = Content()
    var list: MutableList<ContentItem> = mutableListOf()
    list.add(ItemText("Some shitted Text"))
    list.add(ItemCheckBox("Choose it"))
    list.add(ItemText("Another shitted Text\nSHEESH\nFuck you"))
    content.list = list
    val note = Note("Name", content)
    NotePage(note, rememberNavController())
}