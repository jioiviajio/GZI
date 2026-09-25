package com.example.gzi

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LaboratoryScreen(onBack: () -> Unit) {
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize.value

    // Внутренний стейт подэкранов: "sub_menu", "norms", "ppe_testing"
    var currentSubScreen by remember { mutableStateOf("sub_menu") }

    when (currentSubScreen) {
        "norms" -> {
            NormsOfTestsScreen(onBack = { currentSubScreen = "sub_menu" })
        }
        "ppe_testing" -> {
            PpeTestingScreen(onBack = { currentSubScreen = "sub_menu" })
        }
        else -> {
            // Главное подменю Лаборатории
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Лаборатория",
                        fontSize = (fontSize + 6).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onBack) {
                        Text("Назад", fontSize = fontSize.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Разделы лаборатории:", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                // Пункт 1: Нормы испытаний
                Button(
                    onClick = { currentSubScreen = "norms" },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Нормы испытаний", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Пункт 2: Приемка и испытания СИЗ
                Button(
                    onClick = { currentSubScreen = "ppe_testing" },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Приемка и испытания СИЗ", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
@Composable
fun NormsOfTestsScreen(onBack: () -> Unit) {
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize.value

    // Перехватываем кнопку "Назад" телефона: вместо закрытия Лаборатории возвращаем в ее меню
    BackHandler(enabled = true) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Нормы испытаний", fontSize = (fontSize + 4).sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Назад", fontSize = fontSize.sp) }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Раздел находится в разработке.", fontSize = fontSize.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun PpeTestingScreen(onBack: () -> Unit) {
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize.value

    // Перехватываем кнопку "Назад" телефона: вместо закрытия Лаборатории возвращаем в ее меню
    BackHandler(enabled = true) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Приемка и испытания СИЗ", fontSize = (fontSize + 4).sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Назад", fontSize = fontSize.sp) }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Раздел находится в разработке.", fontSize = fontSize.sp, color = MaterialTheme.colorScheme.outline)
    }
}
