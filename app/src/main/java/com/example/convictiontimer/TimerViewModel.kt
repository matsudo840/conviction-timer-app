package com.example.convictiontimer

import android.app.Application
import android.media.MediaPlayer
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
    private var countPlayer: MediaPlayer? = null
    private var intervalPlayer: MediaPlayer? = null
    private var timerJob: Job? = null
    private var totalReps: Int = 0
    private var startTime: Long = 0L
    private val repetitionDurationSeconds = 6

    init {
        tts = TextToSpeech(getApplication(), this)
        countPlayer = MediaPlayer.create(getApplication(), R.raw.count)
        intervalPlayer = MediaPlayer.create(getApplication(), R.raw.interval)
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
        startTime = System.currentTimeMillis()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsedSeconds = 0
            val totalDuration = totalReps * repetitionDurationSeconds

            while (_isRunning.value == true && elapsedSeconds < totalDuration) {
                // Post UI updates from the background thread
                _timerText.postValue(formatTime(elapsedSeconds))

                // Handle Repetition and TTS announcement at the end of a rep
                val secondInRep = elapsedSeconds % repetitionDurationSeconds
                if (secondInRep == 0) {
                    if (elapsedSeconds > 0) {
                        val completedRepNumber = elapsedSeconds / repetitionDurationSeconds
                        if (completedRepNumber <= totalReps) {
                            _currentRep.postValue(completedRepNumber)
                            speakRepetition(completedRepNumber)
                        }
                    }
                }

                // Handle sound pattern playback
                when (secondInRep) {
                    0, 1, 3, 4 -> playIntervalSound() // . (dot)
                    2, 5 -> playCountSound()   // * (star)
                }

                // Increment the second counter
                elapsedSeconds++

                // Accurately delay for 1 second
                val targetTime = startTime + (elapsedSeconds * 1000L)
                val delayMillis = targetTime - System.currentTimeMillis()
                if (delayMillis > 0) {
                    delay(delayMillis)
                }
            }

            // Timer finished or was paused
            if (_isRunning.value == true) {
                _isRunning.postValue(false)
                _timerText.postValue("Done!")
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
        _totalRepsInput.value = ""
        totalReps = 0
        startTime = 0L
        stopAndResetPlayers()
    }

    private fun speakRepetition(rep: Int) {
        Log.d("TTS", "Speaking repetition: $rep")
        tts?.speak("$rep", TextToSpeech.QUEUE_FLUSH, null, "tts_rep_$rep")
    }

    private fun playCountSound() {
        countPlayer?.seekTo(0)
        countPlayer?.start()
    }

    private fun playIntervalSound() {
        intervalPlayer?.seekTo(0)
        intervalPlayer?.start()
    }

    private fun stopAndResetPlayers() {
        if (countPlayer?.isPlaying == true) {
            countPlayer?.pause()
        }
        countPlayer?.seekTo(0)

        if (intervalPlayer?.isPlaying == true) {
            intervalPlayer?.pause()
        }
        intervalPlayer?.seekTo(0)
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
        countPlayer?.release()
        countPlayer = null
        intervalPlayer?.release()
        intervalPlayer = null
        timerJob?.cancel()
    }
}
