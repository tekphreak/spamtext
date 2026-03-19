package com.tekphreak.spamtext

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

object NotePlayer {
    fun playChord() {
        Thread {
            val sampleRate = 44100
            val noteDurationMs = 120
            val noteSamples = sampleRate * noteDurationMs / 1000
            val frequencies = listOf(523.25, 659.25, 783.99) // C5, E5, G5

            val bufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
                AudioTrack.MODE_STREAM
            )
            track.play()

            frequencies.forEach { freq ->
                val buf = ShortArray(noteSamples) { i ->
                    val amplitude = if (i < noteSamples * 0.7) 1.0
                                    else 1.0 - (i - noteSamples * 0.7) / (noteSamples * 0.3)
                    (Short.MAX_VALUE * 0.6 * amplitude *
                        sin(2 * PI * freq * i / sampleRate)).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
            }

            // Flush last note through hardware buffer before stop() discards it
            track.write(ShortArray(bufSize), 0, bufSize)

            track.stop()
            track.release()
        }.start()
    }
}
