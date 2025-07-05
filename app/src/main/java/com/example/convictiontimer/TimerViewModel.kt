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
    private var soundPool: SoundPool? = null
    private var countSoundId: Int = 0
    private var intervalSoundId: Int = 0
    private var timerJob: Job? = null
    private var totalReps: Int = 0
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
                // Accurately delay to the start of the current second
                val targetTime = startTime + (elapsedSeconds * 1000L)
                val delayMillis = targetTime - System.currentTimeMillis()
                if (delayMillis > 0) {
                    delay(delayMillis)
                }

                // Check if paused during delay
                if (_isRunning.value == false) break

                // --- All actions for elapsedSeconds happen here ---

                _timerText.postValue(formatTime(elapsedSeconds + 1))

                val secondInRep = elapsedSeconds % repetitionDurationSeconds
                if (secondInRep == 0) {
                    val currentRepNumber = (elapsedSeconds / repetitionDurationSeconds) + 1
                    if (currentRepNumber <= totalReps) {
                        _currentRep.postValue(currentRepNumber)
                        speakRepetition(currentRepNumber)
                    }
                }

                when (secondInRep) {
                    0, 1, 3, 4 -> playIntervalSound() // . (dot)
                    2, 5 -> playCountSound()   // * (star)
                }

                // Increment for the next loop
                elapsedSeconds++
            }

            // If the timer ran to completion (was not paused)
            if (_isRunning.value == true) {
                // Wait for the final second to elapse before showing "Finish!"
                val finalTargetTime = startTime + (totalDuration * 1000L)
                val finalDelay = finalTargetTime - System.currentTimeMillis()
                if (finalDelay > 0) {
                    delay(finalDelay)
                }
                _isRunning.postValue(false)
                _timerText.postValue("Finish!")
                speakFinish()
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
    }

    private fun speakFinish() {
        tts?.speak("Finish!", TextToSpeech.QUEUE_FLUSH, null, "tts_finish")
    }

    private fun speakRepetition(rep: Int) {
        Log.d("TTS", "Speaking repetition: $rep")
        tts?.speak("$rep", TextToSpeech.QUEUE_FLUSH, null, "tts_rep_$rep")
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

