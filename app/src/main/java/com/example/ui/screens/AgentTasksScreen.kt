package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entity.TaskEntity
import com.example.ui.MainViewModel
import com.example.ui.TaskFilter
import java.io.File

@Composable
fun AgentTasksScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val currentFilter by viewModel.taskFilter.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    var expandedLogTaskId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Universal Task Center",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Parallel multi-tasking: coding, APKs, media, docs, research",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            if (activeTasks.isNotEmpty()) {
                Surface(
                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${activeTasks.size} Running",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Launch Buttons (Horizontal Scroll)
        Text(
            text = "Launch Any Supervised Task",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF38BDF8)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.startAppBuild(
                        "Carpenter Service",
                        "On-demand carpenter booking app with real-time carpenter listings and customer portal"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_task_carpenter")
            ) {
                Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("App Build", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    viewModel.startThumbnail(
                        "Secret Android Trick",
                        "High CTR Viral YouTube thumbnail with glowing text and robot avatar"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_task_thumbnail")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thumbnail", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    viewModel.startCodeFix("Active Project", "Fix memory leak and coroutine lifecycle bug")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_task_code_fix")
            ) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Code Fix", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    viewModel.startVideoEdit("Tech Vlog #1", "Silence cut, captions, and chapters")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_task_video_edit")
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Video Edit", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    viewModel.startDocumentSummarize("Quarterly Report", "Executive brief and action items")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_task_doc_summary")
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Doc Summary", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    viewModel.startEmailDraft("Project Update", "Status report for client team")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_task_email_draft")
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Email Draft", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    viewModel.startResearch("AI Assistant Architecture")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_task_research")
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Research", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    viewModel.startProjectBackup("Full Workspace")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_task_backup")
            ) {
                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Backup", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Pills: All, Active, Completed, Failed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskFilter.values().forEach { filter ->
                val isSelected = currentFilter == filter
                Surface(
                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable { viewModel.taskFilter.value = filter }
                        .testTag("filter_${filter.name.lowercase()}")
                ) {
                    Text(
                        text = when (filter) {
                            TaskFilter.ALL -> "All"
                            TaskFilter.ACTIVE -> "Active (${activeTasks.size})"
                            TaskFilter.COMPLETED -> "Completed"
                            TaskFilter.FAILED -> "Failed/Cancelled"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Task List
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks found for this filter.\nGive any voice prompt or tap a task button above!",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        isExpanded = expandedLogTaskId == task.id,
                        onToggleLogs = {
                            expandedLogTaskId = if (expandedLogTaskId == task.id) null else task.id
                        },
                        onCancel = { viewModel.cancelTask(task.id) },
                        onOpenArtifact = {
                            val path = task.resultArtifactPath ?: return@TaskCard
                            val file = File(path)
                            if (file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(
                                            uri,
                                            if (file.extension == "apk") "application/vnd.android.package-archive" else "*/*"
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    isExpanded: Boolean,
    onToggleLogs: () -> Unit,
    onCancel: () -> Unit,
    onOpenArtifact: () -> Unit
) {
    val isCompleted = task.status == "COMPLETED"
    val isRunning = task.status !in listOf("COMPLETED", "FAILED", "CANCELLED")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Type: ${task.taskType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                }

                val (statusBg, statusColor) = when (task.status) {
                    "COMPLETED" -> Pair(Color(0xFF065F46), Color(0xFF34D399))
                    "FAILED" -> Pair(Color(0xFF991B1B), Color(0xFFF87171))
                    "CANCELLED" -> Pair(Color(0xFF475569), Color(0xFF94A3B8))
                    else -> Pair(Color(0xFF1E3A8A), Color(0xFF60A5FA))
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = task.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Step: ${task.currentStep}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0)
            )

            if (isRunning) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF334155)
                )
            }

            // Real Verified Artifact Banner
            if (isCompleted && task.resultArtifactPath != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (task.resultArtifactType == "apk") "Verified APK Ready" else "Artifact Ready (${task.resultArtifactType})",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Size: ${maxOf(1L, task.resultArtifactSize / 1024)} KB",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = onOpenArtifact,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Artifact", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleLogs,
                    modifier = Modifier.testTag("toggle_logs_${task.id}")
                ) {
                    Text(if (isExpanded) "Hide Logs" else "View Logs", fontSize = 11.sp)
                }

                if (isRunning) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
                    ) {
                        Text("Cancel Task", fontSize = 11.sp)
                    }
                }
            }

            // Expanded Terminal Logs View
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Build & Execution Log:",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.logs.ifBlank { "No logs recorded yet." },
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
