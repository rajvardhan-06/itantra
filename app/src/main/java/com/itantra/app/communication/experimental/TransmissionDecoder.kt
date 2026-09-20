package com.itantra.app.communication.experimental

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater

/**
 * Pluggable abstraction for payload decoding from wire transports.
 */
interface TransmissionDecoder {
    val name: String

    /**
     * Decodes a received byte array into the raw original text.
     */
    fun decode(payload: ByteArray): Result<String>
}

/**
 * Standard decoder for direct UTF-8 payloads.
 */
class Utf8TextDecoder : TransmissionDecoder {
    override val name: String = "UTF-8 Direct Decoder"

    override fun decode(payload: ByteArray): Result<String> {
        return try {
            Result.success(String(payload, Charsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Decompressing decoder supporting Deflate or GZIP compressed streams.
 */
class CompressedTextDecoder(
    private val isGzip: Boolean = false
) : TransmissionDecoder {
    override val name: String = if (isGzip) "GZIP Decompressor" else "Deflate Decompressor"

    override fun decode(payload: ByteArray): Result<String> {
        return try {
            val outputStream = ByteArrayOutputStream()
            if (isGzip) {
                GZIPInputStream(ByteArrayInputStream(payload)).use { input ->
                    input.copyTo(outputStream)
                }
            } else {
                val inflater = Inflater()
                inflater.setInput(payload)
                val buffer = ByteArray(512)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count == 0 && inflater.needsInput()) break
                    outputStream.write(buffer, 0, count)
                }
                inflater.end()
            }
            Result.success(outputStream.toString(Charsets.UTF_8.name()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Research prototype stub for low-bitrate neural acoustic decoding.
 */
class ExperimentalNeuralDecoder : TransmissionDecoder {
    override val name: String = "Experimental Neural Acoustic Decoder"

    override fun decode(payload: ByteArray): Result<String> {
        return Result.failure(
            UnsupportedOperationException(
                "Decoding neural acoustic tokens requires active NPU/DSP neural vocoder synthesis. " +
                    "Standard text decoding via Utf8TextDecoder is active."
            )
        )
    }
}
