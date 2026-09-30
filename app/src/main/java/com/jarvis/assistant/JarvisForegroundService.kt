package com.jarvis.assistant

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class JarvisForegroundService : Service(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            1001,
            NotificationCompat.Builder(this, "jarvis_channel")
                .setContentTitle("JARVIS")
                .setContentText("JARVIS is listening")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()
        )

        tts = TextToSpeech(this, this)

        startListening()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is not available on this device.")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(
            object : android.speech.RecognitionListener {

                override fun onResults(results: android.os.Bundle?) {
                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text = matches?.firstOrNull()
                        ?.lowercase(Locale.getDefault())
                        ?: ""

                    if (text.contains("jarvis")) {
                        val command = text
                            .substringAfter("jarvis")
                            .trim()

                        if (command.isEmpty()) {
                            speak("Yes. I am listening.")
                        } else {
                            JarvisBrain.handle(
                                this@JarvisForegroundService,
                                command
                            ) { response ->
                                speak(response)
                            }
                        }
                    }

                    startListening()
                }

                override fun onError(error: Int) {
                    startListening()
                }

                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(
                    partialResults: android.os.Bundle?
                ) {}
                override fun onEvent(
                    eventType: Int,
                    params: android.os.Bundle?
                ) {}
            }
        )

        val intent = Intent(
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
        }

        speechRecognizer.startListening(intent)
    }

    private fun speak(text: String) {
        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_RESPONSE"
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "jarvis_channel",
            "JARVIS Voice Service",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
