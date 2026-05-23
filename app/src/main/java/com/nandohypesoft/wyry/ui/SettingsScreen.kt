package com.nandohypesoft.wyry.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandohypesoft.wyry.data.StreamConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val profiles by viewModel.profiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    var editingProfile by remember { mutableStateOf<StreamConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Rádios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.addProfile("") }) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(profiles) { profile ->
                val isSelected = profile.id == selectedProfile?.id
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectProfile(profile) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color(0xFF1E1E1E)
                    ),
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectProfile(profile) }
                        )
                        
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(profile.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${profile.host}:${profile.port}", color = Color.Gray, fontSize = 12.sp)
                        }
                        
                        Row {
                            IconButton(onClick = { editingProfile = profile }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        if (editingProfile != null) {
            EditProfileDialog(
                profile = editingProfile!!,
                onDismiss = { editingProfile = null },
                onSave = { 
                    viewModel.updateProfile(it)
                    editingProfile = null
                }
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    profile: StreamConfig,
    onDismiss: () -> Unit,
    onSave: (StreamConfig) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var host by remember { mutableStateOf(profile.host) }
    var port by remember { mutableStateOf(profile.port.toString()) }
    var bitrate by remember { mutableStateOf(profile.bitrate.toString()) }
    var user by remember { mutableStateOf(profile.user) }
    var pass by remember { mutableStateOf(profile.pass) }
    var mount by remember { mutableStateOf(profile.mountpoint) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Rádio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do Perfil") })
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host") })
                Row {
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Porta") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = bitrate, onValueChange = { bitrate = it }, label = { Text("Bitrate") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Usuário") })
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Senha") })
                OutlinedTextField(value = mount, onValueChange = { mount = it }, label = { Text("Mountpoint") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(profile.copy(
                    name = name,
                    host = host,
                    port = port.toIntOrNull() ?: 8000,
                    bitrate = bitrate.toIntOrNull() ?: 128,
                    user = user,
                    pass = pass,
                    mountpoint = mount
                ))
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
