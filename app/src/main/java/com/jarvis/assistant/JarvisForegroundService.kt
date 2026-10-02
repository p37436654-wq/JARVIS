package com.jarvis.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
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

        startListening()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_TALK_NOW) {
            directMode = true

            if (!isSpeaking) {
                startListening()
            }
        }

        return START_STICKY
    }

    private fun startListening() {

        if (isSpeaking) return
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak(
                "Speech recognition is not available on this device."
            )
            return
        }

        speechRecognizer?.destroy()

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

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {}

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
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
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
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
            isListening = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            restartListening()
        }
    }

    private fun restartListening() {

        if (isSpeaking) return

        handler.removeCallbacksAndMessages(null)

        handler.postDelayed(
            {
                if (!isSpeaking && !isListening) {
                    startListening()
                }
            },
            700
        )
    }

    private fun speakAndResume(
        text: String
    ) {

        speak(text)

        handler.postDelayed(
            {
                if (!isSpeaking) {
                    startListening()
                }
            },
            1200
        )
    }

    private fun speak(
        text: String
    ) {

        if (!::tts.isInitialized) return

        isSpeaking = true

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_RESPONSE"
        )
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            tts.language = Locale.getDefault()

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
                                if (!isListening) {
                                    startListening()
                                }
                            },
                            500
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

        speechRecognizer?.destroy()
        speechRecognizer = null

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
