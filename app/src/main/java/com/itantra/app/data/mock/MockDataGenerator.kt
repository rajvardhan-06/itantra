package com.itantra.app.data.mock

import com.itantra.app.domain.model.ItantraMessage
import com.itantra.app.domain.model.MessageDirection
import com.itantra.app.domain.model.MessagePriority
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.SupportedLanguage
import java.util.UUID

/**
 * Provides pre-built mock data for use during UI development and demo purposes.
 * All content is realistic but entirely synthetic — no real transmissions have occurred.
 */
object MockDataGenerator {

    fun sampleMessages(): List<ItantraMessage> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute

        return listOf(
            ItantraMessage(
                id = UUID.randomUUID().toString(),
                content = "नमस्ते, क्या आप मुझे सुन सकते हैं?",
                language = SupportedLanguage.HINDI,
                direction = MessageDirection.SENT,
                status = MessageStatus.DELIVERED,
                priority = MessagePriority.NORMAL,
                timestampMs = now - 2 * hour,
                remoteDeviceId = "device-01"
            ),
            ItantraMessage(
                id = UUID.randomUUID().toString(),
                content = "हाँ, मैं सुन सकता हूँ। संकेत स्पष्ट है।",
                language = SupportedLanguage.HINDI,
                direction = MessageDirection.RECEIVED,
                status = MessageStatus.DELIVERED,
                priority = MessagePriority.NORMAL,
                timestampMs = now - 2 * hour + 3 * minute,
                remoteDeviceId = "device-01"
            ),
            ItantraMessage(
                id = UUID.randomUUID().toString(),
                content = "Hello, can you receive this message?",
                language = SupportedLanguage.ENGLISH,
                direction = MessageDirection.SENT,
                status = MessageStatus.DELIVERED,
                priority = MessagePriority.HIGH,
                timestampMs = now - 45 * minute,
                remoteDeviceId = "device-02"
            ),
            ItantraMessage(
                id = UUID.randomUUID().toString(),
                content = "Yes, loud and clear. Signal strength is good.",
                language = SupportedLanguage.ENGLISH,
                direction = MessageDirection.RECEIVED,
                status = MessageStatus.DELIVERED,
                priority = MessagePriority.NORMAL,
                timestampMs = now - 44 * minute,
                remoteDeviceId = "device-02"
            ),
            ItantraMessage(
                id = UUID.randomUUID().toString(),
                content = "ਸਤ ਸ੍ਰੀ ਅਕਾਲ। ਕੀ ਤੁਸੀਂ ਮੈਨੂੰ ਸੁਣ ਸਕਦੇ ਹੋ?",
                language = SupportedLanguage.HINDI, // Using Hindi as fallback for demo
                direction = MessageDirection.SENT,
                status = MessageStatus.SENT,
                priority = MessagePriority.NORMAL,
                timestampMs = now - 10 * minute,
                remoteDeviceId = "device-01"
            ),
            ItantraMessage(
                id = UUID.randomUUID().toString(),
                content = "ನಮಸ್ಕಾರ, ಸಂದೇಶ ಸ್ವೀಕರಿಸಲಾಗಿದೆ.",
                language = SupportedLanguage.KANNADA,
                direction = MessageDirection.RECEIVED,
                status = MessageStatus.DELIVERED,
                priority = MessagePriority.NORMAL,
                timestampMs = now - 5 * minute,
                remoteDeviceId = "device-03"
            ),
            ItantraMessage(
                id = UUID.randomUUID().toString(),
                content = "Emergency: Medical assistance needed at sector 4.",
                language = SupportedLanguage.ENGLISH,
                direction = MessageDirection.SENT,
                status = MessageStatus.FAILED,
                priority = MessagePriority.URGENT,
                timestampMs = now - minute,
                remoteDeviceId = "device-02"
            )
        ).sortedBy { it.timestampMs }
    }
}
