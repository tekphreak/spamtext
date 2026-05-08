package com.tekphreak.spamtext

import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpamCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        PrefsHelper.init(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val response = buildDefaultResponse()

        if (!PrefsHelper.isServiceEnabled) {
            CallLogger.logSkipped(this, "unknown", "service disabled")
            respondToCall(callDetails, response)
            return
        }

        val phoneNumber = callDetails.handle?.schemeSpecificPart
        if (phoneNumber.isNullOrBlank()) {
            CallLogger.logSkipped(this, "hidden/unknown", "no number available")
            respondToCall(callDetails, response)
            return
        }

        // Check permissions before proceeding
        if (!hasRequiredPermissions()) {
            CallLogger.logSkipped(this, phoneNumber, "missing permissions")
            respondToCall(callDetails, response)
            return
        }

        val stirFailed = checkStirFailed(callDetails)
        val isInContacts = ContactChecker.isInContacts(this, phoneNumber)
        val shouldSend = (PrefsHelper.isTriggerNotInContacts && !isInContacts) ||
                         (PrefsHelper.isTriggerFailedStir && stirFailed)

        respondToCall(callDetails, response)

        if (shouldSend) {
            val resolved = SmsHelper.resolveTemplates(PrefsHelper.smsMessage, phoneNumber)
            scope.launch {
                SmsHelper.sendSms(this@SpamCallScreeningService, phoneNumber, resolved)
                CallLogger.logSmsSent(this@SpamCallScreeningService, phoneNumber, resolved)
            }
        } else {
            logSkipReason(phoneNumber, isInContacts, stirFailed)
        }
    }

    private fun buildDefaultResponse(): Call.Response =
        Call.Response.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun logSkipReason(phoneNumber: String, isInContacts: Boolean, stirFailed: Boolean) {
        val reason = buildString {
            if (!PrefsHelper.isTriggerNotInContacts && !PrefsHelper.isTriggerFailedStir) {
                append("no triggers enabled")
            } else {
                val parts = mutableListOf<String>()
                if (PrefsHelper.isTriggerNotInContacts && isInContacts) parts.add("in contacts")
                if (PrefsHelper.isTriggerFailedStir && !stirFailed) parts.add("STIR passed")
                append(parts.joinToString(", ").ifBlank { "conditions not met" })
            }
        }
        CallLogger.logSkipped(this, phoneNumber, reason)
    }

    private fun checkStirFailed(callDetails: Call.Details): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val verifiedStatusFailed = 2
        return callDetails.callerNumberVerificationStatus == verifiedStatusFailed
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
