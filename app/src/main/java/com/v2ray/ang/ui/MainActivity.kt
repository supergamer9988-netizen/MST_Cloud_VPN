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
import com.v2ray.ang.AppConfig
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // 🔴 LINK CONFIG: ลิงก์รวม vmess ฟรี (เปลี่ยนได้ตามต้องการ)
    val CONFIG_URL = "https://raw.githubusercontent.com/aiboboxx/v2rayfree/main/v2"

    var serverList = mutableListOf<String>()
    lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. UI SETUP (สร้างหน้าจอด้วยโค้ด ไม่ใช้ XML) ---
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212")) // Dark Mode Background
        }

        val title = TextView(this).apply {
            text = "MST CLOUD VPN"
            textSize = 24f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        statusText = TextView(this).apply {
            text = "Initializing Cloud Config..."
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        
        val btnConnect = Button(this).apply {
            text = "LOADING..."
            textSize = 18f
            setPadding(50, 40, 50, 40)
            setBackgroundColor(Color.DKGRAY)
            setTextColor(Color.WHITE)
            isEnabled = false 
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(btnConnect)
        setContentView(layout)

        // --- 2. LOGIC: FETCH CONFIG (ดึงข้อมูลทันทีที่เปิดแอป) ---
        thread {
            try {
                val rawData = URL(CONFIG_URL).readText()
                // กรองเอาเฉพาะบรรทัดที่เป็น vmess://
                serverList = rawData.lines()
                    .filter { it.contains("vmess://") }
                    .filter { it.isNotBlank() }
                    .toMutableList()

                runOnUiThread {
                    if (serverList.isNotEmpty()) {
                        statusText.text = "✅ Ready! Found ${serverList.size} servers."
                        btnConnect.text = "TAP TO CONNECT"
                        btnConnect.isEnabled = true
                        btnConnect.setBackgroundColor(Color.RED)
                    } else {
                        statusText.text = "❌ Error: No servers found in URL."
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "⚠️ Network Error. Please check internet."
                }
            }
        }

        // --- 3. LOGIC: CONNECT/DISCONNECT (สุ่มเซิร์ฟเวอร์) ---
        btnConnect.setOnClickListener {
            if (V2RayServiceManager.v2rayPoint.isRunning) {
                // STOP
                V2RayServiceManager.stopV2Ray(this)
                btnConnect.text = "TAP TO CONNECT"
                btnConnect.setBackgroundColor(Color.RED)
                statusText.text = "🔴 Disconnected"
            } else {
                // START
                if (serverList.isNotEmpty()) {
                    try {
                        AngConfigManager.deleteServer(AppConfig.ANG_PACKAGE) // เคลียร์ของเก่า
                        val randomConfig = serverList.random() // สุ่มตัวใหม่
                        val config = AngConfigManager.importShare(randomConfig)
                        
                        if (config != null) {
                            V2RayServiceManager.startV2Ray(this, config, null, null)
                            btnConnect.text = "🟢 CONNECTED"
                            btnConnect.setBackgroundColor(Color.GREEN)
                            statusText.text = "Active: ${config.remarks}"
                        }
                    } catch (e: Exception) {
                        statusText.text = "Config Error. Try again."
                    }
                }
            }
        }
    }
}
