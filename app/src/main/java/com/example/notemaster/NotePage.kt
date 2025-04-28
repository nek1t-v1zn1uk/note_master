package com.example.notemaster

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.trimmedLength
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.navigation.compose.currentBackStackEntryAsState


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
        var indexInList: Int = 0
        var indexInItem: Int = 0
        var updateValue: (String) -> Unit = {}
        var changeNeeded: Boolean = false
        var cursorStart: Int = 0
        var updateTopBar: () -> Unit = {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePage(noteDao: NoteDao, noteId: Int, navController: NavController){

    var note: Note by remember { mutableStateOf(Note()) }

    LaunchedEffect(noteId) {
        val noteEntity = noteDao.getNoteById(noteId)
        note = noteEntity?.toNote() ?: Note()

        // 1) look at the IDs you *just* deserialized
        val maxLoaded = note.content.list.maxOfOrNull { it.id } ?: 0

        // 2) bump our generator so new items start *after* them
        ContentItem.resetLastId(maxLoaded)

        // 3) add trailing text if needed (and it will get a fresh ID!)
        note.content.ensureTrailingText()
        //Log.d("Shit", "textLaunched:${(note.content.list[0] as ItemText).text}; nodeId:${noteId}")
    }
    note.content.ensureTrailingText()
    var isKeyboard by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    OnKeyboardStartShowing {
        isKeyboard = true
    }
    OnKeyboardStartHiding {
        focusManager.clearFocus() // removes focus from TextField
        isKeyboard = false

        CoroutineScope(Dispatchers.IO).launch {
            noteDao.update(note.toEntity()) // 2. Update in DB
            //Log.d("Shit", "text:${(note.content.list[0] as ItemText).text}; nodeId:${noteId}")
        }
    }

    //start of gallery-picker
    var imageUri = remember { mutableStateOf<Uri?>(null) }
    val waitingForImage = remember { mutableStateOf(false) }
    val context = LocalContext.current
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
        waitingForImage.value = true
        launcher.launch(arrayOf("image/*"))
    }
    //starts when image is pressed
    LaunchedEffect(imageUri.value) {
        imageUri.value?.let { uri ->
            // This block is only triggered after image is selected!
            val newImageItem = ItemImage(uri)
            Log.d("Pictures", "Size:${note.content.list.size}\nFocusedItem.indexInList: ${FocusedItem.indexInList}")
            var item = note.content.list[FocusedItem.indexInList]
            if(item is ItemText) {
                var indexInListOfNewImage = -1

                if (FocusedItem.indexInItem == 0) {
                    Log.d("Pictures", "State: 1")
                    indexInListOfNewImage = 0
                } else if (FocusedItem.indexInItem == item.text.length) {
                    Log.d("Pictures", "State: 2")
                    indexInListOfNewImage = FocusedItem.indexInList + 1
                    if (FocusedItem.indexInList == note.content.list.size - 1)
                        note.content.addComponent(
                            FocusedItem.indexInList + 1,
                            ItemText("")//, style = item.style)
                        )
                } else {
                    Log.d("Pictures", "State: 3")
                    indexInListOfNewImage = FocusedItem.indexInList + 1
                    var firstText = item.text.substring(0, FocusedItem.indexInItem)
                    var secondText =
                        item.text.substring(FocusedItem.indexInItem, item.text.length)
                    Log.d("Shit", "item.text:${item.text};  firstText:${firstText}; secondText:${secondText};")
                    item.text = firstText
                    note.content.addComponent(
                        FocusedItem.indexInList + 1,
                        ItemText(secondText)//, style = item.style)
                    )
                    FocusedItem.updateValue(firstText)
                }
                Log.d("Pictures", "indexInListOfNewImage:${indexInListOfNewImage}")
                note.content.addComponent(
                    indexInListOfNewImage,
                    newImageItem
                )
                imageUri.value = null

                note.lastEdit = LocalDateTime.now()
            }
        }
    }
    //end gallery-picker

    //start of camera
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImagePath by remember { mutableStateOf<Uri?>(null) }
    var waitingForCameraPhoto by remember { mutableStateOf(false) }

    // Generate fresh file + URI
    lateinit var photoFile: File // ← declared at composable level or higher

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
            Log.d("CameraResult", "Camera returned success: $success, URI: $cameraImagePath")
            if (success && cameraImagePath != null) {
                photoUri = cameraImagePath
                waitingForCameraPhoto = false
            }
        }
    )

    fun openCamera() {
        val uri = createCameraImageUri()
        cameraImagePath = uri
        waitingForCameraPhoto = true

        // Grant permission to all camera apps
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

        Log.d("Shit", "File exists: ${photoFile.exists()}, path: ${photoFile.absolutePath}")
        Log.d("Shit", "Launching camera with uri: $uri")

        cameraLauncher.launch(uri)
    }

    // Handle after camera photo is taken
    LaunchedEffect(photoUri) {
        photoUri?.let { uri ->
            val file = File(uri.path ?: "")
            Log.d("PhotoUri", "Handling photo: $uri, file exists: ${file.exists()}")

            try {
                val newImageItem = ItemImage(uri)
                Log.d("MyData", "camera uri:${uri}")
                val item = note.content.list[FocusedItem.indexInList]

                if (item is ItemText) {
                    var indexInListOfNewImage = -1

                    if (FocusedItem.indexInItem == 0) {
                        indexInListOfNewImage = 0
                    } else if (FocusedItem.indexInItem == item.text.length) {
                        indexInListOfNewImage = FocusedItem.indexInList + 1
                        if (FocusedItem.indexInList == note.content.list.size - 1) {
                            note.content.addComponent(
                                FocusedItem.indexInList + 1,
                                ItemText("")//, style = item.style)
                            )
                        }
                    } else {
                        indexInListOfNewImage = FocusedItem.indexInList + 1
                        val firstText = item.text.substring(0, FocusedItem.indexInItem)
                        val secondText = item.text.substring(FocusedItem.indexInItem)
                        item.text = firstText
                        note.content.addComponent(
                            FocusedItem.indexInList + 1,
                            ItemText(secondText)//, style = item.style)
                        )
                        FocusedItem.updateValue(firstText)
                    }

                    note.content.addComponent(indexInListOfNewImage, newImageItem)
                }

                photoUri = null

                note.lastEdit = LocalDateTime.now()
            } catch (e: Exception) {
                Log.e("CrashHandler", "Error while inserting image: ${e.message}", e)
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
    // 1) Get the current NavBackStackEntry as Compose State
    val backStackEntry by navController.currentBackStackEntryAsState()

    // 2) Create a StateFlow<String?> from SavedStateHandle["drawingPath"]
    val drawingPathFlow = backStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("drawingPath", null)

    // 3) Collect it into Compose State
    val drawingPath by (drawingPathFlow?.collectAsState() ?: remember { mutableStateOf<String?>(null) })

    // 4) When a new path arrives, inject your ItemImage and clear the key
    LaunchedEffect(drawingPath) {
        drawingPath?.let { path ->
            val uri = Uri.fromFile(File(path))
            // append right after the focused item (or wherever you’d like)
            note.content.addComponent(
                index = FocusedItem.indexInList + 1,
                item = ItemImage(uri)
            )
            // remove so it doesn’t fire again
            backStackEntry
                ?.savedStateHandle
                ?.remove<String>("drawingPath")
        }
    }
    //end of drawing

    Scaffold(
        containerColor = Color.White,
        //contentWindowInsets = WindowInsets(0),
        // contentWindowInsets: only status+nav bars
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars),
        // push *only* the bottomBar by the IME height
        modifier = Modifier
            //.navigationBarsPadding()
            //.imePadding()
            //.windowInsetsPadding(WindowInsets.navigationBars)
            //.windowInsetsPadding(WindowInsets.ime)
                ,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                noteDao.update(note.toEntity())
                                withContext(Dispatchers.Main) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    ) {
                        Icon (
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = Color.Black,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            if(isKeyboard)
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
                            //checkbox
                            IconButton(
                                onClick = {
                                    val item = note.content.list.get(FocusedItem.indexInList)
                                    if (item is ItemText) {

                                        var firstText = ""
                                        var checkBoxText = ""
                                        var lastText = ""

                                        var i = FocusedItem.indexInItem
                                        var indexBefore = i - 1
                                        var indexAfter = i
                                        while (indexBefore != 0 && item.text[indexBefore] != '\n') {
                                            indexBefore--
                                        }
                                        while (indexAfter != item.text.length && item.text[indexAfter] != '\n') {
                                            indexAfter++
                                        }


                                        lastText = item.text.substring(indexAfter, item.text.length)
                                        checkBoxText =
                                            item.text.substring(indexBefore + 1, indexAfter)
                                        if (indexBefore != 0)
                                            firstText = item.text.substring(0, indexBefore)

                                        val tempIndexInList = FocusedItem.indexInList
                                        FocusedItem.changeNeeded = true
                                        FocusedItem.indexInList += 1
                                        FocusedItem.cursorStart =
                                            FocusedItem.indexInItem - firstText.length

                                        note.content.addComponent(
                                            tempIndexInList,
                                            ItemCheckBox(checkBoxText)//, style = item.style)
                                        )
                                        if (indexBefore != 0)
                                            note.content.addComponent(
                                                tempIndexInList,
                                                ItemText(firstText)//, style = item.style)
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
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(36.dp)
                                )
                            }
                            //highlight(temporary camera)
                            /*IconButton(
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
                                    painter = painterResource(R.drawable.stylus_highlighter),
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(36.dp)
                                )
                            }*/
                            //bigger text
                            /*IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextIncrease,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(36.dp)
                                )
                            }
                            //smaller text
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
                            //bold text
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatBold,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(36.dp)
                                )
                            }
                            */
                        }
                    }
                }
        }
    ) {
        innerPadding ->

        NoteContent(note,
            Modifier
                .padding(innerPadding)
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
            NoteContentTop(
                note,
                modifier = Modifier.onGloballyPositioned { coords ->
                    lazyColumnHeightPx -= coords.size.height
                }
                    .padding(bottom = 16.dp)
            )
        }
        note.content.ensureTrailingText()
        val content: MutableList<ContentItem> = note.content.list


        items(
            count = content.size,
            key = { content[it].id } // 👈 This ensures Compose tracks each item correctly
        ) { index ->
            val focusRequester = remember { FocusRequester() }
            var modifierPart: Modifier = Modifier
            val item = content[index]

            when (item) {
                is ItemCheckBox -> {
                    CheckBoxPart(
                        item.text,
                        item.hasCheckBox,
                        index,
                        focusRequester,
                        Modifier.onGloballyPositioned { coords ->
                            lazyColumnHeightPx -= coords.size.height
                        }
                    )
                }

                is ItemText -> {
                    if (index == content.lastIndex) {
                        modifierPart = modifierPart.heightIn(min = with(density) { lazyColumnHeightPx.toDp() })
                    }
                    Log.d("Shit", "Before:${item.text}")
                    TextPart(
                        note,
                        item,
                        index,
                        focusRequester,
                        modifierPart.fillMaxSize()
                            .onGloballyPositioned { coords ->
                                lazyColumnHeightPx -= coords.size.height
                            }
                    )
                }

                is ItemImage -> {
                    ImagePart(
                        note = note,
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
fun NoteContentTop(note: Note, modifier: Modifier = Modifier){

    var nameValue = note.name // by remember { mutableStateOf(note.name) }
    var dateValue = note.lastEdit //by remember { mutableStateOf(note.lastEdit) }
    var symbolCount = note.content.getSymbolsCount() //by remember { mutableStateOf(note.content.getSymbolsCount()) }

    var refreshTrigger by remember { mutableStateOf(0) }
    FocusedItem.updateTopBar = {
        refreshTrigger++
    }
    LaunchedEffect(refreshTrigger) {
        Log.d("Mmm", "mmm")
    }

    Column(
        modifier = modifier
    ) {
        TextField(
            value = nameValue,
            onValueChange = {
                //nameValue = it
                note.name = it
                note.lastEdit = LocalDateTime.now()
                FocusedItem.updateTopBar()
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
    note: Note,
    item: ItemText,
    indexInList: Int,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier){

    var textFieldValue by remember { mutableStateOf(TextFieldValue(item.text)) }

    LaunchedEffect(item.text) {
        if (indexInList == FocusedItem.indexInList && FocusedItem.changeNeeded) {
            Log.d("Shit", "TB - indexInList:${indexInList}; ")
            focusRequester.requestFocus()
            textFieldValue = textFieldValue.copy(selection = TextRange(FocusedItem.cursorStart))
            FocusedItem.changeNeeded = false
        }
        textFieldValue = textFieldValue.copy(text = item.text)
        Log.d("Shit", "textFieldValue:${textFieldValue.text}; ")
    }

    TextField(
        value = textFieldValue,
        onValueChange = { newValue ->

            if(textFieldValue.text != newValue.text)
                note.lastEdit = LocalDateTime.now()

            textFieldValue = newValue
            item.text = textFieldValue.text

            FocusedItem.indexInList = indexInList
            FocusedItem.indexInItem = newValue.selection.start
            FocusedItem.updateValue = { value: String ->
                textFieldValue = textFieldValue.copy(text = value)
            }
            FocusedItem.updateTopBar()

            /*
            val selection = newValue.selection
            if (!selection.collapsed) {
                val selectedText = newValue.text.substring(selection.start, selection.end)
                Log.d("Selection", "Selected: $selectedText")
            }
            */
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

@Composable
fun ImagePart(
    note: Note,
    item: ItemImage,
    indexInList: Int,
    modifier: Modifier = Modifier
){
    Image(
        painter = rememberAsyncImagePainter(item.uri),
        contentDescription = null,
        modifier = modifier
            .size(200.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
    )
}


@Preview(wallpaper = Wallpapers.NONE)
@Composable
fun NotePagePreviw(){
    //SHIIIITTTTT
    /*var content = Content()
    var list: MutableList<ContentItem> = mutableListOf()
    list.add(ItemText("Some shitted Text"))
    list.add(ItemCheckBox("Choose it"))
    list.add(ItemText("Another shitted Text\nSHEESH\nFuck you"))
    content.list = list
    val note = Note(0, "Name", content)
    NotePage(0, null, rememberNavController())*/
}