package com.deudom.deudom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.ChatMessage
import com.deudom.deudom.data.FirestoreService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatPanel(loanId: String, currentUserPhone: String) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    DisposableEffect(loanId) {
        val listener = FirestoreService.listenToChat(loanId) { updatedMessages ->
            messages.clear()
            messages.addAll(updatedMessages)
        }
        onDispose {
            listener.remove()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderPhone == currentUserPhone
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = msg.message,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Text(
                                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                                color = (if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp).navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Escribe un mensaje...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val msg = ChatMessage(
                                senderPhone = currentUserPhone,
                                message = inputText,
                                timestamp = System.currentTimeMillis()
                            )
                            scope.launch {
                                FirestoreService.sendChatMessage(loanId, msg)
                            }
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
