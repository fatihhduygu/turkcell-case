package com.turkcell.bip.feature.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.webrtc.SurfaceViewRenderer

@Composable
fun ActiveCallScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActiveCallViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Create surfaces once and reuse — never recreate on recomposition
    val localSurface = remember { SurfaceViewRenderer(context) }
    val remoteSurface = remember { SurfaceViewRenderer(context) }

    // Setup surfaces and clean up when screen leaves composition
    DisposableEffect(Unit) {
        viewModel.onIntent(ActiveCallIntent.SetupRemoteSurface(remoteSurface))
        viewModel.onIntent(ActiveCallIntent.SetupLocalSurface(localSurface))
        onDispose {
            runCatching { localSurface.release() }
            runCatching { remoteSurface.release() }
        }
    }

    LaunchedEffect(Unit) {
        for (effect in viewModel.effect) {
            when (effect) {
                ActiveCallEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {

        // Remote video — full screen background
        AndroidView(
            factory = { remoteSurface },
            modifier = Modifier.fillMaxSize()
        )

        // Local video — top right corner
        Box(
            modifier = Modifier
                .size(108.dp, 144.dp)
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { localSurface },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Controls — bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                onClick = { viewModel.onIntent(ActiveCallIntent.ToggleMute) },
                containerColor = if (uiState.isMuted) Color(0xFFE53935) else Color(0xFF424242),
                iconResId = if (uiState.isMuted)
                    android.R.drawable.ic_lock_silent_mode
                else
                    android.R.drawable.ic_lock_silent_mode_off,
                contentDescription = "Mute"
            )

            CallControlButton(
                onClick = { viewModel.onIntent(ActiveCallIntent.EndCall) },
                containerColor = Color(0xFFE53935),
                iconResId = android.R.drawable.ic_menu_close_clear_cancel,
                contentDescription = "End Call",
                size = 72.dp
            )

            CallControlButton(
                onClick = { viewModel.onIntent(ActiveCallIntent.SwitchCamera) },
                containerColor = Color(0xFF424242),
                iconResId = android.R.drawable.ic_menu_camera,
                contentDescription = "Switch Camera"
            )
        }

        // Muted indicator
        if (uiState.isMuted) {
            Text(
                text = "Mikrofon Kapalı",
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CallControlButton(
    onClick: () -> Unit,
    containerColor: Color,
    iconResId: Int,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        colors = IconButtonDefaults.iconButtonColors(containerColor = containerColor)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
