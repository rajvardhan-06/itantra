package com.itantra.app.domain.repository

import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contract for accessing and managing the conversation message store.
 * Implementations may use an in-memory list, Room database, or remote sync.
 */
interface MessageRepository {
    /** Observe all messages in chronological order. */
    fun observeMessages(): Flow<List<ItantraMessage>>

    /** Add a new outgoing or incoming message. */
    suspend fun addMessage(message: ItantraMessage)

    /** Update the delivery status of an existing message. */
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    /** Remove a single message by ID. */
    suspend fun deleteMessage(messageId: String)

    /** Clear all conversation history. */
    suspend fun clearAll()
}
