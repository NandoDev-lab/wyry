package com.nandohypesoft.vibecast

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nandohypesoft.vibecast.ui.MainScreen
import com.nandohypesoft.vibecast.ui.MainViewModel
import com.nandohypesoft.vibecast.ui.SettingsScreen
import com.nandohypesoft.vibecast.ui.MissionScreen
import com.nandohypesoft.vibecast.ui.LoginScreen
import com.nandohypesoft.vibecast.ui.SplashScreen
import com.nandohypesoft.vibecast.ui.theme.VibeCastTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Inicializar Mobile Ads SDK
        MobileAds.initialize(this) {}
        
        requestPermissions()
        
        enableEdgeToEdge()
        setContent {
            VibeCastTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = viewModel()
                    
                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(
                                onTimeout = { 
                                    // Verifica se o usuário é Pro ou tem tempo Ad-Free
                                    val isAdFree = viewModel.isAdFree.value
                                    if (isAdFree) {
                                        navController.navigate("main") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("mission") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToLogin = { navController.navigate("mission") }
                            )
                        }
                        composable("mission") {
                            MissionScreen(
                                viewModel = viewModel,
                                onComplete = { 
                                    navController.navigate("main") {
                                        popUpTo("mission") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("login") { // Mantida apenas para acesso Admin via código
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 0)
        }
    }
}
