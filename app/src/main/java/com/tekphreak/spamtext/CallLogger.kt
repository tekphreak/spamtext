package com.tekphreak.spamtext

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CallLogger {
    private const val LOG_FILE = "spamtext_calls.log"
    private const val MAX_ENTRIES = 200

    private fun maskNumber(number: String): String {
        return if (number.length > 8) {
            number.take(4) + "****" + number.takeLast(4)
        } else {
            "****"
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun appendEntry(context: Context, entry: String) {
        val file = File(context.filesDir, LOG_FILE)
        val lines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
        lines.add(entry)
        if (lines.size > MAX_ENTRIES) {
            lines.subList(0, lines.size - MAX_ENTRIES).clear()
        }
        file.writeText(lines.joinToString("\n") + "\n")
    }

    fun logSmsSent(context: Context, phoneNumber: String, message: String) {
        appendEntry(context, "${timestamp()} -- ${maskNumber(phoneNumber)} - $message")
    }

    fun logSkipped(context: Context, phoneNumber: String, reason: String) {
        appendEntry(context, "${timestamp()} -- ${maskNumber(phoneNumber)} - [skipped: $reason]")
    }

    fun getRecentEntries(context: Context, limit: Int = 50): List<String> {
        val file = File(context.filesDir, LOG_FILE)
        if (!file.exists()) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .reversed()
            .take(limit)
    }

    fun clearLog(context: Context) {
        File(context.filesDir, LOG_FILE).delete()
    }
}
