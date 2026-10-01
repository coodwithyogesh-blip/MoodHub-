package com.example.mesh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MeshBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (
            intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val prefs = MeshRelayPreferences(context)
            if (prefs.enabled) {
                MeshRelayService.start(context)
            }
        }
    }
}
