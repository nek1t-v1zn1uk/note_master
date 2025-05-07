package com.example.notemaster.pages

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import java.util.Calendar
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.example.notemaster.data.ContentItem
import com.example.notemaster.data.ItemFile
import com.example.notemaster.data.ItemImage
import com.example.notemaster.data.ItemText
import com.example.notemaster.data.Reminder
import com.example.notemaster.database.NoteDao
import com.example.notemaster.viewmodels.NotePageViewModel
import com.example.notemaster.viewmodels.NotePageViewModelFactory
import com.example.notemaster.viewmodels.NoteUiState
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.LinkedList
import java.util.Locale
import java.util.Stack
import kotlin.math.max
import kotlin.math.min


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

fun getFileName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) {
            return it.getString(nameIndex)
        }
    }
    return "unknown_file"
}

fun evaluateExpression(input: String): Double {
    // 1) Normalize commas to dots
    val expr = input.replace(',', '.').replace("\\s+".toRegex(), "")
    // 2) Tokenize
    val tokens = tokenize(expr)
    // 3) Infix → RPN
    val outputQueue = LinkedList<String>()
    val opStack = Stack<String>()

    fun precedence(op: String) = when (op) {
        "+", "-" -> 1
        "*", "/" -> 2
        else     -> 0
    }

    for (tok in tokens) {
        when {
            tok.matches("""-?\d+(\.\d+)?""".toRegex()) -> // number (with optional leading –)
                outputQueue += tok
            tok in listOf("+", "-", "*", "/") -> {
                while (opStack.isNotEmpty() &&
                    precedence(opStack.peek()) >= precedence(tok)) {
                    outputQueue += opStack.pop()
                }
                opStack.push(tok)
            }
            tok == "(" ->
                opStack.push(tok)
            tok == ")" -> {
                while (opStack.isNotEmpty() && opStack.peek() != "(") {
                    outputQueue += opStack.pop()
                }
                if (opStack.isEmpty() || opStack.pop() != "(")
                    throw IllegalArgumentException("Mismatched parentheses in \"$input\"")
            }
        }
    }
    while (opStack.isNotEmpty()) {
        val op = opStack.pop()
        if (op in listOf("(", ")"))
            throw IllegalArgumentException("Mismatched parentheses in \"$input\"")
        outputQueue += op
    }

    // 4) Evaluate RPN
    val evalStack = Stack<Double>()
    for (tok in outputQueue) {
        if (tok.matches("""-?\d+(\.\d+)?""".toRegex())) {
            evalStack.push(tok.toDouble())
        } else {
            if (evalStack.size < 2)
                throw IllegalArgumentException("Bad expression: \"$input\"")
            val b = evalStack.pop()
            val a = evalStack.pop()
            val res = when (tok) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> a / b
                else -> throw IllegalStateException("Unknown operator $tok")
            }
            evalStack.push(res)
        }
    }
    if (evalStack.size != 1)
        throw IllegalArgumentException("Bad expression: \"$input\"")
    return evalStack.pop()
}

private fun tokenize(s: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    while (i < s.length) {
        when (val c = s[i]) {
            in '0'..'9', '.' -> {
                val start = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                tokens += s.substring(start, i)
            }
            '+' , '*', '/', '(', ')' -> {
                tokens += c.toString()
                i++
            }
            '-' -> {
                // unary minus if at start or after '(' or another operator
                if (i == 0 || s[i-1] == '(' || s[i-1] in "+-*/") {
                    val start = i
                    i++
                    // consume digits and dot
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    tokens += s.substring(start, i)
                } else {
                    tokens += "-"
                    i++
                }
            }
            else ->
                throw IllegalArgumentException("Invalid character ‘$c’ in expression")
        }
    }
    return tokens
}


