package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.NarayanViewModel
import com.example.ui.Screen
import com.example.ui.screens.AIAssistantScreen
import com.example.ui.screens.AlarmScreen
import com.example.ui.screens.ChessScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LectureDetailScreen
import com.example.ui.screens.LecturesScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PublishVideoScreen
import com.example.ui.screens.SelfieScreen
import com.example.ui.screens.StudyTimerScreen
import com.example.ui.screens.SuperAdminScreen
import com.example.ui.screens.YouTubePlayerScreen
import com.example.ui.theme.MyApplicationTheme

import com.example.data.firebase.configureAppCheck
import com.example.ui.components.AuthGate

class MainActivity : ComponentActivity() {
    private val viewModel: NarayanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthGate(viewModel = viewModel) { userId ->
                        NarayanApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun NarayanApp(viewModel: NarayanViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        Screen.HOME -> HomeScreen(viewModel = viewModel)
        Screen.SELFIE -> SelfieScreen(viewModel = viewModel)
        Screen.AI_ASSISTANT -> AIAssistantScreen(viewModel = viewModel)
        Screen.LECTURES -> LecturesScreen(viewModel = viewModel)
        Screen.LECTURE_DETAIL -> LectureDetailScreen(viewModel = viewModel)
        Screen.MY_NOTES -> NotesScreen(viewModel = viewModel)
        Screen.PUBLISH_VIDEO -> PublishVideoScreen(viewModel = viewModel)
        Screen.STUDY_TIMER -> StudyTimerScreen(viewModel = viewModel)
        Screen.ALARM -> AlarmScreen(viewModel = viewModel)
        Screen.YOUTUBE_PLAYER -> YouTubePlayerScreen(viewModel = viewModel)
        Screen.SUPER_ADMIN -> SuperAdminScreen(viewModel = viewModel)
        Screen.CHESS -> ChessScreen(viewModel = viewModel)
    }
}
