package com.example.convicttimer

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

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech?
    private var soundPool: SoundPool?
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

        loadInitialData()
    }

    internal fun loadInitialData() {
        viewModelScope.launch {
            repository.loadExercises()
            val exercises = repository.getExercises()
            val trainingLogs = repository.loadTrainingLogs()
            val categories = repository.getCategories()
            _uiState.update {
                it.copy(
                    exercises = exercises,
                    trainingLogs = trainingLogs,
                    categories = categories
                )
            }

            if (categories.isNotEmpty()) {
                onCategorySelected(categories.first())
            }
        }
    }

    fun onCategorySelected(category: String) {
        val steps = repository.getStepsForCategory(category)
        val savedState = repository.getCategorySelectionState(category)

        _uiState.update {
            it.copy(
                selectedCategory = category,
                steps = steps
            )
        }

        if (steps.isNotEmpty()) {
            val stepToSelect = savedState?.selectedStep ?: steps.first()
            onStepSelected(stepToSelect)
        } else {
            _uiState.update {
                it.copy(
                    selectedStep = "",
                    selectedExercise = "",
                    levels = emptyList(),
                    selectedLevel = "",
                    totalReps = 0
                )
            }
        }
    }

    fun onStepSelected(step: String) {
        val exercisesInStep = repository.getExercisesForStep(_uiState.value.selectedCategory, step)
        val exerciseName = exercisesInStep.firstOrNull()?.name ?: ""

        _uiState.update {
            it.copy(
                selectedStep = step,
                selectedExercise = exerciseName
            )
        }

        if (exerciseName.isNotEmpty()) {
            val levels = repository.getLevelsForExercise(_uiState.value.selectedCategory, step, exerciseName)
            val savedState = repository.getCategorySelectionState(_uiState.value.selectedCategory)
            _uiState.update {
                it.copy(levels = levels)
            }
            if (levels.isNotEmpty()) {
                val levelToSelect = savedState?.selectedLevel ?: levels.first()
                onLevelSelected(levelToSelect)
            }
        } else {
            _uiState.update {
                it.copy(
                    levels = emptyList(),
                    selectedLevel = "",
                    totalReps = 0
                )
            }
        }
        repository.saveCategorySelectionState(
            _uiState.value.selectedCategory,
            CategorySelectionState(
                selectedStep = _uiState.value.selectedStep,
                selectedLevel = _uiState.value.selectedLevel
            )
        )
    }

    fun onExerciseSelected(exercise: String) {
        val levels = repository.getLevelsForExercise(_uiState.value.selectedCategory, _uiState.value.selectedStep, exercise)
        _uiState.update {
            it.copy(
                selectedExercise = exercise,
                levels = levels,
                selectedLevel = "",
                totalReps = 0
            )
        }
        repository.saveCategorySelectionState(
            _uiState.value.selectedCategory,
            CategorySelectionState(
                selectedStep = _uiState.value.selectedStep,
                selectedLevel = _uiState.value.selectedLevel
            )
        )
    }

    fun onLevelSelected(level: String) {
        val totalReps = repository.getTotalRepsForLevel(_uiState.value.selectedCategory, _uiState.value.selectedStep, _uiState.value.selectedExercise, level)
        val sets = repository.getSetsForLevel(_uiState.value.selectedCategory, _uiState.value.selectedStep, _uiState.value.selectedExercise, level)
        _uiState.update {
            it.copy(
                selectedLevel = level,
                totalReps = totalReps,
                sets = sets
            )
        }
        repository.saveCategorySelectionState(
            _uiState.value.selectedCategory,
            CategorySelectionState(
                selectedStep = _uiState.value.selectedStep,
                selectedLevel = _uiState.value.selectedLevel
            )
        )
    }

    fun incrementTotalReps() {
        _uiState.update { it.copy(totalReps = it.totalReps + 1) }
    }

    fun decrementTotalReps() {
        _uiState.update { it.copy(totalReps = if (it.totalReps > 0) it.totalReps - 1 else 0) }
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
        if (_uiState.value.isRunning || _uiState.value.totalReps <= 0) return

        _uiState.update { it.copy(isRunning = true, currentRep = 0) }
        startTime = System.currentTimeMillis()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsedSeconds = 0
            val totalDuration = (_uiState.value.totalReps + 1) * repetitionDurationSeconds

            while (_uiState.value.isRunning && elapsedSeconds < totalDuration) {
                val targetTime = startTime + (elapsedSeconds * 1000L)
                val delayMillis = targetTime - System.currentTimeMillis()
                if (delayMillis > 0) {
                    delay(delayMillis)
                }

                if (!_uiState.value.isRunning) break

                if (elapsedSeconds < repetitionDurationSeconds) {
                    if (elapsedSeconds == 0) {
                        _uiState.update { it.copy(time = "00:00") }
                    }
                } else {
                    _uiState.update { it.copy(time = formatTime(elapsedSeconds - repetitionDurationSeconds + 1)) }
                }

                val secondInRep = elapsedSeconds % repetitionDurationSeconds

                if (secondInRep == 0) { 
                    val repCycleIndex = elapsedSeconds / repetitionDurationSeconds

                    if (repCycleIndex == 0) {
                        speak("Ready")
                    } else {
                        val repNumber = repCycleIndex
                        _uiState.update { it.copy(currentRep = repNumber) }
                        speak(repNumber.toString())
                    }
                }

                when (secondInRep) {
                    0, 1, 3, 4 -> playIntervalSound()
                    2, 5 -> playCountSound()
                }

                elapsedSeconds++
            }

            if (_uiState.value.isRunning) {
                val finalTargetTime = startTime + (totalDuration * 1000L)
                val finalDelay = finalTargetTime - System.currentTimeMillis()
                if (finalDelay > 0) {
                    delay(finalDelay)
                }

                _uiState.update { it.copy(currentRep = _uiState.value.totalReps, time = "Finish!", isRunning = false) }
                speak("Finish")

                viewModelScope.launch {
                    repository.saveTrainingLog(
                        _uiState.value.selectedCategory,
                        _uiState.value.selectedStep.toInt(),
                        _uiState.value.totalReps
                    )
                    _uiState.update { it.copy(trainingLogs = repository.loadTrainingLogs()) }
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }

        val repsCompleted = (_uiState.value.currentRep) - 1
        if (repsCompleted >= 0) {
            viewModelScope.launch {
                repository.saveTrainingLog(
                    _uiState.value.selectedCategory,
                    _uiState.value.selectedStep.toInt(),
                    repsCompleted
                )
                _uiState.update { it.copy(trainingLogs = repository.loadTrainingLogs()) }
            }
        }

        _uiState.update { it.copy(time = "00:00", currentRep = 0) }
        startTime = 0L
    }

    fun updateLog(index: Int, log: TrainingLog) {
        viewModelScope.launch {
            val currentLogs = _uiState.value.trainingLogs.toMutableList()
            currentLogs[index] = log
            repository.updateAllTrainingLogs(currentLogs)
            _uiState.update { it.copy(trainingLogs = repository.loadTrainingLogs()) }
        }
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

data class CategorySelectionState(
    val selectedStep: String,
    val selectedLevel: String
)

data class TimerUiState(
    val time: String = "00:00",
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val sets: Int = 0,
    val reps: Int = 0,
    val currentSet: Int = 0,
    val currentRep: Int = 0,
    val progress: Float = 0.0f,
    val isResting: Boolean = false,
    val categories: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val exercisesForStep: List<String> = emptyList(),
    val levels: List<String> = emptyList(),
    val selectedCategory: String = "",
    val selectedStep: String = "",
    val selectedExercise: String = "",
    val selectedLevel: String = "",
    val totalReps: Int = 0,
    val exercises: List<Exercise> = emptyList(),
    val trainingLogs: List<TrainingLog> = emptyList()
)