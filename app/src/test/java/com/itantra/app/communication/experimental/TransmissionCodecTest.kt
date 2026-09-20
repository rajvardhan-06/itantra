package com.itantra.app.communication.experimental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying experimental transmission encoders, decoders,
 * lossless compression, and neural feasibility models.
 */
class TransmissionCodecTest {

    @Test
    fun utf8DirectCodec_multilingualRoundTrip() {
        val encoder = Utf8TextEncoder()
        val decoder = Utf8TextDecoder()

        val samples = listOf(
            "Emergency medical dispatch needed at sector 4",
            "तत्काल चिकित्सा सहायता की आवश्यकता है",
            "உடனடி மருத்துவ உதவி தேவைப்படுகிறது",
            "వైద్య సహాయం తక్షణమే అవసరం"
        )

        for (sample in samples) {
            val encoded = encoder.encode(sample).getOrThrow()
            val decoded = decoder.decode(encoded).getOrThrow()
            assertEquals(sample, decoded)
        }
    }

    @Test
    fun compressedTextCodec_deflateRoundTrip() {
        val encoder = CompressedTextEncoder(useGzip = false)
        val decoder = CompressedTextDecoder(isGzip = false)

        val longMultilingualText = (1..15).joinToString("\n") {
            "Point $it: Disaster response alert for region $it. तुरंत सहायता भेजें. உதவி தேவை."
        }

        val encoded = encoder.encode(longMultilingualText).getOrThrow()
        val decoded = decoder.decode(encoded).getOrThrow()

        assertEquals(longMultilingualText, decoded)
        // Compression reduces footprint for repetitive/structured transcripts
        assertTrue(encoded.size < longMultilingualText.toByteArray(Charsets.UTF_8).size)
    }

    @Test
    fun compressedTextCodec_gzipRoundTrip() {
        val encoder = CompressedTextEncoder(useGzip = true)
        val decoder = CompressedTextDecoder(isGzip = true)

        val sample = "Standard GZIP compression test across nodes: 1234567890."
        val encoded = encoder.encode(sample).getOrThrow()
        val decoded = decoder.decode(encoded).getOrThrow()

        assertEquals(sample, decoded)
    }

    @Test
    fun experimentalNeuralCodec_returnsGracefulFailure() {
        val encoder = ExperimentalNeuralEncoder()
        val decoder = ExperimentalNeuralDecoder()

        val encodeRes = encoder.encode("Test neural audio")
        assertTrue(encodeRes.isFailure)
        assertTrue(encodeRes.exceptionOrNull() is UnsupportedOperationException)

        val decodeRes = decoder.decode(byteArrayOf(1, 2, 3))
        assertTrue(decodeRes.isFailure)
        assertTrue(decodeRes.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun neuralTransceiverFeasibility_containsRigorousAnalysis() {
        val matrix = NeuralTransceiverFeasibility.COMPARISON_MATRIX
        assertTrue(matrix.isNotEmpty())
        for (item in matrix) {
            assertNotNull(item.dimension)
            assertTrue(item.advantageFactor.isNotBlank())
        }
        assertTrue(NeuralTransceiverFeasibility.ARCHITECTURAL_CONCLUSION.isNotBlank())
    }
}
