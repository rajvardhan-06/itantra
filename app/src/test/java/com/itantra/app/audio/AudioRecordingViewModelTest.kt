package com.itantra.app.audio

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AudioRecordingViewModel] lifecycle and state transitions
 * using standard JUnit and [runBlocking].
 */
class AudioRecordingViewModelTest {

    private lateinit var mockRecorder: MockAudioRecorder
    private lateinit var viewModel: AudioRecordingViewModel

    @Before
    fun setUp() {
        mockRecorder = MockAudioRecorder()
        viewModel = AudioRecordingViewModel(
            audioRecorder = mockRecorder,
            externalScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
    }

    @Test
    fun `initial state is Idle with no permission`() {
        assertTrue(viewModel.recordingState.value is RecordingState.Idle)
        assertFalse(viewModel.hasPermission.value)
        assertFalse(viewModel.isPermanentlyDenied.value)
    }

    @Test
    fun `permission granted transitions state to Ready`() {
        viewModel.onRequestingPermission()
        assertTrue(viewModel.recordingState.value is RecordingState.RequestingPermission)

        viewModel.onPermissionResult(isGranted = true)
        assertTrue(viewModel.hasPermission.value)
        assertTrue(viewModel.recordingState.value is RecordingState.Ready)
    }

    @Test
    fun `permission denied transitions state to Error`() {
        viewModel.onRequestingPermission()
        viewModel.onPermissionResult(isGranted = false, permanentlyDenied = false)

        assertFalse(viewModel.hasPermission.value)
        assertFalse(viewModel.isPermanentlyDenied.value)
        assertTrue(viewModel.recordingState.value is RecordingState.Error)
    }

    @Test
    fun `permanent permission denial sets isPermanentlyDenied flag and Error state`() {
        viewModel.onRequestingPermission()
        viewModel.onPermissionResult(isGranted = false, permanentlyDenied = true)

        assertFalse(viewModel.hasPermission.value)
        assertTrue(viewModel.isPermanentlyDenied.value)
        val state = viewModel.recordingState.value
        assertTrue(state is RecordingState.Error)
        assertTrue((state as RecordingState.Error).message.contains("App Settings"))
    }

    @Test
    fun `startRecording initiates capture on recorder`() {
        runBlocking {
            val startResult = mockRecorder.startRecording()
            assertTrue(startResult.isSuccess)
            assertTrue(mockRecorder.isRecording())

            val stopResult = mockRecorder.stopRecording()
            assertTrue(stopResult.isSuccess)
            assertFalse(mockRecorder.isRecording())
            val data = stopResult.getOrThrow()
            assertEquals(AudioConfiguration.BYTES_PER_SECOND, data.size)
        }
    }

    @Test
    fun `cancelRecording on mock recorder resets recording state`() {
        runBlocking {
            mockRecorder.startRecording()
            assertTrue(mockRecorder.isRecording())

            mockRecorder.cancelRecording()
            assertFalse(mockRecorder.isRecording())
        }
    }

    @Test
    fun `multiple simultaneous start attempts on recorder fail safely`() {
        runBlocking {
            val firstStart = mockRecorder.startRecording()
            assertTrue(firstStart.isSuccess)

            val secondStart = mockRecorder.startRecording()
            assertFalse(secondStart.isSuccess)
            assertTrue(secondStart.exceptionOrNull() is IllegalStateException)

            mockRecorder.stopRecording()
        }
    }
}
