package com.filesalvage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.filesalvage.ui.*
import com.filesalvage.ui.theme.FileSalvageTheme
import com.filesalvage.viewmodels.Screen
import com.filesalvage.viewmodels.ScanViewModel

class MainActivity : ComponentActivity() {

    private val vm: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FileSalvageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    PermissionsScreen(
                        onAllGranted = { FileSalvageNavHost(vm) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileSalvageNavHost(vm: ScanViewModel) {
    val screen by vm.screen.collectAsState()

    // Animated crossfade navigation
    AnimatedContent(
        targetState  = screen,
        transitionSpec = {
            when (targetState) {
                is Screen.Home -> slideInHorizontally { -it } + fadeIn(tween(300)) togetherWith
                                  slideOutHorizontally { it } + fadeOut(tween(200))
                is Screen.Scanning -> fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                is Screen.Results  -> slideInHorizontally { it } + fadeIn(tween(300)) togetherWith
                                      slideOutHorizontally { -it } + fadeOut(tween(200))
                is Screen.Recovery -> fadeIn(tween(350)) togetherWith fadeOut(tween(250))
            }
        },
        label = "nav"
    ) { currentScreen ->
        when (currentScreen) {
            Screen.Home     -> HomeScreen(vm)
            Screen.Scanning -> ScanningScreen(vm)
            Screen.Results  -> ResultsScreen(vm)
            Screen.Recovery -> RecoveryScreen(vm)
        }
    }
}
