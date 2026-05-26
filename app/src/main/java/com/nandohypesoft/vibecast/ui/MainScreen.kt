package com.nandohypesoft.vibecast.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.nandohypesoft.vibecast.R
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel, 
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val isStreaming by viewModel.isStreaming.collectAsState()
    val isAdFree by viewModel.isAdFree.collectAsState()
    val isBannersBlocked by viewModel.isBannersBlocked.collectAsState()
    val isInterstitialsBlocked by viewModel.isInterstitialsBlocked.collectAsState()
    val adFreeRemaining by viewModel.adFreeTimeRemaining.collectAsState()
    val shouldShowAd by viewModel.shouldShowInterstitial.collectAsState()
    val status by viewModel.status.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isAdReady = AdManager.isAdReady

    // Pré-carrega o anúncio quando entra na tela
    LaunchedEffect(isAdReady) {
        if (!isAdReady) {
            AdManager.loadInterstitial(context)
        }
    }

    // Lógica para exibir anúncio intersticial a cada 60 min se não estiver bloqueado
    if (shouldShowAd && !isInterstitialsBlocked) {
        LaunchedEffect(Unit) {
            val activity = context as? android.app.Activity
            if (activity != null) {
                AdManager.showInterstitial(activity) {
                    viewModel.markInterstitialShown()
                }
            } else {
                viewModel.markInterstitialShown()
            }
        }
    }
    val countdown by viewModel.reconnectCountdown.collectAsState()
    val micVuMeter by viewModel.micVuMeter.collectAsState()
    val musicVuMeter by viewModel.musicVuMeter.collectAsState()
    val micVolume by viewModel.micVolume.collectAsState()
    val micEnabled by viewModel.micEnabled.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()
    val echoEnabled by viewModel.echoEnabled.collectAsState()
    val echoLevel by viewModel.echoLevel.collectAsState()
    val boostEnabled by viewModel.boostEnabled.collectAsState()
    val gateEnabled by viewModel.gateEnabled.collectAsState()
    val currentSongTitle by viewModel.currentSongTitle.collectAsState()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val vignettes by viewModel.vignettes.collectAsState()
    val musicPosition by viewModel.musicPosition.collectAsState()
    val musicDuration by viewModel.musicDuration.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()

    var activeSlotIndex by remember { mutableIntStateOf(-1) }
    var activeVignetteIndex by remember { mutableIntStateOf(-1) }
    
    var showTimeRemaining by remember { mutableStateOf(false) }
    LaunchedEffect(isMusicPlaying) {
        if (isMusicPlaying) {
            while (true) {
                delay(4000)
                showTimeRemaining = !showTimeRemaining
            }
        } else {
            showTimeRemaining = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            when {
                activeSlotIndex != -1 -> {
                    viewModel.setTrackAt(activeSlotIndex, uris.first())
                    activeSlotIndex = -1
                }
                activeVignetteIndex != -1 -> {
                    viewModel.setVignetteAt(activeVignetteIndex, uris.first())
                    activeVignetteIndex = -1
                }
                else -> {
                    viewModel.playMusic(uris)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_vibecast_icon),
                            contentDescription = "Logo VibeCast",
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            if (selectedProfile != null && selectedProfile!!.name.isNotBlank()) {
                                Text(text = selectedProfile!!.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    text = "Sua Rádio tocando e você no comando",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Text(text = "VibeCast Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    text = "Sua Rádio tocando e você no comando",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!isAdFree) {
                        IconButton(onClick = onNavigateToLogin) {
                            Icon(Icons.Default.Star, contentDescription = "Ganhar Tempo Sem Anúncios", tint = Color.Yellow)
                        }
                    } else if (adFreeRemaining > 0) {
                        val hours = (adFreeRemaining / (1000 * 60 * 60))
                        val minutes = (adFreeRemaining / (1000 * 60)) % 60
                        val seconds = (adFreeRemaining / 1000) % 60
                        
                        TextButton(onClick = onNavigateToLogin) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer, 
                                    contentDescription = null, 
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "%02dh %02dm %02ds".format(hours, minutes, seconds),
                                    color = Color.Yellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DigitalControlColumn(
                    label = "MICROFONE",
                    volume = micVolume,
                    vuLevel = micVuMeter,
                    enabled = micEnabled,
                    onVolumeChange = { viewModel.setMicVolume(it) },
                    onToggle = { viewModel.toggleMic() },
                    icon = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff
                )

                DigitalControlColumn(
                    label = "MÚSICA",
                    volume = musicVolume,
                    vuLevel = musicVuMeter,
                    enabled = true,
                    onVolumeChange = { viewModel.setMusicVolume(it) },
                    onToggle = null,
                    icon = Icons.Default.MusicNote
                )
            }

            if (!isBannersBlocked) {
                Spacer(modifier = Modifier.height(16.dp))
                AdBanner(AdManager.BANNER_ID)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Processamento de Voz", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        EffectToggle(
                            label = "RADIO PUNCH",
                            enabled = boostEnabled,
                            onToggle = { viewModel.toggleBoost() },
                            icon = Icons.Default.Campaign
                        )
                        EffectToggle(
                            label = "NOISE GATE",
                            enabled = gateEnabled,
                            onToggle = { viewModel.toggleGate() },
                            icon = Icons.Default.FilterAlt
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GraphicEq, 
                                contentDescription = null, 
                                tint = if (echoEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("ECHO / DELAY", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(checked = echoEnabled, onCheckedChange = { viewModel.toggleEcho() })
                    }
                    if (echoEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("INTENSIDADE", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(80.dp))
                            Slider(
                                value = echoLevel,
                                onValueChange = { viewModel.setEchoLevel(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (!isBannersBlocked) {
                Spacer(modifier = Modifier.height(16.dp))
                AdBanner(AdManager.BANNER_ID)
            }

            Spacer(modifier = Modifier.height(32.dp))

            StreamingControls(isStreaming, status, countdown) { viewModel.toggleStream() }

            if (isStreaming && !status.contains("SYNC")) {
                TextButton(onClick = { viewModel.simulateError() }) {
                    Text("SIMULAR QUEDA DE CONEXÃO", color = Color.Gray, fontSize = 10.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            VignettesPanel(
                vignettes = vignettes,
                onAddVignette = { index ->
                    activeVignetteIndex = index
                    launcher.launch(arrayOf("audio/*"))
                },
                onRemoveVignette = { index -> viewModel.removeVignetteAt(index) },
                onPlayVignette = { index -> viewModel.playVignette(index) }
            )

            if (!isBannersBlocked) {
                Spacer(modifier = Modifier.height(16.dp))
                AdBanner(AdManager.BANNER_ID)
            }

            Spacer(modifier = Modifier.height(32.dp))

            MusicPlayerSection(
                viewModel = viewModel,
                currentSongTitle = currentSongTitle,
                isPlaying = isMusicPlaying,
                playlist = playlist,
                musicPosition = musicPosition,
                musicDuration = musicDuration,
                showTimeRemaining = showTimeRemaining,
                onAddTrackAt = { index ->
                    activeSlotIndex = index
                    launcher.launch(arrayOf("audio/*"))
                },
                onRemoveTrackAt = { index -> viewModel.removeTrackAt(index) },
                onTogglePlay = { viewModel.toggleMusic() },
                onNext = { viewModel.nextSong() },
                onPrevious = { viewModel.previousSong() },
                onClear = { viewModel.clearPlaylist() }
            )

            if (!isBannersBlocked) {
                Spacer(modifier = Modifier.height(24.dp))
                AdBanner(AdManager.BANNER_ID)
            }
        }
    }
}

@Composable
fun AdBanner(adUnitId: String) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun VignettesPanel(
    vignettes: List<android.net.Uri?>,
    onAddVignette: (Int) -> Unit,
    onRemoveVignette: (Int) -> Unit,
    onPlayVignette: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Vinhetas", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until 5) {
                val uri = vignettes.getOrNull(i)
                VignetteButton(
                    index = i,
                    isSet = uri != null,
                    onClick = {
                        if (uri == null) onAddVignette(i) else onPlayVignette(i)
                    },
                    onLongClick = {
                        if (uri != null) onRemoveVignette(i)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VignetteButton(
    index: Int,
    isSet: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                color = if (isSet) MaterialTheme.colorScheme.primary else Color.DarkGray,
                shape = MaterialTheme.shapes.medium
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "V${index + 1}",
            color = if (isSet) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun DigitalControlColumn(
    label: String,
    volume: Float,
    vuLevel: Float,
    enabled: Boolean,
    onVolumeChange: (Float) -> Unit,
    onToggle: (() -> Unit)?,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            DigitalVuMeter(vuLevel)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            VolumeKnob(
                value = volume,
                enabled = enabled,
                onValueChange = onVolumeChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (onToggle != null) {
            FilledTonalIconButton(
                onClick = onToggle,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray
                )
            ) {
                Icon(icon, contentDescription = null)
            }
        } else {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp).padding(12.dp))
        }
    }
}

@Composable
fun VolumeKnob(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = Color.DarkGray
    
    val currentVolume by rememberUpdatedState(value)
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val sensitivity = 0.005f
                        val newValue = (currentVolume - dragAmount.y * sensitivity).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size / 2f
                val radius = size.minDimension / 2f - 10.dp.toPx()
                
                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                
                drawArc(
                    color = if (enabled) primaryColor else Color.Gray,
                    startAngle = 135f,
                    sweepAngle = 270f * value,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                
                val angle = (135f + 270f * value) * PI / 180f
                val dotRadius = radius - 15.dp.toPx()
                val dotX = center.width + dotRadius * cos(angle).toFloat()
                val dotY = center.height + dotRadius * sin(angle).toFloat()
                
                drawCircle(
                    color = if (enabled) Color.White else Color.Gray,
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(dotX, dotY)
                )
            }
            Text(
                text = "${(value * 100).toInt()}%",
                color = if (enabled) Color.White else Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DigitalVuMeter(value: Float) {
    val barCount = 15
    val activeBars = (value * barCount).roundToInt().coerceIn(0, barCount)
    
    Column(
        modifier = Modifier
            .width(12.dp)
            .height(100.dp)
            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in barCount downTo 1) {
            val color = when {
                i > 13 -> Color.Red
                i > 10 -> Color.Yellow
                else -> Color.Green
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (i <= activeBars) color else color.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun StreamingControls(isStreaming: Boolean, status: String, countdown: Int, onToggle: () -> Unit) {
    val isReconnecting = status.contains("RECONECTANDO")
    val buttonColor = when {
        isReconnecting -> Color(0xFFFF9800)
        isStreaming -> Color(0xFF388E3C)
        else -> Color(0xFFD32F2F)
    }

    val buttonText = when {
        isReconnecting -> "$status - ${countdown}s"
        isStreaming -> "AO VIVO"
        else -> "ENTRAR AO VIVO"
    }

    val fontSize = if (isReconnecting) 13.sp else 18.sp

    val icon = when {
        isReconnecting -> Icons.Default.Sync
        isStreaming -> Icons.Default.WifiTethering
        else -> Icons.Default.WifiOff
    }

    Button(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isStreaming || isReconnecting) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.White, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Icon(
                icon, 
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = buttonText.uppercase(),
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun EffectToggle(
    label: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onToggle,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray
            )
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray)
        }
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MusicPlayerSection(
    viewModel: MainViewModel,
    currentSongTitle: String?,
    isPlaying: Boolean,
    playlist: List<android.net.Uri?>,
    musicPosition: Long,
    musicDuration: Long,
    showTimeRemaining: Boolean,
    onAddTrackAt: (Int) -> Unit,
    onRemoveTrackAt: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Programação (5 Slots)", color = Color.White, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClear) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
            
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
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
                
                IconButton(onClick = { onNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 5) {
                    val uri = playlist.getOrNull(i)
                    val isCurrent = uri != null && uri.lastPathSegment == currentSongTitle
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isCurrent) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                        else Color.Black.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${i + 1}.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.width(20.dp)
                            )

                            val displayText = if (isCurrent && showTimeRemaining) {
                                val remaining = (musicDuration - musicPosition).coerceAtLeast(0L)
                                val minutes = (remaining / 1000) / 60
                                val seconds = (remaining / 1000) % 60
                                "TERMINA EM: ${"%02d:%02d".format(minutes, seconds)}"
                            } else {
                                (if (isCurrent) "▶ " else "") + if (uri != null) {
                                    viewModel.getFileName(uri)
                                } else "Vazio"
                            }

                            Text(
                                text = displayText,
                                color = if (uri != null) Color.White else Color.DarkGray,
                                fontSize = 13.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )

                            if (uri != null) {
                                IconButton(onClick = { onRemoveTrackAt(i) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            } else {
                                IconButton(onClick = { onAddTrackAt(i) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
