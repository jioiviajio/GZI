package com.example.gzi

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val sharedPreferences = remember { context.getSharedPreferences("GZI_PREFS", Context.MODE_PRIVATE) }

    val currentUser = Firebase.auth.currentUser
    val nameInput = remember { mutableStateOf(currentUser?.displayName ?: "") }
    val isLoading = remember { mutableStateOf(false) }
    val showPasswordDialog = remember { mutableStateOf(false) }

    val fontSizeValue = remember { mutableStateOf(sharedPreferences.getFloat("chat_font_size", 16f)) }
    val hideAvatars = remember { mutableStateOf(sharedPreferences.getBoolean("hide_avatars", false)) }
    val compactCards = remember { mutableStateOf(sharedPreferences.getBoolean("compact_cards", false)) }
    val isDarkMode = remember { mutableStateOf(sharedPreferences.getBoolean("is_dark_mode", false)) }

    val parseHexToRGB = { hex: String ->
        try {
            val colorInt = android.graphics.Color.parseColor(hex)
            Triple(android.graphics.Color.red(colorInt), android.graphics.Color.green(colorInt), android.graphics.Color.blue(colorInt))
        } catch(e: Exception) { Triple(21, 101, 192) }
    }

    // Оставлен только RGB стейт кнопок интерфейса
    val btnColorRGB = remember { mutableStateOf(parseHexToRGB(sharedPreferences.getString("color_buttons", "#1565C0") ?: "#1565C0")) }

    val formatRGBtoHex = { r: Int, g: Int, b: Int -> String.format("#%02X%02X%02X", r, g, b) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Настройки", fontSize = (fontSizeValue.value + 6).sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Профиль сотрудника", fontSize = (fontSizeValue.value + 2).sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))

                UserAvatar(name = nameInput.value.ifBlank { "Профиль" }, size = (fontSizeValue.value * 4f).toInt().coerceAtMost(100))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameInput.value,
                    onValueChange = { nameInput.value = it },
                    label = { Text("Имя и Фамилия") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showPasswordDialog.value = true }) { Text("Сменить пароль") }
                    Button(onClick = {
                        if (nameInput.value.isNotBlank()) {
                            isLoading.value = true
                            val profileUpdates = userProfileChangeRequest { displayName = nameInput.value.trim() }
                            currentUser?.updateProfile(profileUpdates)?.addOnSuccessListener { isLoading.value = false }
                        }
                    }, enabled = !isLoading.value) { Text("Сменить имя") }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Кастомизация интерфейса", fontSize = (fontSizeValue.value + 1).sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(12.dp))

                // Только один RGB-селектор для кнопок системы
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Цвет интерфейса и кнопок", fontWeight = FontWeight.Medium, fontSize = fontSizeValue.value.sp)
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(btnColorRGB.value.first, btnColorRGB.value.second, btnColorRGB.value.third)))
                    }
                    Slider(value = btnColorRGB.value.first.toFloat(), onValueChange = { btnColorRGB.value = Triple(it.toInt(), btnColorRGB.value.second, btnColorRGB.value.third); sharedPreferences.edit().putString("color_buttons", formatRGBtoHex(it.toInt(), btnColorRGB.value.second, btnColorRGB.value.third)).apply() }, valueRange = 0f..255f)
                    Slider(value = btnColorRGB.value.second.toFloat(), onValueChange = { btnColorRGB.value = Triple(btnColorRGB.value.first, it.toInt(), btnColorRGB.value.third); sharedPreferences.edit().putString("color_buttons", formatRGBtoHex(btnColorRGB.value.first, it.toInt(), btnColorRGB.value.third)).apply() }, valueRange = 0f..255f)
                    Slider(value = btnColorRGB.value.third.toFloat(), onValueChange = { btnColorRGB.value = Triple(btnColorRGB.value.first, btnColorRGB.value.second, it.toInt()); sharedPreferences.edit().putString("color_buttons", formatRGBtoHex(btnColorRGB.value.first, btnColorRGB.value.second, it.toInt())).apply() }, valueRange = 0f..255f)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Включить тёмную тему", fontSize = fontSizeValue.value.sp)
                    Switch(checked = isDarkMode.value, onCheckedChange = { isDarkMode.value = it; sharedPreferences.edit().putBoolean("is_dark_mode", it).apply() })
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Скрыть аватарки в чате", fontSize = fontSizeValue.value.sp)
                    Switch(checked = hideAvatars.value, onCheckedChange = { hideAvatars.value = it })
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Компактный вид карточек", fontSize = fontSizeValue.value.sp)
                    Switch(checked = compactCards.value, onCheckedChange = { compactCards.value = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Размер шрифта чата", fontSize = fontSizeValue.value.sp)
                        Text("${fontSizeValue.value.roundToInt()} sp", fontSize = fontSizeValue.value.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = fontSizeValue.value, onValueChange = { fontSizeValue.value = it }, valueRange = 12f..28f, steps = 7)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Изменен контейнер предпросмотра: теперь карточка системного цвета (primaryContainer)
                Text("Пример сообщения:", fontSize = (fontSizeValue.value - 2).sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(if (compactCards.value) 6.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!hideAvatars.value) {
                            UserAvatar(name = nameInput.value.ifBlank { "Сотрудник" }, size = if (compactCards.value) (fontSizeValue.value * 1.5f).toInt() else (fontSizeValue.value * 2f).toInt())
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = nameInput.value.ifBlank { "Сотрудник" }, fontSize = (fontSizeValue.value - 3).sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Вот так будет выглядеть текст сообщений в чате.", fontSize = fontSizeValue.value.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                sharedPreferences.edit().apply {
                    putFloat("chat_font_size", fontSizeValue.value)
                    putBoolean("hide_avatars", hideAvatars.value)
                    putBoolean("compact_cards", compactCards.value)
                    apply()
                }
                onBack()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Применить изменения") }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Text("Выйти из аккаунта")
        }

        if (showPasswordDialog.value) {
            val newPassword = remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showPasswordDialog.value = false },
                title = { Text("Смена пароля") },
                text = { OutlinedTextField(value = newPassword.value, onValueChange = { newPassword.value = it }, visualTransformation = PasswordVisualTransformation()) },
                confirmButton = { Button(onClick = { if (newPassword.value.length >= 6) { currentUser?.updatePassword(newPassword.value)?.addOnSuccessListener { showPasswordDialog.value = false } } }) { Text("Сохранить") } },
                dismissButton = { TextButton(onClick = { showPasswordDialog.value = false }) { Text("Отмена") } }
            )
        }
    }
}
