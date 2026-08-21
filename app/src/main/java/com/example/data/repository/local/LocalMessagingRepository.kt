package com.example.data.repository.local

import com.example.data.local.BiomateDaoV2
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Conversation
import com.example.domain.model.ConversationType
import com.example.domain.model.Message
import com.example.domain.model.UserProfile
import com.example.domain.repository.MessagingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalMessagingRepository(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : MessagingRepository {

    /**
     * Conversations the user belongs to.
     *
     * The SQL prefilter is a substring match on the encoded member list, so it is checked
     * again here in Kotlin — a `LIKE '%uid%'` would otherwise match a uid that merely
     * appears inside a longer one.
     */
    override fun observeConversations(uid: String): Flow<List<Conversation>> =
        dao.observeConversations(uid).map { list ->
            list.map { it.toDomain() }
                .filter { uid in it.memberIds }
                .sortedByDescending { it.lastMessageAt }
        }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        dao.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    override suspend fun ensureDirectConversation(a: UserProfile, b: UserProfile): Result<String> = runCatching {
        val id = Conversation.directIdFor(a.uid, b.uid)
        val existing = dao.getConversation(id)
        if (existing == null) {
            dao.upsertConversation(
                Conversation(
                    id = id,
                    type = ConversationType.DIRECT,
                    memberIds = listOf(a.uid, b.uid),
                    // Titled per-viewer at render time; stored title is a fallback.
                    title = b.displayName
                ).toEntity()
            )
        }
        id
    }

    override suspend fun ensureTripConversation(
        tripId: String,
        title: String,
        memberIds: List<String>
    ): Result<String> = runCatching {
        val id = Conversation.tripIdFor(tripId)
        val existing = dao.getConversation(id)?.toDomain()
        dao.upsertConversation(
            (existing ?: Conversation(
                id = id,
                type = ConversationType.TRIP,
                memberIds = memberIds,
                title = title,
                tripId = tripId
            )).copy(memberIds = memberIds, title = title, tripId = tripId).toEntity()
        )
        id
    }

    override suspend fun send(conversationId: String, sender: UserProfile, text: String): Result<Unit> = runCatching {
        val body = text.trim()
        require(body.isNotEmpty()) { "Write a message first." }

        val conversation = dao.getConversation(conversationId)?.toDomain()
            ?: error("That conversation no longer exists.")
        // Membership is the authorisation check. Mirrored in the Firestore rules so it
        // holds on the server too, not only in this client.
        require(sender.uid in conversation.memberIds) { "You are not part of that conversation." }

        val now = nowMillis()
        dao.insertMessage(
            Message(
                id = "msg_" + UUID.randomUUID().toString().replace("-", "").take(20),
                conversationId = conversationId,
                senderId = sender.uid,
                senderName = sender.displayName,
                text = body,
                sentAt = now
            ).toEntity()
        )
        dao.upsertConversation(
            conversation.copy(
                lastMessagePreview = body.take(120),
                lastMessageAt = now,
                lastMessageSenderId = sender.uid,
                lastReadAt = conversation.lastReadAt + (sender.uid to now)
            ).toEntity()
        )
    }

    override suspend fun markRead(conversationId: String, uid: String) {
        val conversation = dao.getConversation(conversationId)?.toDomain() ?: return
        if (uid !in conversation.memberIds) return
        dao.upsertConversation(
            conversation.copy(lastReadAt = conversation.lastReadAt + (uid to nowMillis())).toEntity()
        )
    }

    override suspend fun syncTripConversationMembers(tripId: String, memberIds: List<String>) {
        val conversation = dao.getConversationForTrip(tripId)?.toDomain() ?: return
        if (conversation.memberIds.toSet() == memberIds.toSet()) return
        dao.upsertConversation(conversation.copy(memberIds = memberIds).toEntity())
    }
}
