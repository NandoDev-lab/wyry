package com.adoradorespreparados.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adoradorespreparados.studio.ui.StreamingViewModel

import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Aqui você pode tratar se o usuário negou algo essencial
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Solicitar permissões essenciais para API 35 (Android 15)
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO) // Para selecionar MP3 locais
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RadioStudioScreen()
                }
            }
        }
    }
}

@Composable
fun RadioStudioScreen(viewModel: StreamingViewModel = viewModel()) {
    val isStreaming by viewModel.isStreaming.collectAsState()
    val micVolume by viewModel.micVolume.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ADORADORES PREPARADOS",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(text = "STUDIO", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // Indicador ON AIR
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (isStreaming) Color.Red else Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isStreaming) "ON AIR" else "OFF AIR",
                color = if (isStreaming) Color.Red else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Controles de Mixagem
        MixerControl(label = "MICROFONE", value = micVolume, onValueChange = viewModel::setMicVolume)
        Spacer(modifier = Modifier.height(16.dp))
        MixerControl(label = "TRILHA / MÚSICA", value = musicVolume, onValueChange = viewModel::setMusicVolume)

        Spacer(modifier = Modifier.weight(1f))

        // Botão Principal
        Button(
            onClick = { viewModel.toggleStreaming() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isStreaming) Color.DarkGray else Color(0xFFE91E63)
            )
        ) {
            Text(
                text = if (isStreaming) "PARAR TRANSMISSÃO" else "INICIAR AO VIVO",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MixerControl(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 12.sp, color = Color.LightGray)
            Text(text = "${(value * 100).toInt()}%", fontSize = 12.sp, color = Color.LightGray)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = Color(0xFFE91E63), activeTrackColor = Color(0xFFE91E63))
        )
    }
}
