package com.example.notemaster.pages

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notemaster.QuickNoteScreen
import com.example.notemaster.data.Folder
import com.example.notemaster.data.Note
import com.example.notemaster.data.QuickNote
import com.example.notemaster.database.FolderDao
import com.example.notemaster.database.NoteDao
import com.example.notemaster.database.QuickNoteDao
import com.example.notemaster.viewmodels.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.LocalDateTime


@Composable
fun HandleBackPress(
    isCheckState: Boolean,
    onClearCheckState: () -> Unit,
    navController: NavController
) {
    val activity = LocalContext.current as? Activity
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val currentCheckState by rememberUpdatedState(isCheckState)

    DisposableEffect(dispatcher, currentCheckState) {
        if (dispatcher == null) return@DisposableEffect onDispose {}

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentCheckState) {
                    onClearCheckState()
                } else {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        activity?.finish() // close app if nothing to pop
                    }
                }
            }
        }

        dispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteList(
    noteDao: NoteDao,
    quickNoteDao: QuickNoteDao,
    folderDao: FolderDao,
    navController: NavController
){
    val application = LocalContext.current.applicationContext as Application
    val factory = remember {
        NoteListViewModelFactory(
            application = application,
            noteDao = noteDao,
            quickNoteDao = quickNoteDao,
            folderDao = folderDao
        )
    }
    val viewModel: NoteListViewModel = viewModel(factory = factory)

    val notes by viewModel.allNotes.collectAsState()
    val quickNotes by viewModel.quickNotes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val isQuick by viewModel.isQuickNotes.collectAsState()
    val isSec by viewModel.isSecret.collectAsState()
    val isChk by viewModel.isCheckMode.collectAsState()
    val selItemsIds by viewModel.selectedItemsIds.collectAsState()
    val selFoldersIds by viewModel.selectedFoldersIds.collectAsState()


    val context = LocalContext.current
    val activity = remember(context) { context as AppCompatActivity }


    HandleBackPress(
        isCheckState = isChk,
        onClearCheckState = { viewModel.exitCheckMode() },
        navController = navController
    )

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier
            .navigationBarsPadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        var showDialog by remember { mutableStateOf(false) }
                        Text(
                            text = if(isQuick) "Швидкі нотатки" else ("Нотатки" + if(isSec) "\uD83D\uDD12" else ""),
                            modifier = Modifier
                                .clickable(onClick = {
                                    if(!isQuick) {
                                        if (isSec) {
                                            viewModel.exitSecretMode()
                                        } else {
                                            val biometricManager = BiometricManager.from(context)
                                            when (biometricManager.canAuthenticate(
                                                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                            )) {
                                                BiometricManager.BIOMETRIC_SUCCESS -> {
                                                    // 1) Підготовка PromptInfo з дозвілом на резервний системний PIN/пароль
                                                    val promptInfo =
                                                        BiometricPrompt.PromptInfo.Builder()
                                                            .setTitle("Авторизація")
                                                            .setSubtitle("Підтвердіть особу")
                                                            // якщо пристрій не має біометрії або користувач хоче, можна ввійти через системний PIN/пароль
                                                            .setAllowedAuthenticators(
                                                                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                                                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                                            )
                                                            .build()

                                                    // 2) Створюємо BiometricPrompt
                                                    val executor =
                                                        ContextCompat.getMainExecutor(context)
                                                    val biometricPrompt = BiometricPrompt(
                                                        activity, executor,
                                                        object :
                                                            BiometricPrompt.AuthenticationCallback() {
                                                            override fun onAuthenticationSucceeded(
                                                                result: BiometricPrompt.AuthenticationResult
                                                            ) {
                                                                super.onAuthenticationSucceeded(
                                                                    result
                                                                )

                                                                viewModel.enterSecretMode()
                                                            }

                                                            override fun onAuthenticationError(
                                                                errorCode: Int,
                                                                errString: CharSequence
                                                            ) {
                                                                super.onAuthenticationError(
                                                                    errorCode,
                                                                    errString
                                                                )
                                                                // Обробка помилок (наприклад, багато безуспішних спроб)
                                                            }

                                                            override fun onAuthenticationFailed() {
                                                                super.onAuthenticationFailed()
                                                                // Не розпізнано відбиток/обличчя
                                                            }
                                                        }
                                                    )

                                                    // 3) Запускаємо
                                                    biometricPrompt.authenticate(promptInfo)
                                                }

                                                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                                                    // немає біометричного датчика
                                                    viewModel.enterSecretMode()
                                                }

                                                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                                                    // датчик тимчасово недоступний
                                                    showDialog = true
                                                }

                                                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                                    // не налаштований жоден відбиток/обличчя — можна запропонувати поставити пароль
                                                    viewModel.enterSecretMode()
                                                }
                                            }
                                        }
                                    }
                                })
                        )
                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showDialog = false
                                },
                                title = {
                                    Text(text = "Помилка")
                                },
                                text = {
                                    Text("Перевірка біометрії/паролю тимчасово недоступна. Спробуйте пізніше")
                                },
                                confirmButton = {
                                    TextButton(onClick = { showDialog = false }) {
                                        Text("ОК")
                                    }
                                }
                            )
                        }
                    },
                    colors = topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black
                    ),
                    actions = {
                        if(isChk) {

                            if(isQuick){
                                Icon(
                                    imageVector = Icons.Default.NoteAdd,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(height = 40.dp, width = 60.dp)
                                        .padding(end = 20.dp)
                                        .clickable(onClick = {
                                            viewModel.convertQuickNotesToNotes()
                                        })
                                )
                            }
                            else {
                                var isMenu by remember { mutableStateOf(false) }
                                if(isMenu){
                                    Dialog(onDismissRequest = { isMenu = false }){
                                        Box(
                                            modifier = Modifier
                                                .size(500.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White)
                                                .padding(20.dp)
                                        ){
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                            ){
                                                var getName by remember { mutableStateOf(false) }
                                                if(getName) {
                                                    Dialog(onDismissRequest = { getName = false }) {
                                                        Box(
                                                            modifier = Modifier
                                                                .height(140.dp)
                                                                .clip(RoundedCornerShape(16.dp))
                                                                .background(Color.White)
                                                                .padding(20.dp)
                                                        ){
                                                            Column(
                                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                                horizontalAlignment = Alignment.End,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                            ){
                                                                var nameValue by remember { mutableStateOf("") }
                                                                TextField(
                                                                    value = nameValue,
                                                                    onValueChange = { nameValue = it },
                                                                    placeholder = { Text("Нова папка...", color = Color.Black) },
                                                                    colors = TextFieldDefaults.colors(
                                                                        focusedContainerColor = Color(red = 100, green = 100, blue = 255, alpha = 120),
                                                                        unfocusedContainerColor = Color(red = 100, green = 100, blue = 255, alpha = 80),
                                                                        focusedIndicatorColor = Color.Transparent,
                                                                        unfocusedIndicatorColor = Color.Transparent,
                                                                        focusedPlaceholderColor = Color.LightGray,
                                                                        unfocusedPlaceholderColor = Color.LightGray,
                                                                        cursorColor = Color(red = 100, green = 100, blue = 255),
                                                                        selectionColors = TextSelectionColors(
                                                                            handleColor = Color(red = 100, green = 100, blue = 255),
                                                                            backgroundColor = Color(red = 100, green = 100, blue = 255, alpha = 100),
                                                                        ),
                                                                        disabledIndicatorColor = Color(red = 100, green = 100, blue = 255),
                                                                        errorIndicatorColor = Color(red = 100, green = 100, blue = 255),

                                                                        ),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                )
                                                                Button(
                                                                    onClick = {
                                                                        viewModel.addFolder(Folder(name = nameValue))
                                                                        getName = false
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = Color(red = 100, green = 100, blue = 255),
                                                                        contentColor = Color.Black,
                                                                        disabledContainerColor = Color(red = 100, green = 100, blue = 255),
                                                                        disabledContentColor = Color(red = 0, green = 100, blue = 255),
                                                                    )
                                                                ){
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
                                                ){
                                                    Text(
                                                        text = "Створити нову папку",
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
                                                        text = "Винести з папки",
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
                                                for(folder in folders){
                                                    Box(
                                                        contentAlignment = Alignment.CenterStart,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
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
                                                                viewModel.addSelectedToFolder(folder.id)
                                                            }
                                                    ){
                                                        Text(
                                                            text = folder.name,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(height = 40.dp, width = 60.dp)
                                        .padding(end = 20.dp)
                                        .clickable {
                                            isMenu = true
                                        }
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(height = 40.dp, width = 60.dp)
                                    .padding(end = 20.dp)
                                    .clickable(onClick = {
                                        viewModel.deleteSelected()
                                    })
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(height = 40.dp, width = 60.dp)
                                    .padding(end = 20.dp)
                                    .clickable(onClick = {
                                        viewModel.exitCheckMode()
                                    })
                            )
                        }
                    },
                    modifier = Modifier
                )
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(Color.Gray, RoundedCornerShape(2.dp))
                )
            }
        },
        bottomBar = {
            Column() {
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(Color.Gray, RoundedCornerShape(2.dp))
                )
                BottomAppBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0),
                    modifier = Modifier
                        .padding(0.dp)
                ) {
                    Row (
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier
                            .fillMaxSize()

                    ){
                        Column (
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .clickable(onClick = {
                                    viewModel.exitCheckMode()
                                    viewModel.moveToNotes()
                                })
                        ) {
                            var fill = if (!isQuick) Color.Black else Color.Gray
                            Icon(
                                imageVector = Icons.Filled.StickyNote2,
                                contentDescription = "Notes",
                                tint = fill,
                                modifier = Modifier
                                    .size(30.dp)
                            )
                            Text(
                                text="Нотатки",
                                color = fill
                            )
                        }
                        Column (
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .clickable(onClick = {
                                    viewModel.exitCheckMode()
                                    viewModel.moveToQuickNotes()
                                })
                        ) {
                            var fill = if (isQuick) Color.Black else Color.Gray
                            Icon(
                                imageVector = Icons.Filled.NoteAlt,
                                contentDescription = "Notes",
                                tint = fill,
                                modifier = Modifier
                                    .size(30.dp)
                            )
                            Text(
                                text="Швидкі нотатки" + if(!quickNotes.isEmpty())" (${quickNotes.size})" else "",
                                color = fill
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if(!isChk) {
                var isQuickNoteScreen by remember { mutableStateOf(false) }
                if (isQuickNoteScreen) {
                    Dialog(onDismissRequest = { isQuickNoteScreen = false }) {
                        QuickNoteScreen(
                            onSave = { text ->
                                if (text.isNotBlank()) {
                                    viewModel.addQuickNote(
                                        QuickNote(
                                            text = text,
                                            lastEdit = LocalDateTime.now()
                                        )
                                    )
                                }
                                isQuickNoteScreen = false
                            },
                            onCancel = { isQuickNoteScreen = false }
                        )
                    }
                }
                FloatingActionButton(
                    containerColor = Color(red = 100, green = 100, blue = 255),
                    contentColor = Color.Black,
                    onClick = {
                        if (isQuick) {
                            isQuickNoteScreen = true
                        } else {
                            var newNote = Note(isSecret = isSec)
                            newNote.content.ensureTrailingText()
                            viewModel.addNote(newNote)
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }

    ){ innerPadding ->

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
        ) {
            item{
                Divider(
                    thickness = 10.dp,
                    color = Color.White
                )
            }

            if(!isQuick) {

                items(
                    count = folders.size,
                    key = { "folder-${folders[it].id}" }
                ) {
                    var folder = folders[it]
                    Column(
                        modifier = Modifier
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 1f),
                                spotColor = Color.Black.copy(alpha = 1f),
                                clip = true
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.LightGray)
                            .animateContentSize(
                                animationSpec = tween(durationMillis = 250)
                            )
                    ){
                        var isOpen by remember { mutableStateOf(false) }

                        var folderList = notes.filter { it.folderId == folder.id && it.isSecret == isSec }


                        var isPressedFolder by remember { mutableStateOf(false) }
                        val fastInSlowOut = tween<Float>(
                            durationMillis = 300,
                            easing = {
                                if (it < 0.15f) it * 2f // faster start
                                else 1f - (1f - it) * (1f - it) // ease out
                            }
                        )
                        val scaleFolder by animateFloatAsState(
                            targetValue = if (isPressedFolder) 0.90f else 1f,
                            animationSpec = fastInSlowOut,
                            label = "PressScale"
                        )

                        var isCheckedFolder by remember { mutableStateOf(false) }
                        if (!isChk)
                            isCheckedFolder = false

                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = scaleFolder,
                                    scaleY = scaleFolder
                                )
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Color.Black.copy(alpha = 1f),
                                    spotColor = Color.Black.copy(alpha = 1f),
                                    clip = true
                                )
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(
                                    if (!isCheckedFolder) Color.White else Color.LightGray,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(top = 10.dp, bottom = 10.dp, start = 15.dp, end = 15.dp)
                                .pointerInput(Unit) {
                                    coroutineScope {
                                        detectTapGestures(
                                            onPress = {
                                                isPressedFolder = true

                                                // Launch coroutine to detect long press
                                                val longPressJob = launch {
                                                    delay(500)
                                                    if (scaleFolder == 0.90f)
                                                        viewModel.enterCheckMode()
                                                }

                                                val released = tryAwaitRelease()

                                                if (released) {
                                                    longPressJob.cancel()
                                                    if (isChk) {
                                                        isCheckedFolder = !isCheckedFolder
                                                        if (isCheckedFolder) {
                                                            viewModel.selectFolder(folder.id)
                                                            for (item in folderList)
                                                                viewModel.selectItem(item.id)
                                                        } else {
                                                            viewModel.deselectFolder(folder.id)
                                                            for (item in folderList)
                                                                viewModel.deselectItem(item.id)
                                                        }
                                                    } else {
                                                        isOpen = !isOpen
                                                    }
                                                }

                                                isPressedFolder = false
                                            }
                                        )
                                    }
                                }

                        ){
                            Text((if(isOpen) "\uD83D\uDCC2" else "\uD83D\uDCC1") + folder.name)
                            if (isChk) {
                                Box(
                                    contentAlignment = Alignment.CenterEnd,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(end = 10.dp)

                                ) {
                                    Icon(
                                        imageVector = if (isCheckedFolder) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                        tint = Color.Yellow,
                                        contentDescription = "Checked",
                                        modifier = Modifier
                                    )
                                }
                            }
                        }
                        if(isOpen){
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .padding(start = 16.dp, top = 6.dp, bottom = 4.dp, end = 4.dp)
                            ) {
                                for (item in folderList) {

                                    var isPressed by remember { mutableStateOf(false) }
                                    val fastInSlowOut = tween<Float>(
                                        durationMillis = 300,
                                        easing = {
                                            if (it < 0.15f) it * 2f // faster start
                                            else 1f - (1f - it) * (1f - it) // ease out
                                        }
                                    )
                                    val scale by animateFloatAsState(
                                        targetValue = if (isPressed) 0.90f else 1f,
                                        animationSpec = fastInSlowOut,
                                        label = "PressScale"
                                    )

                                    var isChecked by remember { mutableStateOf(false) }
                                    isChecked = selItemsIds.contains(item.id)
                                    if (!isChk)
                                        isChecked = false

                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale
                                            )
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                ambientColor = Color.Black.copy(alpha = 1f),
                                                spotColor = Color.Black.copy(alpha = 1f),
                                                clip = true
                                            )
                                            .background(
                                                if (!isChecked) Color.White else Color.LightGray,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(
                                                top = 20.dp,
                                                bottom = 10.dp,
                                                start = 15.dp,
                                                end = 15.dp
                                            )
                                            .pointerInput(Unit) {
                                                coroutineScope {
                                                    detectTapGestures(
                                                        onPress = {
                                                            isPressed = true

                                                            // Launch coroutine to detect long press
                                                            val longPressJob = launch {
                                                                delay(500)
                                                                if (scale == 0.90f)
                                                                    viewModel.enterCheckMode()
                                                            }

                                                            val released = tryAwaitRelease()

                                                            if (released) {
                                                                longPressJob.cancel()
                                                                if (isChk) {
                                                                    isChecked = !isChecked
                                                                    if (isChecked) {
                                                                        viewModel.selectItem(item.id)
                                                                    } else {
                                                                        viewModel.deselectItem(item.id)
                                                                    }
                                                                } else {
                                                                    navController.navigate("note_page/${item.id}")
                                                                }
                                                            }

                                                            isPressed = false
                                                        }
                                                    )
                                                }
                                            }

                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(end = 50.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontSize = 18.sp,
                                                color = Color.Black,
                                                maxLines = 2
                                            )

                                            Text(
                                                text = item.lastEdit.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "dd MMMM HH:mm",
                                                        Locale("uk")
                                                    )
                                                ),
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                        }


                                        if (isChk) {
                                            Box(
                                                contentAlignment = Alignment.CenterEnd,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(end = 10.dp)

                                            ) {
                                                Icon(
                                                    imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                                    tint = Color.Yellow,
                                                    contentDescription = "Checked",
                                                    modifier = Modifier
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val filtered = notes.filter { it.folderId == null && it.isSecret == isSec }


                //чисті нотатки
                //val filtered = notes.filter { it.isSecret == isSec }

                items(
                    count = filtered.size,
                    key = { "filtered-${filtered[it].id}" }
                ) { index ->
                    var item = filtered[index]
                    var isPressed by remember { mutableStateOf(false) }
                    val fastInSlowOut = tween<Float>(
                        durationMillis = 300,
                        easing = {
                            if (it < 0.15f) it * 2f // faster start
                            else 1f - (1f - it) * (1f - it) // ease out
                        }
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.90f else 1f,
                        animationSpec = fastInSlowOut,
                        label = "PressScale"
                    )

                    var isChecked by remember { mutableStateOf(false) }
                    if (!isChk)
                        isChecked = false

                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                            .fillMaxWidth()
                            .height(100.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 1f),
                                spotColor = Color.Black.copy(alpha = 1f),
                                clip = true
                            )
                            .background(
                                if (!isChecked) Color.White else Color.LightGray,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(top = 20.dp, bottom = 10.dp, start = 15.dp, end = 15.dp)
                            .pointerInput(Unit) {
                                coroutineScope {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true

                                            // Launch coroutine to detect long press
                                            val longPressJob = launch {
                                                delay(500)
                                                if (scale == 0.90f)
                                                    viewModel.enterCheckMode()
                                            }

                                            val released = tryAwaitRelease()

                                            if (released) {
                                                longPressJob.cancel()
                                                if (isChk) {
                                                    isChecked = !isChecked
                                                    if (isChecked) {
                                                        viewModel.selectItem(item.id)
                                                    } else {
                                                        viewModel.deselectItem(item.id)
                                                    }
                                                } else {
                                                    navController.navigate("note_page/${item.id}")
                                                }
                                            }

                                            isPressed = false
                                        }
                                    )
                                }
                            }

                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 50.dp)
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 18.sp,
                                color = Color.Black,
                                maxLines = 2
                            )

                            Text(
                                text = item.lastEdit.format(
                                    DateTimeFormatter.ofPattern(
                                        "dd MMMM HH:mm",
                                        Locale("uk")
                                    )
                                ),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }


                        if (isChk) {
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 10.dp)

                            ) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    tint = Color.Yellow,
                                    contentDescription = "Checked",
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
            else{
                items(
                    count = quickNotes.size,
                    key = { quickNotes[it].id }
                ) { index ->
                    var item = quickNotes[index]
                    var isPressed by remember { mutableStateOf(false) }
                    val fastInSlowOut = tween<Float>(
                        durationMillis = 300,
                        easing = {
                            if (it < 0.15f) it * 2f // faster start
                            else 1f - (1f - it) * (1f - it) // ease out
                        }
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.90f else 1f,
                        animationSpec = fastInSlowOut,
                        label = "PressScale"
                    )

                    var isChecked by remember { mutableStateOf(false) }
                    if (!isChk)
                        isChecked = false

                    var isOpen by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                            .fillMaxWidth()
                            .heightIn(min = 40.dp, max = Int.MAX_VALUE.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 1f),
                                spotColor = Color.Black.copy(alpha = 1f),
                                clip = true
                            )
                            .background(
                                if (!isChecked) Color.White else Color.LightGray,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(top = 20.dp, bottom = 10.dp, start = 15.dp, end = 15.dp)
                            .pointerInput(Unit) {
                                coroutineScope {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true

                                            // Launch coroutine to detect long press
                                            val longPressJob = launch {
                                                delay(500)
                                                if (scale == 0.90f)
                                                    viewModel.enterCheckMode()
                                            }

                                            val released = tryAwaitRelease()

                                            if (released) {
                                                longPressJob.cancel()
                                                if (isChk) {
                                                    isChecked = !isChecked
                                                    if (isChecked) {
                                                        viewModel.selectItem(item.id)
                                                    } else {
                                                        viewModel.deselectItem(item.id)
                                                    }
                                                } else {
                                                    isOpen = !isOpen
                                                }
                                            }

                                            isPressed = false
                                        }
                                    )
                                }
                            }
                            .animateContentSize(
                                animationSpec = tween(durationMillis = 250)
                            )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 50.dp)
                        ) {
                            Text(
                                text = item.text,
                                fontSize = 18.sp,
                                color = Color.Black,
                                maxLines = if(isOpen) Int.MAX_VALUE else 2
                            )

                            Text(
                                text = item.lastEdit.format(
                                    DateTimeFormatter.ofPattern(
                                        "dd MMMM HH:mm",
                                        Locale("uk")
                                    )
                                ),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        if (isChk) {
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(end = 10.dp)

                            ) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    tint = Color.Yellow,
                                    contentDescription = "Checked",
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
            item{
                Divider(
                    thickness = 15.dp,
                    color = Color.White
                )
            }
        }
    }
}
