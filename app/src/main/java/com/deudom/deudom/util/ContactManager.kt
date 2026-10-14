package com.deudom.deudom.util

import android.content.Context
import android.provider.ContactsContract

data class DeviceContact(
    val name: String,
    val phone: String
)

object ContactManager {
    fun getDeviceContacts(context: Context): List<DeviceContact> {
        val contacts = mutableListOf<DeviceContact>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex).filter { char -> char.isDigit() }.takeLast(10)
                if (number.length == 10) {
                    contacts.add(DeviceContact(name, number))
                }
            }
        }
        return contacts.distinctBy { it.phone }
    }
}
