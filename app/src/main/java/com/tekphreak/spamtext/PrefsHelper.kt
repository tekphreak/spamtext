package com.tekphreak.spamtext

import android.content.Context
import android.content.SharedPreferences

object PrefsHelper {
    private const val PREFS_NAME = "spamtext_prefs"
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    private const val KEY_SMS_MESSAGE = "sms_message"
    private const val KEY_TRIGGER_NOT_IN_CONTACTS = "trigger_not_in_contacts"
    private const val KEY_TRIGGER_FAILED_STIR = "trigger_failed_stir"

    const val DEFAULT_SMS_MESSAGE =
        "This number uses an automated anti-telemarketer filter. If you are a real person, please leave a voicemail."

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isServiceEnabled: Boolean
        get() = _prefs?.getBoolean(KEY_SERVICE_ENABLED, false) ?: false
        set(value) { _prefs?.edit()?.putBoolean(KEY_SERVICE_ENABLED, value)?.apply() }

    var smsMessage: String
        get() = _prefs?.getString(KEY_SMS_MESSAGE, DEFAULT_SMS_MESSAGE) ?: DEFAULT_SMS_MESSAGE
        set(value) { _prefs?.edit()?.putString(KEY_SMS_MESSAGE, value)?.apply() }

    var isTriggerNotInContacts: Boolean
        get() = _prefs?.getBoolean(KEY_TRIGGER_NOT_IN_CONTACTS, true) ?: true
        set(value) { _prefs?.edit()?.putBoolean(KEY_TRIGGER_NOT_IN_CONTACTS, value)?.apply() }

    var isTriggerFailedStir: Boolean
        get() = _prefs?.getBoolean(KEY_TRIGGER_FAILED_STIR, true) ?: true
        set(value) { _prefs?.edit()?.putBoolean(KEY_TRIGGER_FAILED_STIR, value)?.apply() }

    private var _prefs: SharedPreferences? = null

    fun init(context: Context) {
        _prefs = prefs(context)
    }
}