/*
class FocusedItem {
    companion object {
        var indexInList: Int = 0
        var indexInItem: Int = 0
        var updateValue: (String) -> Unit = {}
        var changeNeeded: Boolean = false
        var cursorStart: Int = 0
        var updateTopBar: () -> Unit = {}
        var updateContent: () -> Unit = {}
        lateinit var noteDao: NoteDao
        lateinit var focusManager: FocusManager
        var canCalculate = false
        var makeCalculations: () -> Unit = {}
        var updateCalculator: (Boolean) -> Unit = {}
        lateinit var navController: NavController
    }
}
*/
@Composable
fun NotificationPermissionRequester(onGranted: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGranted()
        } else {
            Toast.makeText(context, "Дозвіл на сповіщення відхилено", Toast.LENGTH_SHORT).show()
        }
    }

    // Викликаємо запит десь в UI, наприклад при завантаженні екрана або по кнопці
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Перевіряємо, чи вже є дозвіл
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Запитуємо дозвіл
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onGranted()
            }
        } else {
            onGranted()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePage(noteDao: NoteDao, noteId: Int, navController: NavController){

    val application = LocalContext.current.applicationContext as Application
    val factory = remember { NotePageViewModelFactory(application, noteDao, noteId) }
    val vm: NotePageViewModel = viewModel(factory = factory)

    val uiState by vm.uiState.collectAsState()

    when (uiState) {
        is NoteUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is NoteUiState.Error -> {}
        is NoteUiState.Success -> {
            val note by vm.note.collectAsState()

            val context = LocalContext.current

            var isKeyboard by remember { mutableStateOf(false) }

            val focusManager = LocalFocusManager.current
            OnKeyboardStartShowing {
                isKeyboard = true
            }
            OnKeyboardStartHiding {
                focusManager.clearFocus()
                isKeyboard = false

                //?
                vm.updateNote()
            }

//start of gallery-picker
            var imageUri = remember { mutableStateOf<Uri?>(null) }
            val waitingForImage = remember { mutableStateOf(false) }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri ->
                    if (uri != null) {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        imageUri.value = null
                        imageUri.value = uri
                        waitingForImage.value = false
                    }
                }
            )

            fun openGallery() {
                imageUri.value = null
                waitingForImage.value = true
                launcher.launch(arrayOf("image/*"))
            }
            //starts when image is pressed
            LaunchedEffect(imageUri.value) {
                imageUri.value?.let { uri ->
                    // 1. Copy the image to app's internal storage
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val fileName = "image_${System.currentTimeMillis()}.jpg"
                    val outputFile = File(context.filesDir, fileName)
                    val outputStream = FileOutputStream(outputFile)

                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 2. Get the URI of the copied image
                    val localUri = outputFile.toUri()

                    // 3. Continue with localUri instead of the original one
                    val newImageItem = ItemImage(localUri)

                    val item = vm.getFocusedItem()
                    if (item is ItemText) {
                        var indexInListOfNewImage = -1

                        if (vm.indexInItem.value == 0) {
                            indexInListOfNewImage = vm.itemIndexInList.value
                            vm.setItemIndexInList(vm.itemIndexInList.value + 1)
                        } else if (vm.indexInItem.value == item.text.length) {
                            indexInListOfNewImage = vm.itemIndexInList.value + 1
                            if (vm.itemIndexInList.value == vm.getContentListSize() - 1)
                                vm.addItem(
                                    vm.itemIndexInList.value + 1,
                                    ItemText("")
                                )
                        } else {
                            indexInListOfNewImage = vm.itemIndexInList.value + 1
                            var firstText = item.text.substring(0, vm.indexInItem.value)
                            var secondText = item.text.substring(vm.indexInItem.value)
                            item.text = firstText
                            vm.addItem(
                                vm.itemIndexInList.value + 1,
                                ItemText(secondText)
                            )
                            //!
                            //FocusedItem.updateValue(firstText)
                        }

                        vm.addItem(
                            indexInListOfNewImage,
                            newImageItem
                        )
                        imageUri.value = null
                    }
                }
            }

//end gallery-picker

// start of camera
            var photoUri by remember { mutableStateOf<Uri?>(null) }
            var cameraImagePath by remember { mutableStateOf<Uri?>(null) }
            var waitingForCameraPhoto by remember { mutableStateOf(false) }

            lateinit var photoFile: File

            fun createCameraImageUri(): Uri {
                photoFile = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
                if (!photoFile.exists()) photoFile.createNewFile()
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
            }

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture(),
                onResult = { success ->
                    if (success && cameraImagePath != null) {
                        val fileUri = Uri.fromFile(photoFile)
                        photoUri = fileUri
                        waitingForCameraPhoto = false

                        context.revokeUriPermission(
                            cameraImagePath!!,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
            )

            fun openCamera() {
                val uri = createCameraImageUri()
                cameraImagePath = uri
                waitingForCameraPhoto = true

                val resInfoList = context.packageManager.queryIntentActivities(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(
                        packageName,
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                cameraLauncher.launch(uri)
            }

            LaunchedEffect(photoUri) {
                photoUri?.let { uri ->
                    val file = File(uri.path ?: "")

                    try {
                        val newImageItem = ItemImage(uri)
                        val item = vm.getFocusedItem()

                        if (item is ItemText) {
                            var indexInListOfNewImage = -1

                            if (vm.indexInItem.value == 0) {
                                indexInListOfNewImage = vm.itemIndexInList.value
                                vm.setItemIndexInList(vm.itemIndexInList.value + 1)
                            } else if (vm.indexInItem.value == item.text.length) {
                                indexInListOfNewImage = vm.itemIndexInList.value + 1
                                if (vm.itemIndexInList.value == vm.getContentListSize() - 1) {
                                    vm.addItem(
                                        vm.itemIndexInList.value + 1,
                                        ItemText("")
                                    )
                                }
                            } else {
                                indexInListOfNewImage = vm.itemIndexInList.value + 1
                                val firstText = item.text.substring(0, vm.indexInItem.value)
                                val secondText = item.text.substring(vm.indexInItem.value)
                                item.text = firstText
                                vm.addItem(
                                    vm.itemIndexInList.value + 1,
                                    ItemText(secondText)
                                )
                                //!
                                //FocusedItem.updateValue(firstText)
                            }
                            vm.addItem(indexInListOfNewImage, newImageItem)
                        }

                        photoUri = null

                    } catch (e: Exception) {
                    }
                }
            }


            //for permisssions
            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted ->
                    if (granted) openCamera()
                    // else: Show a dialog or snack
                }
            )
//end of camera

//start of drawing
            val backStackEntry by navController.currentBackStackEntryAsState()

            val drawingPathFlow = backStackEntry
                ?.savedStateHandle
                ?.getStateFlow<String?>("drawingPath", null)

            val drawingPath by (drawingPathFlow?.collectAsState()
                ?: remember { mutableStateOf<String?>(null) })

            LaunchedEffect(drawingPath) {
                drawingPath?.let { path ->
                    val uri = Uri.fromFile(File(path))

                    val newImageItem = ItemImage(uri)
                    val item = vm.getFocusedItem()
                    if (item is ItemText) {
                        var indexInListOfNewImage = -1

                        if (vm.indexInItem.value == 0) {
                            indexInListOfNewImage = vm.itemIndexInList.value
                            vm.setItemIndexInList(vm.itemIndexInList.value + 1)
                        } else if (vm.indexInItem.value == item.text.length) {
                            indexInListOfNewImage = vm.itemIndexInList.value + 1
                            if (vm.itemIndexInList.value == vm.getContentListSize() - 1) {
                                vm.addItem(
                                    vm.itemIndexInList.value + 1,
                                    ItemText("")
                                )
                            }
                        } else {
                            indexInListOfNewImage = vm.itemIndexInList.value + 1
                            val firstText = item.text.substring(0, vm.indexInItem.value)
                            val secondText = item.text.substring(vm.indexInItem.value)

                            item.text = firstText
                            vm.addItem(
                                vm.itemIndexInList.value + 1,
                                ItemText(secondText)
                            )
                            //!
                            //FocusedItem.updateValue(firstText)
                        }

                        vm.addItem(indexInListOfNewImage, newImageItem)
                    }

                    photoUri = null

                    backStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("drawingPath")

                }
            }
//end of drawing

//start of file picking
            val fileUri = remember { mutableStateOf<Uri?>(null) }
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri ->
                    uri?.let {
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        fileUri.value = it
                    }
                }
            )

            fun openFilePicker() {
                fileUri.value = null
                filePickerLauncher.launch(arrayOf("*/*"))
            }

            LaunchedEffect(fileUri.value) {
                fileUri.value?.let { uri ->
                    val fileName = getFileName(context, uri)
                    val newFileItem = ItemFile(uri, fileName)

                    var item = vm.getFocusedItem()
                    if (item is ItemText) {
                        var indexInListOfNewFile = -1

                        if (vm.indexInItem.value == 0) {
                            indexInListOfNewFile = vm.itemIndexInList.value
                            vm.setItemIndexInList(vm.itemIndexInList.value + 1)
                        } else if (vm.indexInItem.value == item.text.length) {
                            indexInListOfNewFile = vm.itemIndexInList.value + 1
                            if (vm.itemIndexInList.value == vm.getContentListSize() - 1)
                                vm.addItem(
                                    vm.itemIndexInList.value + 1,
                                    ItemText("")
                                )
                        } else {
                            indexInListOfNewFile = vm.itemIndexInList.value + 1
                            var firstText = item.text.substring(0, vm.indexInItem.value)
                            var secondText = item.text.substring(vm.indexInItem.value)
                            item.text = firstText
                            vm.addItem(
                                vm.itemIndexInList.value + 1,
                                ItemText(secondText)
                            )
                            //!
                            //FocusedItem.updateValue(firstText)
                        }

                        vm.addItem(
                            indexInListOfNewFile,
                            newFileItem
                        )

                        focusManager.clearFocus()
                        isKeyboard = false
                    }

                }
            }
//end of file picking

            Scaffold(
                containerColor = Color.White,
                contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars),
                modifier = Modifier,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                        ),
                        title = { },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    //?
                                    vm.updateNote()
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    tint = Color.Black,
                                    contentDescription = null
                                )
                            }
                        },
                        actions = {
                            var expanded by remember { mutableStateOf(false) }
                            var showDatePicker by remember { mutableStateOf(false) }
                            var selectedDateMillis: Long? by remember { mutableStateOf(0) }
                            IconButton(
                                onClick = {
                                    expanded = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    tint = Color.Black,
                                    contentDescription = null
                                )
                            }
                            if (showDatePicker) {
                                DateTimePickerModal(
                                    onDateTimeSelected = { millis ->
                                        selectedDateMillis = millis
                                        vm.setReminder(
                                            Reminder(
                                                Instant.ofEpochMilli(millis!!)
                                                    .atZone(ZoneId.systemDefault())
                                                    .toLocalDateTime()
                                            )
                                        )

                                        //!
                                        //FocusedItem.updateTopBar()
                                    },
                                    onDismiss = { showDatePicker = false },
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                containerColor = Color.White
                            ) {
                                if (note!!.hasReminder()) {
                                    DropdownMenuItem(
                                        text = { Text("Видалити нагадування", color = Color.Black) },
                                        onClick = {
                                            expanded = false
                                            vm.setReminder(null)
                                            //!
                                            //FocusedItem.updateTopBar()
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Добавити нагадування", color = Color.Black) },
                                        onClick = {
                                            expanded = false
                                            showDatePicker = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(text = if (note!!.isSecret) "Показати" else "Приховати", color = Color.Black) },
                                    onClick = {
                                        expanded = false
                                        vm.toggleSecret()
                                    }
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (isKeyboard)
                        BottomAppBar(
                            containerColor = Color.White,
                            contentColor = Color.White,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.ime)
                                .height(56.dp)
                        ) {
                            Column() {
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp)
                                        .background(Color.Gray, RoundedCornerShape(2.dp))
                                )
                                Row {
                                    //gallery
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
                                            tint = Color.Black,
                                            modifier = Modifier
                                                .size(36.dp)
                                        )
                                    }
                                    //drawing
                                    IconButton(
                                        onClick = {
                                            navController.navigate("drawing")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Draw,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier
                                                .size(36.dp)
                                        )
                                    }
                                    //camera
                                    IconButton(
                                        onClick = {
                                            if (ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.CAMERA
                                                ) == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                openCamera()
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier
                                                .size(36.dp)
                                        )
                                    }
                                    //file picker
                                    IconButton(
                                        onClick = {
                                            openFilePicker()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AttachFile,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier
                                                .size(36.dp)
                                        )
                                    }
                                    //calculator
                                    val canCalculate by vm.canCalculate.collectAsState()
                                    IconButton(
                                        onClick = {
                                            if (vm.canCalculate.value) {
                                                vm.makeCalculations()
                                                vm.setCanCalculate(false)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = null,
                                            tint = if (canCalculate) Color.Black else Color.Gray,
                                            modifier = Modifier
                                                .size(36.dp)
                                        )
                                    }
                                }
                            }
                        }
                }
            ) { innerPadding ->

                NoteContent(
                    vm,
                    navController,
                    Modifier
                        .padding(innerPadding)
                )
            }
        }

    }
}


