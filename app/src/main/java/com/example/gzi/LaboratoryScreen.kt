package com.example.gzi

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ИСПРАВЛЕННАЯ МОДЕЛЬ: Все поля снабжены дефолтными значениями для успешного парсинга Firestore
data class PpeCell(
    val number: Int = 0,
    val organization: String = "",
    val note: String = "",
    val date: String = "",
    val status: String = ""
)

@Composable
fun LaboratoryScreen(onBack: () -> Unit) {
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize.value
    var currentSubScreen by remember { mutableStateOf("sub_menu") }

    when (currentSubScreen) {
        "norms" -> NormsOfTestsScreen(onBack = { currentSubScreen = "sub_menu" })
        "ppe_testing" -> PpeTestingScreen(onBack = { currentSubScreen = "sub_menu" })
        else -> {
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
                    Text("Лаборатория", fontSize = (fontSize + 6).sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onBack) { Text("Назад", fontSize = fontSize.sp) }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Разделы лаборатории:", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

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
fun PpeTestingScreen(onBack: () -> Unit) {
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize.value
    val context = LocalContext.current
    val db = remember { Firebase.firestore }

    BackHandler(enabled = true) { onBack() }

    val organizationsList = remember {
        listOf("Сургутские РЭС", "Нижневартовские ЭС", "Нефтеюганский РЭС", "Когалымские ЭС", "Служба СДТУ")
    }

    var currentMenuMode by remember { mutableStateOf("Приемка") }
    val receptionShelf = remember { mutableStateListOf<PpeCell>().apply { repeat(32) { add(PpeCell(number = it + 1)) } } }
    val deliveryShelf = remember { mutableStateListOf<PpeCell>().apply { repeat(32) { add(PpeCell(number = it + 1)) } } }

    var showActionDialog by remember { mutableStateOf(false) }
    var selectedCellIndex by remember { mutableStateOf(-1) }
    var ppeBufferForTransfer by remember { mutableStateOf<PpeCell?>(null) }

    var selectedOrg by remember { mutableStateOf(organizationsList.first()) }
    var noteText by remember { mutableStateOf("") }

    // СИНХРОНИЗАЦИЯ: Слушаем базу данных в реальном времени
    LaunchedEffect(Unit) {
        db.collection("reception_shelf").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                for (doc in snapshot.documents) {
                    val cell = doc.toObject(PpeCell::class.java)
                    if (cell != null && cell.number in 1..32) {
                        receptionShelf[cell.number - 1] = cell
                    }
                }
            }
        }
        db.collection("delivery_shelf").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                for (doc in snapshot.documents) {
                    val cell = doc.toObject(PpeCell::class.java)
                    if (cell != null && cell.number in 1..32) {
                        deliveryShelf[cell.number - 1] = cell
                    }
                }
            }
        }
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Приемка", "Выдача").forEach { mode ->
                val isActive = currentMenuMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                        .clickable { currentMenuMode = mode }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = mode, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (ppeBufferForTransfer != null && currentMenuMode == "Выдача") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(text = "⚠️ Выберите свободную ячейку Выдачи для СИЗ (${ppeBufferForTransfer?.organization})", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = (fontSize - 2).sp, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
            }
        }

        val activeShelf = if (currentMenuMode == "Приемка") receptionShelf else deliveryShelf

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(32) { index ->
                val cell = activeShelf[index]
                val isFilled = cell.organization.isNotBlank()

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(color = if (isFilled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                        .border(width = 1.dp, color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(8.dp))
                        .clickable {
                            selectedCellIndex = index
                            if (currentMenuMode == "Приемка") {
                                if (!isFilled) {
                                    selectedOrg = organizationsList.first()
                                    noteText = ""
                                    showActionDialog = true
                                } else {
                                    ppeBufferForTransfer = cell.copy(status = "Испытано")
                                    noteText = cell.note
                                    currentMenuMode = "Выдача"
                                    Toast.makeText(context, "СИЗ переведены в статус 'Испытано'. Выберите ячейку на стеллаже Выдачи.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                if (ppeBufferForTransfer != null && !isFilled) {
                                    showActionDialog = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
                        Text(text = cell.number.toString(), fontSize = (fontSize - 4).sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        if (isFilled) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = cell.organization.take(6) + "..", fontSize = (fontSize - 5).sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(text = cell.status, fontSize = (fontSize - 6).sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text(if (currentMenuMode == "Приемка") "Приемка СИЗ в ячейку №${selectedCellIndex + 1}" else "Размещение на стеллаже Выдачи") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (currentMenuMode == "Приемка") {
                        Text("Выберите организацию:", fontSize = (fontSize - 2).sp, modifier = Modifier.padding(bottom = 4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            organizationsList.forEach { org ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(if (selectedOrg == org) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { selectedOrg = org }.padding(8.dp)
                                ) {
                                    Text(org, fontSize = (fontSize - 2).sp, fontWeight = if (selectedOrg == org) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    } else {
                        Text("Перенос СИЗ организации: ${ppeBufferForTransfer?.organization}", fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
                        Text("Статус: Испытано", fontSize = (fontSize - 2).sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Примечание к СИЗ") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentDateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                        val cellNum = selectedCellIndex + 1

                        if (currentMenuMode == "Приемка") {
                            val newCell = PpeCell(number = cellNum, organization = selectedOrg, note = noteText, date = currentDateStr, status = "Принято")
                            db.collection("reception_shelf").document("cell_$cellNum").set(newCell)
                        } else {
                            val buffer = ppeBufferForTransfer
                            if (buffer != null) {
                                val newDeliveryCell = PpeCell(number = cellNum, organization = buffer.organization, note = noteText, date = currentDateStr, status = "Испытано")
                                db.collection("delivery_shelf").document("cell_$cellNum").set(newDeliveryCell)

                                // Стираем данные из старой ячейки на бэкенде
                                val clearedCell = PpeCell(number = buffer.number, organization = "", note = "", date = "", status = "")
                                db.collection("reception_shelf").document("cell_${buffer.number}").set(clearedCell)
                            }
                            ppeBufferForTransfer = null
                        }
                        showActionDialog = false
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showActionDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun NormsOfTestsScreen(onBack: () -> Unit) {
    val fontSize = MaterialTheme.typography.bodyLarge.fontSize.value
    BackHandler(enabled = true) { onBack() }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Нормы испытаний", fontSize = (fontSize + 4).sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Назад", fontSize = fontSize.sp) }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Раздел находится в разработке.", fontSize = fontSize.sp, color = MaterialTheme.colorScheme.outline)
    }
}
