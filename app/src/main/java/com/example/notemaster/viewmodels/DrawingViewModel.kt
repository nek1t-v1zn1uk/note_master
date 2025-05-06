package com.example.notemaster.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.example.notemaster.database.NoteDao
import com.example.notemaster.database.QuickNoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.plus
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.collections.get
import kotlin.collections.plus
import kotlin.compareTo
import kotlin.math.ceil
import kotlin.math.roundToInt

class DrawingViewModel(
    application: Application,
    private val backgroundImageUri: Uri? = null,
    private val navController: NavController,
    //private val noteDao: NoteDao,
    //private val quickNoteDao: QuickNoteDao
) : AndroidViewModel(application) {

    data class MyStroke(
        val points: List<Offset>,
        val color: Color,
        val width: Float
    )

    private val _strokes = MutableStateFlow<MutableList<MyStroke>>(mutableListOf())
    val strokes: StateFlow<List<MyStroke>> = _strokes.asStateFlow()

    private val _currentPoints = MutableStateFlow<MutableList<Offset>>(mutableListOf())
    val currentPoints: StateFlow<List<Offset>> = _currentPoints.asStateFlow()

    private val _selectedColor = MutableStateFlow<Color>(Color.Black)
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow<Float>(4f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _scale = MutableStateFlow<Float>(1f)
    val scale: StateFlow<Float> = _scale.asStateFlow()

    private val _panX = MutableStateFlow<Float>(0f)
    val panX: StateFlow<Float> = _panX.asStateFlow()

    private val _panY = MutableStateFlow<Float>(0f)
    val panY: StateFlow<Float> = _panY.asStateFlow()

    private val _backgroundImage = MutableStateFlow<Bitmap?>(null)
    val backgroundImage: StateFlow<Bitmap?> = _backgroundImage.asStateFlow()

    init{
        backgroundImageUri?.let { uri ->
            try {
                val inputStream = application.contentResolver.openInputStream(uri)
                _backgroundImage.value = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch (e: Exception) {}
        }
    }

    fun clearStrokes() {
        _strokes.value = mutableListOf()
    }
    fun removeLastStroke(){
        if(_strokes.value.isNotEmpty())
            _strokes.value = (_strokes.value - _strokes.value.last()) as MutableList<MyStroke>
    }
    fun setSelectedColor(c: Color){
        _selectedColor.value = c
    }
    fun setStrokeWidth(f: Float) {
        _strokeWidth.value = f
    }
    fun setScale(f: Float) {
        _scale.value = f
    }
    fun setPanX(f: Float) {
        _panX.value = f
    }
    fun setPanY(f: Float) {
        _panY.value = f
    }
    fun setCurrentPoints(l: MutableList<Offset>) {
        _currentPoints.value = l
    }
    fun addCurrentPoint(o: Offset) {
        _currentPoints.value = (_currentPoints.value + o) as MutableList<Offset>
    }
    fun clearCurrentPoints(){
        _currentPoints.value = mutableListOf()
    }
    fun addStroke(){
        _strokes.value += MyStroke(_currentPoints.value, _selectedColor.value, _strokeWidth.value / _scale.value)
        clearCurrentPoints()
    }

    fun saveImage() {
        if (backgroundImageUri != null) {
            val savedUri = updateImageWithDrawing()
            savedUri?.let {
                //успішно збережено
            }
        } else {
            val file = saveDrawingToPng()
            file?.let {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("drawingPath", it.absolutePath)
            }
        }
        navController.popBackStack()
    }


    fun saveDrawingToPng(): File? {
        if (_strokes.value.isEmpty()) {
            return null
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (stroke in _strokes.value) {
            for (point in stroke.points) {
                minX = minOf(minX, point.x)
                minY = minOf(minY, point.y)
                maxX = maxOf(maxX, point.x)
                maxY = maxOf(maxY, point.y)
            }
        }

        val drawingWidth = ceil(maxX - minX).toInt()
        val drawingHeight = ceil(maxY - minY).toInt()

        val padding = 20f
        val bitmapWidth = (drawingWidth * _scale.value + 2 * padding).toInt().coerceAtLeast(1)
        val bitmapHeight = (drawingHeight * _scale.value + 2 * padding).toInt().coerceAtLeast(1)


        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.White.toArgb())


        val paint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }


        canvas.scale(_scale.value, _scale.value)
        canvas.translate(padding / _scale.value - minX, padding / _scale.value - minY)


        for (stroke in _strokes.value) {
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


        val file = File(getApplication<Application>().cacheDir, "drawing_${UUID.randomUUID()}.png")
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

    fun updateImageWithDrawing(): Uri? {
        val file = File(backgroundImageUri!!.path ?: return null)
        val original = BitmapFactory.decodeFile(file.absolutePath)
            ?.copy(Bitmap.Config.ARGB_8888, true)
            ?: return null


        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        _strokes.value.forEach { stroke ->
            stroke.points.forEach { pt ->
                minX = minOf(minX, pt.x - 20)
                minY = minOf(minY, pt.y - 20)
                maxX = maxOf(maxX, pt.x + 20)
                maxY = maxOf(maxY, pt.y + 20)
            }
        }


        if (minX >= 0 && minY >= 0 && maxX <= original.width && maxY <= original.height) {
            return drawStrokesOnBitmap(file, original)
        }


        val leftExtend   = maxOf(0f,   -minX).roundToInt()
        val topExtend    = maxOf(0f,   -minY).roundToInt()
        val rightExtend  = maxOf(0f,   maxX - original.width).roundToInt()
        val bottomExtend = maxOf(0f,   maxY - original.height).roundToInt()

        val newW = original.width  + leftExtend + rightExtend
        val newH = original.height + topExtend  + bottomExtend


        val enlarged = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enlarged)
        // можна зробити transparent
        canvas.drawColor(android.graphics.Color.WHITE)


        canvas.drawBitmap(original, leftExtend.toFloat(), topExtend.toFloat(), null)


        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        _strokes.value.forEach { stroke ->
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


        return try {
            FileOutputStream(file).use { out ->
                enlarged.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            backgroundImageUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun drawStrokesOnBitmap(
        file: File,
        bitmap: Bitmap,
        //strokes: List<MyStroke>
    ): Uri? {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        _strokes.value.forEach { stroke ->
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
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



}

class DrawingViewModelFactory(
    private val application: Application,
    private val backgroundImageUri: Uri? = null,
    private val navController: NavController,
    //private val noteDao: NoteDao,
    //private val quickNoteDao: QuickNoteDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DrawingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DrawingViewModel(application, backgroundImageUri, navController/*, noteDao, quickNoteDao*/) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
