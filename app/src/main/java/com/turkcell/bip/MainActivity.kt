package com.turkcell.bip

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.turkcell.bip.feature.call.ActiveCallScreen
import com.turkcell.bip.feature.client.OutgoingCallScreen
import com.turkcell.bip.feature.home.HomeScreen
import com.turkcell.bip.feature.host.IncomingCallScreen
import com.turkcell.bip.feature.splash.SplashScreen
import com.turkcell.bip.navigation.AppDestination
import com.turkcell.bip.navigation.popOrFinish
import com.turkcell.bip.ui.theme.BipTheme
import dagger.hilt.android.AndroidEntryPoint

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BipTheme {
                val context = LocalContext.current
                val activity = remember(context) { context as? Activity }
                val backStack = rememberNavBackStack(AppDestination.Splash)

                // Track whether required permissions are granted
                var permissionsGranted by remember {
                    mutableStateOf(REQUIRED_PERMISSIONS.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    })
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    permissionsGranted = results.values.all { it }
                }

                // Request permissions on first launch
                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(REQUIRED_PERMISSIONS)
                    }
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.popOrFinish(activity) },
                    entryProvider = entryProvider {
                        entry<AppDestination.Splash> {
                            SplashScreen(
                                onNavigateToHome = {
                                    backStack.add(AppDestination.Home)
                                    backStack.remove(AppDestination.Splash)
                                }
                            )
                        }
                        entry<AppDestination.Home> {
                            HomeScreen(
                                onNavigateToOutgoingCall = {
                                    backStack.add(AppDestination.OutgoingCall)
                                },
                                onNavigateToIncomingCall = {
                                    backStack.add(AppDestination.IncomingCall)
                                }
                            )
                        }
                        entry<AppDestination.IncomingCall> {
                            IncomingCallScreen(
                                onNavigateToActiveCall = {
                                    backStack.removeLastOrNull()
                                    backStack.add(AppDestination.ActiveCall)
                                    startCallService()
                                },
                                onNavigateBack = { backStack.popOrFinish(activity) }
                            )
                        }
                        entry<AppDestination.OutgoingCall> {
                            OutgoingCallScreen(
                                onNavigateToActiveCall = {
                                    backStack.removeLastOrNull()
                                    backStack.add(AppDestination.ActiveCall)
                                    startCallService()
                                },
                                onNavigateBack = { backStack.popOrFinish(activity) }
                            )
                        }
                        entry<AppDestination.ActiveCall> {
                            ActiveCallScreen(
                                onNavigateBack = {
                                    stopCallService()
                                    backStack.popOrFinish(activity)
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun startCallService() {
        val intent = android.content.Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun stopCallService() {
        val intent = android.content.Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_STOP
        }
        startService(intent)
    }
}
