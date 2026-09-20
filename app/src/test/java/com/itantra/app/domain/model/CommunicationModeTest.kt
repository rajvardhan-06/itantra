package com.itantra.app.domain.model

import com.itantra.app.domain.translation.NoOpTranslationEngine
import com.itantra.app.domain.translation.TranslationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying CommunicationMode enumerations, support flags,
 * and TranslationEngine contracts introduced in Phase 9.
 */
class CommunicationModeTest {

    @Test
    fun communicationModes_allEnumValuesConfigured() {
        val modes = CommunicationMode.entries
        assertEquals(4, modes.size)

        assertTrue(modes.contains(CommunicationMode.PUSH_TO_TALK))
        assertTrue(modes.contains(CommunicationMode.TEXT_ONLY))
        assertTrue(modes.contains(CommunicationMode.VOICE_ASSISTED))
        assertTrue(modes.contains(CommunicationMode.PHONE_MODE))
    }

    @Test
    fun communicationModes_supportFlagsAndDescriptions() {
        // Supported modes
        assertTrue(CommunicationMode.PUSH_TO_TALK.isSupported)
        assertTrue(CommunicationMode.TEXT_ONLY.isSupported)
        assertTrue(CommunicationMode.VOICE_ASSISTED.isSupported)

        // Experimental / In-Design modes
        assertFalse(CommunicationMode.PHONE_MODE.isSupported)

        for (mode in CommunicationMode.entries) {
            assertTrue(mode.displayName.isNotBlank())
            assertTrue(mode.description.isNotBlank())
        }
    }

    @Test
    fun noOpTranslationEngine_identicalLanguagesPassThrough() = runBlocking {
        val engine = NoOpTranslationEngine()
        val text = "नमस्ते, यह एक परीक्षण है"
        val result = engine.translate(text, "hi", "hi")

        assertTrue(result.isSuccess)
        assertEquals(text, result.getOrNull())
        assertTrue(engine.isLanguagePairAvailable("hi", "hi"))
    }

    @Test
    fun noOpTranslationEngine_distinctLanguagesReturnGracefulFailure() = runBlocking {
        val engine = NoOpTranslationEngine()
        val text = "नमस्ते"
        val result = engine.translate(text, "hi", "ta")

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is TranslationException.TranslationUnavailableException)
        assertFalse(engine.isLanguagePairAvailable("hi", "ta"))
    }
}
