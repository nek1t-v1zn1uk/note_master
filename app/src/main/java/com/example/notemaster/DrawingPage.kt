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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream
import java.util.*

@Composable
fun DrawingPage(navController: NavController) {
    val context = LocalContext.current

    // All finished strokes
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    // In-progress stroke
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    // Hold the size of the Canvas
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }       // capture size here
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            currentStroke = listOf(pos)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStroke = currentStroke + change.position
                        },
                        onDragEnd = {
                            strokes += currentStroke
                            currentStroke = emptyList()
                        },
                        onDragCancel = {
                            currentStroke = emptyList()
                        }
                    )
                }
        ) {
            // draw saved strokes
            strokes.forEach { drawSmoothPath(it) }
            // draw current stroke
            if (currentStroke.isNotEmpty()) drawSmoothPath(currentStroke)
        }

        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
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
            Text("Save & Return")
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothPath(points: List<Offset>) {
    if (points.size < 2) return
    val path = androidx.compose.ui.graphics.AndroidPath().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
    }
    drawPath(path, color = Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
}

private fun saveStrokesAsPng(
    context: Context,
    strokes: List<List<Offset>>,
    width: Int,
    height: Int
): File {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }
    strokes.forEach { stroke ->
        if (stroke.size < 2) return@forEach
        val path = Path().apply {
            moveTo(stroke[0].x, stroke[0].y)
            for (i in 1 until stroke.size) {
                val prev = stroke[i - 1]
                val curr = stroke[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                quadTo(prev.x, prev.y, midX, midY)
            }
        }
        canvas.drawPath(path, paint)
    }

    val file = File(context.cacheDir, "drawing_${UUID.randomUUID()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file
}