package com.example.wyry.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wyry.data.StreamConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val isStreaming by viewModel.isStreaming.collectAsState()
    val status by viewModel.status.collectAsState()
    val vuMeter by viewModel.vuMeter.collectAsState()
    val config by viewModel.streamConfig.collectAsState()
    val micVolume by viewModel.micVolume.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adoradores Preparados Studio") },
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

            // VU Meter
            VuMeter(vuMeter)

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            StreamingControls(isStreaming) { viewModel.toggleStream() }

            Spacer(modifier = Modifier.height(24.dp))

            // Mixing Sliders
            VolumeSlider("Microfone", micVolume) { viewModel.setMicVolume(it) }
            VolumeSlider("Música", musicVolume) { viewModel.setMusicVolume(it) }

            Spacer(modifier = Modifier.height(24.dp))

            // Config Form
            ConfigForm(config) { viewModel.updateConfig(it) }
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
fun VuMeter(value: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("VU Meter", color = Color.Gray, fontSize = 12.sp)
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (value > 0.8f) Color.Red else Color.Green,
            trackColor = Color.DarkGray,
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
fun VolumeSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White)
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun ConfigForm(config: StreamConfig, onConfigChange: (StreamConfig) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Configuração do Servidor", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = config.host,
                onValueChange = { onConfigChange(config.copy(host = it)) },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = config.port.toString(),
                    onValueChange = { onConfigChange(config.copy(port = it.toIntOrNull() ?: 8000)) },
                    label = { Text("Porta") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = config.bitrate.toString(),
                    onValueChange = { onConfigChange(config.copy(bitrate = it.toIntOrNull() ?: 128)) },
                    label = { Text("Bitrate") },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = config.user,
                onValueChange = { onConfigChange(config.copy(user = it)) },
                label = { Text("Usuário") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = config.pass,
                onValueChange = { onConfigChange(config.copy(pass = it)) },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = config.mountpoint,
                onValueChange = { onConfigChange(config.copy(mountpoint = it)) },
                label = { Text("Mountpoint") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
