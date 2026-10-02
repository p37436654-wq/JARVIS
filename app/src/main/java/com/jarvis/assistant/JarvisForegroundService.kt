package com.jarvis.assistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class JarvisForegroundService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_TALK_NOW =
            "com.jarvis.assistant.ACTION_TALK_NOW"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech

    private var directMode = false
    private var isListening = false
    private var isSpeaking = false
    private var ttsReady = false

    private val handler =
        Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            1001,
            NotificationCompat.Builder(
                this,
                "jarvis_channel"
            )
                .setContentTitle("JARVIS")
                .setContentText("JARVIS is listening")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()
        )

        tts = TextToSpeech(this, this)

        // Give Android a moment to finish creating the service.
        handler.postDelayed(
            {
                if (hasMicrophonePermission()) {
                    startListening()
                }
            },
            1000
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_TALK_NOW) {

            directMode = true

            if (!isSpeaking && hasMicrophonePermission()) {
                stopListening()
                handler.postDelayed(
                    {
                        startListening()
                    },
                    300
                )
            }
        }

        return START_STICKY
    }

    private fun hasMicrophonePermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun stopListening() {

        isListening = false

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }

        speechRecognizer = null
    }

    private fun startListening() {

        if (isSpeaking) return
        if (isListening) return

        if (!hasMicrophonePermission()) {
            speak("Microphone permission is required.")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is not available on this device.")
            return
        }

        stopListening()

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(
                    error: Int
                ) {

                    isListening = false

                    if (!isSpeaking) {
                        restartListening()
                    }
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    isListening = false

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        matches
                            ?.firstOrNull()
                            ?.trim()
                            ?: ""

                    if (directMode) {

                        directMode = false

                        if (text.isNotBlank()) {

                            JarvisBrain.handle(
                                this@JarvisForegroundService,
                                text
                            ) { response ->

                                speakAndResume(response)
                            }

                        } else {

                            speakAndResume(
                                "I didn't catch that."
                            )
                        }

                        return
                    }

                    val lowerText =
                        text.lowercase(Locale.getDefault())

                    if (lowerText.contains("jarvis")) {

                        val command =
                            lowerText
                                .substringAfter("jarvis")
                                .trim()

                        if (command.isEmpty()) {

                            speakAndResume(
                                "Yes. I am listening."
                            )

                        } else {

                            JarvisBrain.handle(
                                this@JarvisForegroundService,
                                command
                            ) { response ->

                                speakAndResume(response)
                            }
                        }

                    } else {

                        restartListening()
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )

        val recognitionIntent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
                )
            }

        try {

            speechRecognizer?.startListening(
                recognitionIntent
            )

        } catch (_: Exception) {

            isListening = false
            restartListening()
        }
    }

    private fun restartListening() {

        if (isSpeaking) return

        handler.removeCallbacksAndMessages(null)

        handler.postDelayed(
            {

                if (
                    !isSpeaking &&
                    !isListening &&
                    hasMicrophonePermission()
                ) {
                    startListening()
                }

            },
            1000
        )
    }

    private fun speakAndResume(
        text: String
    ) {

        speak(text)
    }

    private fun speak(
        text: String
    ) {

        if (!::tts.isInitialized) return

        if (!ttsReady) {
            handler.postDelayed(
                {
                    if (ttsReady) {
                        speak(text)
                    }
                },
                500
            )
            return
        }

        isSpeaking = true

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_RESPONSE"
        )
    }

    override fun onInit(
        status: Int
    ) {

        if (status == TextToSpeech.SUCCESS) {

            val result =
                tts.setLanguage(
                    Locale.getDefault()
                )

            ttsReady =
                result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED

            tts.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {

                    override fun onStart(
                        utteranceId: String?
                    ) {
                        isSpeaking = true
                    }

                    override fun onDone(
                        utteranceId: String?
                    ) {

                        isSpeaking = false

                        handler.postDelayed(
                            {
                                if (
                                    !isSpeaking &&
                                    !isListening
                                ) {
                                    startListening()
                                }
                            },
                            700
                        )
                    }

                    override fun onError(
                        utteranceId: String?
                    ) {

                        isSpeaking = false
                        restartListening()
                    }
                }
            )

        } else {

            ttsReady = false
        }
    }

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                "jarvis_channel",
                "JARVIS",
                NotificationManager.IMPORTANCE_LOW
            )

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        stopListening()

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
                    
            
