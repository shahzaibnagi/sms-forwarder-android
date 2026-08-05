package com.virtualmax.smsforwarder

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SmsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val sender = inputData.getString("SENDER") ?: return Result.failure()
        val body = inputData.getString("BODY") ?: return Result.failure()
        
        val sharedPref = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val webhookUrl = sharedPref.getString("WEBHOOK_URL", "") ?: return Result.failure()

        return try {
            val url = URL(webhookUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val json = JSONObject()
            json.put("sender", sender)
            json.put("message", body)

            OutputStreamWriter(connection.outputStream).use { it.write(json.toString()) }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Log.d("SmsWorker", "✅ SMS Forwarded Successfully: $responseCode")
                Result.success()
            } else {
                Log.e("SmsWorker", "⚠️ Server Error: $responseCode. WorkManager will retry.")
                Result.retry() // Server down hai, baad mein auto-retry karega
            }
        } catch (e: Exception) {
            Log.e("SmsWorker", "❌ Network Failed: ${e.message}. WorkManager will retry.")
            Result.retry() // Internet drop ho gaya, auto-retry karega
        }
    }
}