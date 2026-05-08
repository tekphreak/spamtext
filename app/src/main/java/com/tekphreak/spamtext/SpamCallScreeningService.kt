package com.tekphreak.spamtext

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService

class SpamCallScreeningService : CallScreeningService() {

    override fun onCreate() {
        super.onCreate()
        PrefsHelper.init(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        // Belt-and-suspenders: re-init in case the service instance was reused
        PrefsHelper.init(this)

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

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

        val stirFailed = checkStirFailed(callDetails)
        val isInContacts = ContactChecker.isInContacts(this, phoneNumber)
        val notInContacts = PrefsHelper.isTriggerNotInContacts && !isInContacts
        val triggerOnStir = PrefsHelper.isTriggerFailedStir && stirFailed
        val shouldSend = notInContacts || triggerOnStir

        // Respond to the call first, then do work — no coroutine needed since
        // SmsManager.sendTextMessage dispatches instantly to the telephony layer
        respondToCall(callDetails, response)

        if (shouldSend) {
            val resolved = SmsHelper.resolveTemplates(PrefsHelper.smsMessage, phoneNumber)
            SmsHelper.sendSms(this, phoneNumber, resolved)
            CallLogger.logSmsSent(this, phoneNumber, resolved)
        } else {
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
    }

    private fun checkStirFailed(callDetails: Call.Details): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        // VERIFIED_STATUS_FAILED = 2 (API 30+, resolved at runtime to avoid compile-time issue)
        val verifiedStatusFailed = 2
        return callDetails.callerNumberVerificationStatus == verifiedStatusFailed
    }
}
