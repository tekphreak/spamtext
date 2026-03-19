package com.tekphreak.spamtext

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactChecker {
    fun isInContacts(context: Context, phoneNumber: String): Boolean {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null
            )
            cursor?.use { it.count > 0 } ?: false
        } catch (e: SecurityException) {
            false
        }
    }
}
