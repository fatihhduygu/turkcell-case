package com.turkcell.bip.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class AppDestination : NavKey {
    @Serializable data object Splash : AppDestination()
    @Serializable data object Home : AppDestination()
    @Serializable data object IncomingCall : AppDestination()
    @Serializable data object OutgoingCall : AppDestination()
    @Serializable data object ActiveCall : AppDestination()
}
