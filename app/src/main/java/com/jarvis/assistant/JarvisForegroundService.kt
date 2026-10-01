package com.jarvis.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
    private val handler = Handler(Looper.getMainLooper())

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

            startListening()
        }

        return START_STICKY
    }

    private fun startListening() {

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

            object : android.speech.RecognitionListener {

                override fun onResults(results: Bundle?) {

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

                                speak(response)

                                resumeListening()
                            }

                        } else {

                            speak(
                                "I didn't catch that."
                            )

                            resumeListening()
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

                            speak(
                                "Yes. I am listening."
                            )

                        } else {

                            JarvisBrain.handle(
                                this@JarvisForegroundService,
                                command
                            ) { response ->

                                speak(response)
                            }
                        }
                    }

                    resumeListening()
                }

                override fun onError(error: Int) {
                    resumeListening()
                }

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {}

                override fun onBeginningOfSpeech() {}
                override fun onEndOfSpeech() {}
                override fun onRmsChanged(
                    rmsdB: Float
                ) {}

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {}

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
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
                )
            }

        speechRecognizer?.startListening(intent)
    }

    private fun resumeListening() {

        handler.removeCallbacksAndMessages(null)

        handler.postDelayed({

            if (!directMode) {
                startListening()
            }

        }, 2500)
    }

    private fun speak(text: String) {

        if (::tts.isInitialized) {

            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_RESPONSE"
            )
        }
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            tts.language = Locale.US
            tts.setSpeechRate(0.95f)
            tts.setPitch(0.85f)
        }
    }

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                "jarvis_channel",
                "JARVIS Voice Service",
                NotificationManager.IMPORTANCE_LOW
            )

        getSystemService(
            NotificationManager::class.java
        ).createNotificationChannel(channel)
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        speechRecognizer?.destroy()

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}
