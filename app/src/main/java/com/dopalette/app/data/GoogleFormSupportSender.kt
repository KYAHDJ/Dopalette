package com.dopalette.app.data

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Dosevia-style Google Form sender (no WebView). */
object GoogleFormSupportSender {

    fun submit(email: String, topic: String, message: String): Result<Unit> {
        return runCatching {
            val url = URL(SupportFormConfig.FORM_RESPONSE_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                instanceFollowRedirects = false
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent", "DoPalette")
            }

            val data = buildFormBody(
                mapOf(
                    SupportFormConfig.ENTRY_TOPIC to topic.trim(),
                    SupportFormConfig.ENTRY_EMAIL to email.trim(),
                    SupportFormConfig.ENTRY_MESSAGE to message.trim(),
                )
            )

            BufferedWriter(OutputStreamWriter(conn.outputStream, Charsets.UTF_8)).use { out ->
                out.write(data)
                out.flush()
            }

            val code = conn.responseCode
            conn.disconnect()

            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw IllegalStateException("Google Form is restricted. Open the form settings and allow public responses.")
            }
            if (code !in 200..399) {
                throw IllegalStateException("Server returned $code")
            }
        }
    }

    private fun buildFormBody(fields: Map<String, String>): String {
        return fields.entries.joinToString("&") { (key, value) ->
            val encodedKey = URLEncoder.encode(key, "UTF-8")
            val encodedValue = URLEncoder.encode(value, "UTF-8")
            "$encodedKey=$encodedValue"
        }
    }
}
