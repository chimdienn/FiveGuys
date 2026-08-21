package com.example.domain.model

enum class ConnectionStatus { PENDING, ACCEPTED, REJECTED }

/**
 * A directed connection request that becomes a symmetric relationship once accepted.
 *
 * [id] is deterministic — see [connectionIdFor] — so that two users cannot end up with
 * two competing request documents for the same pair.
 */
data class Connection(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: ConnectionStatus,
    val createdAt: Long,
    val respondedAt: Long? = null
) {
    val participantIds: List<String> get() = listOf(requesterId, addresseeId)

    fun otherParty(uid: String): String = if (uid == requesterId) addresseeId else requesterId

    companion object {
        /** Order-independent id so A->B and B->A collide onto one document. */
        fun connectionIdFor(a: String, b: String): String =
            listOf(a, b).sorted().joinToString("__")
    }
}

enum class ConversationType { DIRECT, TRIP }

data class Conversation(
    val id: String,
    val type: ConversationType,
    /** Every uid permitted to read and write this conversation. */
    val memberIds: List<String>,
    val title: String,
    /** Set for [ConversationType.TRIP] conversations, null for direct ones. */
    val tripId: String? = null,
    val lastMessagePreview: String = "",
    val lastMessageAt: Long = 0L,
    val lastMessageSenderId: String? = null,
    /** uid -> timestamp of the last message that user has read. */
    val lastReadAt: Map<String, Long> = emptyMap()
) {
    fun unreadFor(uid: String): Boolean {
        if (lastMessageAt == 0L) return false
        if (lastMessageSenderId == uid) return false
        return lastMessageAt > (lastReadAt[uid] ?: 0L)
    }

    companion object {
        fun directIdFor(a: String, b: String): String = "direct__" + listOf(a, b).sorted().joinToString("__")
        fun tripIdFor(tripId: String): String = "trip__$tripId"
    }
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val sentAt: Long,
    val isSystem: Boolean = false
)
