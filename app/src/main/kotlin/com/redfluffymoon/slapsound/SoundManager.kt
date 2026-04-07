package com.redfluffymoon.slapsound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Manages sound playback for slap events using [SoundPool] for low-latency audio.
 *
 * Two sound packs are available:
 *  - [SoundMode.PAIN]  – "ouch / pain" sounds
 *  - [SoundMode.FUNNY] – cartoon / funny sounds
 *
 * Call [load] once (e.g. in ViewModel init) and [release] when done.
 */
class SoundManager(private val context: Context) {

    enum class SoundMode { PAIN, FUNNY }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(audioAttributes)
        .build()

    // Maps resource ID → SoundPool stream ID (populated after load)
    private val painSoundIds = mutableListOf<Int>()
    private val funnySoundIds = mutableListOf<Int>()

    /** Raw resource IDs for each pack */
    private val painResources = listOf(
        R.raw.pain1,
        R.raw.pain2,
        R.raw.pain3,
        R.raw.pain4,
        R.raw.pain5
    )

    private val funnyResources = listOf(
        R.raw.funny1,
        R.raw.funny2,
        R.raw.funny3,
        R.raw.funny4,
        R.raw.funny5
    )

    /**
     * Load all sound resources into SoundPool.
     * Must be called before [playRandomSound].
     */
    fun load() {
        painResources.forEach { resId ->
            painSoundIds.add(soundPool.load(context, resId, 1))
        }
        funnyResources.forEach { resId ->
            funnySoundIds.add(soundPool.load(context, resId, 1))
        }
    }

    /**
     * Play a randomly chosen sound from the given [mode] pack.
     */
    fun playRandomSound(mode: SoundMode) {
        val ids = when (mode) {
            SoundMode.PAIN -> painSoundIds
            SoundMode.FUNNY -> funnySoundIds
        }
        if (ids.isEmpty()) return
        val soundId = ids.random()
        soundPool.play(
            soundId,
            /* leftVolume  */ 1f,
            /* rightVolume */ 1f,
            /* priority   */ 1,
            /* loop       */ 0,
            /* rate        */ 1f
        )
    }

    /**
     * Release all SoundPool resources. Call this when the manager is no longer needed.
     */
    fun release() {
        soundPool.release()
        painSoundIds.clear()
        funnySoundIds.clear()
    }
}
