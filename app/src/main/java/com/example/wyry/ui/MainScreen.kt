package com.example.wyry.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wyry.data.StreamConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val isStreaming by viewModel.isStreaming.collectAsState()
    val status by viewModel.status.collectAsState()
    val vuMeter by viewModel.vuMeter.collectAsState()
    val micVolume by viewModel.micVolume.collectAsState()
    val micEnabled by viewModel.micEnabled.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()
    val currentSongTitle by viewModel.currentSongTitle.collectAsState()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.playMusic(uris)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adoradores Preparados Studio") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ON AIR Indicator
            StatusIndicator(isStreaming, status)

            Spacer(modifier = Modifier.height(24.dp))

            // Studio Mixer Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(250.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertical Mic Control
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        VerticalVolumeSlider(
                            value = micVolume,
                            enabled = micEnabled,
                            onValueChange = { viewModel.setMicVolume(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(onClick = { viewModel.toggleMic() }) {
                            Icon(
                                if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (micEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        Text("MIC", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Vertical VU Meter
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        VerticalVuMeter(vuMeter)
                        Spacer(modifier = Modifier.height(48.dp)) // Alinhamento com os botões
                        Text("LEVEL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Vertical Music Control
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        VerticalVolumeSlider(
                            value = musicVolume,
                            onValueChange = { viewModel.setMusicVolume(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(onClick = { launcher.launch(arrayOf("audio/*")) }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("MUSIC", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            StreamingControls(isStreaming) { viewModel.toggleStream() }

            Spacer(modifier = Modifier.height(24.dp))

            // Music Player Controls
            MusicPlayerSection(
                currentTitle = currentSongTitle,
                isPlaying = isMusicPlaying,
                onSelectMusic = { launcher.launch(arrayOf("audio/*")) },
                onTogglePlay = { viewModel.toggleMusic() },
                onNext = { viewModel.nextSong() },
                onPrevious = { viewModel.previousSong() }
            )
        }
    }
}

@Composable
fun VerticalVolumeSlider(
    value: Float,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .width(180.dp)
                .graphicsLayer {
                    rotationZ = 270f
                },
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
                activeTrackColor = if (enabled) MaterialTheme.colorScheme.primary else Color.DarkGray
            )
        )
    }
}

@Composable
fun VerticalVuMeter(value: Float) {
    val barCount = 20
    val activeBars = (value * barCount).roundToInt().coerceIn(0, barCount)
    
    Column(
        modifier = Modifier
            .width(24.dp)
            .height(180.dp)
            .background(Color.Black, shape = MaterialTheme.shapes.small)
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)
    ) {
        for (i in barCount downTo 1) {
            val color = when {
                i > 17 -> Color.Red
                i > 14 -> Color.Yellow
                else -> Color.Green
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        if (i <= activeBars) color else color.copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
fun StatusIndicator(isStreaming: Boolean, status: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                if (isStreaming) Color.Red else Color.Gray,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.White, shape = MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun StreamingControls(isStreaming: Boolean, onToggle: () -> Unit) {
    Button(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isStreaming) Color.DarkGray else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isStreaming) "OFF AIR" else "ON AIR", fontSize = 20.sp)
    }
}

@Composable
fun MusicPlayerSection(
    currentTitle: String?,
    isPlaying: Boolean,
    onSelectMusic: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Playlist", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = currentTitle ?: "Nenhuma música selecionada",
                color = if (currentTitle != null) Color.Green else Color.Gray,
                fontSize = 14.sp,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White)
                }
                
                Button(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(56.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
                
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onSelectMusic,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Selecionar Músicas")
            }
        }
    }
}
