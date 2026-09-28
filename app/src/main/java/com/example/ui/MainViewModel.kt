package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ArushiApplication
import com.example.action.DeviceActionBridge
import com.example.agent.AutonomousBuildRunner
import com.example.agent.ProjectWorkspaceManager
import com.example.agent.WorkspaceFileInfo
import com.example.ai.AssistantState
import com.example.ai.GeminiLiveVoiceClient
import com.example.audio.AudioOutputPlayer
import com.example.audio.MicrophoneRecorder
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.service.ArushiAssistantService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as ArushiApplication).database
    val workspaceManager = ProjectWorkspaceManager(application, database)
    val buildRunner = AutonomousBuildRunner(application, database, workspaceManager)

    val audioOutputPlayer = AudioOutputPlayer(application)
    val microphoneRecorder = MicrophoneRecorder(application)
    val deviceActionBridge = DeviceActionBridge(application)
    val geminiClient = GeminiLiveVoiceClient(
        application,
        audioOutputPlayer,
        deviceActionBridge,
        database
    )

    // Service connection for cross-app background tasks
    private var assistantService: ArushiAssistantService? = null
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound = _isServiceBound.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? ArushiAssistantService.LocalBinder
            assistantService = binder?.getService()
            _isServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            assistantService = null
            _isServiceBound.value = false
        }
    }

    // Reactive StateFlows
    val assistantState = geminiClient.assistantState
    val amplitude = microphoneRecorder.amplitude
    val recognizedText = microphoneRecorder.recognizedText
    val lastResponseText = geminiClient.lastResponseText
    val detectedLanguage = geminiClient.detectedLanguage

    val allTasks = database.taskDao().getAllTasks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activeTasks = database.taskDao().getActiveTasks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allProjects = database.projectDao().getAllProjects().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val conversationHistory = database.conversationDao().getAllMessages().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _selectedProject = MutableStateFlow<ProjectEntity?>(null)
    val selectedProject = _selectedProject.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<WorkspaceFileInfo>>(emptyList())
    val projectFiles = _projectFiles.asStateFlow()

    private val _fileContent = MutableStateFlow<String?>(null)
    val fileContent = _fileContent.asStateFlow()

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName = _selectedFileName.asStateFlow()

    init {
        // Start foreground service
        ArushiAssistantService.startService(application)
        val intent = Intent(application, ArushiAssistantService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        microphoneRecorder.onSpeechRecognized = { speech ->
            geminiClient.processUserSpeech(speech, assistantService)
        }
    }

    fun toggleListening() {
        if (microphoneRecorder.isRecording.value) {
            microphoneRecorder.stopListening()
        } else {
            audioOutputPlayer.stopPlayback()
            microphoneRecorder.startListening()
        }
    }

    fun stopSpeaking() {
        geminiClient.stopSpeaking()
    }

    fun processDirectPrompt(prompt: String) {
        audioOutputPlayer.stopPlayback()
        geminiClient.processUserSpeech(prompt, assistantService)
    }

    fun playSpeakerDiagnostic() {
        viewModelScope.launch {
            audioOutputPlayer.playDiagnosticTone()
        }
    }

    fun startAppBuild(name: String, description: String) {
        assistantService?.startAppBuildTask(name, description)
            ?: viewModelScope.launch {
                val taskId = "task_${System.currentTimeMillis()}"
                val task = TaskEntity(
                    id = taskId,
                    projectId = null,
                    title = "Build App: $name",
                    taskType = "APP_BUILD",
                    status = "PLANNING",
                    currentStep = "Initiating build"
                )
                database.taskDao().insertTask(task)
                buildRunner.executeAppBuild(taskId, name, description) { _, _ -> }
            }
    }

    fun startThumbnail(title: String, topic: String) {
        assistantService?.startThumbnailTask(title, topic)
    }

    fun startVideoEdit(title: String, instructions: String) {
        assistantService?.startVideoEditTask(title, instructions)
    }

    fun cancelTask(taskId: String) {
        assistantService?.cancelCurrentTask()
        viewModelScope.launch {
            database.taskDao().cancelTask(taskId)
        }
    }

    fun selectProject(project: ProjectEntity) {
        _selectedProject.value = project
        _fileContent.value = null
        _selectedFileName.value = null
        viewModelScope.launch {
            val files = workspaceManager.listFiles(project.id)
            _projectFiles.value = files
        }
    }

    fun viewFile(relativePath: String, fileName: String) {
        val proj = _selectedProject.value ?: return
        _selectedFileName.value = fileName
        viewModelScope.launch {
            val content = workspaceManager.readFile(proj.id, relativePath)
            _fileContent.value = content
        }
    }

    fun closeFileViewer() {
        _fileContent.value = null
        _selectedFileName.value = null
    }

    fun openWhatsApp() {
        deviceActionBridge.openWhatsApp()
    }

    fun openApp(appName: String) {
        deviceActionBridge.openApp(appName)
    }

    fun makeCall(number: String) {
        deviceActionBridge.makeCall(number)
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: Exception) {}
        microphoneRecorder.release()
        audioOutputPlayer.release()
        super.onCleared()
    }
}