@Composable
fun NoteContent(
    vm: NotePageViewModel,
    navController: NavController,
    modifier: Modifier
){

    val note by vm.note.collectAsState()

    val contentListSnapshot = remember(note) {
        // Робимо чистий List, щоб він не змінювався в середині Composition
        note?.content?.list.orEmpty().toList()
    }

    val density = LocalDensity.current
    var lazyColumnHeightPx by remember { mutableStateOf(0) }

    //?
//    var recomposeTrigger by remember { mutableStateOf(0) }
//    val forceRecompose: () -> Unit = { recomposeTrigger++ }
//    FocusedItem.updateContent = forceRecompose

    //key(recomposeTrigger) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    lazyColumnHeightPx = coords.size.height
                }
        ) {
            item {
                NoteContentTop(
                    vm,
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            lazyColumnHeightPx -= coords.size.height
                        }
                        .padding(bottom = 16.dp)
                )
            }


            itemsIndexed(
                items = contentListSnapshot,
                key   = { _, item -> item.id }
            ) { index, item ->
                val focusRequester = remember { FocusRequester() }
                var modifierPart: Modifier = Modifier

                when (item) {

                    is ItemText -> {
                        if (index == contentListSnapshot.lastIndex) {
                            modifierPart =
                                modifierPart.heightIn(min = with(density) { lazyColumnHeightPx.toDp() })
                        }
                        TextPart(
                            vm,
                            item,
                            index,
                            focusRequester,
                            modifierPart
                                .fillMaxSize()
                                .onGloballyPositioned { coords ->
                                    lazyColumnHeightPx -= coords.size.height
                                }
                        )
                    }

                    is ItemImage -> {
                        ImagePart(
                            vm = vm,
                            item = item,
                            indexInList = index,
                            navController = navController,
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    lazyColumnHeightPx -= coords.size.height
                                }
                        )
                    }

                    is ItemFile -> {
                        FilePart(
                            vm = vm,
                            item = item,
                            indexInList = index,
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    lazyColumnHeightPx -= coords.size.height
                                }
                        )
                    }
                }
            }

        }
    //}
}

