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
        
        // Target Apps: JazzCash, Easypaisa, NayaPay, SadaPay
        val targetApps = listOf("com.msys.jazzcash", "pk.com.telenor.phoenix", "com.nayapay.app", "pk.sadapay.app")
        
        if (targetApps.contains(packageName)) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getString("android.text") ?: ""
            
            Log.d("VMAX_NOTIF", "Payment App: $packageName | Title: $title | Text: $text")

            // App ka naam identify karein
            val appName = when(packageName) {
                "com.msys.jazzcash" -> "JazzCash"
                "pk.com.telenor.phoenix" -> "Easypaisa"
                "com.nayapay.app" -> "NayaPay"
                "pk.sadapay.app" -> "SadaPay"
                else -> "Payment App"
            }

            // SharedPreferences se Webhook URL read karein
            val prefs = getSharedPreferences("VMAX_PREFS", MODE_PRIVATE)
            val savedUrl = prefs.getString("WEBHOOK_URL", "")
            
            // NOTE: Agar app ke UI mein URL save karne ka option properly set nahi hai 
            // toh apni testing ke liye apna Pinggy URL yahan neeche paste kar dein:
            val webhookUrl = if (savedUrl.isNullOrEmpty()) "YOUR_PINGGY_URL_HERE/api/sms-webhook" else savedUrl

            if (webhookUrl.isNotEmpty() && !webhookUrl.contains("YOUR_PINGGY_URL_HERE")) {
                thread {
                    try {
                        val url = URL(webhookUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true

                        // JSON Data Payload (Fake Server ki formatting ke mutabiq)
                        val jsonString = "{\"sender\": \"$appName (App)\", \"message\": \"$title - $text\"}"

                        OutputStreamWriter(conn.outputStream).use { os ->
                            os.write(jsonString)
                        }

                        Log.d("VMAX_NOTIF", "Notification forwarded! Response Code: ${conn.responseCode}")
                    } catch (e: Exception) {
                        Log.e("VMAX_NOTIF", "Forwarding failed: ${e.message}")
                    }
                }
            } else {
                Log.e("VMAX_NOTIF", "Webhook URL khali hai ya Pinggy URL update nahi kiya!")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Hamein notification remove hone se farq nahi parta, code ignore karega
    }
}
