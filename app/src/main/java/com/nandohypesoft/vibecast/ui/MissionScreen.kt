package com.nandohypesoft.vibecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MissionScreen(viewModel: MainViewModel, onComplete: () -> Unit) {
    var selectedMissionAds by remember { mutableIntStateOf(0) }
    var adsWatched by remember { mutableIntStateOf(0) }
    var rewardHours by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val isAdReady = AdManager.isAdReady
    val deviceId by viewModel.deviceId.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    // Tenta carregar o anúncio e monitora falhas
    LaunchedEffect(isAdReady, isAdmin) {
        if (!isAdReady && !isAdmin) {
            AdManager.loadInterstitial(context)
            // Tenta novamente a cada 5 segundos se não carregar
            while (!AdManager.isAdReady) {
                delay(5000)
                AdManager.loadInterstitial(context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isAdmin) {
            // Mensagem para Parceiros (Whitelisted)
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Yellow
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Acesso Especial Ativo",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Por ser um parceiro do app, você utilizará sem interrupções de anúncios.",
                fontSize = 16.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("ENTRAR NO ESTÚDIO")
            }
        } else if (selectedMissionAds == 0) {
            Text(
                text = "Escolha sua Missão",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Assista aos anúncios agora e transmita sem interrupções depois!",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            MissionCard("LOCUÇÃO RÁPIDA", "1 Anúncio", "1 Hora livre", Color(0xFF4CAF50)) {
                selectedMissionAds = 1
                rewardHours = 1
            }
            Spacer(modifier = Modifier.height(16.dp))
            MissionCard("TURNO DE TRABALHO", "5 Anúncios", "8 Horas livre", Color(0xFF2196F3)) {
                selectedMissionAds = 5
                rewardHours = 8
            }
            Spacer(modifier = Modifier.height(16.dp))
            MissionCard("ESTÚDIO 24H", "10 Anúncios", "24 Horas livre", Color(0xFFE91E63)) {
                selectedMissionAds = 10
                rewardHours = 24
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = onComplete) {
                Text("Continuar com interrupções (a cada 60 min)", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            // Tela de Progresso da Missão
            Text(
                text = "Progresso da Missão",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { adsWatched.toFloat() / selectedMissionAds },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Anúncios assistidos: $adsWatched / $selectedMissionAds",
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            val buttonText = when {
                !isAdReady -> "CARREGANDO ANÚNCIO..."
                selectedMissionAds == 1 -> "Assistir para ganhar 1 hora de programação"
                selectedMissionAds == 5 -> "Assistir para ganhar 8 horas sem interrupção"
                selectedMissionAds == 10 -> "Assistir para ganhar 24 horas sem interrupção"
                else -> "ASSISTIR PRÓXIMO ANÚNCIO"
            }

            Button(
                onClick = {
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        AdManager.showInterstitial(activity) {
                            adsWatched++
                            if (adsWatched >= selectedMissionAds) {
                                viewModel.addAdFreeTime(rewardHours)
                                onComplete()
                            }
                        }
                    }
                },
                enabled = isAdReady,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buttonText.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
            
            TextButton(onClick = { selectedMissionAds = 0; adsWatched = 0 }) {
                Text("Cancelar Missão", color = Color.Red.copy(alpha = 0.7f))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Privacidade: Este ID é anônimo e usado apenas para liberação de acesso remoto. Para excluir seu registro, entre em contato através do compartilhamento abaixo.",
            fontSize = 9.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "Olá, meu ID VibeCast é: $deviceId. Gostaria de solicitar acesso Pro.")
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Enviar ID via"))
            },
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: $deviceId",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Share, 
                    contentDescription = null, 
                    modifier = Modifier.size(14.dp), 
                    tint = Color.Yellow
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionCard(title: String, ads: String, reward: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.2f), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "$ads = $reward", fontSize = 12.sp, color = color)
            }
        }
    }
}
