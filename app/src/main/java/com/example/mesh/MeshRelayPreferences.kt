package com.example.mesh

import android.content.Context

class MeshRelayPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("mesh_relay", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var batteryLimitPercent: Int
        get() = prefs.getInt(KEY_BATTERY_LIMIT, 20)
        set(value) = prefs.edit().putInt(KEY_BATTERY_LIMIT, value.coerceIn(5, 100)).apply()

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BATTERY_LIMIT = "battery_limit"
    }
}
