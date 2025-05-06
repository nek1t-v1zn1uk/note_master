package com.example.notemaster.pages

import android.app.Application
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
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
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
import androidx.compose.material3.Slider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.notemaster.viewmodels.DrawingViewModel
import com.example.notemaster.viewmodels.DrawingViewModelFactory
import kotlinx.coroutines.Job
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingPage(navController: NavController, existingImageUri: Uri? = null) {

    val application = LocalContext.current.applicationContext as Application

    val factory  = remember { DrawingViewModelFactory(application, existingImageUri, navController) }
    val vm: DrawingViewModel = viewModel(factory = factory)

    val strokes by vm.strokes.collectAsState()
    val currentPoints by vm.currentPoints.collectAsState()
    val backgroundImage by vm.backgroundImage.collectAsState()
    val selectedColor by vm.selectedColor.collectAsState()
    val strokeWidth by vm.strokeWidth.collectAsState()
    val scale by vm.scale.collectAsState()
    val panX by vm.panX.collectAsState()
    val panY by vm.panY.collectAsState()

    val context = LocalContext.current

//    var canvasSize by remember { mutableStateOf(IntSize.Zero) }


    var isDrawing by remember { mutableStateOf(false) }

    val drawingDebounceDelay = 100L
    var drawingJob by remember { mutableStateOf<Job?>(null) }
    var isPotentialDrawing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    /*
    LaunchedEffect(existingImageUri) {
        existingImageUri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                backgroundImage.value = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch (e: Exception) {}
        }
    }
    */

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
                    IconButton(onClick = { vm.clearStrokes() }) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = { vm.removeLastStroke() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = {
                        //?
                        vm.saveImage()
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
                                    .clickable { vm.setSelectedColor(color) }
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 1.dp,
                                        color = if (selectedColor == color) Color.Gray else Color.Black,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Slider(
                            value = strokeWidth,
                            onValueChange = { vm.setStrokeWidth(it) },
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
                /*.transformable(
                    state = rememberTransformableState { zoomChange, panChange, _ ->
                        vm.setScale(vm.scale.value * zoomChange)
                        vm.setPanX(vm.panX.value + panChange.x)
                        vm.setPanY(vm.panY.value + panChange.y)
                    }
                )*/
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // Cancel any pending drawing
                        drawingJob?.cancel()
                        isPotentialDrawing = false;
                        // 1) compute the new scale
                        val newScale = scale * zoom

                        // 2) recompute panX/Y so that the point under "centroid" remains fixed:
                        vm.setPanX((panX - centroid.x) * zoom + centroid.x + pan.x)
                        vm.setPanY((panY - centroid.y) * zoom + centroid.y + pan.y)

                        // 3) commit the new scale
                        vm.setScale(newScale)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            // Start a coroutine to handle the delay
                            drawingJob = coroutineScope.launch {
                                isPotentialDrawing = true
                                delay(drawingDebounceDelay)
                                if (isPotentialDrawing) {
                                    val scaledOffset = Offset((pos.x - panX) / scale, (pos.y - panY) / scale)
                                    vm.setCurrentPoints(mutableListOf(scaledOffset))
                                    isDrawing = true
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            if (isDrawing) {
                                change.consume()
                                val scaledOffset = Offset((change.position.x - panX) / scale, (change.position.y - panY) / scale)
                                vm.addCurrentPoint(scaledOffset)
                            }
                        },
                        onDragEnd = {
                            drawingJob?.cancel() // Cancel delay
                            isPotentialDrawing = false
                            if (isDrawing) {
                                vm.addStroke()
                                isDrawing = false
                            }
                        },
                        onDragCancel = {
                            drawingJob?.cancel()
                            isPotentialDrawing = false
                            vm.clearCurrentPoints()
                            isDrawing = false
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        //canvasSize = it
                    }
            ) {
                val scaledStrokeWidth = strokeWidth / scale
                val scaledPanX = panX
                val scaledPanY = panY

                translate(left = scaledPanX, top = scaledPanY) {
                    scale(scaleX = scale, scaleY = scale, pivot = Offset(0f, 0f)) {
                        backgroundImage?.let { image ->
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


private fun DrawScope.drawSmoothPath(
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
    drawPath(path, color = color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

