package com.youtubeonauto.myobd

import org.json.JSONObject

object ObdData {
    @Volatile var connected: Boolean = false
    @Volatile var status: String = "Servis başlatılmadı"
    @Volatile var deviceName: String = ""
    @Volatile var rpm: Int? = null
    @Volatile var speedKph: Int? = null
    @Volatile var coolantC: Int? = null
    @Volatile var engineLoadPct: Int? = null
    @Volatile var fuelPct: Int? = null
    @Volatile var voltage: Double? = null
    @Volatile var updatedAt: Long = 0L

    @Synchronized
    fun reset(message: String) {
        connected = false
        status = message
        rpm = null
        speedKph = null
        coolantC = null
        engineLoadPct = null
        fuelPct = null
        voltage = null
        updatedAt = System.currentTimeMillis()
    }

    fun asJson(): JSONObject = JSONObject()
        .put("connected", connected)
        .put("status", status)
        .put("device", deviceName)
        .put("rpm", rpm ?: JSONObject.NULL)
        .put("speedKph", speedKph ?: JSONObject.NULL)
        .put("coolantC", coolantC ?: JSONObject.NULL)
        .put("engineLoadPct", engineLoadPct ?: JSONObject.NULL)
        .put("fuelPct", fuelPct ?: JSONObject.NULL)
        .put("voltage", voltage ?: JSONObject.NULL)
        .put("updatedAt", updatedAt)
}
