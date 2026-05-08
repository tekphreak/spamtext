package com.tekphreak.spamtext

import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.content.ContextCompat

class SpamCallScreeningService : CallScreeningService() {

        override fun onCreate() {
                    super.onCreate()
                            PrefsHelper.init(this)
        }

            override fun onScreenCall(callDetails: Call.Details) {
                        // Belt-and-suspenders: re-init prefs in case the service was reused
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

                                if (!hasRequiredPermissions()) {
                                                CallLogger.logSkipped(this, phoneNumber, "missing permissions")
                                                            respondToCall(callDetails, response)
                                                                        return
                                }

                                        val stirFailed = checkStirFailed(callDetails)
                                                val isInContacts = ContactChecker.isInContacts(this, phoneNumber)
                                                        val shouldSend = (PrefsHelper.isTriggerNotInContacts && !isInContacts) ||
                                (PrefsHelper.isTriggerFailedStir && stirFailed)

                                        // Respond first, then do work synchronously — no coroutine needed since
                                                // SmsManager.sendTextMessage dispatches instantly to the telephony layer.
                                                        // (A coroutine scope cancelled in onDestroy could kill the send before it runs.)
                                                                respondToCall(callDetails, response)

                                                                        if (shouldSend) {
                                                                                        val resolved = SmsHelper.resolveTemplates(PrefsHelper.smsMessage, phoneNumber)
                                                                                                    SmsHelper.sendSms(this, phoneNumber, resolved)
                                                                                                                CallLogger.logSmsSent(this, phoneNumber, resolved)
                                                                        } else {
                                                                                        logSkipReason(phoneNumber, isInContacts, stirFailed)
                                                                        }
            }

                private fun hasRequiredPermissions(): Boolean {
                            return ContextCompat.checkSelfPermission(
                                            this, android.Manifest.permission.READ_CONTACTS
                                        ) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(
                                            this, android.Manifest.permission.SEND_SMS
                                        ) == PackageManager.PERMISSION_GRANTED
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
                                    // VERIFIED_STATUS_FAILED = 2 (API 30+, resolved at runtime to avoid compile-time issue)
                                    val verifiedStatusFailed = 2
                                    return callDetails.callerNumberVerificationStatus == verifiedStatusFailed
                        }
}
