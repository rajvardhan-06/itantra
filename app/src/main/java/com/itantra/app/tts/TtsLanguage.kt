package com.itantra.app.tts

import java.util.Locale

/**
 * Enumeration of supported TTS languages across the iTantra offline speech synthesis pipeline.
 *
 * Maps ISO 639-1 language codes to appropriate regional [Locale] instances for voice synthesis.
 */
enum class TtsLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val locale: Locale,
    val script: String
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        locale = Locale("en", "IN"),
        script = "Latin"
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिन्दी",
        locale = Locale("hi", "IN"),
        script = "Devanagari"
    ),
    BENGALI(
        code = "bn",
        displayName = "Bengali",
        nativeName = "বাংলা",
        locale = Locale("bn", "IN"),
        script = "Bengali"
    ),
    GUJARATI(
        code = "gu",
        displayName = "Gujarati",
        nativeName = "ગુજરાતી",
        locale = Locale("gu", "IN"),
        script = "Gujarati"
    ),
    MARATHI(
        code = "mr",
        displayName = "Marathi",
        nativeName = "मराठी",
        locale = Locale("mr", "IN"),
        script = "Devanagari"
    ),
    KANNADA(
        code = "kn",
        displayName = "Kannada",
        nativeName = "ಕನ್ನಡ",
        locale = Locale("kn", "IN"),
        script = "Kannada"
    ),
    MALAYALAM(
        code = "ml",
        displayName = "Malayalam",
        nativeName = "മലയാളം",
        locale = Locale("ml", "IN"),
        script = "Malayalam"
    ),
    TAMIL(
        code = "ta",
        displayName = "Tamil",
        nativeName = "தமிழ்",
        locale = Locale("ta", "IN"),
        script = "Tamil"
    ),
    TELUGU(
        code = "te",
        displayName = "Telugu",
        nativeName = "తెలుగు",
        locale = Locale("te", "IN"),
        script = "Telugu"
    ),
    ODIA(
        code = "or",
        displayName = "Odia",
        nativeName = "ଓଡ଼ିଆ",
        locale = Locale("or", "IN"),
        script = "Odia"
    );

    companion object {
        private val codeMap: Map<String, TtsLanguage> by lazy {
            entries.associateBy { it.code.lowercase().trim() }
        }

        /**
         * Resolves a [TtsLanguage] from an ISO 639-1 code. Returns null if unsupported.
         */
        fun fromCode(code: String?): TtsLanguage? {
            if (code.isNullOrBlank()) return null
            return codeMap[code.lowercase().trim()]
        }

        /**
         * Returns whether the given ISO language code is supported by the TTS pipeline.
         */
        fun isSupported(code: String?): Boolean {
            return fromCode(code) != null
        }
    }
}
