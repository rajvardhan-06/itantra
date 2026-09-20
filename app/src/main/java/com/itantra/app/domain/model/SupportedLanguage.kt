package com.itantra.app.domain.model

/**
 * Represents a language supported by the iTantra communication system.
 * All ten Indian languages plus English are supported for offline STT/TTS.
 */
enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val script: String
) {
    HINDI("hi", "Hindi", "हिन्दी", "Devanagari"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી", "Gujarati"),
    MARATHI("mr", "Marathi", "मराठी", "Devanagari"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ", "Kannada"),
    MALAYALAM("ml", "Malayalam", "മലയാളം", "Malayalam"),
    TAMIL("ta", "Tamil", "தமிழ்", "Tamil"),
    TELUGU("te", "Telugu", "తెలుగు", "Telugu"),
    ODIA("or", "Odia", "ଓଡ଼ିଆ", "Odia"),
    BENGALI("bn", "Bengali", "বাংলা", "Bengali"),
    ENGLISH("en", "English", "English", "Latin");

    companion object {
        /** Find a language by its BCP-47 code, defaulting to HINDI if not found. */
        fun fromCode(code: String): SupportedLanguage =
            values().firstOrNull { it.code == code } ?: HINDI

        /** The default language used on first launch. */
        val DEFAULT = HINDI
    }
}
