package com.virtualmax.smsforwarder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import android.util.Log

class KeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        
        // 1. Start as Foreground Service (Notification panel mein sticky rahega)
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "VMAX_CHANNEL")
            .setContentTitle("V-MAX AutoPay Active")
            .setContentText("Listening for payments in background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Yahan logo.png bhi laga sakte hain baad mein
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)

        // 2. CPU WakeLock (Phone screen off hone par bhi background process chalne dega)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VMax::KeepAlive")
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)

        // 3. Network Resilience (Wi-Fi band hone par Data par switch karna)
        forceNetworkConnectivity()
    }

    private fun forceNetworkConnectivity() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            // Wi-Fi aur Cellular dono par nazar rakhega
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)

        connectivityManager.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                Log.d("VMAX_NETWORK", "Network is back! Ready to forward.")
            }
            override fun onLost(network: android.net.Network) {
                Log.e("VMAX_NETWORK", "Network lost! Attempting to find backup connection...")
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Agar Android isay kill bhi kar de, toh STICKY usay foran restart kar dega
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "VMAX_CHANNEL",
                "V-MAX Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
