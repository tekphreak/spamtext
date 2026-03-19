package com.tekphreak.spamtext

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsHelper {
    fun resolveTemplates(message: String, phoneNumber: String = ""): String {
        val now = Date()
        return message
            .replace("[timestamp]", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(now))
            .replace("[date]", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now))
            .replace("[time]", SimpleDateFormat("HH:mm:ss", Locale.US).format(now))
            .replace("[numbercalled]", phoneNumber)
    }

    fun sendSms(context: Context, phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
