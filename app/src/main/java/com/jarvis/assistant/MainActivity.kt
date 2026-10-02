package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private lateinit var tts: TextToSpeech

    private val microphonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startJarvisService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        webView = WebView(this)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.webViewClient = WebViewClient()

        webView.addJavascriptInterface(
            JarvisBridge(),
            "AndroidJarvis"
        )

        setContentView(webView)

        webView.loadUrl(
            "file:///android_asset/jarvis.html"
        )

        requestMicrophonePermission()
    }

    private fun requestMicrophonePermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startJarvisService()
        } else {
            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun startJarvisService() {

        val serviceIntent =
            Intent(
                this,
                JarvisForegroundService::class.java
            )

        ContextCompat.startForegroundService(
            this,
            serviceIntent
        )
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            tts.language = Locale.getDefault()
        }
    }

    override fun onDestroy() {

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        if (::webView.isInitialized) {
            webView.destroy()
        }

        super.onDestroy()
    }

    inner class JarvisBridge {

        @JavascriptInterface
        fun speak(text: String) {

            if (::tts.isInitialized) {

                tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JARVIS_HTML"
                )
            }
        }

        @JavascriptInterface
        fun openAccessibilitySettings() {

            startActivity(
                Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                )
            )
        }

        @JavascriptInterface
        fun talkNow() {

            val intent =
                Intent(
                    this@MainActivity,
                    JarvisForegroundService::class.java
                ).apply {
                    action =
                        JarvisForegroundService.ACTION_TALK_NOW
                }

            ContextCompat.startForegroundService(
                this@MainActivity,
                intent
            )
        }
    }
}
