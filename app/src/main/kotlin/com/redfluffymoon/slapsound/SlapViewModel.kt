package com.redfluffymoon.slapsound

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the main screen.
 *
 * @param isListening      True when the sensor is active.
 * @param slapDetected     True for a short period immediately after a slap – drives visual flash.
 * @param slapCount        Total slaps detected in the current session.
 * @param threshold        Current acceleration threshold (m/s²).
 * @param soundMode        Currently selected sound pack.
 * @param sensorAvailable  False when the device has no accelerometer.
 */
data class SlapUiState(
    val isListening: Boolean = false,
    val slapDetected: Boolean = false,
    val slapCount: Int = 0,
    val threshold: Float = SensorHelper.DEFAULT_THRESHOLD,
    val soundMode: SoundManager.SoundMode = SoundManager.SoundMode.PAIN,
    val sensorAvailable: Boolean = true
)

/**
 * ViewModel that owns the [SensorHelper] and [SoundManager] lifecycle,
 * keeps the UI state as a [StateFlow], and bridges sensor events to sound playback.
 */
class SlapViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SlapUiState())
    val uiState: StateFlow<SlapUiState> = _uiState.asStateFlow()

    private val soundManager = SoundManager(application)

    private val sensorHelper = SensorHelper(
        context = application,
        threshold = SensorHelper.DEFAULT_THRESHOLD,
        cooldownMs = SensorHelper.DEFAULT_COOLDOWN_MS,
        onSlapDetected = ::onSlapDetected
    )

    init {
        soundManager.load()
        _uiState.update { it.copy(sensorAvailable = sensorHelper.isSensorAvailable) }
    }

    // ── Public actions ───────────────────────────────────────────────────────

    /** Toggle the sensor on/off. */
    fun toggleListening() {
        val current = _uiState.value
        if (current.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        sensorHelper.start()
        _uiState.update { it.copy(isListening = true) }

        // Start background foreground service
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, SlapDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    fun stopListening() {
        sensorHelper.stop()
        _uiState.update { it.copy(isListening = false, slapDetected = false) }

        // Stop background service
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, SlapDetectionService::class.java))
    }

    /** Update the sensitivity threshold from the slider (0f..1f normalised value). */
    fun setThreshold(value: Float) {
        val mapped = SensorHelper.MIN_THRESHOLD +
                value * (SensorHelper.MAX_THRESHOLD - SensorHelper.MIN_THRESHOLD)
        sensorHelper.threshold = mapped
        _uiState.update { it.copy(threshold = mapped) }
    }

    /** Switch between pain and funny sound packs. */
    fun setSoundMode(mode: SoundManager.SoundMode) {
        _uiState.update { it.copy(soundMode = mode) }
    }

    /** Reset the slap counter for this session. */
    fun resetCount() {
        _uiState.update { it.copy(slapCount = 0) }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /** Called on the sensor thread when a slap is detected. */
    private fun onSlapDetected() {
        val mode = _uiState.value.soundMode
        soundManager.playRandomSound(mode)

        viewModelScope.launch {
            // Increment count and show flash; capture new count for the notification
            _uiState.update { it.copy(slapDetected = true, slapCount = it.slapCount + 1) }
            val newCount = _uiState.value.slapCount

            // Notify background service with the up-to-date count
            val intent = Intent(SlapDetectionService.ACTION_SLAP_DETECTED).apply {
                putExtra(SlapDetectionService.EXTRA_SLAP_COUNT, newCount)
                setPackage(getApplication<Application>().packageName)
            }
            getApplication<Application>().sendBroadcast(intent)

            // Keep the "SLAP DETECTED!" banner visible for 600 ms, then clear it
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(slapDetected = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorHelper.stop()
        soundManager.release()
    }
}
