package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

object GameAudioEngine {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(Dispatchers.Default)
    var isMuted = false

    private val audioTrack: AudioTrack by lazy {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build().apply {
                play()
            }
    }

    private fun playPcm(samples: ShortArray) {
        if (isMuted) return
        scope.launch {
            try {
                audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_NON_BLOCKING)
            } catch (e: Exception) {
                // Ignore audio write errors
            }
        }
    }

    // 1. Block Mining Hit (Short dull tap)
    fun playMineHit() {
        val numSamples = (SAMPLE_RATE * 0.05f).toInt()
        val samples = ShortArray(numSamples)
        val rand = Random(42)
        for (i in 0 until numSamples) {
            val env = 1.0f - (i.toFloat() / numSamples)
            val noise = (rand.nextFloat() * 2f - 1f) * 0.5f
            val tone = sin(2.0 * Math.PI * 180.0 * (i.toDouble() / SAMPLE_RATE)).toFloat() * 0.5f
            samples[i] = ((noise + tone) * env * 12000).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(samples)
    }

    // 2. Block Break Crunch
    fun playBlockBreak() {
        val numSamples = (SAMPLE_RATE * 0.12f).toInt()
        val samples = ShortArray(numSamples)
        val rand = Random
        for (i in 0 until numSamples) {
            val env = (1.0f - (i.toFloat() / numSamples))
            val noise = (rand.nextFloat() * 2f - 1f)
            val tone = sin(2.0 * Math.PI * 120.0 * (i.toDouble() / SAMPLE_RATE)).toFloat() * 0.4f
            samples[i] = ((noise + tone) * env * env * 18000).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(samples)
    }

    // 3. Block Place Thud
    fun playBlockPlace() {
        val numSamples = (SAMPLE_RATE * 0.08f).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = 240.0 - t * 100.0
            val env = (1.0f - t) * (1.0f - t)
            val tone = sin(2.0 * Math.PI * freq * (i.toDouble() / SAMPLE_RATE)).toFloat()
            samples[i] = (tone * env * 22000).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(samples)
    }

    // 4. Jump
    fun playJump() {
        val numSamples = (SAMPLE_RATE * 0.1f).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = 180.0 + t * 240.0
            val env = 1.0f - t
            val tone = sin(2.0 * Math.PI * freq * (i.toDouble() / SAMPLE_RATE)).toFloat()
            samples[i] = (tone * env * 15000).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(samples)
    }

    // 5. Craft Success Chime
    fun playCraftSuccess() {
        val numSamples = (SAMPLE_RATE * 0.25f).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = if (t < 0.5f) 523.25 else 659.25 // C5 to E5
            val env = (1.0f - (t % 0.5f) * 2f)
            val tone = sin(2.0 * Math.PI * freq * (i.toDouble() / SAMPLE_RATE)).toFloat()
            samples[i] = (tone * env * 16000).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(samples)
    }

    // 6. Complex Structure Magical Build Fanfare
    fun playStructureBuild() {
        val numSamples = (SAMPLE_RATE * 0.45f).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = when {
                t < 0.25f -> 440.0 // A4
                t < 0.50f -> 554.37 // C#5
                t < 0.75f -> 659.25 // E5
                else -> 880.0 // A5
            }
            val env = (1.0f - (t % 0.25f) * 3f).coerceIn(0f, 1f)
            val tone = sin(2.0 * Math.PI * freq * (i.toDouble() / SAMPLE_RATE)).toFloat()
            samples[i] = (tone * env * 18000).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(samples)
    }

    // 7. Footstep
    fun playFootstep() {
        val numSamples = (SAMPLE_RATE * 0.04f).toInt()
        val samples = ShortArray(numSamples)
        val rand = Random
        for (i in 0 until numSamples) {
            val env = 1.0f - (i.toFloat() / numSamples)
            val noise = (rand.nextFloat() * 2f - 1f) * 0.3f
            samples[i] = (noise * env * 8000).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(samples)
    }
}
