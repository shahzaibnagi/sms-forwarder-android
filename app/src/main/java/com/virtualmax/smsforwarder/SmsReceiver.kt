package com.virtualmax.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import androidx.work.*

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return
        
        val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isActive = sharedPref.getBoolean("IS_ACTIVE", false)
        if (!isActive) return // Engine band hai toh kuch nahi karna

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        
        val allowedSenders = sharedPref.getString("SENDERS", "3737, 8558") ?: ""
        val senderList = allowedSenders.split(",").map { it.trim() }

        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
            val sender = sms.displayOriginatingAddress ?: ""
            val body = sms.messageBody ?: ""

            val isAllowed = senderList.any { sender.contains(it, ignoreCase = true) }

            if (isAllowed) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // Sirf internet hone par bhejega
                    .build()

                val inputData = workDataOf("SENDER" to sender, "BODY" to body)

                val workRequest = OneTimeWorkRequestBuilder<SmsWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}