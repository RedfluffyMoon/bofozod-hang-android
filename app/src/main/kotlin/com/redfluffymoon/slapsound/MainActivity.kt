package com.redfluffymoon.slapsound

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.redfluffymoon.slapsound.ui.theme.SlapSoundTheme

/**
 * Main (and only) activity.  The UI is built entirely with Jetpack Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: SlapViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Permission result is informational; we don't block the main feature on it
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SlapSoundTheme {
                val uiState by viewModel.uiState.collectAsState()
                SlapScreen(
                    uiState = uiState,
                    onToggle = viewModel::toggleListening,
                    onThresholdChange = viewModel::setThreshold,
                    onModeChange = viewModel::setSoundMode,
                    onResetCount = viewModel::resetCount
                )
            }
        }
    }
}

// ── Composable UI ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlapScreen(
    uiState: SlapUiState,
    onToggle: () -> Unit,
    onThresholdChange: (Float) -> Unit,
    onModeChange: (SoundManager.SoundMode) -> Unit,
    onResetCount: () -> Unit
) {
    // Background flashes red/orange when a slap is detected
    val backgroundColor by animateColorAsState(
        targetValue = if (uiState.slapDetected)
            Color(0xFFFF8C42)
        else
            MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 300),
        label = "background"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🦴 ${stringResource(R.string.app_name)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Sensor availability warning
                if (!uiState.sensorAvailable) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.no_sensor),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Status banner
                StatusBanner(slapDetected = uiState.slapDetected, isListening = uiState.isListening)

                // Slap counter
                SlapCounter(count = uiState.slapCount, onReset = onResetCount)

                // Start / Stop button
                Button(
                    onClick = onToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState.sensorAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isListening)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (uiState.isListening) stringResource(R.string.stop) else stringResource(R.string.start),
                        fontSize = 18.sp
                    )
                }

                // Sensitivity slider
                SensitivityCard(
                    threshold = uiState.threshold,
                    onThresholdChange = onThresholdChange
                )

                // Sound mode selector
                SoundModeCard(
                    currentMode = uiState.soundMode,
                    onModeChange = onModeChange
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(slapDetected: Boolean, isListening: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                slapDetected -> MaterialTheme.colorScheme.error
                isListening -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        StatusBannerContent(slapDetected = slapDetected, isListening = isListening)
    }
}

@Composable
private fun StatusBannerContent(slapDetected: Boolean, isListening: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = slapDetected,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Text(
                text = stringResource(R.string.status_slap_detected),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onError,
                textAlign = TextAlign.Center
            )
        }
        AnimatedVisibility(
            visible = !slapDetected,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Text(
                text = if (isListening) stringResource(R.string.status_listening) else stringResource(R.string.status_idle),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (isListening)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SlapCounter(count: Int, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.slap_count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "$count",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}

@Composable
private fun SensitivityCard(threshold: Float, onThresholdChange: (Float) -> Unit) {
    // Convert absolute threshold back to 0..1 normalised for the slider
    val sliderValue = (threshold - SensorHelper.MIN_THRESHOLD) /
            (SensorHelper.MAX_THRESHOLD - SensorHelper.MIN_THRESHOLD)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.sensitivity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.threshold_label, threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when {
                        threshold < 12f -> stringResource(R.string.sensitivity_very)
                        threshold < 20f -> stringResource(R.string.sensitivity_normal)
                        threshold < 30f -> stringResource(R.string.sensitivity_less)
                        else -> stringResource(R.string.sensitivity_hard)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = onThresholdChange,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.sensitivity_most),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sensitivity_least),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SoundModeCard(
    currentMode: SoundManager.SoundMode,
    onModeChange: (SoundManager.SoundMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.sound_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(8.dp))
            SoundManager.SoundMode.entries.forEach { mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeChange(mode) }
                    )
                    val label = when (mode) {
                        SoundManager.SoundMode.PAIN -> stringResource(R.string.mode_pain)
                        SoundManager.SoundMode.FUNNY -> stringResource(R.string.mode_funny)
                    }
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun SlapScreenPreview() {
    SlapSoundTheme {
        SlapScreen(
            uiState = SlapUiState(isListening = true, slapCount = 3),
            onToggle = {},
            onThresholdChange = {},
            onModeChange = {},
            onResetCount = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SlapScreenSlapDetectedPreview() {
    SlapSoundTheme {
        SlapScreen(
            uiState = SlapUiState(isListening = true, slapDetected = true, slapCount = 7),
            onToggle = {},
            onThresholdChange = {},
            onModeChange = {},
            onResetCount = {}
        )
    }
}
