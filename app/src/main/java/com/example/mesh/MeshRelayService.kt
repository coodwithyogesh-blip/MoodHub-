package com.example.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.UUID

class MeshRelayService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var preferences: MeshRelayPreferences
    private var serverSocket: BluetoothServerSocket? = null

    override fun onCreate() {
        super.onCreate()
        preferences = MeshRelayPreferences(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        if (!preferences.enabled) stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!preferences.enabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAcceptLoop()
        return START_STICKY
    }

    private fun startAcceptLoop() {
        if (serverSocket != null) return

        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        if (!adapter.isEnabled) return

        scope.launch {
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME,
                    SERVICE_UUID
                )

                while (isActive && preferences.enabled) {
                    val socket = try {
                        serverSocket?.accept()
                    } catch (_: Exception) {
                        null
                    }

                    if (socket != null) {
                        launch { handlePeer(socket) }
                    }
                }
            } catch (_: SecurityException) {
                // Runtime BLUETOOTH_CONNECT permission must be granted by the UI.
            } catch (_: Exception) {
                // The service remains persistent; the next start/restart retries.
            }
        }
    }

    private suspend fun handlePeer(socket: BluetoothSocket) {
        socket.use { peer ->
            val input = BufferedInputStream(peer.inputStream)
            val output = BufferedOutputStream(peer.outputStream)

            // Protocol implementation will be added in the next mesh layer:
            // handshake -> encrypted envelope -> dedup -> TTL -> next-hop forwarding.
            val buffer = ByteArray(4096)
            while (preferences.enabled) {
                val count = try {
                    input.read(buffer)
                } catch (_: Exception) {
                    -1
                }
                if (count <= 0) break

                // Do not display or persist relay payload here.
                // It remains opaque to the relay UI.
                output.flush()
            }
        }
    }

    override fun onDestroy() {
        serverSocket?.close()
        serverSocket = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Mesh Relay Active")
            .setContentText("This phone is helping forward encrypted mesh traffic.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Mesh Relay",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    companion object {
        private const val CHANNEL_ID = "mesh_relay"
        private const val NOTIFICATION_ID = 4101
        private const val SERVICE_NAME = "MoodHub Mesh"
        private val SERVICE_UUID: UUID =
            UUID.fromString("6f4f5f4c-2f8d-4e9f-9d7a-2a8e7a1f20a1")

        fun start(context: Context) {
            val intent = Intent(context, MeshRelayService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MeshRelayService::class.java))
        }
    }
}
