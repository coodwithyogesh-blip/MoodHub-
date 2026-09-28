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
import java.util.concurrent.ConcurrentHashMap

class ArushiAssistantService : Service() {

    private val tag = "ArushiAssistantService"
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var database: AppDatabase
    private lateinit var workspaceManager: ProjectWorkspaceManager
    private lateinit var buildRunner: AutonomousBuildRunner

    private val _currentTask = MutableStateFlow<TaskEntity?>(null)
    val currentTask = _currentTask.asStateFlow()

    // Multi-task parallel jobs map
    private val activeJobs = ConcurrentHashMap<String, Job>()

    inner class LocalBinder : Binder() {
        fun getService(): ArushiAssistantService = this@ArushiAssistantService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        workspaceManager = ProjectWorkspaceManager(this, database)
        buildRunner = AutonomousBuildRunner(this, database, workspaceManager)
        Log.d(tag, "ArushiAssistantService started with Universal Multi-Tasking engine")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_CANCEL_TASK) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            if (taskId != null) {
                cancelTask(taskId)
            } else {
                cancelAllTasks()
            }
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            buildForegroundNotification("Arushi is active", "Ready for voice & background tasks", 0f, false)
        )
        return START_STICKY
    }

    fun startAppBuildTask(projectName: String, description: String) {
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

        val job = serviceScope.launch {
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask
            updateMultiTaskNotification("Building $projectName", 0.05f)

            val result = buildRunner.executeAppBuild(taskId, projectName, description) { step, progress ->
                updateMultiTaskNotification(step, progress)
            }

            _currentTask.value = result
            activeJobs.remove(taskId)
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — $projectName Ready!", "APK built and verified successfully.")
            }
            updateActiveNotificationSummary()
        }
        activeJobs[taskId] = job
    }

    fun startThumbnailTask(title: String, topic: String) {
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

        val job = serviceScope.launch {
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask
            updateMultiTaskNotification("Designing Thumbnail: $title", 0.1f)

            val result = buildRunner.executeThumbnailGeneration(taskId, title, topic) { step, progress ->
                updateMultiTaskNotification(step, progress)
            }

            _currentTask.value = result
            activeJobs.remove(taskId)
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Thumbnail Ready!", "YouTube 16:9 Thumbnail generated and saved.")
            }
            updateActiveNotificationSummary()
        }
        activeJobs[taskId] = job
    }

    fun startVideoEditTask(title: String, instructions: String) {
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

        val job = serviceScope.launch {
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask
            updateMultiTaskNotification("Editing Video: $title", 0.1f)

            val result = buildRunner.executeVideoEdit(taskId, title, instructions) { step, progress ->
                updateMultiTaskNotification(step, progress)
            }

            _currentTask.value = result
            activeJobs.remove(taskId)
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Video Edit Ready!", "Subtitles and chapter markers created.")
            }
            updateActiveNotificationSummary()
        }
        activeJobs[taskId] = job
    }

    fun startCodeFixTask(projectName: String, issueDesc: String) {
        val taskId = "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val initialTask = TaskEntity(
            id = taskId,
            projectId = null,
            title = "Fix Code: $projectName",
            taskType = "CODE_FIX",
            status = "PLANNING",
            progress = 0.1f,
            currentStep = "Analyzing codebase and AST"
        )

        val job = serviceScope.launch {
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask
            updateMultiTaskNotification("Fixing code in $projectName", 0.1f)

            val result = buildRunner.executeCodeFix(taskId, projectName, issueDesc) { step, progress ->
                updateMultiTaskNotification(step, progress)
            }

            _currentTask.value = result
            activeJobs.remove(taskId)
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Code Fix Complete!", "Resolved bug and created checkpoint.")
            }
            updateActiveNotificationSummary()
        }
        activeJobs[taskId] = job
    }

    fun startDocumentSummarizeTask(title: String, content: String) {
        val taskId = "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val initialTask = TaskEntity(
            id = taskId,
            projectId = null,
            title = "Summary: $title",
            taskType = "DOC_SUMMARIZE",
            status = "PLANNING",
            progress = 0.15f,
            currentStep = "Reading document content"
        )

        val job = serviceScope.launch {
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask
            updateMultiTaskNotification("Summarizing $title", 0.15f)

            val result = buildRunner.executeDocumentSummarize(taskId, title, content) { step, progress ->
                updateMultiTaskNotification(step, progress)
            }

            _currentTask.value = result
            activeJobs.remove(taskId)
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Document Summary Ready!", "Executive summary report exported.")
            }
            updateActiveNotificationSummary()
        }
        activeJobs[taskId] = job
    }

    fun startEmailDraftTask(subject: String, details: String) {
        val taskId = "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val initialTask = TaskEntity(
            id = taskId,
            projectId = null,
            title = "Email: $subject",
            taskType = "EMAIL_DRAFT",
            status = "PLANNING",
            progress = 0.2f,
            currentStep = "Structuring tone and key points"
        )

        val job = serviceScope.launch {
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask
            updateMultiTaskNotification("Drafting email: $subject", 0.2f)

            val result = buildRunner.executeEmailDraft(taskId, subject, details) { step, progress ->
                updateMultiTaskNotification(step, progress)
            }

            _currentTask.value = result
            activeJobs.remove(taskId)
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Email Draft Ready!", "Professional copy drafted & saved.")
            }
            updateActiveNotificationSummary()
        }
        activeJobs[taskId] = job
    }

    fun startResearchTask(topic: String) {
        val taskId = "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val initialTask = TaskEntity(
            id = taskId,
            projectId = null,
            title = "Research: $topic",
            taskType = "RESEARCH",
            status = "PLANNING",
            progress = 0.15f,
            currentStep = "Synthesizing research questions"
        )

        val job = serviceScope.launch {
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask
            updateMultiTaskNotification("Researching: $topic", 0.15f)

            val result = buildRunner.executeResearch(taskId, topic) { step, progress ->
                updateMultiTaskNotification(step, progress)
            }

            _currentTask.value = result
            activeJobs.remove(taskId)
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Research Dossier Ready!", "Comprehensive report compiled.")
            }
            updateActiveNotificationSummary()
        }
        activeJobs[taskId] = job
    }

    fun startProjectBackupTask(projectName: String) {
        val taskId = "task_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val initialTask = TaskEntity(
            id = taskId,
            projectId = null,
            title = "Backup: $projectName",
            taskType = "PROJECT_BACKUP",
            status = "PLANNING",
            progress = 0.2f,
            currentStep = "Creating workspace snapshot"
        )

        val job = serviceScope.launch {
            database.taskDao().insertTask(initialTask)
            _currentTask.value = initialTask
            updateMultiTaskNotification("Backing up $projectName", 0.2f)

            val result = buildRunner.executeProjectBackup(taskId, projectName) { step, progress ->
                updateMultiTaskNotification(step, progress)
            }

            _currentTask.value = result
            activeJobs.remove(taskId)
            if (result.status == "COMPLETED") {
                showCompletionNotification("Arushi — Backup Complete!", "Project checkpoint archived.")
            }
            updateActiveNotificationSummary()
        }
        activeJobs[taskId] = job
    }

    fun cancelTask(taskId: String) {
        val job = activeJobs.remove(taskId)
        job?.cancel()
        serviceScope.launch {
            database.taskDao().cancelTask(taskId)
            val current = _currentTask.value
            if (current?.id == taskId) {
                _currentTask.value = current.copy(status = "CANCELLED", currentStep = "Cancelled by user")
            }
            updateActiveNotificationSummary()
        }
    }

    fun cancelAllTasks() {
        activeJobs.forEach { (id, job) ->
            job.cancel()
            serviceScope.launch {
                database.taskDao().cancelTask(id)
            }
        }
        activeJobs.clear()
        val current = _currentTask.value
        if (current != null) {
            _currentTask.value = current.copy(status = "CANCELLED", currentStep = "Cancelled by user")
        }
        updateForegroundNotification("All Tasks Cancelled", "Arushi is in standby", 0f, false)
    }

    private fun updateMultiTaskNotification(latestStep: String, progress: Float) {
        val count = activeJobs.size
        val title = if (count > 1) "Arushi ($count active tasks)" else "Arushi Assistant"
        updateForegroundNotification(title, latestStep, progress, true)
    }

    private fun updateActiveNotificationSummary() {
        val count = activeJobs.size
        if (count == 0) {
            updateForegroundNotification("Arushi is active", "Ready for voice & background tasks", 0f, false)
        } else {
            updateForegroundNotification("Arushi ($count tasks running)", "Tasks executing in background", 0.5f, true)
        }
    }

    private fun updateForegroundNotification(title: String, message: String, progress: Float, showProgress: Boolean = true) {
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
            System.currentTimeMillis().toInt(),
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
        nm.notify(System.currentTimeMillis().toInt(), notification)
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
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel All", cancelPendingIntent)
        }

        return builder.build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_CANCEL_TASK = "com.example.action.CANCEL_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"

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
