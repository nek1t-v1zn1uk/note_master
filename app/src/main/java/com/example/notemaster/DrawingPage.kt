package com.example.notemaster

import android.R.attr.delay
import kotlinx.coroutines.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
// for Compose drawing
import androidx.compose.ui.graphics.Path as AndroidPath
// for Android Canvas bitmap saving
import android.graphics.Path
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.*
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.BottomAppBar
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Job
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class MyStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingPage(navController: NavController, existingImageUri: Uri? = null) {
    val context = LocalContext.current

    val strokes = remember { mutableStateListOf<MyStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(4f) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var scale by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }

    var isDrawing by remember { mutableStateOf(false) }

    val drawingDebounceDelay = 100L // Adjust as needed
    var drawingJob by remember { mutableStateOf<Job?>(null) }
    var isPotentialDrawing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope() // Add this line

    val backgroundImage = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(existingImageUri) {
        existingImageUri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                backgroundImage.value = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch (e: Exception) {
                Log.e("DrawingPage", "Error loading background image: ${e.message}", e)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                ),
                actions = {
                    IconButton(onClick = { strokes.clear() }) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = { if (strokes.isNotEmpty()) strokes.removeLast() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = {
                        if (existingImageUri != null) {
                            val savedUri = updateImageWithDrawing(context, existingImageUri, strokes)
                            savedUri?.let {
                                // Optionally, notify the user about successful save
                            }
                        } else {
                            val file = saveDrawingToPng(context, strokes, panX, panY, scale, canvasSize.width, canvasSize.height)
                            file?.let {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("drawingPath", it.absolutePath)
                            }
                        }
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Done")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                Modifier
                    .windowInsetsPadding(WindowInsets.ime)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Row {
                        listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Magenta).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(color, shape = CircleShape)
                                    .size(36.dp)
                                    .clickable { selectedColor = color }
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 1.dp,
                                        color = if (selectedColor == color) Color.Gray else Color.Black,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        //Text("Товщина: %.0f".format(strokeWidth), modifier = Modifier.padding(end = 8.dp))
                        androidx.compose.material3.Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 1f..20f,
                            steps = 18,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // Cancel any pending drawing
                        drawingJob?.cancel()
                        isPotentialDrawing = false;
                        // ... handle transform gestures ...
                        // 1) compute the new scale
                        val newScale = scale * zoom

                        // 2) recompute panX/Y so that the point under "centroid" remains fixed:
                        panX = (panX - centroid.x) * zoom + centroid.x + pan.x
                        panY = (panY - centroid.y) * zoom + centroid.y + pan.y

                        // 3) commit the new scale
                        scale = newScale
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            // Start a coroutine to handle the delay
                            drawingJob = coroutineScope.launch {  // Use coroutineScope here
                                isPotentialDrawing = true
                                delay(drawingDebounceDelay)
                                if (isPotentialDrawing) { // Check if still a potential draw
                                    val scaledOffset = Offset((pos.x - panX) / scale, (pos.y - panY) / scale)
                                    currentPoints = listOf(scaledOffset)
                                    isDrawing = true
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            if (isDrawing) {
                                change.consume()
                                val scaledOffset = Offset((change.position.x - panX) / scale, (change.position.y - panY) / scale)
                                currentPoints += scaledOffset
                            }
                        },
                        onDragEnd = {
                            drawingJob?.cancel() // Cancel delay
                            isPotentialDrawing = false
                            if (isDrawing) {
                                strokes += MyStroke(currentPoints, selectedColor, strokeWidth / scale)
                                currentPoints = emptyList()
                                isDrawing = false
                            }
                        },
                        onDragCancel = {
                            drawingJob?.cancel()
                            isPotentialDrawing = false
                            currentPoints = emptyList()
                            isDrawing = false
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
            ) {
                val scaledStrokeWidth = strokeWidth / scale
                val scaledPanX = panX
                val scaledPanY = panY

                translate(left = scaledPanX, top = scaledPanY) {
                    scale(scaleX = scale, scaleY = scale, pivot = Offset(0f, 0f)) {
                        // Draw the background image first
                        backgroundImage.value?.let { image ->
                            drawImage(image.asImageBitmap(), topLeft = Offset.Zero)
                        }
                        strokes.forEach { stroke ->
                            drawSmoothPath(stroke.points, stroke.color, stroke.width)
                        }
                        if (currentPoints.isNotEmpty()) {
                            drawSmoothPath(currentPoints, selectedColor, scaledStrokeWidth)
                        }
                    }
                }
            }
        }
    }
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothPath(
    points: List<Offset>,
    color: Color,
    width: Float
) {
    if (points.size < 2) return
    val path = AndroidPath().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
    }
    drawPath(path, color = color, style = Stroke(width = width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}


fun saveDrawingToPng(
    context: Context,
    strokes: List<MyStroke>,
    panX: Float,
    panY: Float,
    scale: Float,
    canvasWidth: Int,
    canvasHeight: Int
): File? {
    if (strokes.isEmpty()) {
        return null
    }

    // 1. Find the bounding box of the entire drawing in world space
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    for (stroke in strokes) {
        for (point in stroke.points) {
            minX = minOf(minX, point.x)
            minY = minOf(minY, point.y)
            maxX = maxOf(maxX, point.x)
            maxY = maxOf(maxY, point.y)
        }
    }

    val drawingWidth = ceil(maxX - minX).toInt()
    val drawingHeight = ceil(maxY - minY).toInt()

    // Add some padding
    val padding = 20f
    val bitmapWidth = (drawingWidth * scale + 2 * padding).toInt().coerceAtLeast(1)
    val bitmapHeight = (drawingHeight * scale + 2 * padding).toInt().coerceAtLeast(1)

    // 2. Create a bitmap large enough to hold the scaled drawing
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(Color.White.toArgb())

    // 3. Set up paint
    val paint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }

    // 4. Apply the scale and a translation to center the drawing
    canvas.scale(scale, scale)
    canvas.translate(padding / scale - minX, padding / scale - minY)

    // 5. Draw all strokes
    for (stroke in strokes) {
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.width

        if (stroke.points.size < 2) continue

        val path = Path().apply {
            moveTo(stroke.points[0].x, stroke.points[0].y)
            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                quadTo(prev.x, prev.y, midX, midY)
            }
        }
        canvas.drawPath(path, paint)
    }

    // 6. Save the bitmap
    val file = File(context.cacheDir, "drawing_${UUID.randomUUID()}.png")
    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun updateImageWithDrawing(
    context: Context,
    existingImageUri: Uri,
    strokes: List<MyStroke>
): Uri? {
    // 1. Отримуємо файл із URI
    val file = File(existingImageUri.path ?: return null)
    val original = BitmapFactory.decodeFile(file.absolutePath)
        ?.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        ?: return null

    // 2. Знаходимо межі штрихів
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    strokes.forEach { stroke ->
        stroke.points.forEach { pt ->
            minX = minOf(minX, pt.x - 20)
            minY = minOf(minY, pt.y - 20)
            maxX = maxOf(maxX, pt.x + 20)
            maxY = maxOf(maxY, pt.y + 20)
        }
    }

    // Якщо штрихи не виходять за межі — малюємо прямо на оригіналі
    if (minX >= 0 && minY >= 0 && maxX <= original.width && maxY <= original.height) {
        return drawStrokesOnBitmap(file, original, strokes)
    }

    // 3. Визначаємо, на скільки збільшити полотно
    val leftExtend   = maxOf(0f,   -minX).roundToInt()
    val topExtend    = maxOf(0f,   -minY).roundToInt()
    val rightExtend  = maxOf(0f,   maxX - original.width).roundToInt()
    val bottomExtend = maxOf(0f,   maxY - original.height).roundToInt()

    val newW = original.width  + leftExtend + rightExtend
    val newH = original.height + topExtend  + bottomExtend

    // 4. Створюємо збільшене полотно
    val enlarged = android.graphics.Bitmap.createBitmap(newW, newH, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(enlarged)
    // фон (за потреби можна зробити transparent)
    canvas.drawColor(android.graphics.Color.WHITE)

    // 5. Малюємо оригінал зі зсувом
    canvas.drawBitmap(original, leftExtend.toFloat(), topExtend.toFloat(), null)

    // 6. Малюємо штрихи зі зсувом
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.width

        val path = Path().apply {
            moveTo(stroke.points[0].x + leftExtend, stroke.points[0].y + topExtend)
            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                val midX = (prev.x + curr.x) / 2f + leftExtend
                val midY = (prev.y + curr.y) / 2f + topExtend
                quadTo(prev.x + leftExtend, prev.y + topExtend, midX, midY)
            }
        }
        canvas.drawPath(path, paint)
    }

    // 7. Перезаписуємо файл
    return try {
        FileOutputStream(file).use { out ->
            enlarged.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        existingImageUri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Виніс малювання без зміни розміру
private fun drawStrokesOnBitmap(
    file: File,
    bitmap: android.graphics.Bitmap,
    strokes: List<MyStroke>
): Uri? {
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.width

        val path = Path().apply {
            moveTo(stroke.points[0].x, stroke.points[0].y)
            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                quadTo(prev.x, prev.y, midX, midY)
            }
        }
        canvas.drawPath(path, paint)
    }
    return try {
        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}