package com.virtualmax.smsforwarder

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var tvLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        tvLogs = findViewById(R.id.tvLogs)

        val etWebhook = findViewById<TextInputEditText>(R.id.etWebhookUrl)
        val etSenders = findViewById<TextInputEditText>(R.id.etSenders)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnTest = findViewById<MaterialButton>(R.id.btnTest)
        val switchEngine = findViewById<SwitchMaterial>(R.id.switchEngine)
        val tvEngineStatus = findViewById<TextView>(R.id.tvEngineStatus)
        val cardMaster = findViewById<MaterialCardView>(R.id.cardMaster)

        // Load Saved Settings
        etWebhook.setText(sharedPref.getString("WEBHOOK_URL", ""))
        etSenders.setText(sharedPref.getString("SENDERS", "3737, 8558"))
        val isActive = sharedPref.getBoolean("IS_ACTIVE", false)
        switchEngine.isChecked = isActive
        updateEngineUI(isActive, tvEngineStatus, cardMaster)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 100)
        }

        switchEngine.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("IS_ACTIVE", isChecked).apply()
            updateEngineUI(isChecked, tvEngineStatus, cardMaster)
            addLog(if (isChecked) "Engine Activated! Listening to SMS." else "Engine Deactivated.")
        }

        btnSave.setOnClickListener {
            val url = etWebhook.text.toString().trim()
            val senders = etSenders.text.toString().trim()
            
            if (url.startsWith("http")) {
                sharedPref.edit()
                    .putString("WEBHOOK_URL", url)
                    .putString("SENDERS", senders)
                    .apply()
                addLog("Configuration saved successfully.")
                Toast.makeText(this, "Config Saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid HTTP/HTTPS URL", Toast.LENGTH_SHORT).show()
            }
        }

        btnTest.setOnClickListener {
            val url = etWebhook.text.toString().trim()
            if (url.isNotEmpty()) {
                addLog("Testing connection to server...")
                testWebhookConnection(url)
            } else {
                addLog("Error: Webhook URL is empty.")
            }
        }
    }

    private fun updateEngineUI(isActive: Boolean, tvStatus: TextView, card: MaterialCardView) {
        if (isActive) {
            tvStatus.text = "Engine Online"
            tvStatus.setTextColor(Color.parseColor("#10B981")) 
            card.strokeColor = Color.parseColor("#10B981")
        } else {
            tvStatus.text = "Engine Offline"
            tvStatus.setTextColor(Color.parseColor("#888888")) 
            card.strokeColor = Color.parseColor("#2A2A3A")
        }
    }

    private fun addLog(message: String) {
        runOnUiThread {
            val currentLogs = tvLogs.text.toString()
            tvLogs.text = "> $message\n$currentLogs"
        }
    }

    private fun testWebhookConnection(webhookUrl: String) {
        thread {
            try {
                val url = URL(webhookUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject()
                json.put("sender", "TEST_SYSTEM")
                json.put("message", "Rs. 500 received. Trx ID: 123456789")

                OutputStreamWriter(connection.outputStream).use { it.write(json.toString()) }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    addLog("✅ Success! Server responded with code: $responseCode")
                } else {
                    addLog("⚠️ Error! Server code: $responseCode")
                }
            } catch (e: Exception) {
                addLog("❌ Connection Failed: ${e.localizedMessage}")
            }
        }
    }
}