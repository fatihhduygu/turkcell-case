package com.turkcell.bip

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.turkcell.bip.feature.client.ClientScreen
import com.turkcell.bip.feature.home.HomeScreen
import com.turkcell.bip.feature.host.HostScreen
import com.turkcell.bip.feature.splash.SplashScreen
import com.turkcell.bip.navigation.AppDestination
import com.turkcell.bip.navigation.popOrFinish
import com.turkcell.bip.ui.theme.BipTheme
import dagger.hilt.android.AndroidEntryPoint

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
                                onNavigateToHost = { backStack.add(AppDestination.Host) },
                                onNavigateToClient = { backStack.add(AppDestination.Client) }
                            )
                        }
                        entry<AppDestination.Host> {
                            HostScreen(onBack = { backStack.popOrFinish(activity) })
                        }
                        entry<AppDestination.Client> {
                            ClientScreen(onBack = { backStack.popOrFinish(activity) })
                        }
                    }
                )
            }
        }
    }
}
