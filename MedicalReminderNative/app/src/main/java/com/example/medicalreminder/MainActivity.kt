package com.example.medicalreminder

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.medicalreminder.data.ReminderRepository
import com.example.medicalreminder.theme.MedicalReminderTheme
import com.example.medicalreminder.ui.AddEditMedicineScreen
import com.example.medicalreminder.ui.HomeScreen
import com.example.medicalreminder.ui.report.MonthlyReportScreen

sealed class Screen {
    object Home : Screen()
    object MonthlyReport : Screen()
    object Add : Screen()
    data class Edit(val medicineId: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: ReminderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ReminderRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            MedicalReminderTheme {
                // Request Notification Permission on Android 13+
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasNotificationPermission = isGranted
                    }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    // Check exact alarm permission on Android 12+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                        if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                repository = repository,
                                onNavigateToAdd = { currentScreen = Screen.Add },
                                onNavigateToEdit = { id -> currentScreen = Screen.Edit(id) },
                                onNavigateToReport = { currentScreen = Screen.MonthlyReport }
                            )
                        }
                        is Screen.MonthlyReport -> {
                            BackHandler { currentScreen = Screen.Home }
                            MonthlyReportScreen(
                                repository = repository,
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.Add -> {
                            BackHandler { currentScreen = Screen.Home }
                            AddEditMedicineScreen(
                                repository = repository,
                                medicineId = null,
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.Edit -> {
                            BackHandler { currentScreen = Screen.Home }
                            AddEditMedicineScreen(
                                repository = repository,
                                medicineId = screen.medicineId,
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}
