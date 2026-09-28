package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sin

class AudioOutputPlayer(private val context: Context) {

    private val tag = "AudioOutputPlayer"
    private var audioTrack: AudioTrack? = null
    private var tts: TextToSpeech? = null
    private val isTtsReady = AtomicBoolean(false)

    private val playbackScope = CoroutineScope(Dispatchers.IO + Job())
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private var playbackJob: Job? = null
    private val isInterrupted = AtomicBoolean(false)

    var onPlaybackStarted: (() -> Unit)? = null
    var onPlaybackEnded: (() -> Unit)? = null

    init {
        initAudioTrack(24000)
        initTts()
        startQueueConsumer()
    }

    private fun initAudioTrack(sampleRate: Int) {
        try {
            audioTrack?.release()
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, sampleRate * 2)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
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
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            Log.d(tag, "AudioTrack initialized successfully at ${sampleRate}Hz")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AudioTrack: ${e.message}", e)
        }
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.15f) // Slightly higher, youthful expressive pitch for Arushi
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlaying.value = true
                        onPlaybackStarted?.invoke()
                    }

                    override fun onDone(utteranceId: String?) {
                        _isPlaying.value = false
                        onPlaybackEnded?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                        onPlaybackEnded?.invoke()
                    }
                })
                isTtsReady.set(true)
                Log.d(tag, "Native TTS ready as expressive audio fallback")
            } else {
                Log.w(tag, "TTS init returned status: $status")
            }
        }
    }

    private fun startQueueConsumer() {
        playbackJob = playbackScope.launch {
            for (chunk in audioQueue) {
                if (isInterrupted.get()) continue

                val track = audioTrack ?: continue
                try {
                    _isPlaying.value = true
                    onPlaybackStarted?.invoke()

                    if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        track.play()
                    }

                    var offset = 0
                    while (offset < chunk.size && !isInterrupted.get()) {
                        val written = track.write(chunk, offset, chunk.size - offset)
                        if (written > 0) {
                            offset += written
                        } else {
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error writing to AudioTrack: ${e.message}", e)
                } finally {
                    if (audioQueue.isEmpty) {
                        _isPlaying.value = false
                        onPlaybackEnded?.invoke()
                    }
                }
            }
        }
    }

    /**
     * Enqueues PCM audio chunk (16-bit PCM, 24kHz or 16kHz Mono) returned by Gemini Live
     */
    fun enqueuePcmChunk(pcmData: ByteArray) {
        isInterrupted.set(false)
        audioQueue.trySend(pcmData)
    }

    /**
     * Fallback speech playback when Gemini returns text modality or offline
     */
    fun speakText(text: String, languageCode: String = "hi") {
        stopPlayback()
        if (isTtsReady.get()) {
            val loc = when (languageCode.lowercase()) {
                "hindi", "hi" -> Locale("hi", "IN")
                "english", "en" -> Locale.US
                "marathi", "mr" -> Locale("mr", "IN")
                "gujarati", "gu" -> Locale("gu", "IN")
                "bengali", "bn" -> Locale("bn", "IN")
                "tamil", "ta" -> Locale("ta", "IN")
                "telugu", "te" -> Locale("te", "IN")
                else -> Locale("hi", "IN")
            }
            tts?.language = loc
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "arushi_utterance_${System.currentTimeMillis()}")
        }
    }

    /**
     * Interrupts and halts all current playback immediately, clearing queued chunks
     */
    fun stopPlayback() {
        isInterrupted.set(true)
        // Drain queued chunks
        while (!audioQueue.isEmpty) {
            audioQueue.tryReceive()
        }
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.w(tag, "AudioTrack flush warning: ${e.message}")
        }
        tts?.stop()
        _isPlaying.value = false
        onPlaybackEnded?.invoke()
    }

    /**
     * Plays a pure 440Hz diagnostic tone for 1.2 seconds through AudioTrack
     * (Section 15 & 44 Speaker Diagnostic Test)
     */
    suspend fun playDiagnosticTone() = withContext(Dispatchers.IO) {
        stopPlayback()
        val sampleRate = 24000
        val durationMs = 1200
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSnd = ByteArray(2 * numSamples)

        val frequency = 440.0 // Standard A4 concert pitch
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
            // Apply gentle fade-in and fade-out envelope to avoid audio clicks
            val envelope = when {
                i < 500 -> i / 500.0
                i > numSamples - 500 -> (numSamples - i) / 500.0
                else -> 1.0
            }
            val sampleVal = (sin(angle) * 30000 * envelope).toInt().toShort()
            generatedSnd[2 * i] = (sampleVal.toInt() and 0x00ff).toByte()
            generatedSnd[2 * i + 1] = ((sampleVal.toInt() and 0xff00) shr 8).toByte()
        }

        try {
            _isPlaying.value = true
            onPlaybackStarted?.invoke()

            initAudioTrack(sampleRate)
            audioTrack?.write(generatedSnd, 0, generatedSnd.size)
            Thread.sleep(durationMs.toLong())
        } catch (e: Exception) {
            Log.e(tag, "Failed to play diagnostic 440Hz tone: ${e.message}", e)
        } finally {
            _isPlaying.value = false
            onPlaybackEnded?.invoke()
        }
    }

    fun release() {
        stopPlayback()
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
        try {
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
