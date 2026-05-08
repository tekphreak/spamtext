package com.tekphreak.spamtext

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpamCallScreeningService : CallScreeningService() {

    override fun onCreate() {
    super.onCreate()
    PrefsHelper.init(this)
}

    private val scope = CoroutineScope(SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        if (!PrefsHelper.isServiceEnabled) {
            respondToCall(callDetails, response)
            return
        }

        val phoneNumber = callDetails.handle?.schemeSpecificPart
        if (phoneNumber.isNullOrBlank()) {
            respondToCall(callDetails, response)
            return
        }

        val stirFailed = checkStirFailed(callDetails)
        val notInContacts = PrefsHelper.isTriggerNotInContacts &&
                !ContactChecker.isInContacts(this, phoneNumber)
        val triggerOnStir = PrefsHelper.isTriggerFailedStir && stirFailed
        val shouldSend = notInContacts || triggerOnStir

        respondToCall(callDetails, response)

        if (shouldSend) {
            val resolved = SmsHelper.resolveTemplates(PrefsHelper.smsMessage, phoneNumber)
            scope.launch {
                SmsHelper.sendSms(this@SpamCallScreeningService, phoneNumber, resolved)
                CallLogger.logSmsSent(this@SpamCallScreeningService, phoneNumber, resolved)
            }
        } else {
            val reason = buildString {
                if (!PrefsHelper.isTriggerNotInContacts && !PrefsHelper.isTriggerFailedStir) append("no triggers enabled")
                else if (!notInContacts && !triggerOnStir) append("contact found / STIR passed")
            }
            CallLogger.logSkipped(this, phoneNumber, reason.ifBlank { "conditions not met" })
        }
    }

    private fun checkStirFailed(callDetails: Call.Details): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        // VERIFIED_STATUS_FAILED = 2 (API 30+, resolved at runtime to avoid compile-time issue)
        val verifiedStatusFailed = 2
        return callDetails.callerNumberVerificationStatus == verifiedStatusFailed
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
