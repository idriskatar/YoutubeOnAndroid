package com.youtubeonauto.myobd

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var selectButton: Button
    private var selectedDevice: BluetoothDevice? = null
    private val handler = Handler(Looper.getMainLooper())
    private val permissionRequestCode = 71

    private val refreshStatus = object : Runnable {
        override fun run() {
            if (!::statusText.isInitialized) return
            statusText.text = "${ObdData.status}\n${ObdData.deviceName}\n\nYerel panel: http://127.0.0.1:8765/"
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        handler.post(refreshStatus)
        ensureBluetoothPermission()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshStatus)
        super.onDestroy()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
            setBackgroundColor(0xFF111820.toInt())
        }
        val title = TextView(this).apply {
            text = "MyOBD"
            textSize = 30f
            setTextColor(0xFFE7F4F2.toInt())
            gravity = Gravity.CENTER
        }
        val intro = TextView(this).apply {
            text = "ELM327 bağlantısını başlat ve araç verilerini yerel web panelinde görüntüle."
            textSize = 16f
            setTextColor(0xFFB5C5C8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(24))
        }
        deviceText = TextView(this).apply {
            text = "Henüz ELM327 seçilmedi"
            textSize = 16f
            setTextColor(0xFF66E0B8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(18))
        }
        selectButton = makeButton("Eşleştirilmiş cihaz seç") { showBondedDevices() }
        val startButton = makeButton("Bağlan ve servisi başlat") { startObdService() }
        val stopButton = makeButton("Bağlantıyı ve servisi durdur") { stopService(Intent(this, ObdWebService::class.java)) }
        val openButton = makeButton("Youtube On Auto'da paneli aç") {
            val open = Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8765/"))
                .setClassName("com.kododake.aabrowser", "com.kododake.aabrowser.MainActivity")
            try {
                startActivity(open)
            } catch (_: Exception) {
                Toast.makeText(this, "Önce Youtube On Auto uygulamasını yükleyin.", Toast.LENGTH_LONG).show()
            }
        }
        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFE7F4F2.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, 0)
        }
        listOf(title, intro, deviceText, selectButton, startButton, stopButton, openButton, statusText).forEach { view ->
            root.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                if (view is Button) setMargins(0, dp(7), 0, dp(7))
            })
        }
        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun ensureBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = mutableListOf(Manifest.permission.BLUETOOTH_CONNECT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions += Manifest.permission.POST_NOTIFICATIONS
            }
            requestPermissions(permissions.toTypedArray(), permissionRequestCode)
            return false
        }
        selectButton.isEnabled = true
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            val bluetoothIndex = permissions.indexOf(Manifest.permission.BLUETOOTH_CONNECT)
            val granted = bluetoothIndex >= 0 && grantResults.getOrNull(bluetoothIndex) == PackageManager.PERMISSION_GRANTED
            selectButton.isEnabled = granted
            if (!granted) Toast.makeText(this, "Eşleştirilmiş OBD cihazlarına erişim izni gerekli.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showBondedDevices() {
        if (!ensureBluetoothPermission()) return
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Önce Bluetooth'u açın.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            return
        }
        val devices = try {
            adapter.bondedDevices.sortedBy { it.name ?: it.address }
        } catch (_: SecurityException) {
            emptyList()
        }
        if (devices.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Eşleştirilmiş cihaz yok")
                .setMessage("ELM327'yi telefonun Bluetooth ayarlarından eşleştirin, sonra burada seçin.")
                .setPositiveButton("Bluetooth ayarları") { _, _ -> startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                .setNegativeButton("Kapat", null)
                .show()
            return
        }
        val labels = devices.map { "${it.name ?: "ELM327"}\n${it.address}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("ELM327 cihazını seç")
            .setItems(labels) { _, index ->
                selectedDevice = devices[index]
                deviceText.text = "Seçilen cihaz\n${devices[index].name ?: "ELM327"}"
            }
            .show()
    }

    private fun startObdService() {
        if (!ensureBluetoothPermission()) return
        val device = selectedDevice
        if (device == null) {
            showBondedDevices()
            return
        }
        val intent = Intent(this, ObdWebService::class.java)
            .setAction(ObdWebService.ACTION_START)
            .putExtra(ObdWebService.EXTRA_DEVICE_ADDRESS, device.address)
        startForegroundService(intent)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
