package com.example.notemaster


import android.content.Context
import android.graphics.Bitmap
// for Compose drawing
import androidx.compose.ui.graphics.Path as AndroidPath
// for Android Canvas bitmap saving
import android.graphics.Path
import android.os.Parcelable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream
import java.util.*

data class MyStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float
)

val colorPalette = listOf(
    listOf(Color(0xFFEF5350), Color(0xFFFF7043), Color(0xFFFFCA28)), // Red-Orange-Yellow
    listOf(Color(0xFFFFEE58), Color(0xFFCDDC39), Color(0xFF66BB6A)), // Yellow-Green
    listOf(Color(0xFF26C6DA), Color(0xFF42A5F5), Color(0xFF7E57C2)), // Cyan-Blue-Purple
    listOf(Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF000000))  // Pink-Purple-Black
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingPage(navController: NavController) {
    val context = LocalContext.current

    val strokes = remember { mutableStateListOf<MyStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(4f) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var selectedMainIndex by remember { mutableStateOf(0) }
    var selectedShadeIndex by remember { mutableStateOf(0) }


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
                    IconButton(onClick = { strokes.clear() }
                    ) {
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
                    IconButton(
                        onClick = {
                            if (canvasSize.width > 0 && canvasSize.height > 0) {
                                val file = saveStrokesAsPng(
                                    context,
                                    strokes,
                                    canvasSize.width,
                                    canvasSize.height
                                )
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("drawingPath", file.absolutePath)
                            }
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {

        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos -> currentPoints = listOf(pos) },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints += change.position
                            },
                            onDragEnd = {
                                strokes += MyStroke(currentPoints, selectedColor, strokeWidth)
                                currentPoints = emptyList()
                            },
                            onDragCancel = { currentPoints = emptyList() }
                        )
                    }
            ) {
                strokes.forEach { stroke ->
                    drawSmoothPath(stroke.points, stroke.color, stroke.width)
                }
                if (currentPoints.isNotEmpty()) {
                    drawSmoothPath(currentPoints, selectedColor, strokeWidth)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                /*
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Shade row
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color.White, shape = RoundedCornerShape(32.dp))
                            .padding(8.dp)
                    ) {
                        colorPalette[selectedMainIndex].forEachIndexed { i, shade ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(4.dp)
                                    .background(shade, shape = CircleShape)
                                    .border(
                                        width = if (i == selectedShadeIndex) 3.dp else 1.dp,
                                        color = if (i == selectedShadeIndex) Color.Gray else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedShadeIndex = i
                                        selectedColor = colorPalette[selectedMainIndex][selectedShadeIndex]
                                    }
                            )
                        }
                    }

                    // Main color row
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color.White, shape = RoundedCornerShape(32.dp))
                            .padding(8.dp)
                    ) {
                        colorPalette.forEachIndexed { i, shades ->
                            val mainColor = shades[2] // middle tone

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(4.dp)
                                    .background(mainColor, shape = RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (i == selectedMainIndex) 3.dp else 1.dp,
                                        color = if (i == selectedMainIndex) Color.Gray else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedMainIndex = i
                                        selectedShadeIndex = 2
                                        selectedColor = colorPalette[i][2]
                                    }
                            )
                        }
                    }
                }*/
                // Color selector
                Row {
                    listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Magenta).forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                                .size(36.dp)
                                .clickable { selectedColor = color }
                                .border(
                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                    color = if (selectedColor == color) Color.Gray else Color.Black,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }

                // Thickness selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Товщина: %.0f".format(strokeWidth), modifier = Modifier.padding(end = 8.dp))
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
    drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = width))
}
private fun saveStrokesAsPng(
    context: Context,
    strokes: List<MyStroke>,
    width: Int,
    height: Int
): File {
    val originalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(originalBitmap)

    // Draw all strokes
    strokes.forEach { stroke ->
        val paint = android.graphics.Paint().apply {
            color = stroke.color.toArgb()
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = stroke.width
            isAntiAlias = true
        }

        if (stroke.points.size < 2) return@forEach
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

    // 1. Compute bounds of actual drawing
    val allPoints = strokes.flatMap { it.points }
    val minX = (allPoints.minOfOrNull { it.x }?.toInt() ?: 0) - 20
    val minY = (allPoints.minOfOrNull { it.y }?.toInt() ?: 0) - 20
    val maxX = (allPoints.maxOfOrNull { it.x }?.toInt() ?: width) + 20
    val maxY = (allPoints.maxOfOrNull { it.y }?.toInt() ?: height) + 20

    val cropLeft = minX.coerceAtLeast(0)
    val cropTop = minY.coerceAtLeast(0)
    val cropRight = maxX.coerceAtMost(width)
    val cropBottom = maxY.coerceAtMost(height)

    var cropWidth = cropRight - cropLeft
    var cropHeight = cropBottom - cropTop

    // 2. Enforce minimum size
    cropWidth = cropWidth.coerceAtLeast(400)
    cropHeight = cropHeight.coerceAtLeast(400)

    // 3. Center crop region inside canvas if bounds too tight
    val centerX = (cropLeft + cropRight) / 2
    val centerY = (cropTop + cropBottom) / 2

    val newLeft = (centerX - cropWidth / 2).coerceIn(0, width - cropWidth)
    val newTop = (centerY - cropHeight / 2).coerceIn(0, height - cropHeight)

    // 4. Crop the bitmap
    val cropped = Bitmap.createBitmap(originalBitmap, newLeft, newTop, cropWidth, cropHeight)

    // 5. Save
    val file = File(context.cacheDir, "drawing_${UUID.randomUUID()}.png")
    FileOutputStream(file).use { out ->
        cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    return file
}

