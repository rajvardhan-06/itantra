package com.itantra.app.data.repository

import com.itantra.app.data.mock.MockDataGenerator
import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation of [MessageRepository] backed by a [MutableStateFlow].
 * Pre-populated with sample messages from [MockDataGenerator] for demonstration.
 *
 * When a Room database is introduced, replace this class while keeping the
 * [MessageRepository] interface unchanged.
 */
class MessageRepositoryImpl : MessageRepository {

    private val _messages = MutableStateFlow(MockDataGenerator.sampleMessages())

    override fun observeMessages(): Flow<List<ItantraMessage>> = _messages.asStateFlow()

    override suspend fun addMessage(message: ItantraMessage) {
        _messages.update { current -> (current + message).sortedBy { it.timestampMs } }
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        _messages.update { current ->
            current.map { if (it.id == messageId) it.copy(status = status) else it }
        }
    }

    override suspend fun deleteMessage(messageId: String) {
        _messages.update { current -> current.filter { it.id != messageId } }
    }

    override suspend fun clearAll() {
        _messages.value = emptyList()
    }
}
