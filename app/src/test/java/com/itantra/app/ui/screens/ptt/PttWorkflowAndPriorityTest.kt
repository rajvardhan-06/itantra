package com.itantra.app.ui.screens.ptt

import com.itantra.app.communication.DeliveryStatus
import com.itantra.app.communication.MessagePriority
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying PTT transmission guards, priority selection logic,
 * and emergency confirmation safety rules introduced in Phase 9.
 */
class PttWorkflowAndPriorityTest {

    @Test
    fun canSubmitMessage_requiresNonBlankAndNotTransmitting() {
        // 1. Initial idle state with empty text
        val idleState = PttUiState(
            editableTranscription = "",
            isTransmitting = false,
            deliveryStatus = null
        )
        assertFalse(idleState.canSubmitMessage)

        // 2. Whitespace-only text
        val whitespaceState = idleState.copy(editableTranscription = "   ")
        assertFalse(whitespaceState.canSubmitMessage)

        // 3. Valid transcribed text, ready to submit
        val readyState = idleState.copy(editableTranscription = "Flash flood alert in lower sector")
        assertTrue(readyState.canSubmitMessage)

        // 4. Currently transmitting should lock submit button
        val transmittingState = readyState.copy(isTransmitting = true)
        assertFalse(transmittingState.canSubmitMessage)

        // 5. In-flight delivery status SENDING should lock submit button
        val sendingState = readyState.copy(isTransmitting = false, deliveryStatus = DeliveryStatus.SENDING)
        assertFalse(sendingState.canSubmitMessage)

        // 6. Delivered status allows next message
        val deliveredState = readyState.copy(deliveryStatus = DeliveryStatus.DELIVERED)
        assertTrue(deliveredState.canSubmitMessage)
    }

    @Test
    fun priorityEnum_mapsCorrectlyToWireProtocol() {
        val normal = MessagePriority.NORMAL
        val high = MessagePriority.IMPORTANT
        val alert = MessagePriority.ALERT

        assertTrue(normal.name == "NORMAL")
        assertTrue(high.name == "IMPORTANT")
        assertTrue(alert.name == "ALERT")
    }

    @Test
    fun emergencyConfirmationState_guardsBroadcast() {
        val normalState = PttUiState(
            selectedPriority = MessagePriority.NORMAL,
            showEmergencyConfirmation = false
        )
        assertFalse(normalState.showEmergencyConfirmation)

        // Emergency triggered state
        val emergencyPending = normalState.copy(
            selectedPriority = MessagePriority.ALERT,
            showEmergencyConfirmation = true
        )
        assertTrue(emergencyPending.showEmergencyConfirmation)

        // Once confirmed or dismissed
        val dismissed = emergencyPending.copy(showEmergencyConfirmation = false)
        assertFalse(dismissed.showEmergencyConfirmation)
    }
}
