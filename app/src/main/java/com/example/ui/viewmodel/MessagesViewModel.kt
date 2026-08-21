package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.domain.model.Conversation
import com.example.domain.model.ConversationType
import com.example.domain.model.Message
import com.example.domain.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A conversation with its display name resolved for the current viewer.
 *
 * A direct conversation has no single correct title — it is "Sarah Chen" to Alex and
 * "Alex Rivera" to Sarah — so the title is computed per viewer rather than stored.
 */
data class ConversationRow(
    val conversation: Conversation,
    val title: String,
    val subtitle: String,
    val unread: Boolean,
    val otherProfile: UserProfile?
)

class MessagesViewModel(
    private val container: AppContainer,
    private val profileFlow: StateFlow<UserProfile?>
) : ViewModel() {

    private val _openConversationId = MutableStateFlow<String?>(null)
    val openConversationId: StateFlow<String?> = _openConversationId.asStateFlow()

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** The viewer's own uid, so message bubbles can tell "mine" from "theirs". */
    val currentUid: StateFlow<String?> = profileFlow
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val allProfiles: StateFlow<List<UserProfile>> =
        container.profileRepository.observeAllProfiles()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawConversations: StateFlow<List<Conversation>> = profileFlow
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.messagingRepository.observeConversations(profile.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val conversations: StateFlow<List<ConversationRow>> =
        combine(rawConversations, allProfiles, profileFlow) { list, profiles, me ->
            _isLoading.value = false
            if (me == null) return@combine emptyList()
            val byUid = profiles.associateBy { it.uid }

            list.map { conversation ->
                val other = if (conversation.type == ConversationType.DIRECT) {
                    conversation.memberIds.firstOrNull { it != me.uid }?.let(byUid::get)
                } else null

                ConversationRow(
                    conversation = conversation,
                    title = other?.displayName ?: conversation.title,
                    subtitle = conversation.lastMessagePreview.ifBlank {
                        if (conversation.type == ConversationType.TRIP) {
                            "${conversation.memberIds.size} people · no messages yet"
                        } else {
                            "No messages yet"
                        }
                    },
                    unread = conversation.unreadFor(me.uid),
                    otherProfile = other
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tripConversations: StateFlow<List<ConversationRow>> = conversations
        .map { list -> list.filter { it.conversation.type == ConversationType.TRIP } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val directConversations: StateFlow<List<ConversationRow>> = conversations
        .map { list -> list.filter { it.conversation.type == ConversationType.DIRECT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = conversations
        .map { list -> list.count { it.unread } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val openConversation: StateFlow<ConversationRow?> =
        combine(_openConversationId, conversations) { id, list ->
            list.firstOrNull { it.conversation.id == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>> = _openConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else container.messagingRepository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearMessage() {
        _message.value = null
    }

    fun open(conversationId: String) {
        _openConversationId.value = conversationId
        _draft.value = ""
        val uid = profileFlow.value?.uid ?: return
        viewModelScope.launch { container.messagingRepository.markRead(conversationId, uid) }
    }

    fun close() {
        _openConversationId.value = null
        _draft.value = ""
    }

    fun setDraft(text: String) {
        _draft.value = text
    }

    fun send() {
        val conversationId = _openConversationId.value ?: return
        val me = profileFlow.value ?: return
        val text = _draft.value.trim()
        if (text.isEmpty()) return

        // Cleared before the send so the input empties immediately; restored on failure so
        // a dropped message is never silently lost.
        _draft.value = ""
        viewModelScope.launch {
            container.messagingRepository.send(conversationId, me, text)
                .onFailure {
                    _draft.value = text
                    _message.value = it.message ?: "Message not sent."
                }
        }
    }

    fun markRead() {
        val conversationId = _openConversationId.value ?: return
        val uid = profileFlow.value?.uid ?: return
        viewModelScope.launch { container.messagingRepository.markRead(conversationId, uid) }
    }
}
