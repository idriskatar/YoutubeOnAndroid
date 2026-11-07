package com.youtubeonauto.myobd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

class ObdWebService : Service() {
    @Volatile private var running = false
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var bluetoothSocket: BluetoothSocket? = null
    private var serverThread: Thread? = null
    private var obdThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopWorkers()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val address = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS)
        if (address.isNullOrBlank()) {
            ObdData.reset("ELM327 seçilmedi")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (running) return START_NOT_STICKY

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "MyOBD bağlantısı", NotificationManager.IMPORTANCE_LOW)
            )
        }
        startForeground(
            NOTIFICATION_ID,
            buildNotification("ELM327'ye bağlanıyor"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
        running = true
        ObdData.deviceName = address
        ObdData.reset("ELM327'ye bağlanıyor")
        startLocalServer()
        startObdLoop(address)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopWorkers()
        super.onDestroy()
    }

    private fun buildNotification(message: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.myobd_icon)
            .setContentTitle("MyOBD")
            .setContentText(message)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    private fun startLocalServer() {
        serverThread = Thread({
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress(InetAddress.getByName(LOOPBACK), PORT), 12)
                serverSocket = server
                while (running) {
                    val client = server.accept()
                    Thread({ serve(client) }, "MyOBD-http-client").apply { isDaemon = true; start() }
                }
            } catch (error: Exception) {
                if (running) ObdData.status = "Yerel web servisi açılamadı: ${error.message ?: "port kullanılıyor"}"
            }
        }, "MyOBD-http-server").apply { isDaemon = true; start() }
    }

    private fun serve(client: Socket) {
        client.use { connection ->
            try {
                val reader = BufferedReader(InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))
                val requestLine = reader.readLine().orEmpty()
                while (!reader.readLine().isNullOrEmpty()) { /* consume request headers */ }
                val path = requestLine.split(' ').getOrNull(1)?.substringBefore('?').orEmpty()
                when (path) {
                    "/", "/index.html" -> respond(connection, 200, "text/html; charset=utf-8", assetsHtml())
                    "/api/state", "/api/health" -> respond(connection, 200, "application/json; charset=utf-8", ObdData.asJson().toString())
                    "/favicon.ico" -> respond(connection, 204, "image/x-icon", "")
                    else -> respond(connection, 404, "application/json; charset=utf-8", JSONObject().put("error", "not_found").toString())
                }
            } catch (_: Exception) {
                // A client can close a local page request while navigating away.
            }
        }
    }

    private fun assetsHtml(): String = assets.open("index.html").bufferedReader().use { it.readText() }

    private fun respond(socket: Socket, status: Int, contentType: String, body: String) {
        val reason = if (status == 200) "OK" else if (status == 204) "No Content" else "Not Found"
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val headers = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.US_ASCII)
        socket.getOutputStream().apply { write(headers); write(bytes); flush() }
    }

    private fun startObdLoop(address: String) {
        obdThread = Thread({
            while (running) {
                try {
                    if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        throw SecurityException("Bluetooth bağlantı izni kaldırıldı")
                    }
                    val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
                        ?: throw IllegalStateException("Bluetooth kullanılamıyor")
                    if (!adapter.isEnabled) throw IllegalStateException("Telefon Bluetooth'u kapalı")
                    val device = adapter.getRemoteDevice(address)
                    ObdData.deviceName = runCatching { device.name ?: address }.getOrDefault(address)
                    ObdData.status = "${ObdData.deviceName} cihazına bağlanıyor"

                    val socket = connectSocket(device)
                    bluetoothSocket = socket
                    socket.inputStream
                    socket.outputStream
                    ObdData.connected = true
                    ObdData.status = "OBD bağlantısı etkin"

                    sendCommand(socket, "ATZ", 3000)
                    sendCommand(socket, "ATE0", 1200)
                    sendCommand(socket, "ATL0", 1200)
                    sendCommand(socket, "ATS0", 1200)
                    sendCommand(socket, "ATH0", 1200)
                    sendCommand(socket, "ATSP0", 1200)
                    sendCommand(socket, "ATAT1", 1200)

                    while (running && socket.isConnected) {
                        parseBytes(sendCommand(socket, "010C"), "010C")?.let { (a, b) -> ObdData.rpm = ((a * 256 + b) / 4) }
                        parseBytes(sendCommand(socket, "010D"), "010D")?.firstOrNull()?.let { ObdData.speedKph = it }
                        parseBytes(sendCommand(socket, "0105"), "0105")?.firstOrNull()?.let { ObdData.coolantC = it - 40 }
                        parseBytes(sendCommand(socket, "0104"), "0104")?.firstOrNull()?.let { ObdData.engineLoadPct = (it * 100) / 255 }
                        parseBytes(sendCommand(socket, "012F"), "012F")?.firstOrNull()?.let { ObdData.fuelPct = (it * 100) / 255 }
                        parseVoltage(sendCommand(socket, "ATRV"))?.let { ObdData.voltage = it }
                        ObdData.updatedAt = System.currentTimeMillis()
                        Thread.sleep(POLL_INTERVAL_MS)
                    }
                    throw IllegalStateException("OBD bağlantısı kapandı")
                } catch (error: Exception) {
                    ObdData.reset("Bağlantı kesildi; yeniden deneniyor")
                    closeBluetoothSocket()
                    if (running) try { Thread.sleep(RECONNECT_DELAY_MS) } catch (_: InterruptedException) { }
                }
            }
        }, "MyOBD-bluetooth").apply { isDaemon = true; start() }
    }

    private fun connectSocket(device: android.bluetooth.BluetoothDevice): BluetoothSocket {
        val secureSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        try {
            secureSocket.connect()
            return secureSocket
        } catch (error: Exception) {
            runCatching { secureSocket.close() }
            val insecureSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            insecureSocket.connect()
            return insecureSocket
        }
    }

    private fun sendCommand(socket: BluetoothSocket, command: String, timeoutMs: Long = COMMAND_TIMEOUT_MS): String {
        val output = socket.outputStream
        val input = socket.inputStream
        output.write((command + "\r").toByteArray(StandardCharsets.US_ASCII))
        output.flush()
        val result = StringBuilder()
        val buffer = ByteArray(256)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (running && System.currentTimeMillis() < deadline) {
            if (input.available() > 0) {
                val count = input.read(buffer)
                if (count > 0) {
                    val chunk = String(buffer, 0, count, StandardCharsets.US_ASCII)
                    result.append(chunk)
                    if (chunk.contains('>') || result.contains(">")) break
                }
            } else {
                Thread.sleep(12L)
            }
        }
        return result.toString().replace(">", "").trim()
    }

    private fun parseBytes(response: String, requestPid: String): List<Int>? {
        val responseCode = "41${requestPid.takeLast(2)}"
        val compact = response.uppercase(Locale.US).replace(Regex("[^0-9A-F]"), "")
        val start = compact.lastIndexOf(responseCode)
        if (start < 0) return null
        val payload = compact.substring(start + responseCode.length)
        if (payload.length < 2) return null
        return payload.chunked(2).mapNotNull { it.toIntOrNull(16) }
    }

    private fun parseVoltage(response: String): Double? {
        val match = Regex("([0-9]{1,2}(?:\\.[0-9]+)?)").find(response) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    private fun closeBluetoothSocket() {
        runCatching { bluetoothSocket?.close() }
        bluetoothSocket = null
    }

    private fun stopWorkers() {
        running = false
        closeBluetoothSocket()
        runCatching { serverSocket?.close() }
        serverSocket = null
        serverThread?.interrupt()
        obdThread?.interrupt()
        serverThread = null
        obdThread = null
        ObdData.reset("Servis durduruldu")
    }

    companion object {
        const val ACTION_START = "com.youtubeonauto.myobd.START"
        const val ACTION_STOP = "com.youtubeonauto.myobd.STOP"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val LOOPBACK = "127.0.0.1"
        const val PORT = 8765
        private const val CHANNEL_ID = "myobd_connection"
        private const val NOTIFICATION_ID = 8765
        private const val COMMAND_TIMEOUT_MS = 1800L
        private const val POLL_INTERVAL_MS = 300L
        private const val RECONNECT_DELAY_MS = 2500L
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
