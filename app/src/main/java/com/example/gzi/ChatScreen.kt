package com.example.gzi

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Locale

data class NoteItem(
    val id: String,
    val text: String,
    val userId: String,
    val authorName: String,
    val timestamp: com.google.firebase.Timestamp? = null,
    val cardColorStr: String? = ""
)

@Composable
fun UserAvatar(name: String, size: Int = 40) {
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifEmpty { "?" }

    val colors = listOf(Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC))
    val backgroundColor = colors[name.fold(0) { acc, c -> acc + c.code }.coerceAtLeast(0) % colors.size]

    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, color = Color.White, fontSize = (size / 2.5).sp, fontWeight = FontWeight.Bold)
    }
}

fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}
// ЗАМЕНИТЕ ВТОРУЮ ЧАСТЬ ФАЙЛА CHATSCREEN.KT ЭТИМ КОДОМ:
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    progress: Float,
    isExpanded: Boolean,
    onBackToMenu: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("GZI_PREFS", Context.MODE_PRIVATE) }

    val chatFontSize = remember { mutableStateOf(sharedPreferences.getFloat("chat_font_size", 16f)) }
    val hideAvatars = remember { mutableStateOf(sharedPreferences.getBoolean("hide_avatars", false)) }
    val compactCards = remember { mutableStateOf(sharedPreferences.getBoolean("compact_cards", false)) }

    val isDarkMode = sharedPreferences.getBoolean("is_dark_mode", false)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                chatFontSize.value = sharedPreferences.getFloat("chat_font_size", 16f)
                hideAvatars.value = sharedPreferences.getBoolean("hide_avatars", false)
                compactCards.value = sharedPreferences.getBoolean("compact_cards", false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val currentUserId = currentUser?.uid ?: "unknown"
    val currentDisplayName = currentUser?.displayName ?: "Сотрудник"

    val textInput = remember { mutableStateOf("") }
    val notesList = remember { mutableStateOf(listOf<NoteItem>()) }
    val selectedNoteForMyMenu = remember { mutableStateOf<NoteItem?>(null) }
    val selectedNoteForOthersMenu = remember { mutableStateOf<NoteItem?>(null) }
    val noteToEdit = remember { mutableStateOf<NoteItem?>(null) }
    val editTextValue = remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { onScrollStateChanged(it) }
    }

    LaunchedEffect(Unit) {
        db.collection("notes")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    notesList.value = snapshots.map { doc ->
                        NoteItem(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            userId = doc.getString("userId") ?: "",
                            authorName = doc.getString("authorName") ?: "Неизвестный",
                            timestamp = doc.getTimestamp("createdAt"),
                            cardColorStr = doc.getString("cardColor") ?: ""
                        )
                    }
                }
            }
    }

    val containerShape = if (isExpanded) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Column(
        modifier = Modifier.fillMaxSize().clip(containerShape).background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        // ИСПРАВЛЕНО: Добавлен .clickable { onBackToMenu() } (так как триггер инвертирован, вызов вернет шторку в fullHeight)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (!isExpanded) onBackToMenu() } // Тач мгновенно разворачивает шторку
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                // ИСПРАВЛЕНО: Текст сокращен, размер шрифта теперь равен общему системному chatFontSize
                Text(
                    text = "Потяните вверх или кликните",
                    fontSize = chatFontSize.value.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                FilledIconButton(onClick = onBackToMenu, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Bottom, reverseLayout = true) {
            items(notesList.value.reversed()) { note ->
                val isMyNote = note.userId == currentUserId

                val finalCardColor = remember(note.cardColorStr, note.authorName, isDarkMode) {
                    if (!note.cardColorStr.isNullOrBlank()) {
                        try { Color(android.graphics.Color.parseColor(note.cardColorStr)) } catch (e: Exception) { null }
                    } else if (isMyNote) {
                        null
                    } else {
                        val hash = note.authorName.hashCode()
                        if (isDarkMode) {
                            val r = (hash and 0xFF) % 40 + 45
                            val g = ((hash shr 8) and 0xFF) % 40 + 45
                            val b = ((hash shr 16) and 0xFF) % 40 + 45
                            Color(r, g, b)
                        } else {
                            val r = (hash and 0xFF) % 40 + 215
                            val g = ((hash shr 8) and 0xFF) % 40 + 215
                            val b = ((hash shr 16) and 0xFF) % 40 + 215
                            Color(r, g, b)
                        }
                    }
                }

                val cardColor = finalCardColor ?: if (isMyNote) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (!isMyNote && isDarkMode && note.cardColorStr.isNullOrBlank()) Color.White else MaterialTheme.colorScheme.onSurface

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        // ИСПРАВЛЕНО: Клик по карточке сообщения в мини-режиме тоже разворачивает чат
                        .combinedClickable(
                            onClick = { if (!isExpanded) onBackToMenu() },
                            onLongClick = { if (isExpanded) { if (isMyNote) selectedNoteForMyMenu.value = note else selectedNoteForOthersMenu.value = note } }
                        ),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(if (compactCards.value) 6.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!hideAvatars.value) {
                            UserAvatar(name = note.authorName, size = if (compactCards.value) (chatFontSize.value * 1.5f).toInt() else (chatFontSize.value * 2f).toInt())
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = note.authorName, fontSize = (chatFontSize.value - 3).sp, color = textColor, fontWeight = FontWeight.Bold)
                                Text(text = formatTime(note.timestamp), fontSize = (chatFontSize.value - 4).sp, color = textColor.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = note.text, fontSize = chatFontSize.value.sp, color = textColor)
                        }
                    }
                }
            }
        }
        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textInput.value.isEmpty()) {
                        Text(
                            text = "Введите сообщение...",
                            fontSize = chatFontSize.value.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = textInput.value,
                        onValueChange = { textInput.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = chatFontSize.value.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        if (textInput.value.isNotBlank()) {
                            val note = hashMapOf(
                                "text" to textInput.value.trim(),
                                "userId" to currentUserId,
                                "authorName" to currentDisplayName,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "cardColor" to ""
                            )
                            db.collection("notes").add(note)
                            textInput.value = ""
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size((chatFontSize.value * 1.4f).dp.coerceAtMost(30.dp))
                    )
                }
            }
        }
    }

    if (selectedNoteForMyMenu.value != null) {
        AlertDialog(
            onDismissRequest = { selectedNoteForMyMenu.value = null },
            title = { Text("Управление элементом", fontSize = chatFontSize.value.sp) },
            text = {
                Column {
                    Text("Выберите индивидуальный цвет карточки:", fontSize = (chatFontSize.value - 2).sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val elementPalettes = listOf(
                            "Дефолт" to "",
                            "Красный" to "#FFCDD2",
                            "Зелёный" to "#C8E6C9",
                            "Синий" to "#BBDEFB",
                            "Жёлтый" to "#FFF9C4",
                            "Фиолет" to "#E1BEE7"
                        )

                        elementPalettes.forEach { (_, hexStr) ->
                            val circleBg = if (hexStr.isEmpty()) Color.LightGray else Color(android.graphics.Color.parseColor(hexStr))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(circleBg)
                                    .clickable {
                                        db.collection("notes").document(selectedNoteForMyMenu.value!!.id)
                                            .update("cardColor", hexStr)
                                        selectedNoteForMyMenu.value = null
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    noteToEdit.value = selectedNoteForMyMenu.value
                    editTextValue.value = selectedNoteForMyMenu.value!!.text
                    selectedNoteForMyMenu.value = null
                }) { Text("Редактировать", fontSize = chatFontSize.value.sp) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        db.collection("notes").document(selectedNoteForMyMenu.value!!.id).delete()
                        selectedNoteForMyMenu.value = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить", fontSize = chatFontSize.value.sp) }
            }
        )
    }

    if (noteToEdit.value != null) {
        AlertDialog(
            onDismissRequest = { noteToEdit.value = null },
            title = { Text("Редактирование", fontSize = chatFontSize.value.sp) },
            text = {
                OutlinedTextField(
                    value = editTextValue.value,
                    onValueChange = { editTextValue.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = chatFontSize.value.sp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editTextValue.value.isNotBlank()) {
                        db.collection("notes").document(noteToEdit.value!!.id).update("text", editTextValue.value)
                        noteToEdit.value = null
                    }
                }) { Text("Сохранить", fontSize = chatFontSize.value.sp) }
            },
            dismissButton = { TextButton(onClick = { noteToEdit.value = null }) { Text("Отмена", fontSize = chatFontSize.value.sp) } }
        )
    }

    if (selectedNoteForOthersMenu.value != null) {
        AlertDialog(
            onDismissRequest = { selectedNoteForOthersMenu.value = null },
            title = { Text("Выберите действие", fontSize = chatFontSize.value.sp) },
            text = { Text("Действия с сообщением пользователя ${selectedNoteForOthersMenu.value!!.authorName}:", fontSize = chatFontSize.value.sp) },
            confirmButton = {
                TextButton(onClick = {
                    textInput.value = "@${selectedNoteForOthersMenu.value!!.authorName}, " + textInput.value
                    selectedNoteForOthersMenu.value = null
                }) { Text("Упомянуть", fontSize = chatFontSize.value.sp) }
            },
            dismissButton = {
                TextButton(onClick = {
                    textInput.value = "> ${selectedNoteForOthersMenu.value!!.authorName}: ${selectedNoteForOthersMenu.value!!.text}\n" + textInput.value
                    selectedNoteForOthersMenu.value = null
                }) { Text("Цитировать", fontSize = chatFontSize.value.sp) }
            }
        )
    }
}
