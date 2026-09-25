package com.example.gzi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.gzi.ui.theme.GZITheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    // Сейф-стейты для динамического управления экраном и триггером автопроверки апдейтов
    private var currentDestinationState = mutableStateOf("main_container")
    private var shouldAutoCheckUpdate = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseMessaging.getInstance().subscribeToTopic("app_updates")
        FirebaseMessaging.getInstance().subscribeToTopic("chat_messages_topic")

        // Обработка холодного старта (приложение было полностью закрыто в памяти)
        val startDestination = intent.getStringExtra("navigate_to") ?: "main_container"
        if (startDestination == "update") {
            currentDestinationState.value = "settings"
            shouldAutoCheckUpdate.value = true
        } else if (startDestination == "chat") {
            currentDestinationState.value = "chat"
        } else {
            currentDestinationState.value = "main_container"
        }

        setContent {
            val sharedPreferences = remember { this.getSharedPreferences("GZI_PREFS", Context.MODE_PRIVATE) }

            var isDarkMode by remember { mutableStateOf(sharedPreferences.getBoolean("is_dark_mode", false)) }
            var buttonColorStr by remember { mutableStateOf(sharedPreferences.getString("color_buttons", "#1565C0") ?: "#1565C0") }
            var appFontSize by remember { mutableStateOf(sharedPreferences.getFloat("chat_font_size", 16f)) }

            val listener = remember {
                android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "is_dark_mode" -> isDarkMode = sharedPreferences.getBoolean("is_dark_mode", false)
                        "color_buttons" -> buttonColorStr = sharedPreferences.getString("color_buttons", "#1565C0") ?: "#1565C0"
                        "chat_font_size" -> appFontSize = sharedPreferences.getFloat("chat_font_size", 16f)
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

            GZITheme(darkTheme = isDarkMode, buttonColor = primaryColor, chatFontSize = appFontSize) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        currentScreen = currentDestinationState.value,
                        autoCheckForUpdates = shouldAutoCheckUpdate.value,
                        onScreenChanged = { currentDestinationState.value = it },
                        onResetAutoCheck = { shouldAutoCheckUpdate.value = false }
                    )
                }
            }
        }
    }

    // Обработка горячего старта (приложение было свернуто и пользователь кликнул по пушу)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val screenToNavigate = intent.getStringExtra("navigate_to")
        if (screenToNavigate == "update") {
            currentDestinationState.value = "settings"
            shouldAutoCheckUpdate.value = true // Взводим флаг автопроверки «на лету»
        } else if (screenToNavigate == "chat") {
            currentDestinationState.value = "chat"
        }
    }
}
@Composable
fun AppNavigation(
    currentScreen: String,
    autoCheckForUpdates: Boolean,
    onScreenChanged: (String) -> Unit,
    onResetAutoCheck: () -> Unit
) {
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isNameRequired by remember { mutableStateOf(user != null && user?.displayName.isNullOrBlank()) }

    // Аппаратная кнопка "Назад" перехватывает управление на второстепенных экранах системы
    BackHandler(enabled = currentScreen == "settings" || currentScreen == "laboratory") {
        onScreenChanged("main_container")
    }

    LaunchedEffect(Unit) {
        Firebase.auth.addAuthStateListener { auth ->
            user = auth.currentUser
            isNameRequired = auth.currentUser != null && auth.currentUser?.displayName.isNullOrBlank()
            if (auth.currentUser == null) onScreenChanged("main_container")
        }
    }

    if (user == null) {
        AuthScreen()
    } else if (isNameRequired) {
        NameSetupScreen(onNameSaved = {
            isNameRequired = false
            onScreenChanged("main_container")
        })
    } else {
        when (currentScreen) {
            "main_container" -> MainContainerScreen(
                onOpenSettings = { onScreenChanged("settings") },
                onOpenLaboratory = { onScreenChanged("laboratory") } // Передаем событие открытия лаборатории
            )
            "settings" -> SettingsScreen(
                autoCheckForUpdates = autoCheckForUpdates,
                onBack = {
                    onResetAutoCheck() // Сбрасываем триггер при выходе из настроек
                    onScreenChanged("main_container")
                },
                onSignOut = {
                    onResetAutoCheck()
                    Firebase.auth.signOut()
                }
            )
            "chat" -> ChatScreen(
                progress = 0f,
                isExpanded = true,
                onBackToMenu = { onScreenChanged("main_container") },
                onScrollStateChanged = { _ -> }
            )
            "laboratory" -> LaboratoryScreen(
                onBack = { onScreenChanged("main_container") }
            )
        }
    }
}
