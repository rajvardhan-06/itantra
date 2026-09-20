package com.itantra.app.domain.communication

import com.itantra.app.communication.CommunicationManager
import com.itantra.app.communication.DeliveryStatus
import com.itantra.app.communication.MessagePriority
import com.itantra.app.communication.MockCommunicationTransport
import com.itantra.app.data.repository.MessageRepositoryImpl
import com.itantra.app.demo.DemoSimulationManager
import com.itantra.app.domain.model.MessageDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validates the complete SIH Demonstration loopback and fault injection pipeline.
 */
class EndToEndDemoPipelineTest {

    private lateinit var testScope: CoroutineScope
    private lateinit var messageRepository: MessageRepositoryImpl
    private lateinit var mockTransport: MockCommunicationTransport
    private lateinit var commManager: CommunicationManager
    private lateinit var demoManager: DemoSimulationManager

    @Before
    fun setUp() {
        testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        messageRepository = MessageRepositoryImpl()
        mockTransport = MockCommunicationTransport(autoEchoAck = true)
        commManager = CommunicationManager(
            transport = mockTransport,
            messageRepository = messageRepository,
            externalScope = testScope,
            ackTimeoutMs = 100L,
            retryBackoffBaseMs = 25L
        )
        demoManager = DemoSimulationManager(
            communicationManager = commManager,
            mockTransport = mockTransport,
            messageRepository = messageRepository
        )
    }

    @After
    fun tearDown() {
        commManager.release()
        testScope.cancel()
    }

    @Test
    fun `single-device SIH demo loopback injects multilingual peer message correctly`() = runBlocking {
        // Step 1: Simulate incoming Hindi message from relief camp peer
        val result = demoManager.simulateIncomingPeerMessage(0) // Hindi preset
        assertTrue("Injection must succeed", result.isSuccess)

        // Allow message processing over flow
        delay(150L)

        // Step 2: Verify message was stored in repository as RECEIVED
        val messages = messageRepository.observeMessages().first()
        assertFalse("Repository must contain the received message", messages.isEmpty())

        val received = messages.last()
        assertEquals(MessageDirection.RECEIVED, received.direction)
        assertEquals("hi", received.language.code)
        assertTrue(received.content.contains("बाढ़ राहत केंद्र"))

        // Step 3: Verify last demo action updated
        val lastAction = demoManager.lastSimulatedAction.value
        assertNotNull(lastAction)
        assertTrue(lastAction!!.contains("Hindi"))
    }

    @Test
    fun `single-device SIH demo loopback verifies Bengali preset and ACK emission`() = runBlocking {
        val result = demoManager.simulateIncomingPeerMessage(1) // Bengali preset
        assertTrue(result.isSuccess)

        delay(150L)

        val messages = messageRepository.observeMessages().first()
        val received = messages.last()
        assertEquals("bn", received.language.code)
        assertTrue(received.content.contains("মেডিকেল"))

        // Auto ACK should have been transmitted back over mock transport
        val sentPayloads = mockTransport.getSentPayloads()
        assertTrue("Transport should have emitted ACK packet back", sentPayloads.isNotEmpty())
    }

    @Test
    fun `corrupted CRC32 packet injection is safely rejected by validator`() = runBlocking {
        val initialCount = messageRepository.observeMessages().first().size

        // Inject bad CRC packet
        val result = demoManager.injectCorruptedPacket()
        assertTrue(result.isSuccess)

        delay(150L)

        val postCount = messageRepository.observeMessages().first().size
        assertEquals("Corrupted packet must NOT be stored in message repository", initialCount, postCount)
    }

    @Test
    fun `fault injection and recovery demonstrates transmission resilience`() = runBlocking {
        // 1. Inject simulated transport transmission failure
        demoManager.toggleSimulatedTransportFailure(true)
        assertTrue(demoManager.isFaultInjected.value)

        // Send a message -> should fail transmission
        val sendResult = commManager.sendMessage(
            text = "Emergency report test",
            languageCode = "en",
            priority = MessagePriority.ALERT
        )
        assertTrue("Send call initiates and returns queued message", sendResult.isSuccess)

        val msgId = sendResult.getOrThrow().messageId
        delay(300L)

        // Verify delivery status is marked FAILED
        val statuses = commManager.deliveryStatuses.value
        assertEquals(DeliveryStatus.FAILED, statuses[msgId])

        // 2. Clear fault
        demoManager.toggleSimulatedTransportFailure(false)
        assertFalse(demoManager.isFaultInjected.value)

        // 3. Retry message
        val retryResult = commManager.retryMessage(msgId)
        assertTrue("Retry must succeed after fault cleared", retryResult.isSuccess)

        delay(200L)
        val updatedStatuses = commManager.deliveryStatuses.value
        assertEquals(DeliveryStatus.DELIVERED, updatedStatuses[msgId])
    }

    @Test
    fun `demo session reset reinitializes repository and deduplication cache`() = runBlocking {
        // Inject a message first
        demoManager.simulateIncomingPeerMessage(2) // English
        delay(100L)
        assertFalse(messageRepository.observeMessages().first().isEmpty())

        // Reset demo session
        demoManager.resetDemoSession()
        delay(100L)

        val messagesAfterReset = messageRepository.observeMessages().first()
        assertTrue("Messages should be cleared after session reset", messagesAfterReset.isEmpty())
        assertFalse("Faults should be cleared", demoManager.isFaultInjected.value)
    }
}
