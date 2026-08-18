package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SoundSynthesizer {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playTap() {
        scope.launch {
            // Crisp short high tone
            playTone(880, 40)
        }
    }

    fun playCorrect() {
        scope.launch {
            // Upward sweet chime
            playTone(523, 70) // C5
            playTone(659, 70) // E5
            playTone(784, 150) // G5
        }
    }

    fun playWrong() {
        scope.launch {
            // Sudden buzzy downbeat
            playTone(180, 250)
        }
    }

    fun playGameOver() {
        scope.launch {
            // Sad descending chord
            playTone(440, 120)
            playTone(392, 120)
            playTone(349, 120)
            playTone(293, 250)
        }
    }

    private fun playTone(frequencyHz: Int, durationMs: Int) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs / 1000.0 * sampleRate).toInt()
            val sample = DoubleArray(numSamples)
            val generatedSnd = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                // Generate sine wave
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequencyHz.toDouble()))
                
                // Fade-out envelope at the last 20% of samples to prevent cracking/popping sounds
                val fadeOut = if (i > numSamples * 0.8) {
                    (numSamples - i).toDouble() / (numSamples * 0.2)
                } else {
                    1.0
                }
                
                // Keep peak amplitude moderate (0.5 max) to sound clean
                val valShort = (sample[i] * 16384 * fadeOut).toInt()
                generatedSnd[i] = valShort.toShort()
            }

            val bufferSize = numSamples * 2
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSnd, 0, numSamples)
            audioTrack.play()
            
            // Wait for static audio to complete playing before freeing track memory
            Thread.sleep(durationMs.toLong() + 20)
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
