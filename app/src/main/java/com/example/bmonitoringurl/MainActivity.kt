package com.example.bmonitoringurl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    // test:
    // http://10.0.2.2:8000

    private lateinit var urlAdapter: UrlAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(applicationContext)

        val etUrlInput = findViewById<EditText>(R.id.etUrlInput)
        val btnAddUrl = findViewById<Button>(R.id.btnAddUrl)
        val btnStartService = findViewById<Button>(R.id.btnStartService)
        val btnStopService = findViewById<Button>(R.id.btnStopService)
        val rvUrls = findViewById<RecyclerView>(R.id.rvUrls)

        // notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Konfiguracja RecyclerView i Adaptera
        urlAdapter = UrlAdapter(mutableListOf()) { urlToDelete ->
            deleteUrl(urlToDelete)
        }
        rvUrls.layoutManager = LinearLayoutManager(this)
        rvUrls.adapter = urlAdapter

        CoroutineScope(Dispatchers.IO).launch {
            val urls = db.urlDao().getAllUrls()
            withContext(Dispatchers.Main) {
                urlAdapter.updateData(urls)
            }
        }

        // Dodawanie URL
        btnAddUrl.setOnClickListener {
            val inputUrl = etUrlInput.text.toString().trim()

            if (!URLUtil.isValidUrl(inputUrl)) {
                Toast.makeText(this, "Nieprawidłowy format.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnAddUrl.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                val isReachable = checkUrlExists(inputUrl)

                withContext(Dispatchers.Main) {
                    btnAddUrl.isEnabled = true
                    if (isReachable) {
                        saveUrlToDatabase(inputUrl)
                        etUrlInput.text.clear()
                    } else {
                        Toast.makeText(this@MainActivity, "Serwer nie odpowiada.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // uruchamianie monitorownia
        btnStartService.setOnClickListener {
            val serviceIntent = Intent(this, UrlMonitorService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, "Usługa uruchomiona", Toast.LENGTH_SHORT).show()
        }

        // zatrzymanie monitorowania
        btnStopService.setOnClickListener {
            val serviceIntent = Intent(this, UrlMonitorService::class.java)
            stopService(serviceIntent)
            Toast.makeText(this, "Usługa zatrzymana", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun saveUrlToDatabase(url: String) {
        db.urlDao().insert(MonitoredUrl(url = url))
        // przeładowanie całego recycle View
        val updatedUrls = db.urlDao().getAllUrls()
        withContext(Dispatchers.Main) {
            urlAdapter.updateData(updatedUrls)
            Toast.makeText(this@MainActivity, "Dodano!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteUrl(url: MonitoredUrl) {
        CoroutineScope(Dispatchers.IO).launch {
            db.urlDao().delete(url)
            val updatedUrls = db.urlDao().getAllUrls()
            withContext(Dispatchers.Main) {
                urlAdapter.updateData(updatedUrls)
            }
        }
    }

    private fun checkUrlExists(urlString: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.disconnect()
        }
    }
}