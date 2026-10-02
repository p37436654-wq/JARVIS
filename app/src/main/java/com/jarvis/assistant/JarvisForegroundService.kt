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
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
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

            object : android.speech.RecognitionListener {

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

                override fun onError(
                    error: Int
                ) {

                    isListening = false

                    restartListening()
                }

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {}

               
