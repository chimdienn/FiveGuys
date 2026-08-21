package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChatMessage
import com.example.ui.BiomateViewModel
import com.example.ui.components.AdventurerAvatar
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SandBackground
import com.example.ui.theme.SurfaceVariantSand
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaDark
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TerracottaSurface
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

data class DirectChatConversation(
    val id: String,
    val username: String,
    val displayName: String,
    val initials: String,
    val lastMessage: String,
    val timeLabel: String,
    val unread: Boolean = false
)

@Composable
fun MessagesScreen(
    viewModel: BiomateViewModel,
    modifier: Modifier = Modifier
) {
    val tripMessages by viewModel.tripMessages.collectAsState()
    var openChatForConversation by remember { mutableStateOf<DirectChatConversation?>(null) }
    var openActivityGroupChat by remember { mutableStateOf(false) }

    val directChats = remember {
        listOf(
            DirectChatConversation(
                id = "1",
                username = "aaron.abbott",
                displayName = "Aaron Abbott",
                initials = "AA",
                lastMessage = "Hey are you still bringing the 4-person tent for the ridge camp?",
                timeLabel = "09:15 AM",
                unread = true
            ),
            DirectChatConversation(
                id = "2",
                username = "jeffrey.jiang",
                displayName = "Jeffrey Jiang",
                initials = "JJ",
                lastMessage = "Packed the sat phone and extra electrolyte packs.",
                timeLabel = "Yesterday"
            ),
            DirectChatConversation(
                id = "3",
                username = "tanish.rathor",
                displayName = "Tanish Rathor",
                initials = "TR",
                lastMessage = "See you at the trailhead car park at 7:30 AM sharp!",
                timeLabel = "Yesterday"
            ),
            DirectChatConversation(
                id = "4",
                username = "maya.lin",
                displayName = "Maya Lin",
                initials = "ML",
                lastMessage = "Shared the elevation profile to our trip board.",
                timeLabel = "2 days ago"
            ),
            DirectChatConversation(
                id = "5",
                username = "pavan.kumar",
                displayName = "Pavan Kumar",
                initials = "PK",
                lastMessage = "Hey! Great matching with you for the Wilsons Prom hike.",
                timeLabel = "3 days ago"
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SandBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: "Current activities" (Matching Screenshot 3)
        item {
            Text(
                text = "Current activities",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TextCharcoal
                )
            )
        }

        // Active Group Activity Card (Uluru and Kata Tjuta Group!)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TerracottaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openActivityGroupChat = true }
                    .testTag("current_activity_chat_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Uluru and Kata Tjuta Group!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextCharcoal
                            )
                        )
                        Text(
                            text = "10:42 AM",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    // Overlapping Avatar Cluster
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        listOf("AR", "JJ", "TR", "ML").forEachIndexed { index, init ->
                            AdventurerAvatar(
                                initials = init,
                                sizeDp = 34,
                                borderColor = TerracottaPrimary,
                                backgroundColor = Color.White,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TerracottaContainer)
                                .border(1.dp, TerracottaPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+2",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerracottaDark
                            )
                        }
                    }

                    // Latest message snippet
                    Text(
                        text = "jeffrey.jiang: we just saw a snake here so just be careful and carry compression bandages!",
                        fontSize = 12.sp,
                        color = TextCharcoal,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Section 2: "All messages"
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "All messages",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TextCharcoal
                )
            )
        }

        // Direct Conversations
        items(directChats) { chat ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openChatForConversation = chat }
                    .testTag("direct_chat_item_${chat.username.replace(".", "_")}")
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdventurerAvatar(
                        initials = chat.initials,
                        sizeDp = 44,
                        borderColor = TerracottaPrimary,
                        backgroundColor = TerracottaContainer
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.username,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            Text(
                                text = chat.timeLabel,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = chat.lastMessage,
                            fontSize = 12.sp,
                            color = if (chat.unread) TextCharcoal else TextMuted,
                            fontWeight = if (chat.unread) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Footer: "Start more conversations!"
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Start more conversations!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Direct Chat Dialog
    openChatForConversation?.let { chat ->
        var inputMessage by remember { mutableStateOf("") }
        val localChatMessages = remember {
            mutableStateListOf(
                ChatMessage(
                    id = 1L,
                    tripId = "direct",
                    channel = "DIRECT",
                    senderName = chat.displayName,
                    messageText = chat.lastMessage,
                    timestamp = "09:15 AM"
                ),
                ChatMessage(
                    id = 2L,
                    tripId = "direct",
                    channel = "DIRECT",
                    senderName = "Alex Rivera (You)",
                    messageText = "On it! I've packed everything into the team checklist.",
                    timestamp = "09:20 AM"
                )
            )
        }

        Dialog(onDismissRequest = { openChatForConversation = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SandBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Chat Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdventurerAvatar(initials = chat.initials, sizeDp = 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = chat.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                Text(text = "@${chat.username}", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                        IconButton(onClick = { openChatForConversation = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chat messages list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(localChatMessages) { msg ->
                            val isMe = msg.senderName.contains("You")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) TerracottaPrimary else Color.White
                                    ),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = msg.messageText,
                                            fontSize = 12.sp,
                                            color = if (isMe) Color.White else TextDark
                                        )
                                        Text(
                                            text = msg.timestamp,
                                            fontSize = 9.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.7f) else TextMuted,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            placeholder = { Text("Write a message...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputMessage.isNotBlank()) {
                                    localChatMessages.add(
                                        ChatMessage(
                                            id = System.currentTimeMillis(),
                                            tripId = "direct",
                                            channel = "DIRECT",
                                            senderName = "Alex Rivera (You)",
                                            messageText = inputMessage,
                                            timestamp = "Just now"
                                        )
                                    )
                                    inputMessage = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TerracottaPrimary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Activity Group Chat Dialog (Uluru and Kata Tjuta Group!)
    if (openActivityGroupChat) {
        var inputGroupMessage by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { openActivityGroupChat = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SandBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Uluru and Kata Tjuta Group!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDark)
                            )
                            Text(text = "5 active expedition members", fontSize = 11.sp, color = TextMuted)
                        }
                        IconButton(onClick = { openActivityGroupChat = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tripMessages) { msg ->
                            val isMe = msg.senderName.contains("You")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) TerracottaPrimary else Color.White
                                    ),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (!isMe) {
                                            Text(text = msg.senderName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TerracottaDark)
                                        }
                                        Text(
                                            text = msg.messageText,
                                            fontSize = 12.sp,
                                            color = if (isMe) Color.White else TextDark
                                        )
                                        Text(
                                            text = msg.timestamp,
                                            fontSize = 9.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.7f) else TextMuted,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputGroupMessage,
                            onValueChange = { inputGroupMessage = it },
                            placeholder = { Text("Message team...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputGroupMessage.isNotBlank()) {
                                    viewModel.sendMessage(inputGroupMessage)
                                    inputGroupMessage = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TerracottaPrimary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
