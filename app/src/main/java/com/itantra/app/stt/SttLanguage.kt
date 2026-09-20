package com.itantra.app.stt

import com.itantra.app.domain.model.SupportedLanguage

/**
 * Enumeration of languages supported in the iTantra STT pipeline.
 *
 * Each language entry specifies its standard ISO 639-1 code, display metadata,
 * script, whether an offline model package is currently available for local inference,
 * and estimated model size in bytes.
 */
enum class SttLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val script: String,
    val isModelAvailable: Boolean,
    val modelSizeBytes: Long
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        script = "Latin",
        isModelAvailable = true,
        modelSizeBytes = 42_000_000L // ~40 MB small model
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिन्दी",
        script = "Devanagari",
        isModelAvailable = true,
        modelSizeBytes = 45_000_000L // ~43 MB small model
    ),
    BENGALI(
        code = "bn",
        displayName = "Bengali",
        nativeName = "বাংলা",
        script = "Bengali",
        isModelAvailable = true,
        modelSizeBytes = 48_000_000L // ~46 MB small model
    ),
    GUJARATI(
        code = "gu",
        displayName = "Gujarati",
        nativeName = "ગુજરાતી",
        script = "Gujarati",
        isModelAvailable = false,
        modelSizeBytes = 46_000_000L
    ),
    MARATHI(
        code = "mr",
        displayName = "Marathi",
        nativeName = "मराठी",
        script = "Devanagari",
        isModelAvailable = false,
        modelSizeBytes = 47_000_000L
    ),
    KANNADA(
        code = "kn",
        displayName = "Kannada",
        nativeName = "ಕನ್ನಡ",
        script = "Kannada",
        isModelAvailable = false,
        modelSizeBytes = 50_000_000L
    ),
    MALAYALAM(
        code = "ml",
        displayName = "Malayalam",
        nativeName = "മലയാളം",
        script = "Malayalam",
        isModelAvailable = false,
        modelSizeBytes = 52_000_000L
    ),
    TAMIL(
        code = "ta",
        displayName = "Tamil",
        nativeName = "தமிழ்",
        script = "Tamil",
        isModelAvailable = false,
        modelSizeBytes = 53_000_000L
    ),
    TELUGU(
        code = "te",
        displayName = "Telugu",
        nativeName = "తెలుగు",
        script = "Telugu",
        isModelAvailable = false,
        modelSizeBytes = 51_000_000L
    ),
    ODIA(
        code = "or",
        displayName = "Odia",
        nativeName = "ଓଡ଼ିଆ",
        script = "Odia",
        isModelAvailable = false,
        modelSizeBytes = 49_000_000L
    );

    val formattedModelSize: String
        get() = "${modelSizeBytes / (1024 * 1024)} MB"

    companion object {
        fun fromCode(code: String): SttLanguage? {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
        }

        fun fromSupportedLanguage(lang: SupportedLanguage): SttLanguage {
            return fromCode(lang.code) ?: HINDI
        }
    }
}
