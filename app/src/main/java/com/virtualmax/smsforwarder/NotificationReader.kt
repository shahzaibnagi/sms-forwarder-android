package com.virtualmax.smsforwarder

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import kotlin.concurrent.thread

class NotificationReader : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        sendToServer("System", "V-MAX Universal Engine Connected! Scanning ALL notifications for text...")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val extractedData = java.lang.StringBuilder()

        // 1. Har kisim ka text extract karo
        val standardKeys = listOf("android.title", "android.text", "android.bigText", "android.subText", "android.summaryText")
        for (key in standardKeys) {
            val value = extras.getCharSequence(key)?.toString()
            if (!value.isNullOrEmpty() && !extractedData.contains(value)) {
                extractedData.append(value).append(" | ")
            }
        }

        val ticker = sbn.notification.tickerText?.toString()
        if (!ticker.isNullOrEmpty() && !extractedData.contains(ticker)) {
            extractedData.append("Ticker: ").append(ticker).append(" | ")
        }

        for (key in extras.keySet()) {
            val value = extras.get(key)
            if (value is CharSequence || value is String) {
                val strVal = value.toString()
                if (strVal.length > 3 && !extractedData.contains(strVal)) {
                    extractedData.append(strVal).append(" | ")
                }
            }
        }

        val finalMessage = extractedData.toString().trim().removeSuffix("|").trim()
        
        // 2. Chotay huroof (lowercase) mein convert karo taake case-sensitive ka masla na aye
        val checkText = finalMessage.lowercase()

        // 🚨 MASTER FILTER: Ab hum package name nahi check kar rahe. Sirf text par focus hai!
        if (checkText.contains("in your jazzcash mobile account") || 
            (checkText.contains("jazzcash") && checkText.contains("received")) ||
            (checkText.contains("easypaisa") && checkText.contains("received")) ||
            (checkText.contains("nayapay") && checkText.contains("received")) ||
            (checkText.contains("sadapay") && checkText.contains("received"))) {
            
            val appName = when {
                checkText.contains("jazzcash") -> "JazzCash"
                checkText.contains("easypaisa") -> "Easypaisa"
                checkText.contains("nayapay") -> "NayaPay"
                checkText.contains("sadapay") -> "SadaPay"
                else -> "Payment App"
            }

            sendToServer(appName, finalMessage)
        }
    }

    private fun sendToServer(sender: String, message: String) {
        val prefs = getSharedPreferences("VMAX_PREFS", MODE_PRIVATE)
        val webhookUrl = prefs.getString("WEBHOOK_URL", "")
        
        val dbHelper = LogDbHelper(applicationContext)
        val safeMessage = message.replace("\"", "'").replace("\n", " ")
        dbHelper.addLog(sender, safeMessage)

        if (!webhookUrl.isNullOrEmpty()) {
            thread {
                try {
                    val url = URL(webhookUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true

                    val jsonString = "{\"sender\": \"$sender (App)\", \"message\": \"$safeMessage\"}"

                    OutputStreamWriter(conn.outputStream).use { os ->
                        os.write(jsonString)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
