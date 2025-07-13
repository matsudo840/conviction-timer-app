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

class TimerViewModel(
    application: Application,
    private val repository: TimerRepository = TimerRepository(application)
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

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

    // Exercise-related state
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _steps = MutableStateFlow<List<String>>(emptyList())
    val steps: StateFlow<List<String>> = _steps.asStateFlow()

    private val _exercisesForStep = MutableStateFlow<List<String>>(emptyList())
    val exercisesForStep: StateFlow<List<String>> = _exercisesForStep.asStateFlow()

    private val _levels = MutableStateFlow<List<String>>(emptyList())
    val levels: StateFlow<List<String>> = _levels.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedStep = MutableStateFlow("")
    val selectedStep: StateFlow<String> = _selectedStep.asStateFlow()

    private val _selectedExercise = MutableStateFlow("")
    val selectedExercise: StateFlow<String> = _selectedExercise.asStateFlow()

    private val _selectedLevel = MutableStateFlow("")
    val selectedLevel: StateFlow<String> = _selectedLevel.asStateFlow()

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

        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.loadExercises()
            val categories = repository.getCategories()
            _categories.value = categories

            // Auto-select the first item to ensure UI is populated
            if (categories.isNotEmpty()) {
                onCategorySelected(categories.first(), isInitialLoad = true)
            }
        }
    }

    fun onCategorySelected(category: String, isInitialLoad: Boolean = false) {
        _selectedCategory.value = category
        val steps = repository.getStepsForCategory(category)
        _steps.value = steps

        if (steps.isNotEmpty()) {
            val stepToSelect = if (isInitialLoad) steps.first() else selectedStep.value.ifEmpty { steps.first() }
            onStepSelected(stepToSelect, isInitialLoad = isInitialLoad)
        } else {
            // Handle case where a category has no steps
            _selectedStep.value = ""
            _selectedExercise.value = ""
            _levels.value = emptyList()
            _selectedLevel.value = ""
            _totalReps.value = 0
        }
    }

    fun onStepSelected(step: String, isInitialLoad: Boolean = false) {
        _selectedStep.value = step
        val exercisesInStep = repository.getExercisesForStep(_selectedCategory.value, step)
        val exerciseName = exercisesInStep.firstOrNull()?.name ?: ""
        _selectedExercise.value = exerciseName // This might be redundant if you only have one exercise per step

        if (exerciseName.isNotEmpty()) {
            val levels = repository.getLevelsForExercise(_selectedCategory.value, step, exerciseName)
            _levels.value = levels
            if (levels.isNotEmpty()) {
                val levelToSelect = if (isInitialLoad) levels.first() else selectedLevel.value.ifEmpty { levels.first() }
                onLevelSelected(levelToSelect)
            }
        } else {
            _levels.value = emptyList()
            _selectedLevel.value = ""
            _totalReps.value = 0
        }
    }

    fun onExerciseSelected(exercise: String) {
        // This function might not be needed if the exercise is determined by the step
        // but we keep it for consistency with the previous structure.
        _selectedExercise.value = exercise
        val levels = repository.getLevelsForExercise(_selectedCategory.value, _selectedStep.value, exercise)
        _levels.value = levels
        _selectedLevel.value = ""
        _totalReps.value = 0
    }

    fun onLevelSelected(level: String) {
        _selectedLevel.value = level
        val totalReps = repository.getTotalRepsForLevel(_selectedCategory.value, _selectedStep.value, _selectedExercise.value, level)
        _totalReps.value = totalReps
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