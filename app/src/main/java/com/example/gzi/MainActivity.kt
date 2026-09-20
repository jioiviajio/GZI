package com.example.gzi

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedPreferences = remember { this.getSharedPreferences("GZI_PREFS", Context.MODE_PRIVATE) }

            var isDarkMode by remember { mutableStateOf(sharedPreferences.getBoolean("is_dark_mode", false)) }
            var buttonColorStr by remember { mutableStateOf(sharedPreferences.getString("color_buttons", "#1565C0") ?: "#1565C0") }

            val listener = remember {
                android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "is_dark_mode" -> isDarkMode = sharedPreferences.getBoolean("is_dark_mode", false)
                        "color_buttons" -> buttonColorStr = sharedPreferences.getString("color_buttons", "#1565C0") ?: "#1565C0"
                    }
                }
            }

            DisposableEffect(sharedPreferences) {
                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            val primaryColor = remember(buttonColorStr) {
                try { Color(android.graphics.Color.parseColor(buttonColorStr)) } catch (e: Exception) { Color(0xFF1565C0) }
            }

            val colors = if (isDarkMode) {
                darkColorScheme(primary = primaryColor, primaryContainer = primaryColor.copy(alpha = 0.3f))
            } else {
                lightColorScheme(primary = primaryColor, primaryContainer = primaryColor.copy(alpha = 0.15f))
            }

            MaterialTheme(colorScheme = colors) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

// ЭТА ФУНКЦИЯ ДОЛЖНА СТОЯТЬ СТРОГО ЗДЕСЬ — ПОСЛЕ ЗАКРЫВАЮЩЕЙ СКОБКИ КЛАССА
@Composable
fun AppNavigation() {
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }
    var currentScreen by remember { mutableStateOf("main_container") }
    var isNameRequired by remember { mutableStateOf(user != null && user?.displayName.isNullOrBlank()) }

    LaunchedEffect(Unit) {
        Firebase.auth.addAuthStateListener { auth ->
            user = auth.currentUser
            isNameRequired = auth.currentUser != null && auth.currentUser?.displayName.isNullOrBlank()
            if (auth.currentUser == null) currentScreen = "main_container"
        }
    }

    if (user == null) {
        AuthScreen()
    } else if (isNameRequired) {
        NameSetupScreen(onNameSaved = {
            isNameRequired = false
            currentScreen = "main_container"
        })
    } else {
        when (currentScreen) {
            "main_container" -> MainContainerScreen(onOpenSettings = { currentScreen = "settings" })
            "settings" -> SettingsScreen(
                onBack = { currentScreen = "main_container" },
                onSignOut = { Firebase.auth.signOut() }
            )
        }
    }
}
