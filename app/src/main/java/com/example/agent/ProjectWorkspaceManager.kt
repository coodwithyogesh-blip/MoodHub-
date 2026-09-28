package com.example.agent

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CheckpointEntity
import com.example.data.local.entity.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class WorkspaceFileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

class ProjectWorkspaceManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val tag = "ProjectWorkspaceMgr"

    private val baseWorkspaceDir: File by lazy {
        File(context.filesDir, "arushi_workspace").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getProjectDir(projectId: String): File {
        val dir = File(baseWorkspaceDir, projectId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun createProject(name: String, type: String, description: String): ProjectEntity = withContext(Dispatchers.IO) {
        val id = "proj_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val projDir = getProjectDir(id)

        val project = ProjectEntity(
            id = id,
            name = name,
            projectType = type,
            description = description,
            status = "CREATED",
            rootDir = projDir.absolutePath
        )
        database.projectDao().insertProject(project)

        // Populate initial files based on type
        when (type) {
            "mobile_app" -> initializeAppFiles(projDir, name, description)
            "thumbnail" -> initializeThumbnailFiles(projDir, name, description)
            "video" -> initializeVideoFiles(projDir, name, description)
            "document" -> initializeDocumentFiles(projDir, name, description)
            "email" -> initializeEmailFiles(projDir, name, description)
            "research" -> initializeResearchFiles(projDir, name, description)
            else -> initializeGenericFiles(projDir, name, description)
        }

        createCheckpoint(id, "Initial Commit", "Initial project creation")
        project
    }

    private fun initializeAppFiles(projectDir: File, appName: String, description: String) {
        val srcDir = File(projectDir, "src/main/java/com/app").apply { mkdirs() }
        val resDir = File(projectDir, "src/main/res/values").apply { mkdirs() }

        // Manifest
        File(projectDir, "AndroidManifest.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.app.$appName">
                <uses-permission android:name="android.permission.INTERNET" />
                <application
                    android:label="$appName"
                    android:theme="@style/Theme.Material3.DayNight">
                    <activity android:name=".MainActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()
        )

        // Build gradle
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            }
            android {
                namespace = "com.app.$appName"
                compileSdk = 34
                defaultConfig {
                    applicationId = "com.app.${appName.lowercase().replace(" ", "")}"
                    minSdk = 24
                    targetSdk = 34
                    versionCode = 1
                    versionName = "1.0.0"
                }
            }
            dependencies {
                implementation("androidx.compose.material3:material3:1.3.0")
                implementation("androidx.core:core-ktx:1.13.0")
            }
            """.trimIndent()
        )

        // MainActivity
        File(srcDir, "MainActivity.kt").writeText(
            """
            package com.app

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.material3.*
            import androidx.compose.runtime.*

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        MaterialTheme {
                            ServiceDashboardScreen(title = "$appName")
                        }
                    }
                }
            }
            """.trimIndent()
        )

        // Screens / UI
        File(srcDir, "ServiceScreens.kt").writeText(
            """
            package com.app

            import androidx.compose.foundation.layout.*
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ServiceDashboardScreen(title: String) {
                Scaffold(
                    topBar = { TopAppBar(title = { Text(title) }) }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                        Text("$description", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { /* Book Service */ }) {
                            Text("Book Now")
                        }
                    }
                }
            }
            """.trimIndent()
        )

        File(projectDir, "README.md").writeText(
            """
            # $appName
            $description

            Generated by Arushi AI Autonomous Agent Orchestrator.
            """.trimIndent()
        )
    }

    private fun initializeThumbnailFiles(projectDir: File, name: String, description: String) {
        File(projectDir, "thumbnail_spec.json").writeText(
            """
            {
              "title": "$name",
              "style": "High-CTR Bold Vibrant",
              "aspectRatio": "16:9",
              "elements": ["Bold Hindi/English typography", "High-contrast background", "Glowing focal icon"],
              "description": "$description"
            }
            """.trimIndent()
        )
    }

    private fun initializeVideoFiles(projectDir: File, name: String, description: String) {
        File(projectDir, "video_edit_plan.json").writeText(
            """
            {
              "project": "$name",
              "action": "YouTube Video Edit",
              "aspectRatio": "16:9 / Shorts 9:16",
              "features": ["Silence removal", "Animated subtitles", "Dynamic intro hook", "Audio leveling"],
              "description": "$description"
            }
            """.trimIndent()
        )
    }

    private fun initializeDocumentFiles(projectDir: File, name: String, description: String) {
        File(projectDir, "summary_report.md").writeText(
            """
            # Executive Summary: $name
            
            ## Overview
            $description
            
            ## Key Takeaways
            - Comprehensive structural analysis completed.
            - Core objectives and critical action items identified.
            
            ## Action Items
            1. Review summarized points with stakeholders.
            2. Implement priority recommendations.
            
            *Generated by Arushi AI Universal Agent*
            """.trimIndent()
        )
    }

    private fun initializeEmailFiles(projectDir: File, name: String, description: String) {
        File(projectDir, "email_draft.txt").writeText(
            """
            Subject: $name
            
            Hi there,
            
            I hope this email finds you well.
            
            Regarding: $description
            
            Please let me know if you have any questions or if you would like any adjustments made.
            
            Best regards,
            Arushi AI Assistant
            """.trimIndent()
        )
    }

    private fun initializeResearchFiles(projectDir: File, name: String, description: String) {
        File(projectDir, "research_dossier.md").writeText(
            """
            # Research Report: $name
            
            ## Abstract & Scope
            $description
            
            ## Market & Technical Findings
            - Comparative landscape and best practice benchmarks.
            - Emerging trends and efficiency factors.
            
            ## Strategic Recommendations
            - Focus on core value drivers and iterative validation.
            
            *Prepared autonomously by Arushi AI*
            """.trimIndent()
        )
    }

    private fun initializeGenericFiles(projectDir: File, name: String, description: String) {
        File(projectDir, "README.md").writeText("# $name\n$description")
    }

    suspend fun listFiles(projectId: String): List<WorkspaceFileInfo> = withContext(Dispatchers.IO) {
        val projDir = getProjectDir(projectId)
        val result = mutableListOf<WorkspaceFileInfo>()

        projDir.walkTopDown().maxDepth(5).forEach { file ->
            if (file != projDir) {
                result.add(
                    WorkspaceFileInfo(
                        name = file.name,
                        path = file.relativeTo(projDir).path,
                        isDirectory = file.isDirectory,
                        size = if (file.isFile) file.length() else 0L,
                        lastModified = file.lastModified()
                    )
                )
            }
        }
        result
    }

    suspend fun readFile(projectId: String, relativePath: String): String = withContext(Dispatchers.IO) {
        val file = File(getProjectDir(projectId), relativePath)
        if (file.exists() && file.isFile) file.readText() else "File not found"
    }

    suspend fun writeFile(projectId: String, relativePath: String, content: String) = withContext(Dispatchers.IO) {
        val file = File(getProjectDir(projectId), relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    suspend fun createCheckpoint(projectId: String, name: String, desc: String): CheckpointEntity = withContext(Dispatchers.IO) {
        val checkpointId = "cp_${System.currentTimeMillis()}"
        val files = listFiles(projectId)
        val entity = CheckpointEntity(
            id = checkpointId,
            projectId = projectId,
            checkpointName = name,
            description = desc,
            fileCount = files.size
        )
        database.projectDao().insertCheckpoint(entity)
        entity
    }
}
