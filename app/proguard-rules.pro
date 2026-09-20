# Add project specific ProGuard rules here.
# By default, the flags in this file are applied to keep rules.

# iTantra — Offline Multilingual Communication
# ProGuard rules for release builds

# Keep domain models (needed for future serialisation/Room integration)
-keep class com.itantra.app.domain.model.** { *; }

# Keep offline engine interfaces for pluggable swapping
-keep interface com.itantra.app.audio.AudioRecorder { *; }
-keep interface com.itantra.app.stt.SpeechToTextEngine { *; }
-keep interface com.itantra.app.tts.TextToSpeechEngine { *; }
-keep interface com.itantra.app.communication.CommunicationTransport { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
