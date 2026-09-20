package com.example.gzi

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

@Composable
fun MainMenuScreen(onOpenChat: () -> Unit, onOpenSettings: () -> Unit) {
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
    val currentDisplayName = Firebase.auth.currentUser?.displayName ?: "Сотрудник"
    var previewNotesList by remember { mutableStateOf(listOf<NoteItem>()) }

    LaunchedEffect(Unit) {
        db.collection("notes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(3)
            .addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    previewNotesList = snapshots.map { doc ->
                        NoteItem(id = doc.id, text = doc.getString("text") ?: "", userId = doc.getString("userId") ?: "", authorName = doc.getString("authorName") ?: "Неизвестный")
                    }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(name = currentDisplayName, size = if (chatFontSize > 20) 48 else 40)
                Spacer(modifier = Modifier.width(8.dp))
                // ИЗМЕНЕНО: Новое название системы
                Text(text = "Приложение ПСГиИ", fontSize = (chatFontSize + 2).sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpenSettings) { Text("Настройки", fontSize = chatFontSize.sp) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Разделы системы:", fontSize = (chatFontSize - 2).sp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        ) {
            Text("Испытания", fontSize = chatFontSize.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Text("Лаборатория", fontSize = chatFontSize.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text("Последние сообщения чата:", fontSize = (chatFontSize - 2).sp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth().wrapContentHeight().clickable { onOpenChat() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (previewNotesList.isEmpty()) {
                    Text("В чате пока нет сообщений.", fontSize = chatFontSize.sp, modifier = Modifier.padding(8.dp))
                } else {
                    previewNotesList.asReversed().forEach { note ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!hideAvatars) {
                                UserAvatar(name = note.authorName, size = if (compactCards) (chatFontSize * 1.2f).toInt() else (chatFontSize * 1.5f).toInt())
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column {
                                Text(text = "${note.authorName}:", fontSize = (chatFontSize - 3).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = note.text, fontSize = chatFontSize.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
