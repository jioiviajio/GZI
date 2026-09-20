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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment // <- ВОТ ЭТОТ ИМПОРТ ИСПРАВЛЯЕТ ОШИБКУ
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest

@Composable
fun AuthScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("GZI_PREFS", Context.MODE_PRIVATE)
    }

    val savedUsername = remember { sharedPreferences.getString("saved_username", "") ?: "" }
    val savedPassword = remember { sharedPreferences.getString("saved_password", "") ?: "" }
    val savedRememberMe = remember { sharedPreferences.getBoolean("remember_password", false) }

    var usernameInput by remember { mutableStateOf(savedUsername) }
    var password by remember { mutableStateOf(savedPassword) }
    var rememberPassword by remember { mutableStateOf(savedRememberMe) }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Вход в систему GZI", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it.trim() },
            label = { Text("Логин (например, Krasnov_DA)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberPassword,
                onCheckedChange = { rememberPassword = it }
            )
            Text("Запомнить пароль", style = MaterialTheme.typography.bodyMedium)
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (usernameInput.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = ""
                    val virtualEmail = "${usernameInput.lowercase()}@gzi.com"

                    Firebase.auth.signInWithEmailAndPassword(virtualEmail, password)
                        .addOnSuccessListener {
                            val editor = sharedPreferences.edit()
                            editor.putString("saved_username", usernameInput)
                            editor.putBoolean("remember_password", rememberPassword)

                            if (rememberPassword) {
                                editor.putString("saved_password", password)
                            } else {
                                editor.remove("saved_password")
                            }
                            editor.apply()
                        }
                        .addOnFailureListener {
                            isLoading = false
                            errorMessage = "Неверный логин или пароль"
                        }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            else Text("Войти")
        }
    }
}

@Composable
fun NameSetupScreen(onNameSaved: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Добро пожаловать!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Введите ваше имя и фамилию для отображения в чате", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Имя и Фамилия (кириллицей)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (fullName.isNotBlank()) {
                    isLoading = true
                    val profileUpdates = userProfileChangeRequest {
                        displayName = fullName.trim()
                    }
                    Firebase.auth.currentUser?.updateProfile(profileUpdates)
                        ?.addOnSuccessListener { onNameSaved() }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Сохранить и войти")
        }
    }
}
