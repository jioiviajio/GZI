package com.example.gzi

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit, onSignOut: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("GZI_PREFS", Context.MODE_PRIVATE)
    }

    val currentUser = Firebase.auth.currentUser
    var nameInput by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Считываем размер шрифта интерфейса
    var fontSizeValue by remember { mutableStateOf(sharedPreferences.getFloat("chat_font_size", 16f)) }
    var hideAvatars by remember { mutableStateOf(sharedPreferences.getBoolean("hide_avatars", false)) }
    var compactCards by remember { mutableStateOf(sharedPreferences.getBoolean("compact_cards", false)) }
    var isDarkMode by remember { mutableStateOf(sharedPreferences.getBoolean("is_dark_mode", false)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Настройки", fontSize = (fontSizeValue + 6).sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. БЛОК ПРОФИЛЯ
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Профиль сотрудника", fontSize = (fontSizeValue + 2).sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                UserAvatar(name = nameInput.ifBlank { "Профиль" }, size = (fontSizeValue * 4f).toInt().coerceAtMost(100))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Имя и Фамилия (кириллицей)", fontSize = fontSizeValue.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = fontSizeValue.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showPasswordDialog = true }) {
                        Text("Сменить пароль", fontSize = fontSizeValue.sp)
                    }
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                isLoading = true
                                val profileUpdates = userProfileChangeRequest { displayName = nameInput.trim() }
                                currentUser?.updateProfile(profileUpdates)?.addOnSuccessListener { isLoading = false }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text("Сменить имя", fontSize = fontSizeValue.sp) // Переименовано
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // 2. БЛОК ИНТЕРФЕЙСА С ОБНОВЛЕННЫМ НАЗВАНИЕМ И КНОПКОЙ ВЫХОДА
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Внешний вид чата", fontSize = (fontSizeValue + 2).sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(12.dp))

                // Тёмная тема
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Включить тёмную тему", fontSize = fontSizeValue.sp)
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = {
                            isDarkMode = it
                            sharedPreferences.edit().putBoolean("is_dark_mode", it).apply()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ползунок шрифта интерфейса (Переименован)
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Размер шрифта интерфейса", fontSize = fontSizeValue.sp)
                        Text("${fontSizeValue.roundToInt()} sp", fontSize = fontSizeValue.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = fontSizeValue,
                        onValueChange = {
                            fontSizeValue = it
                            sharedPreferences.edit().putFloat("chat_font_size", it).apply()
                        },
                        valueRange = 12f..28f,
                        steps = 7
                    )
                }

                // Скрыть аватарки
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Скрыть аватарки в чате", fontSize = fontSizeValue.sp)
                    Switch(
                        checked = hideAvatars,
                        onCheckedChange = {
                            hideAvatars = it
                            sharedPreferences.edit().putBoolean("hide_avatars", it).apply()
                        }
                    )
                }

                // Компактный вид карточек
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Компактный вид карточек", fontSize = fontSizeValue.sp)
                    Switch(
                        checked = compactCards,
                        onCheckedChange = {
                            compactCards = it
                            sharedPreferences.edit().putBoolean("compact_cards", it).apply()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // ПРЕДПРОСМОТР
                Text("Пример сообщения:", fontSize = (fontSizeValue - 2).sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(if (compactCards) 6.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!hideAvatars) {
                            UserAvatar(name = "Иван Иванов", size = if (compactCards) (fontSizeValue * 1.5f).toInt() else (fontSizeValue * 2f).toInt())
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Иван Иванов", fontSize = (fontSizeValue - 3).sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Вот так будет выглядеть текст сообщений в чате.", fontSize = fontSizeValue.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // КНОПКА: Применить и выйти (Переименована)
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Применить и выйти", fontSize = fontSizeValue.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // КНОПКА: Выйти из аккаунта (Перенесена сюда)
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Выйти из аккаунта", fontSize = fontSizeValue.sp)
        }

        if (showPasswordDialog) {
            var newPassword by remember { mutableStateOf("") }
            var dialogMessage by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text("Смена пароля", fontSize = fontSizeValue.sp) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Новый пароль", fontSize = fontSizeValue.sp) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newPassword.length >= 6) { currentUser?.updatePassword(newPassword)?.addOnSuccessListener { showPasswordDialog = false } }
                    }) { Text("Сохранить", fontSize = fontSizeValue.sp) }
                },
                dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("Отмена", fontSize = fontSizeValue.sp) } }
            )
        }
    }
}
