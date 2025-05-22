package com.bluebridge.bluebridgeapp.network

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.bluebridge.bluebridgeapp.R


class SmsApi(private val context: Context) {
    private val TAG = "SmsApi"
    private val smsManager = SmsManager.getDefault()
    private val serverNumber: String = context.getString(R.string.sms_server_number)

    fun sendSms(command: String, location: Location? = null) {
        try {
            val message = buildMessage(command, location)
            smsManager.sendTextMessage(serverNumber, null, message, null, null)
            Log.d(TAG, "SMS sent successfully: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            throw e
        }
    }

    private fun buildMessage(command: String, location: Location?): String {
        return when (command) {
            "GNW" -> "GNW${location?.let { " ${it.latitude},${it.longitude}" } ?: ""}"
            "SH" -> "SH${location?.let { " ${it.latitude},${it.longitude}" } ?: ""}"
            else -> throw IllegalArgumentException("Unknown command: $command")
        }
    }
}

data class Location(
    val latitude: Double,
    val longitude: Double
)
