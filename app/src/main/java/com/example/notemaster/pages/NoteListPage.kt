package com.example.notemaster.pages

import android.app.Activity
import android.app.Application
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
import androidx.compose.ui.Alignment
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notemaster.QuickNoteScreen
import com.example.notemaster.data.Note
import com.example.notemaster.data.QuickNote
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
    noteDao2: NoteDao,
    quickNoteDao2: QuickNoteDao,
    navController: NavController
){
    val application = LocalContext.current.applicationContext as Application
    val factory = remember {
        NoteListViewModelFactory(
            application = application,
            noteDao = noteDao2,
            quickNoteDao = quickNoteDao2
        )
    }
    val viewModel: NoteListViewModel = viewModel(factory = factory)

    val notes by viewModel.allNotes.collectAsState()
    val quickNotes by viewModel.quickNotes.collectAsState()
    val isQuick by viewModel.isQuickNotes.collectAsState()
    val isSec by viewModel.isSecret.collectAsState()
    val isChk by viewModel.isCheckMode.collectAsState()
    val selIds by viewModel.selectedIds.collectAsState()


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
            var isQuickNoteScreen by remember { mutableStateOf(false) }
            if(isQuickNoteScreen) {
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
                    if(isQuick){
                        isQuickNoteScreen = true
                    }
                    else {
                        var newNote = Note(isSecret = isSec)
                        newNote.content.ensureTrailingText()
                        viewModel.addNote(newNote)
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
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
                val filtered = notes.filter { it.isSecret == isSec }

                items(
                    count = filtered.size,
                    key = { filtered[it].id }
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
                                                        viewModel.select(item.id)
                                                    } else {
                                                        viewModel.deselect(item.id)
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
                                                        viewModel.select(item.id)
                                                    } else {
                                                        viewModel.deselect(item.id)
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
