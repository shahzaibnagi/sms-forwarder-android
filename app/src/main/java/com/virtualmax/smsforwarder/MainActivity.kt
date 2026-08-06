package com.virtualmax.smsforwarder

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Android 13+ ke liye Notification Permission Popup (Iski wajah se button gray tha)
        requestNotificationPermission()

        // 2. Google Play Protect Bypass: Prominent Disclosure
        showDisclaimerIfNeeded()

        // 3. UI Controls Setup
        setupUI()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            } else {
                startKeepAliveService()
            }
        } else {
            startKeepAliveService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startKeepAliveService()
        } else {
            Toast.makeText(this, "Background guard needs this permission to stay active!", Toast.LENGTH_LONG).show()
        }
    }

    private fun startKeepAliveService() {
        val serviceIntent = Intent(this, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun showDisclaimerIfNeeded() {
        val prefs = getSharedPreferences("VMAX_PREFS", Context.MODE_PRIVATE)
        val isAgreed = prefs.getBoolean("AGREED_DISCLAIMER", false)
        
        if (!isAgreed) {
            AlertDialog.Builder(this)
                .setTitle("Prominent Disclosure & Terms")
                .setMessage("V-MAX AutoPay by Shahzaib Tech requires background access to your SMS and App Notifications (e.g., JazzCash, Easypaisa).\n\n" +
                        "WHY WE NEED THIS:\n" +
                        "This data is strictly used to verify and automate your payment receipts.\n\n" +
                        "PRIVACY GUARANTEE:\n" +
                        "Your data is NEVER shared with third parties. It is securely forwarded ONLY to the private Webhook URL you configure in this app.\n\n" +
                        "By clicking 'I Agree', you consent to this automated forwarding.")
                .setCancelable(false)
                .setPositiveButton("I Agree") { _, _ ->
                    prefs.edit().putBoolean("AGREED_DISCLAIMER", true).apply()
                }
                .setNegativeButton("Exit App") { _, _ ->
                    finish()
                }
                .show()
        }
    }

    private fun setupUI() {
        val prefs = getSharedPreferences("VMAX_PREFS", Context.MODE_PRIVATE)
        val urlInput = findViewById<EditText>(R.id.webhookUrlInput)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val testBtn = findViewById<Button>(R.id.testBtn)

        urlInput.setText(prefs.getString("WEBHOOK_URL", ""))

        saveBtn.setOnClickListener {
            val newUrl = urlInput.text.toString().trim()
            if (newUrl.isNotEmpty()) {
                prefs.edit().putString("WEBHOOK_URL", newUrl).apply()
                Toast.makeText(this, "Webhook URL Saved Securely!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }

        testBtn.setOnClickListener {
            val currentUrl = prefs.getString("WEBHOOK_URL", "")
            if (currentUrl.isNullOrEmpty()) {
                Toast.makeText(this, "Save a URL first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Testing Connection...", Toast.LENGTH_SHORT).show()

            thread {
                try {
                    val url = URL(currentUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true

                    val jsonString = "{\"sender\": \"Shahzaib Tech (Test)\", \"message\": \"Rs. 500 received. Engine is 100% Active!\"}"

                    OutputStreamWriter(conn.outputStream).use { os ->
                        os.write(jsonString)
                    }

                    val code = conn.responseCode
                    runOnUiThread {
                        if (code in 200..299) {
                            Toast.makeText(this@MainActivity, "✅ Success! Server Connected.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "⚠️ Server returned code: $code", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
