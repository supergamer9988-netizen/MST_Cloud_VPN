package com.v2ray.ang.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.handler.V2RayServiceManager
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    val CONFIG_URL = "https://raw.githubusercontent.com/aiboboxx/v2rayfree/main/v2"
    var rawConfigData: String = ""
    lateinit var statusText: TextView
    lateinit var btnConnect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Setup Minimal Dark UI (No XML)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212")) // Dark Background
            setPadding(50, 50, 50, 50)
        }

        val title = TextView(this).apply {
            text = "MST CLOUD VPN"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }

        statusText = TextView(this).apply {
            text = "Initializing..."
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 100)
        }

        btnConnect = Button(this).apply {
            text = "CONNECT"
            textSize = 20f
            setBackgroundColor(Color.parseColor("#1E88E5")) // Blue
            setTextColor(Color.WHITE)
            setPadding(40, 40, 40, 40)
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(btnConnect)
        setContentView(layout)

        // 2. Fetch Config on Startup
        thread {
            try {
                runOnUiThread { statusText.text = "Fetching Config..." }
                val rawData = URL(CONFIG_URL).readText()
                rawConfigData = rawData
                runOnUiThread { 
                    statusText.text = "Ready to Connect"
                    statusText.setTextColor(Color.GREEN)
                }
            } catch (e: Exception) {
                runOnUiThread { 
                    statusText.text = "Fetch Error: ${e.message}" 
                    statusText.setTextColor(Color.RED)
                }
            }
        }

        // 3. Connect/Disconnect Logic
        btnConnect.setOnClickListener {
            if (V2RayServiceManager.isRunning()) {
                // Disconnect
                V2RayServiceManager.stopVService(this)
                updateUI(false)
            } else {
                // Connect
                if (rawConfigData.isNotBlank()) {
                    try {
                        statusText.text = "Connecting..."
                        
                        // Clear old configs
                        MmkvManager.removeAllServer()
                        
                        // Import new configs
                        AngConfigManager.importBatchConfig(rawConfigData, "", false)
                        
                        // Get list of all imported servers
                        val serverList = MmkvManager.decodeServerList()
                        
                        if (serverList.isNotEmpty()) {
                            // Randomly select one
                            val randomGuid = serverList.random()
                            
                            // Start Service with selected guid
                            V2RayServiceManager.startVService(this, randomGuid)
                            updateUI(true)
                        } else {
                            statusText.text = "No Valid Servers Found"
                        }
                    } catch (e: Exception) {
                        statusText.text = "Error: ${e.message}"
                        statusText.setTextColor(Color.RED)
                    }
                } else {
                    Toast.makeText(this, "Config not loaded yet", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Initial UI State Check
        updateUI(V2RayServiceManager.isRunning())
    }

    private fun updateUI(isConnected: Boolean) {
        runOnUiThread {
            if (isConnected) {
                btnConnect.text = "DISCONNECT"
                btnConnect.setBackgroundColor(Color.parseColor("#D32F2F")) // Red
                statusText.text = "CONNECTED"
                statusText.setTextColor(Color.GREEN)
            } else {
                btnConnect.text = "CONNECT"
                btnConnect.setBackgroundColor(Color.parseColor("#1E88E5")) // Blue
                statusText.text = "DISCONNECTED"
                statusText.setTextColor(Color.LTGRAY)
            }
        }
    }
}