@Composable
fun NoteContentTop(vm: NotePageViewModel, modifier: Modifier = Modifier){

    val note by vm.note.collectAsState()

    var context = LocalContext.current
    var nameValue by remember { mutableStateOf(TextFieldValue(note!!.name)) }
    //var dateValue = note!!.lastEdit
    //var symbolCount = note!!.content.getSymbolsCount()

    //?
    /*
    var refreshTrigger by remember { mutableStateOf(0) }
    FocusedItem.updateTopBar = {
        refreshTrigger++
    }
    LaunchedEffect(refreshTrigger) {
        Log.d("Mmm", "mmm")
    }
    */
    Column(
        modifier = modifier
    ) {
        TextField(
            value = nameValue,
            onValueChange = { newValue ->

                if(nameValue.text != newValue.text) {
                    vm.setName(newValue.text)
                }

                nameValue = newValue

                //?
                //FocusedItem.updateTopBar()
            },
            placeholder = { Text("Назва", fontSize = 24.sp) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 24.sp, color = Color.Black),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
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

                )
        )
        //note.lastEdit = note.lastEdit.withYear(2024)
        var format = "dd MMMM HH:mm"
        if (note!!.lastEdit.year != LocalDateTime.now().year)
            format = "dd MMMM yyyy HH:mm"
        Text(
            text = note!!.lastEdit.format(
                DateTimeFormatter.ofPattern(
                    format,
                    Locale("uk")
                )
            ) + "   |   " + note!!.content.getSymbolsCount() + " символ(-ів)",
            color = Color.Gray,
            modifier = Modifier
                .padding(start = 14.dp)
        )

        if(note!!.hasReminder()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp,  Color(red = 100, green = 100, blue = 255), RoundedCornerShape(8.dp))
                    .background(Color(red = 100, green = 100, blue = 255, alpha = 40))
            ) {
                var showDatePicker by remember { mutableStateOf(false) }
                if(showDatePicker){
                    DateTimePickerModal(
                        onDateTimeSelected = { millis ->
                            vm.setReminder(
                                Reminder(
                                    Instant.ofEpochMilli(millis!!).atZone(ZoneId.systemDefault())
                                        .toLocalDateTime(),
                                    note!!.reminder!!.descrition
                                )
                            )
                            //?
                            //FocusedItem.updateTopBar()
                        },
                        onDismiss = { showDatePicker = false },
                    )
                }
                val format = "dd.MM.yyyy\nHH:mm"
                Text(
                    text = note!!.reminder!!.date.format(DateTimeFormatter.ofPattern(format)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxHeight()
                        .clickable(onClick = {
                            showDatePicker = true
                        })
                )

                TextField(
                    value = note!!.reminder!!.descrition,
                    onValueChange = {
                        vm.setReminder(
                            Reminder(
                                note!!.reminder!!.date,
                                it
                            )
                        )
                    } ,
                    placeholder = { Text("Опис...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
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
            }
        }
    }

}

@Composable
fun TextPart(
    vm: NotePageViewModel,
    item: ItemText,
    indexInList: Int,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier){

    val note by vm.note.collectAsState()

    var textFieldValue by remember { mutableStateOf(TextFieldValue(item.text)) }

    //?
    LaunchedEffect(item.text) {
        if (indexInList == vm.itemIndexInList.value && vm.changeNeeded.value) {
            //Крч воно тут було, але з ним калькулятор не працював
            //focusRequester.requestFocus()
            textFieldValue = textFieldValue.copy(text = item.text, selection = TextRange(vm.indexInItem.value))
            vm.setChangeNeeded(false)
        }
        textFieldValue = textFieldValue.copy(text = item.text)
    }

    TextField(
        value = textFieldValue,
        onValueChange = { newValue ->

            if(textFieldValue.text != newValue.text)
                vm.updateLastEdit()

            textFieldValue = newValue
            item.text = textFieldValue.text

            vm.setItemIndexInList(indexInList)
            vm.setIndexInItem(newValue.selection.start)
            //?
//            FocusedItem.updateValue = { value: String ->
//                textFieldValue = textFieldValue.copy(text = value)
//            }
            //?
            //FocusedItem.updateTopBar()

            vm.checkCalculate()
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
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
        singleLine = false,
        maxLines = Int.MAX_VALUE,
        textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    val cursorPosition = textFieldValue.selection.start

                    vm.setItemIndexInList(indexInList)
                    vm.setIndexInItem(cursorPosition)

                    //?
//                    FocusedItem.updateValue = { value: String ->
//                        textFieldValue = textFieldValue.copy(text = value)
//                    }

                    //?
                    //FocusedItem.updateTopBar()
                }
            }
    )
}


