package com.example.notemaster

import android.R
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteMasterTheme {
                Surface {
                    MyApp()
                }
            }
        }
    }
}

@Composable
fun MyApp() {
    Log.d("NavArgs", "NotePage opened")


    val list by remember { mutableStateOf(arrayOf<Note>(
        Note("Лабораторні", "no content"),
        Note("Курсовий проект", "no content"),
        Note("Днюхи", "no content"),
        Note("Велосипед", "no content"),
        Note("Закупки", "no content"),
        Note("Закупки", "no content"),
        Note("Закупки", "no content"),
        Note("Закупки", "no content"),
        Note("Закупки", "no content"),
        Note("Закупки", "no content")
    )
    )}

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { NoteList(list, navController) }
        composable(
            route = "note_page/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            Log.d("NavArgs", "list size: ${list.size}")
            val noteId = backStackEntry.arguments?.getInt("noteId")!!
            Log.d("NavArgs", "Received noteId: $noteId, list size: ${list.size}")

            NotePage(list[noteId])
        }
        composable("profile") { HomeScreen(navController) }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    // UI for Home screen and a button to navigate
    Button(onClick = { navController.navigate("profile") }) {
        Text("Go to Profile")
    }
}
