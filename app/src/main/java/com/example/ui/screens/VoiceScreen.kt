package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.AssistantState
import com.example.ui.MainViewModel
import com.example.ui.components.SonicWaveSphere

@Composable
fun VoiceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assistantState by viewModel.assistantState.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val lastResponseText by viewModel.lastResponseText.collectAsState()
    val detectedLanguage by viewModel.detectedLanguage.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val textInput by viewModel.textInput.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Status Header with Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF818CF8), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.arushi_avatar_1790566310452),
                        contentDescription = "Arushi AI Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Arushi AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Language: $detectedLanguage",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // State Pill
            val (badgeBg, badgeText, badgeColor) = when (assistantState) {
                AssistantState.LISTENING -> Triple(Color(0xFF0284C7).copy(alpha = 0.25f), "LISTENING", Color(0xFF38BDF8))
                AssistantState.SPEAKING -> Triple(Color(0xFF9333EA).copy(alpha = 0.25f), "SPEAKING", Color(0xFFC084FC))
                AssistantState.WORKING -> Triple(Color(0xFF059669).copy(alpha = 0.25f), "AGENT ACTIVE", Color(0xFF34D399))
                AssistantState.CONNECTING -> Triple(Color(0xFFD97706).copy(alpha = 0.25f), "CONNECTING", Color(0xFFFBBF24))
                AssistantState.ERROR -> Triple(Color(0xFFDC2626).copy(alpha = 0.25f), "ERROR", Color(0xFFF87171))
                AssistantState.IDLE -> Triple(Color(0xFF4F46E5).copy(alpha = 0.2f), "STANDBY", Color(0xFFA5B4FC))
            }
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Active Parallel Tasks Banner (Shows all active running tasks)
        if (activeTasks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Running Tasks (${activeTasks.size})",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Executing in parallel",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                activeTasks.take(3).forEach { task ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                OutlinedButton(
                                    onClick = { viewModel.cancelTask(task.id) },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Cancel", fontSize = 10.sp, color = Color(0xFFF87171))
                                }
                            }
                            Text(
                                text = task.currentStep,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { task.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Reactive Sonic Wave Sphere
        SonicWaveSphere(
            state = assistantState,
            amplitude = amplitude,
            onClick = {
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    viewModel.toggleListening()
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Conversation / Response Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (recognizedText.isNotBlank()) {
                    Text(
                        text = "You: \"$recognizedText\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = if (lastResponseText.isNotBlank()) {
                        lastResponseText
                    } else {
                        "Tap the sphere or speak/type any task below: app building, thumbnail, code fixing, summaries, emails, or phone actions!"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    lineHeight = 21.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Suggestion Voice Chips
        Text(
            text = "Suggested Tasks & Voice Prompts",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                "Carpenter app bana do",
                "Thumbnail bana do",
                "Code bug fix karo",
                "Document summarize karo",
                "Ek email draft karo",
                "Is topic par research karo",
                "Project backup bana do",
                "WhatsApp kholo",
                "Call Rahul",
                "Ek mast joke sunao",
                "English please",
                "Hindi mein bolo"
            )
            chips.forEach { chip ->
                Surface(
                    color = Color(0xFF312E81),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4F46E5)),
                    modifier = Modifier
                        .clickable { viewModel.processDirectPrompt(chip) }
                        .testTag("chip_${chip.replace(" ", "_")}")
                ) {
                    Text(
                        text = chip,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Universal Text Input Bar (Section 11 Universal Text Control)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { viewModel.textInput.value = it },
                placeholder = { Text("Ask Arushi anything or type a task...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("universal_text_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { viewModel.submitTextInput() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1))
                    .testTag("btn_send_text_task")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Command",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speaker 440Hz Test Button (Section 15 & 44)
            FilledTonalButton(
                onClick = { viewModel.playSpeakerDiagnostic() },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier.testTag("speaker_test_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Test Speaker 440Hz",
                    tint = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Speaker", color = Color.White, fontSize = 11.sp)
            }

            // Central Mic Button
            val isListening = assistantState == AssistantState.LISTENING
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) Color(0xFFEF4444) else Color(0xFF6366F1)
                    )
                    .clickable {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleListening()
                        }
                    }
                    .testTag("mic_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Stop Speaking / Cancel Button
            FilledTonalButton(
                onClick = { viewModel.stopSpeaking() },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier.testTag("stop_speaking_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Speaking",
                    tint = Color(0xFFF87171)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop", color = Color.White, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
