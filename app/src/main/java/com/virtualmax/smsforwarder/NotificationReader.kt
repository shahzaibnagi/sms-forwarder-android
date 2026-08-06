package com.virtualmax.smsforwarder

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import kotlin.concurrent.thread

class NotificationReader : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // 🚨 CORRECTED PLAY STORE PACKAGE NAMES
        val targetApps = listOf(
            "com.techlogix.mobilinkcustomer", // JazzCash (Real Name)
            "pk.com.telenor.phoenix",         // Easypaisa
            "com.nayapay.app",                // NayaPay
            "com.sadapay.app"                 // SadaPay (Corrected)
        )
        
        if (targetApps.contains(packageName)) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getString("android.text") ?: ""
            
            Log.d("VMAX_NOTIF", "Payment App: $packageName | Title: $title | Text: $text")

            val appName = when(packageName) {
                "com.techlogix.mobilinkcustomer" -> "JazzCash"
                "pk.com.telenor.phoenix" -> "Easypaisa"
                "com.nayapay.app" -> "NayaPay"
                "com.sadapay.app" -> "SadaPay"
                else -> "Payment App"
            }

            val prefs = getSharedPreferences("VMAX_PREFS", MODE_PRIVATE)
            val webhookUrl = prefs.getString("WEBHOOK_URL", "")

            if (!webhookUrl.isNullOrEmpty()) {
                thread {
                    try {
                        val url = URL(webhookUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true

                        val jsonString = "{\"sender\": \"$appName (App)\", \"message\": \"$title - $text\"}"

                        OutputStreamWriter(conn.outputStream).use { os ->
                            os.write(jsonString)
                        }

                        Log.d("VMAX_NOTIF", "Notification forwarded! Response Code: ${conn.responseCode}")
                    } catch (e: Exception) {
                        Log.e("VMAX_NOTIF", "Forwarding failed: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
