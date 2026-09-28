package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

class MicrophoneRecorder(private val context: Context) {

    private val tag = "MicrophoneRecorder"
    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude = _amplitude.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText = _recognizedText.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onPcmChunkCaptured: ((ByteArray) -> Unit)? = null

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    init {
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(tag, "SpeechRecognizer: Ready for speech")
                    }
                    override fun onBeginningOfSpeech() {
                        Log.d(tag, "SpeechRecognizer: User started speaking")
                    }
                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2 to 10 dB to 0.0 .. 1.0 range
                        val norm = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                        _amplitude.value = norm
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {
                        buffer?.let { onPcmChunkCaptured?.invoke(it) }
                    }
                    override fun onEndOfSpeech() {
                        Log.d(tag, "SpeechRecognizer: End of speech detected")
                    }
                    override fun onError(error: Int) {
                        Log.w(tag, "SpeechRecognizer error: $error")
                        // If listening was active, we can softly restart or notify
                        _isRecording.value = false
                        _amplitude.value = 0f
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            _recognizedText.value = text
                            Log.d(tag, "Speech recognized: $text")
                            onSpeechRecognized?.invoke(text)
                        }
                        _isRecording.value = false
                        _amplitude.value = 0f
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _recognizedText.value = it
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    /**
     * Starts listening with SpeechRecognizer (supporting mixed Hindi, English, Hinglish)
     */
    fun startListening() {
        if (_isRecording.value) return
        _isRecording.value = true
        _recognizedText.value = ""

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US", "mr-IN", "gu-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            startAudioRecordLoop()
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognition: ${e.message}", e)
            _isRecording.value = false
        }
    }

    /**
     * Continuous AudioRecord streaming for live amplitude & raw PCM chunk extraction
     */
    private fun startAudioRecordLoop() {
        recordJob?.cancel()
        recordJob = scope.launch {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val bufferSize = maxOf(minBufferSize, 2048)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(tag, "AudioRecord could not initialize")
                    return@launch
                }

                audioRecord?.startRecording()
                val buffer = ByteArray(bufferSize)

                while (isActive && _isRecording.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        onPcmChunkCaptured?.invoke(chunk)

                        // Compute root mean square (RMS) amplitude
                        var sum = 0.0
                        var count = 0
                        var i = 0
                        while (i < read - 1) {
                            val sample = (chunk[i].toInt() and 0xFF) or (chunk[i + 1].toInt() shl 8)
                            sum += sample * sample
                            count++
                            i += 2
                        }
                        if (count > 0) {
                            val rms = sqrt(sum / count)
                            val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                            _amplitude.value = normalized
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "AudioRecord loop error: ${e.message}")
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                    audioRecord = null
                } catch (_: Exception) {}
            }
        }
    }

    fun stopListening() {
        _isRecording.value = false
        _amplitude.value = 0f
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        recordJob?.cancel()
    }

    fun release() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