@Composable
fun ImagePart(
    vm: NotePageViewModel,
    item: ItemImage,
    indexInList: Int,
    navController: NavController,
    modifier: Modifier = Modifier
){

    val note by vm.note.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current


    val size = remember(item.uri) {
        getImageSize(context, item.uri)
    }
    val width = size?.first?:0
    val height = size?.second?:0
    var widthOfImage = 0
    var heightOfImage = 0
    if(height > 200){
        heightOfImage = 200
        widthOfImage = 200 * width / height
    }
    else{
        heightOfImage = height
        widthOfImage = width
    }

    if(widthOfImage > LocalConfiguration.current.screenWidthDp - 40){
        widthOfImage = LocalConfiguration.current.screenWidthDp - 40
        heightOfImage = height * widthOfImage / width
    }


    var menuExpanded by remember { mutableStateOf(false) }
    var imageOffsetOnScreen by remember { mutableStateOf(Offset.Zero) }

    var globalHeightOfImage  by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
            .clickable(onClick = {
                focusManager.clearFocus()
                menuExpanded = true
            })
        /*
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val down = event.changes.firstOrNull()?.takeIf { it.pressed }
                    if (down != null) {
                        clickOffset = down.position
                        menuExpanded = true
                    }
                }
            }
        }
        */
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.uri),
            contentDescription = null,
            modifier = modifier
                .size(width = widthOfImage.dp, height = heightOfImage.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                .onGloballyPositioned { coords ->
                    val position = coords.positionInWindow()
                    imageOffsetOnScreen = position
                    globalHeightOfImage = coords.size.height
                }
        )
        if (menuExpanded) {
            val popupX = 20f
            val popupY = max(globalHeightOfImage / 2f - 20 * globalHeightOfImage / heightOfImage, 0f)

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(popupX.toInt(), popupY.toInt()),
                onDismissRequest = { menuExpanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
                ) {
                    IconButton(
                        onClick = {
                            menuExpanded = false
                            navController.navigate("drawing?imageUri=${item.uri.toString()}")
                        },
                        modifier = Modifier
                            .size(width = 50.dp, height = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            tint = Color.Black,
                            contentDescription = null,
                            modifier = Modifier
                                .size(30.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val file = File(item.uri.path ?: "")
                            val sharedUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            val mimeType = context.contentResolver.getType(sharedUri) ?: "image/*"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(sharedUri, mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(intent, "Відкрити зображення")
                            val canOpen = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
                            if (canOpen) {
                                context.startActivity(chooser)
                            } else {
                                Toast.makeText(context, "Немає додатків для відкриття зображення", Toast.LENGTH_SHORT).show()
                            }

                        },
                        modifier = Modifier
                            .size(width = 50.dp, height = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            tint = Color.Black,
                            contentDescription = null,
                            modifier = Modifier
                                .size(30.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            menuExpanded = false
                            vm.removeItemAt(indexInList)

                            //?
                            //FocusedItem.updateContent()
                        },
                        modifier = Modifier
                            .size(width = 50.dp, height = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            tint = Color.Black,
                            contentDescription = null,
                            modifier = Modifier
                                .size(30.dp)
                        )
                    }
                }
            }
        }
    }
}
fun getImageSize(context: Context, uri: Uri): Pair<Int, Int>? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            Pair(options.outWidth, options.outHeight)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun FilePart(
    vm: NotePageViewModel,
    item: ItemFile,
    indexInList: Int,
    modifier: Modifier = Modifier
) {
    val note by vm.note.collectAsState()


    val context = LocalContext.current

    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF2F2F2))
            .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val mimeType = context.contentResolver.getType(item.uri) ?: "*/*"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(item.uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(intent, "Відкрити файл")
                        val canOpen = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
                        if (canOpen) {
                            context.startActivity(chooser)
                        } else {
                            Toast.makeText(context, "Немає додатків для відкриття", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLongPress = {
                        menuExpanded = true
                    }
                )
            }
            .padding(12.dp)
    ) {
        Text(
            text = "📄 ${item.fileName}",
            color = Color.Black,
            fontSize = 16.sp
        )


        if (menuExpanded) {
            Popup(
                alignment = Alignment.CenterStart,
                offset = IntOffset(0, 0),
                onDismissRequest = { menuExpanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
                ) {
                    IconButton(onClick = {
                        menuExpanded = false
                        val mimeType = context.contentResolver.getType(item.uri) ?: "*/*"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(item.uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(intent, "Відкрити файл")
                        val canOpen = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
                        if (canOpen) {
                            context.startActivity(chooser)
                        } else {
                            Toast.makeText(context, "Немає додатків для відкриття", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.Black)
                    }

                    IconButton(onClick = {
                        menuExpanded = false
                        vm.removeItemAt(indexInList)
                        //?
                        //FocusedItem.updateContent()
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Black)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerModal(
    onDateTimeSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    initialTime: Long? = null
) {

    var step by remember { mutableStateOf(0) }

    var pickedDateMillis by remember { mutableStateOf<Long?>(initialTime ?: System.currentTimeMillis()) }


    if (step == 0) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = pickedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = dateState.selectedDateMillis
                    step = 1
                }) { Text("Далі") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Скасувати") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (step == 1 && pickedDateMillis != null) {

        val timeState = rememberTimePickerState(
            initialHour = 0,
            initialMinute = 0,
            is24Hour = true
        )

        AlertDialog(
            text = {
                TimePicker(state = timeState)
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val pickedCalendar = Calendar.getInstance().apply {
                        timeInMillis = pickedDateMillis!!
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                    }
                    onDateTimeSelected(pickedCalendar.timeInMillis)
                    onDismiss()
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Скасувати") }
            }
        )
    }
}
