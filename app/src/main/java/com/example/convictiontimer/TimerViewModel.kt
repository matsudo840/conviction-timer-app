package com.example.convictiontimer

import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class TimerViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _currentRep = MutableLiveData(0)
    val currentRep: LiveData<Int> = _currentRep

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _totalReps = MutableStateFlow(0)
    val totalReps: StateFlow<Int> = _totalReps.asStateFlow()

    private var tts: TextToSpeech? = null
    private var soundPool: SoundPool? = null
    private var countSoundId: Int = 0
    private var intervalSoundId: Int = 0
    private var timerJob: Job? = null
    private var startTime: Long = 0L
    private val repetitionDurationSeconds = 6

    init {
        tts = TextToSpeech(getApplication(), this)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        countSoundId = soundPool!!.load(getApplication(), R.raw.count, 1)
        intervalSoundId = soundPool!!.load(getApplication(), R.raw.interval, 1)
    }

    fun incrementTotalReps() {
        _totalReps.update { it + 1 }
    }

    fun decrementTotalReps() {
        _totalReps.update { if (it > 0) it - 1 else 0 }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            } else {
                Log.d("TTS", "TTS Initialized successfully")
            }
        } else {
            Log.e("TTS", "TTS Initialization failed.")
        }
    }

    fun startTimer() {
        val currentTotalReps = _totalReps.value
        if (_isRunning.value == true || currentTotalReps <= 0) return

        _isRunning.value = true
        _currentRep.value = 0
        startTime = System.currentTimeMillis()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsedSeconds = 0
            val totalDuration = (currentTotalReps + 1) * repetitionDurationSeconds

            while (_isRunning.value == true && elapsedSeconds < totalDuration) {
                val targetTime = startTime + (elapsedSeconds * 1000L)
                val delayMillis = targetTime - System.currentTimeMillis()
                if (delayMillis > 0) {
                    delay(delayMillis)
                }

                if (_isRunning.value == false) break

                // Timer display logic
                if (elapsedSeconds < repetitionDurationSeconds) {
                    // During the initial "Ready" phase, keep the timer at 00:00
                    if (elapsedSeconds == 0) {
                        _timerText.postValue("00:00")
                    }
                } else {
                    // After the "Ready" phase, start counting up.
                    // elapsedSeconds=6 should be 1, 7 should be 2, etc.
                    _timerText.postValue(formatTime(elapsedSeconds - repetitionDurationSeconds + 1))
                }

                val secondInRep = elapsedSeconds % repetitionDurationSeconds

                // Voice and Rep Count Logic
                if (secondInRep == 0) { // Actions at 0s, 6s, 12s, etc.
                    val repCycleIndex = elapsedSeconds / repetitionDurationSeconds // 0, 1, 2...

                    if (repCycleIndex == 0) {
                        // First rep cycle (starts at 0s), rep count is 0
                        speak("Ready")
                    } else {
                        // Subsequent rep cycles (e.g., at 6s, repCycleIndex is 1)
                        val repNumber = repCycleIndex // This will be 1, 2, 3...
                        _currentRep.postValue(repNumber)
                        speak(repNumber.toString())
                    }
                }

                // Sound effect logic (remains unchanged)
                when (secondInRep) {
                    0, 1, 3, 4 -> playIntervalSound() // . (dot)
                    2, 5 -> playCountSound()   // * (star)
                }

                elapsedSeconds++
            }

            // Timer completion logic
            if (_isRunning.value == true) {
                // Wait for the final second to fully elapse
                val finalTargetTime = startTime + (totalDuration * 1000L)
                val finalDelay = finalTargetTime - System.currentTimeMillis()
                if (finalDelay > 0) {
                    delay(finalDelay)
                }

                // Final state update
                _currentRep.postValue(currentTotalReps)
                _timerText.postValue("Finish!")
                speak("Finish")
                _isRunning.postValue(false)
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isRunning.value = false
    }

    fun resetTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        _timerText.value = "00:00"
        _currentRep.value = 0
        _totalReps.value = 0
        startTime = 0L
    }

    // A single, simplified speak function
    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun playCountSound() {
        soundPool?.play(countSoundId, 1f, 1f, 0, 0, 1f)
    }

    private fun playIntervalSound() {
        soundPool?.play(intervalSoundId, 1f, 1f, 0, 0, 1f)
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        soundPool?.release()
        soundPool = null
        timerJob?.cancel()
    }
}