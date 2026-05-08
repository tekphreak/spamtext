package com.tekphreak.spamtext

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class PhoneCallReceiver : BroadcastReceiver() {

          override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
                        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
                        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

                        PrefsHelper.init(context)

                                // If neither trigger is enabled there is nothing to do
                                        val anyTriggerEnabled = PrefsHelper.isTriggerNotInContacts || PrefsHelper.isTriggerFailedStir
                        if (!anyTriggerEnabled) {
                                          CallLogger.logSkipped(context, "unknown", "no triggers enabled")
                                                      return
                        }

                                @Suppress("DEPRECATION")
                                        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                                                if (phoneNumber.isNullOrBlank()) {
                                                                  CallLogger.logSkipped(context, "hidden/unknown", "no number in broadcast")
                                                                              return
                                                }

                                                        val isInContacts = ContactChecker.isInContacts(context, phoneNumber)
                                                                val shouldSend = PrefsHelper.isTriggerNotInContacts && !isInContacts

                        if (shouldSend) {
                                          val resolved = SmsHelper.resolveTemplates(PrefsHelper.smsMessage, phoneNumber)
                                                      SmsHelper.sendSms(context, phoneNumber, resolved)
                                                                  CallLogger.logSmsSent(context, phoneNumber, resolved)
                        } else {
                                          val reason = when {
                                                                !PrefsHelper.isTriggerNotInContacts -> "trigger disabled"
                                                                isInContacts -> "in contacts"
                                                                else -> "conditions not met"
                                          }
                                                      CallLogger.logSkipped(context, phoneNumber, reason)
                        }
          }
}
