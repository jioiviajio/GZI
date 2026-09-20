package com.example.gzi

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

data class NoteItem(val id: String, val text: String, val userId: String, val authorName: String)

// КОМПОНЕНТ ДИНАМИЧЕСКОЙ АВАТАРКИ
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

// ЭКРАН ЧАТА
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(onOpenProfile: () -> Unit, onBackToMenu: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("GZI_PREFS", Context.MODE_PRIVATE) }

    var chatFontSize by remember { mutableStateOf(sharedPreferences.getFloat("chat_font_size", 16f)) }
    var hideAvatars by remember { mutableStateOf(sharedPreferences.getBoolean("hide_avatars", false)) }
    var compactCards by remember { mutableStateOf(sharedPreferences.getBoolean("compact_cards", false)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                chatFontSize = sharedPreferences.getFloat("chat_font_size", 16f)
                hideAvatars = sharedPreferences.getBoolean("hide_avatars", false)
                compactCards = sharedPreferences.getBoolean("compact_cards", false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val currentUserId = currentUser?.uid ?: "unknown"
    val currentDisplayName = currentUser?.displayName ?: "Сотрудник"

    var textInput by remember { mutableStateOf("") }
    var notesList by remember { mutableStateOf(listOf<NoteItem>()) }

    var selectedNoteForMyMenu by remember { mutableStateOf<NoteItem?>(null) }
    var selectedNoteForOthersMenu by remember { mutableStateOf<NoteItem?>(null) }
    var noteToEdit by remember { mutableStateOf<NoteItem?>(null) }
    var editTextValue by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("notes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                if (snapshots != null) {
                    notesList = snapshots.map { doc ->
                        NoteItem(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            userId = doc.getString("userId") ?: "",
                            authorName = doc.getString("authorName") ?: "Неизвестный"
                        )
                    }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onBackToMenu,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), reverseLayout = true) {
            items(notesList) { note ->
                val isMyNote = note.userId == currentUserId
                Card(
                    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { }, onLongClick = {
                        if (isMyNote) selectedNoteForMyMenu = note else selectedNoteForOthersMenu = note
                    }),
                    colors = CardDefaults.cardColors(containerColor = if (isMyNote) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(if (compactCards) 6.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!hideAvatars) {
                            UserAvatar(name = note.authorName, size = if (compactCards) (chatFontSize * 1.5f).toInt() else (chatFontSize * 2f).toInt())
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.authorName,
                                fontSize = (chatFontSize - 3).sp,
                                color = if (isMyNote) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = note.text, fontSize = chatFontSize.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // 3. БЛОК ВВОДА СООБЩЕНИЯ (Абсолютное выравнивание через BasicTextField)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically // Идеальное центрирование по оси
        ) {
            // Создаем кастомный контейнер для ввода с точно такой же высотой и скруглением, как у кнопки
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp) // Минимальная высота как у кнопки, но может расти вверх
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (textInput.isEmpty()) {
                    Text(
                        text = "Введите сообщение...",
                        fontSize = chatFontSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                // Чистый ввод без скрытых системных отступов
                androidx.compose.foundation.text.BasicTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = chatFontSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка-квадрат с точно такими же геометрическими параметрами
            FilledIconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        val note = hashMapOf(
                            "text" to textInput,
                            "userId" to currentUserId,
                            "authorName" to currentDisplayName,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("notes").add(note)
                        textInput = ""
                    }
                },
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size((chatFontSize * 1.4f).dp.coerceAtMost(30.dp))
                )
            }
        }
    }

    // ДИАЛОГ ДЛЯ СВОЕГО СООБЩЕНИЯ
    if (selectedNoteForMyMenu != null) {
        AlertDialog(
            onDismissRequest = { selectedNoteForMyMenu = null },
            title = { Text("Выберите действие", fontSize = chatFontSize.sp) },
            text = { Text("Что вы хотите сделать с этим сообщением?", fontSize = chatFontSize.sp) },
            confirmButton = {
                TextButton(onClick = {
                    noteToEdit = selectedNoteForMyMenu
                    editTextValue = selectedNoteForMyMenu!!.text
                    selectedNoteForMyMenu = null
                }) { Text("Редактировать", fontSize = chatFontSize.sp) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        db.collection("notes").document(selectedNoteForMyMenu!!.id).delete()
                        selectedNoteForMyMenu = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить", fontSize = chatFontSize.sp) }
            }
        )
    }

    // ДИАЛОГ РЕДАКТИРОВАНИЯ ТЕКСТА
    if (noteToEdit != null) {
        AlertDialog(
            onDismissRequest = { noteToEdit = null },
            title = { Text("Редактирование", fontSize = chatFontSize.sp) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = chatFontSize.sp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editTextValue.isNotBlank()) {
                        db.collection("notes").document(noteToEdit!!.id).update("text", editTextValue)
                        noteToEdit = null
                    }
                }) { Text("Сохранить", fontSize = chatFontSize.sp) }
            },
            dismissButton = { TextButton(onClick = { noteToEdit = null }) { Text("Отмена", fontSize = chatFontSize.sp) } }
        )
    }

    // ДИАЛОГ ДЛЯ ЧУЖОГО СООБЩЕНИЯ
    if (selectedNoteForOthersMenu != null) {
        AlertDialog(
            onDismissRequest = { selectedNoteForOthersMenu = null },
            title = { Text("Выберите действие", fontSize = chatFontSize.sp) },
            text = { Text("Действия с сообщением пользователя ${selectedNoteForOthersMenu!!.authorName}:", fontSize = chatFontSize.sp) },
            confirmButton = {
                TextButton(onClick = {
                    textInput = "@${selectedNoteForOthersMenu!!.authorName}, " + textInput
                    selectedNoteForOthersMenu = null
                }) { Text("Упомянуть", fontSize = chatFontSize.sp) }
            },
            dismissButton = {
                TextButton(onClick = {
                    textInput = "> ${selectedNoteForOthersMenu!!.authorName}: ${selectedNoteForOthersMenu!!.text}\n" + textInput
                    selectedNoteForOthersMenu = null
                }) { Text("Цитировать", fontSize = chatFontSize.sp) }
            }
        )
    }
}
