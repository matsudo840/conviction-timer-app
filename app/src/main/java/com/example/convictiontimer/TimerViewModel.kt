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

    private var tts: TextToSpeech? = null
    private var timerJob: Job? = null

    init {
        tts = TextToSpeech(getApplication(), this)
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

    fun startTimer(totalReps: Int) {
        if (_isRunning.value == true || totalReps <= 0) return

        _isRunning.value = true
        _currentRep.value = 0

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (rep in 1..totalReps) {
                // Eccentric (2 seconds)
                for (i in 2 downTo 1) {
                    _timerText.value = "00:0" + i
                    delay(1000L)
                }
                // Bottom hold (1 second)
                _timerText.value = "00:01"
                delay(1000L)
                // Concentric (2 seconds)
                for (i in 2 downTo 1) {
                    _timerText.value = "00:0" + i
                    delay(1000L)
                }
                // Pause (1 second)
                _timerText.value = "00:01"
                delay(1000L)

                _currentRep.value = rep
                speakRepetition(rep)
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
    }

    private fun speakRepetition(rep: Int) {
        val words = arrayOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
        if (rep > 0 && rep <= words.size) {
            tts?.speak(words[rep - 1], TextToSpeech.QUEUE_FLUSH, null, "")
        } else {
            tts?.speak("Repetition \$rep", TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        timerJob?.cancel()
    }
}