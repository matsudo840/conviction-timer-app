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
import java.io.BufferedReader
import java.io.InputStreamReader
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

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

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

        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            val exerciseList = mutableListOf<Exercise>()
            val inputStream = getApplication<Application>().resources.openRawResource(R.raw.exercises)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Skip header
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = line!!.split(",")
                val exercise = Exercise(
                    category = tokens[0],
                    step = tokens[1].toInt(),
                    name = tokens[2],
                    level = tokens[3],
                    sets = tokens[4].toInt(),
                    totalReps = tokens[5].toInt()
                )
                exerciseList.add(exercise)
            }
            _exercises.value = exerciseList
            val categories = exerciseList.map { it.category }.distinct()
            _categories.value = categories

            // Auto-select the first item of each category to ensure UI is populated
            if (categories.isNotEmpty()) {
                val category = categories.first()
                _selectedCategory.value = category
                val steps = exerciseList.filter { it.category == category }.map { it.step.toString() }.distinct()
                _steps.value = steps

                if (steps.isNotEmpty()) {
                    val step = steps.first()
                    _selectedStep.value = step
                    val exercisesForStep = exerciseList.filter { it.category == category && it.step.toString() == step }
                    val exerciseName = exercisesForStep.firstOrNull()?.name ?: ""
                    _selectedExercise.value = exerciseName
                    val levels = exercisesForStep.map { it.level }.distinct()
                    _levels.value = levels

                    if (levels.isNotEmpty()) {
                        val level = levels.first()
                        _selectedLevel.value = level
                        val exercise = exerciseList.find { it.category == category && it.step.toString() == step && it.name == exerciseName && it.level == level }
                        _totalReps.value = exercise?.totalReps ?: 0
                    }
                }
            }
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        _steps.value = _exercises.value.filter { it.category == category }.map { it.step.toString() }.distinct()
        _selectedStep.value = ""
        _selectedExercise.value = ""
        _selectedLevel.value = ""
    }

    fun onStepSelected(step: String) {
        _selectedStep.value = step
        val exercises = _exercises.value.filter { it.category == _selectedCategory.value && it.step.toString() == step }
        val exerciseName = exercises.firstOrNull()?.name ?: ""
        _selectedExercise.value = exerciseName
        _levels.value = exercises.map { it.level }.distinct()
        _selectedLevel.value = ""
    }

    fun onExerciseSelected(exercise: String) {
        _selectedExercise.value = exercise
        _levels.value = _exercises.value.filter { it.category == _selectedCategory.value && it.step.toString() == _selectedStep.value && it.name == exercise }.map { it.level }.distinct()
        _selectedLevel.value = ""
    }

    fun onLevelSelected(level: String) {
        _selectedLevel.value = level
        val exercise = _exercises.value.find { it.category == _selectedCategory.value && it.step.toString() == _selectedStep.value && it.name == _selectedExercise.value && it.level == level }
        _totalReps.value = exercise?.totalReps ?: 0
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