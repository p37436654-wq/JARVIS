package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val title = TextView(this).apply {
            text = "JARVIS"
            textSize = 32f
            setPadding(0, 0, 0, 40)
        }

        val status = TextView(this).apply {
            text = "Your personal AI assistant"
            textSize = 18f
            setPadding(0, 0, 0, 40)
        }

        val startButton = Button(this).apply {
            text = "START JARVIS"
            setOnClickListener {
                val intent = Intent(
                    this@MainActivity,
                    JarvisForegroundService::class.java
                )
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    intent
                )
                status.text = "JARVIS is listening..."
            }
        }

        val accessibilityButton = Button(this).apply {
            text = "ENABLE PHONE CONTROL"
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(startButton)
        layout.addView(accessibilityButton)

        setContentView(layout)
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                100
            )
        }
    }
}
