package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ArushiApplication
import com.example.MainActivity
import com.example.R
import com.example.agent.AutonomousBuildRunner
import com.example.agent.ProjectWorkspaceManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ArushiAssistantService : Service() {

    private val tag = "ArushiAssistantService"
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var database: AppDatabase
    private lateinit var workspaceManager: ProjectWorkspaceManager
    private lateinit var buildRunner: AutonomousBuildRunner

    private val _currentTask = MutableStateFlow<TaskEntity?>(null)
    val currentTask = _currentTask.asStateFlow()

    private var activeJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): ArushiAssistantService = this@ArushiAssistantService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        workspaceManager = ProjectWorkspaceManager(this, database)
        buildRunner = AutonomousBuildRunner(this, database, workspaceManager)
        Log.d(tag, "ArushiAssistantService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_CANCEL_TASK) {
            cancelCurrentTask()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification("Arushi is active", "Ready for voice & background tasks", 0f, false))
        return START_STICKY
    }

    fun startAppBuildTask(projectName: String, description: String) {
        activeJob?.cancel()
        activeJob = serviceScope.launch {
            val taskId = "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
            val initialTask = TaskEntity(
                id = taskId,
                projectId = null,
                title = "Build App: $projectName",
                taskType = "APP_BUILD",
                status = "PLANNING",
                progress = 0.05f,
                currentStep = "Initiating build orchestrator"
            )
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask

            updateNotification("Building $projectName", "Starting workspace & planning...", 0.05f)

            val result = buildRunner.executeAppBuild(taskId, projectName, description) { step, progress ->
                updateNotification("Building $projectName", step, progress)
            }

            _currentTask.value = result
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — $projectName Ready!", "APK has been built successfully and is ready.")
            }
        }
    }

    fun startThumbnailTask(title: String, topic: String) {
        activeJob?.cancel()
        activeJob = serviceScope.launch {
            val taskId = "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
            val initialTask = TaskEntity(
                id = taskId,
                projectId = null,
                title = "Thumbnail: $title",
                taskType = "THUMBNAIL_GEN",
                status = "PLANNING",
                progress = 0.1f,
                currentStep = "Designing thumbnail layout"
            )
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask

            updateNotification("Generating Thumbnail", "Designing 16:9 banner...", 0.1f)

            val result = buildRunner.executeThumbnailGeneration(taskId, title, topic) { step, progress ->
                updateNotification("Generating Thumbnail", step, progress)
            }

            _currentTask.value = result
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Thumbnail Ready!", "YouTube 16:9 Thumbnail generated and saved.")
            }
        }
    }

    fun startVideoEditTask(title: String, instructions: String) {
        activeJob?.cancel()
        activeJob = serviceScope.launch {
            val taskId = "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
            val initialTask = TaskEntity(
                id = taskId,
                projectId = null,
                title = "Video Edit: $title",
                taskType = "VIDEO_EDIT",
                status = "PLANNING",
                progress = 0.1f,
                currentStep = "Analyzing video structure"
            )
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask

            updateNotification("Editing Video", "Analyzing silence & chapters...", 0.1f)

            val result = buildRunner.executeVideoEdit(taskId, title, instructions) { step, progress ->
                updateNotification("Editing Video", step, progress)
            }

            _currentTask.value = result
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Video Edit Ready!", "Subtitles and chapter markers created.")
            }
        }
    }

    fun cancelCurrentTask() {
        activeJob?.cancel()
        val current = _currentTask.value
        if (current != null) {
            serviceScope.launch {
                database.taskDao().cancelTask(current.id)
                _currentTask.value = current.copy(status = "CANCELLED", currentStep = "Cancelled by user")
            }
        }
        updateNotification("Task Cancelled", "Arushi is in standby", 0f, false)
    }

    private fun updateNotification(title: String, message: String, progress: Float, showProgress: Boolean = true) {
        val notification = buildForegroundNotification(title, message, progress, showProgress)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(title: String, message: String) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ArushiApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun buildForegroundNotification(
        title: String,
        message: String,
        progress: Float,
        showProgress: Boolean = true
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, ArushiAssistantService::class.java).apply {
            action = ACTION_CANCEL_TASK
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, ArushiApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (showProgress) {
            builder.setProgress(100, (progress * 100).toInt(), false)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        }

        return builder.build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002
        const val ACTION_CANCEL_TASK = "com.example.action.CANCEL_TASK"

        fun startService(context: Context) {
            val intent = Intent(context, ArushiAssistantService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
