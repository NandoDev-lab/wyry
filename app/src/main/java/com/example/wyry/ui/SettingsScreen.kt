package com.example.wyry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wyry.data.StreamConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val config by viewModel.streamConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações da Rádio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                .verticalScroll(rememberScrollState())
        ) {
            ConfigForm(config) { viewModel.updateConfig(it) }
        }
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
                label = { Text("Host (Ex: radio.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = config.port.toString(),
                    onValueChange = { onConfigChange(config.copy(port = it.toIntOrNull() ?: 8000)) },
                    label = { Text("Porta") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = config.bitrate.toString(),
                    onValueChange = { onConfigChange(config.copy(bitrate = it.toIntOrNull() ?: 128)) },
                    label = { Text("Bitrate (kbps)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = config.user,
                onValueChange = { onConfigChange(config.copy(user = it)) },
                label = { Text("Usuário") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = config.pass,
                onValueChange = { onConfigChange(config.copy(pass = it)) },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = config.mountpoint,
                onValueChange = { onConfigChange(config.copy(mountpoint = it)) },
                label = { Text("Mountpoint (Ex: /live)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
