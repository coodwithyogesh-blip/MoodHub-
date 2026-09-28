package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        Spacer(modifier = Modifier.height(16.dp))

        // Top Status Header with Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                AssistantState.WORKING -> Triple(Color(0xFF059669).copy(alpha = 0.25f), "RUNNING AGENT", Color(0xFF34D399))
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

        // Active Background Task Banner (if user returns or task is active)
        if (activeTasks.isNotEmpty()) {
            val task = activeTasks.first()
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Job: ${task.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = task.currentStep,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.cancelTask(task.id) },
                        modifier = Modifier.testTag("cancel_active_task_button")
                    ) {
                        Text("Cancel", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

        // Conversation / Response Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (recognizedText.isNotBlank()) {
                    Text(
                        text = "You: \"$recognizedText\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = if (lastResponseText.isNotBlank()) {
                        lastResponseText
                    } else {
                        "Tap the sphere or mic button below to talk to Arushi in Hindi, Hinglish, or English!"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Suggestion Voice Chips
        Text(
            text = "Suggested Voice Prompts",
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
                "WhatsApp kholo",
                "Carpenter app bana do",
                "Call Rahul",
                "Ek mast joke sunao",
                "YouTube thumbnail bana do",
                "Mere liye video edit karo",
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
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
                Text("Test Speaker", color = Color.White, fontSize = 12.sp)
            }

            // Central Mic Button
            val isListening = assistantState == AssistantState.LISTENING
            Box(
                modifier = Modifier
                    .size(64.dp)
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
                    modifier = Modifier.size(32.dp)
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
                Text("Stop", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
