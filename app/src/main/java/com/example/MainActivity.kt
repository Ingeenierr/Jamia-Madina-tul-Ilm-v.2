package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.SyllabusDatabase
import com.example.data.SyllabusRepository
import com.example.notification.NotificationHelper
import com.example.ui.AppDashboard
import com.example.ui.SyllabusViewModel
import com.example.ui.SyllabusViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize modern Edge-To-Edge support for gorgeous Material 3 layout bleed
        enableEdgeToEdge()

        // 2. Initialize database and repository layers
        val database = SyllabusDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = SyllabusRepository(database.syllabusDao())

        // 3. Initialize background notification channels for real-time alerts
        NotificationHelper.createNotificationChannel(applicationContext)

        // 4. Instantiate our syllabus viewmodel using standard architecture pattern factories
        val viewModel: SyllabusViewModel by viewModels {
            SyllabusViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AppDashboard(
                        viewModel = viewModel,
                        innerPadding = innerPadding
                    )
                }
            }
        }
    }
}
