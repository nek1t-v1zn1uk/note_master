package com.example.notemaster

import android.R
import android.R.attr.delay
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.notemaster.ui.theme.NoteMasterTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteList(list: Array<Note>, navController: NavController){

    var isCheckState by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier
            .navigationBarsPadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Нотатки"
                        )
                    },
                    colors = topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black
                    ),
                    actions = {
                        if(isCheckState) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(height = 40.dp, width = 60.dp)
                                    .padding(end = 20.dp)
                                    .clickable(onClick = {
                                        isCheckState = false
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
                        var isFirst by remember { mutableStateOf(true) }
                        Column (
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .clickable(onClick = {
                                    isFirst = true
                                })
                        ) {
                            var fill = if (isFirst) Color.Black else Color.Gray
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
                                    isFirst = false
                                })
                        ) {
                            var fill = if (!isFirst) Color.Black else Color.Gray
                            Icon(
                                imageVector = Icons.Filled.NoteAlt,
                                contentDescription = "Notes",
                                tint = fill,
                                modifier = Modifier
                                    .size(30.dp)
                            )
                            Text(
                                text="Швидкі нотатки",
                                color = fill
                            )
                        }
                    }
                }
            }
        }
    ){ innerPadding ->

        val scrollState = rememberScrollState()

        Column(
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
                .verticalScroll(scrollState)
        ) {
            Divider(
                thickness = 10.dp,
                color = Color.White
            )
            for(item in list){
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
                if(!isCheckState)
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
                        .background(if(!isChecked)Color.White else Color.LightGray, RoundedCornerShape(16.dp))
                        .padding(vertical = 20.dp, horizontal = 15.dp)
                        .pointerInput(Unit) {
                            coroutineScope {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true

                                        // Launch coroutine to detect long press
                                        val longPressJob = launch {
                                            delay(500)
                                            if(scale==0.90f)
                                                isCheckState = true
                                        }

                                        val released = tryAwaitRelease()

                                        if (released) {
                                            longPressJob.cancel()
                                            if(isCheckState) {
                                                isChecked = !isChecked
                                            } else{
                                                Log.d("NavArgs", "index: ${list.indexOf(item)}")
                                                navController.navigate("note_page/${list.indexOf(item)}")
                                            }
                                        }

                                        isPressed = false
                                    }
                                )
                            }
                        }

                ) {
                    Text(
                        text = item.name,
                        fontSize = 18.sp
                    )

                    if(isCheckState) {
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
            Divider(
                thickness = 15.dp,
                color = Color.White
            )
        }
    }
}



@Preview(showBackground = true,
    device = "spec:width=393dp,height=873dp", name = "MyXiaomi", apiLevel = 35,
    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
)
@Composable
fun NoteListPreview(){
    val list = arrayOf<Note>(
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
    NoteMasterTheme{
        NoteList(list, rememberNavController())
    }
}
