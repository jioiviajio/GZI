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
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember { context.getSharedPreferences("GZI_PREFS", Context.MODE_PRIVATE) }

            var isDarkMode by remember { mutableStateOf(sharedPreferences.getBoolean("is_dark_mode", false)) }

            DisposableEffect(sharedPreferences) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "is_dark_mode") {
                        isDarkMode = sharedPreferences.getBoolean("is_dark_mode", false)
                    }
                }
                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            val colors = if (isDarkMode) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colors) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }
    var currentScreen by remember { mutableStateOf("main_menu") }

    LaunchedEffect(Unit) {
        Firebase.auth.addAuthStateListener { auth ->
            user = auth.currentUser
            // Если разлогинились, принудительно сбрасываем экран на главное меню
            if (auth.currentUser == null) currentScreen = "main_menu"
        }
    }

    if (user == null) {
        AuthScreen()
    } else {
        when (currentScreen) {
            "main_menu" -> MainMenuScreen(
                onOpenChat = { currentScreen = "chat" },
                onOpenSettings = { currentScreen = "settings" }
            )
            "chat" -> ChatScreen(
                onOpenProfile = { currentScreen = "settings" },
                onBackToMenu = { currentScreen = "main_menu" }
            )
            "settings" -> SettingsScreen(
                onBack = { currentScreen = "main_menu" },
                onSignOut = { Firebase.auth.signOut() } // Передали команду выхода в настройки
            )
        }
    }
}
