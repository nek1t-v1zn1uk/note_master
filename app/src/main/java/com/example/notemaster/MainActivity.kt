package com.example.notemaster

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import com.example.notemaster.ui.theme.NoteMasterTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shortcutManager = getSystemService(ShortcutManager::class.java)

        val shortcutId = "quick_note_pin" // Унікальний ID для дії закріплення ярлика
        val pinIntent = Intent(this, QuickNoteActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        val shortcut = ShortcutInfo.Builder(this, shortcutId)
            .setShortLabel(getString(R.string.shortcut_quick_note_short_label)) // Змінено label для дії
            .setLongLabel(getString(R.string.shortcut_quick_note_long_label)) // Змінено label для дії
            .setIcon(Icon.createWithResource(this, R.drawable.quick_note)) // Змініть іконку, якщо потрібно
            // !!! ВАЖЛИВО: Цей Intent більше не відкриває Activity напряму !!!
            // !!! Він використовується лише для запиту на створення ярлика !!!
            .setIntent(Intent(this, MainActivity::class.java).apply { // Intent для дії в контекстному меню
                action = ACTION_PIN_QUICK_NOTE
            })
            .build()

        @Suppress("WorkerThread")
        shortcutManager.dynamicShortcuts = listOf(shortcut)

        // --- Обробка Intent, якщо користувач натиснув на ярлик "Швидка нотатка" в меню ---
        if (intent?.action == ACTION_PIN_QUICK_NOTE) {
            if (shortcutManager.isRequestPinShortcutSupported) {
                val shortcutToPin = ShortcutInfo.Builder(this, "pinned_quick_note") // ID для створеного ярлика
                    .setShortLabel(getString(R.string.shortcut_quick_note_short_label))
                    .setLongLabel(getString(R.string.shortcut_quick_note_long_label))
                    .setIcon(Icon.createWithResource(this, R.drawable.quick_note))
                    .setIntent(pinIntent) // Intent для відкриття QuickNoteActivity
                    .build()

                val successCallback = PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(this, ShortcutPinnedReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )

                shortcutManager.requestPinShortcut(shortcutToPin, successCallback.intentSender)
            } else {
                Toast.makeText(this, "Неможливо створити ярлик", Toast.LENGTH_SHORT).show()
            }
            // Після спроби створення ярлика, завершуємо цю Activity, щоб користувач не залишився тут
            finish()
        }

        // --- Compose UI починається тут ---
        setContent {
            NoteMasterTheme {
                Surface {
                    MyApp()
                }
            }
        }
    }

    companion object {
        const val ACTION_PIN_QUICK_NOTE = "com.example.notemaster.ACTION_PIN_QUICK_NOTE"
    }
}

// --- BroadcastReceiver для обробки результату закріплення ярлика ---
class ShortcutPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action == "com.android.launcher.action.PIN_SHORTCUT_SUCCESS") {
            Toast.makeText(context, "Ярлик створено успішно", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MyApp() {
    /*
    Log.d("NavArgs", "NotePage opened")

    var cont1 = Content()
    cont1.addComponent(0, ItemText("Sheesh\nWeed"))
    cont1.addComponent(1, ItemImage(Uri.parse("content://com.example.notemaster.fileprovider/camera_images/IMG_1745263475600.jpg")))
    cont1.addComponent(2, ItemText("Coke"))
    var cont2 = Content()
    cont2.addComponent(0, ItemText("Sheesh\nWeed\nCoke"))
    var cont3 = Content()
    cont3.addComponent(0, ItemText("Sheesh\nWeed\nCoke"))
    var cont4 = Content()
    cont4.addComponent(0, ItemText("Sheesh\nWeed\nCoke"))
    var cont5 = Content()
    cont5.addComponent(0, ItemText("Sheesh\nWeed\nCoke"))

    val list by remember { mutableStateOf(arrayOf<Note>(
        Note("Лабораторні", cont1),
        Note("Курсовий проект", cont2),
        Note("Днюхи", cont3),
        Note("Велосипед", cont4),
        Note("Закупки", cont5),
        Note("Закупки"),
        Note("Закупки"),
        Note("Закупки"),
        Note("Закупки"),
        Note("Закупки")
    )
    )}
    */

    val context = LocalContext.current

    val db = remember {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "notes_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    val noteDao = db.noteDao()


    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { NoteList(navController, noteDao) }
        composable(
            route = "note_page/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId")!!
            NotePage(noteDao, noteId, navController)
        }
        composable(
            route = "drawing?imageUri={imageUri}",
            arguments = listOf(navArgument("imageUri") {
                nullable = true
                defaultValue = null
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri")
            DrawingPage(navController = navController, existingImageUri = imageUriString?.let { Uri.parse(it) })
        }
        composable("profile") { HomeScreen(navController) }
    }

    NotificationHelper.createChannel(context)
    NotificationPermissionRequester { }
    LaunchedEffect(Unit) {
        requestIgnoreBatteryOptimizations(context)
        requestExactAlarmsPermission(context)
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    // UI for Home screen and a button to navigate
    Button(onClick = { navController.navigate("profile") }) {
        Text("Go to Profile")
    }
}
