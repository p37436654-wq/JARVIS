package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object JarvisBrain {

    private const val GROQ_URL =
        "https://api.groq.com/openai/v1/chat/completions"

    private const val MODEL =
        "openai/gpt-oss-120b"

    fun handle(
        context: Context,
        command: String,
        callback: (String) -> Unit
    ) {
        val trimmed = command.trim()
        val lower = trimmed.lowercase()

        when {

            lower.startsWith("open settings") ||
            lower == "settings" ||
            lower.startsWith("open phone settings") -> {

                try {
                    val intent = Intent(
                        Intent.ACTION_SETTINGS
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startActivity(intent)

                    callback("Opening phone settings.")

                } catch (e: Exception) {

                    callback("I couldn't open phone settings.")
                }
            }

            lower.startsWith("open ") -> {

                val appName =
                    trimmed.substringAfter("open ").trim()

                if (appName.isBlank()) {

                    callback("Tell me which app to open.")

                } else if (openApp(context, appName)) {

                    callback("Opening $appName.")

                } else {

                    callback(
                        "I couldn't find $appName on this phone."
                    )
                }
            }

            lower.startsWith("search ") -> {

                val query =
                    trimmed.substringAfter("search ").trim()

                if (query.isBlank()) {

                    callback(
                        "Tell me what you want me to search for."
                    )

                } else {

                    try {

                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://www.google.com/search?q=" +
                                    Uri.encode(query)
                            )
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }

                        context.startActivity(intent)

                        callback(
                            "Searching for $query."
                        )

                    } catch (e: Exception) {

                        callback(
                            "I couldn't open the search."
                        )
                    }
                }
            }

            else -> {
                askGroq(trimmed, callback)
            }
        }
    }

    private fun openApp(
        context: Context,
        appName: String
    ): Boolean {

        val packageManager =
            context.packageManager

        val apps =
            packageManager.getInstalledApplications(0)

        val target =
            apps.firstOrNull {

                packageManager
                    .getApplicationLabel(it)
                    .toString()
                    .equals(
                        appName,
                        ignoreCase = true
                    )
            }

        if (target != null) {

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        target.packageName
                    )

            if (launchIntent != null) {

                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                context.startActivity(
                    launchIntent
                )

                return true
            }
        }

        return false
    }

    private fun askGroq(
        command: String,
        callback: (String) -> Unit
    ) {

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            var connection:
                HttpURLConnection? = null

            try {

                val apiKey =
                    BuildConfig.GROQ_API_KEY

                if (apiKey.isBlank()) {

                    withContext(
                        Dispatchers.Main
                    ) {
                        callback(
                            "Groq API key is not configured yet."
                        )
                    }

                    return@launch
                }

                connection =
                    URL(GROQ_URL)
                        .openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    30000

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $apiKey"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.doOutput = true

                val body =
                    JSONObject().apply {

                        put(
                            "model",
                            MODEL
                        )

                        put(
                            "messages",
                            org.json.JSONArray().apply {

                                put(
                                    JSONObject().apply {

                                        put(
                                            "role",
                                            "system"
                                        )

                                        put(
                                            "content",
                                            """
                                            You are JARVIS, a concise Android personal assistant.

                                            Never claim that you performed an action unless the app actually performed it.

                                            Give short, natural spoken responses.
                                            """.trimIndent()
                                        )
                                    }
                                )

                                put(
                                    JSONObject().apply {

                                        put(
                                            "role",
                                            "user"
                                        )

                                        put(
                                            "content",
                                            command
                                        )
                                    }
                                )
                            }
                        )
                    }

                connection.outputStream.use {

                    it.write(
                        body
                            .toString()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }

                val status =
                    connection.responseCode

                val stream =
                    if (status in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val response =
                    stream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: ""

                if (status !in 200..299) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        callback(
                            "Groq request failed with HTTP $status."
                        )
                    }

                    return@launch
                }

                val json =
                    JSONObject(response)

                val answer =
                    json
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                withContext(
                    Dispatchers.Main
                ) {

                    callback(
                        answer.trim()
                    )
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    callback(
                        "I couldn't connect to Groq."
                    )
                }

            } finally {

                connection?.disconnect()
            }
        }
    }
}
    

    
