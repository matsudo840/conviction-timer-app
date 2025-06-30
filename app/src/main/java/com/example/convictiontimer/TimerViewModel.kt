package com.example.convictiontimer

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class TimerViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val _timerText = MutableLiveData("00:00")
    val timerText: LiveData<String> = _timerText

    private val _currentRep = MutableLiveData(0)
    val currentRep: LiveData<Int> = _currentRep

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _totalRepsInput = MutableLiveData("")
    val totalRepsInput: LiveData<String> = _totalRepsInput

    private var tts: TextToSpeech? = null
    private var timerJob: Job? = null
    private var totalReps: Int = 0
    private var totalElapsedTime: Int = 0 // Total elapsed time in seconds
    private var startTime: Long = 0L // Added for precise timing
    private val repetitionDurationSeconds = 6 // 2 (Eccentric) + 1 (Hold) + 2 (Concentric) + 1 (Pause)

    init {
        tts = TextToSpeech(getApplication(), this)
    }

    fun setTotalRepsInput(value: String) {
        _totalRepsInput.value = value
        totalReps = value.toIntOrNull() ?: 0
    }

    fun incrementTotalReps() {
        totalReps++
        _totalRepsInput.value = totalReps.toString()
    }

    fun decrementTotalReps() {
        if (totalReps > 0) {
            totalReps--
            _totalRepsInput.value = totalReps.toString()
        }
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
        if (_isRunning.value == true || totalReps <= 0) return

        _isRunning.value = true
        _currentRep.value = 0
        totalElapsedTime = 0 // Reset total elapsed time
        startTime = System.currentTimeMillis() // Record start time

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastSpokenRep = 0
            for (second in 0 until (totalReps * repetitionDurationSeconds)) {
                val targetTime = startTime + (second * 1000L)
                val currentTime = System.currentTimeMillis()
                val delayMillis = targetTime - currentTime

                if (delayMillis > 0) {
                    delay(delayMillis)
                }

                totalElapsedTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                _timerText.value = formatTime(totalElapsedTime)

                val currentRepNumber = (totalElapsedTime / repetitionDurationSeconds) + 1
                if (currentRepNumber > lastSpokenRep && currentRepNumber <= totalReps) {
                    _currentRep.value = currentRepNumber
                    speakRepetition(currentRepNumber)
                    lastSpokenRep = currentRepNumber
                }
            }
            _isRunning.value = false
            _timerText.value = "Done!"
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
        _totalRepsInput.value = ""
        totalReps = 0
        totalElapsedTime = 0
        startTime = 0L
    }

    private fun speakRepetition(rep: Int) {
        Log.d("TTS", "Speaking repetition: $rep")
        val words = arrayOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
        if (rep > 0 && rep <= words.size) {
            tts?.speak(words[rep - 1], TextToSpeech.QUEUE_FLUSH, null, "")
        } else {
            tts?.speak("Repetition $rep", TextToSpeech.QUEUE_FLUSH, null, "")
        }
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
        timerJob?.cancel()
    }
}