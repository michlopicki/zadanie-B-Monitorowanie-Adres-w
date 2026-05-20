package com.example.bmonitoringurl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.net.URL
import java.security.MessageDigest
import androidx.core.net.toUri

class UrlMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // częstotliwość odświeżania
    private val checkIntervalMs = 5_000L // 5 sekund

    companion object {
        private const val FOREGROUND_CHANNEL_ID = "monitor_service_channel"
        private const val ALERT_CHANNEL_ID = "url_alert_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
        startMonitoringLoop()
        // w razie ubicia aplikacji zrestartuj jak zwolnią się zasoby
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                checkUrls()
                delay(checkIntervalMs)
            }
        }
    }

    private suspend fun checkUrls() {
        val db = AppDatabase.getDatabase(applicationContext)
        val urls = db.urlDao().getAllUrls()

        // pętla po adresach w bazie danych sprawdzająca czy hash strony się zmienił
        for (monitoredUrl in urls) {
            try {
                val content = URL(monitoredUrl.url).readText()
                val currentHash = hashString(content)

                if (monitoredUrl.lastContentHash != null && monitoredUrl.lastContentHash != currentHash) {
                    showAlertNotification(monitoredUrl.url)
                }

                monitoredUrl.lastContentHash = currentHash
                db.urlDao().update(monitoredUrl)

            } catch (e: Exception) {
                e.printStackTrace()
                // ignoruje bo i tak spróbuje za interwał
                // TODO: co jak serwer przestanie odpowiadać na dłużej? Alert?
            }
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }


    // POWIADOMIENIA
    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // stałe powiadomienie informujące o działaniu w tle
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Monitorowanie URL włączone")
            .setContentText("Aplikacja sprawdza strony w tle")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showAlertNotification(url: String) {
        // powiadomienie o zmianie na stronie
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        val pendingIntent = PendingIntent.getActivity(
            this, url.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Zmiana na stronie!")
            .setContentText("Zawartość $url uległa zmianie.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(url.hashCode(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // Kanał dla ciągłego powiadomienia
            val fgChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Śledzenie URL działa w tle",
                NotificationManager.IMPORTANCE_LOW
            )

            // Kanał dla alertów
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alerty o zmianach",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(fgChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }
}