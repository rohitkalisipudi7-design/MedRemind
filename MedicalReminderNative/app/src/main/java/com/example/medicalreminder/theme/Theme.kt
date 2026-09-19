package com.example.medicalreminder.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val KineticDarkColorScheme = darkColorScheme(
    primary = PrimaryRed,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryDeep,
    secondary = Ink,
    onSecondary = Canvas,
    background = Canvas,
    surface = SurfaceCard,
    surfaceVariant = SurfaceBone,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Charcoal,
    error = StatusCritical
)

@Composable
fun MedicalReminderTheme(
    darkTheme: Boolean = true, // Default to Dark Mode across the entire app
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = KineticDarkColorScheme, typography = Typography, content = content)
}
