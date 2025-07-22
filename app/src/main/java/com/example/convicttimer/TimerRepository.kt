package com.example.convicttimer

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerRepository(private val application: Application) {

    private var exercises: List<Exercise> = emptyList()
    private val sharedPreferences = application.getSharedPreferences("user_progress", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun loadExercises() {
        // Avoid reloading data if it's already there
        if (exercises.isNotEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                val exerciseList = mutableListOf<Exercise>()
                val inputStream = application.resources.openRawResource(R.raw.exercises)
                val reader = BufferedReader(InputStreamReader(inputStream))
                reader.readLine() // Skip CSV header
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    val exercise = Exercise(
                        category = tokens[0],
                        step = tokens[1].toInt(),
                        name = tokens[2],
                        level = tokens[3],
                        sets = tokens[5].toInt(),
                        totalReps = tokens[4].toInt()
                    )
                    exerciseList.add(exercise)
                }
                exercises = exerciseList
            } catch (e: Exception) {
                Log.e("TimerRepository", "Error loading exercises: ${e.message}")
            }
        }
    }

    fun getExercises(): List<Exercise> {
        return exercises
    }

    fun getCategories(): List<String> {
        return exercises.map { it.category }.distinct()
    }

    fun getStepsForCategory(category: String): List<String> {
        return exercises.filter { it.category == category }.map { it.step.toString() }.distinct()
    }

    fun getExercisesForStep(category: String, step: String): List<Exercise> {
        return exercises.filter { it.category == category && it.step.toString() == step }
    }

    fun getLevelsForExercise(category: String, step: String, exerciseName: String): List<String> {
        return exercises
            .filter { it.category == category && it.step.toString() == step && it.name == exerciseName }
            .map { it.level }
            .distinct()
    }

    fun getTotalRepsForLevel(category: String, step: String, exerciseName: String, level: String): Int {
        return exercises.find {
            it.category == category && it.step.toString() == step && it.name == exerciseName && it.level == level
        }?.totalReps ?: 0
    }

    fun getSetsForLevel(category: String, step: String, exerciseName: String, level: String): Int {
        return exercises.find {
            it.category == category && it.step.toString() == step && it.name == exerciseName && it.level == level
        }?.sets ?: 0
    }

    fun saveCategorySelectionState(category: String, state: CategorySelectionState) {
        val json = gson.toJson(state)
        sharedPreferences.edit().putString("selection_state_" + category, json).apply()
    }

    fun getCategorySelectionState(category: String): CategorySelectionState? {
        val json = sharedPreferences.getString("selection_state_" + category, null)
        return gson.fromJson(json, CategorySelectionState::class.java)
    }

    suspend fun saveTrainingLog(category: String, step: Int, reps: Int) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(application.filesDir, "training_log.csv")
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = sdf.format(Date())
                val logLine = "$currentDate,$category,$step,$reps\n"

                if (!file.exists()) {
                    file.writeText("Date,Category,Step,Reps\n")
                }
                file.appendText(logLine)
                Log.d("TimerRepository", "Saved training log: $logLine")
            } catch (e: Exception) {
                Log.e("TimerRepository", "Error saving training log: ${e.message}")
            }
        }
    }

    suspend fun loadTrainingLogs(): List<TrainingLog> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(application.filesDir, "training_log.csv")
                if (!file.exists()) {
                    Log.d("TimerRepository", "Training log file does not exist.")
                    return@withContext emptyList<TrainingLog>()
                }

                val logList = mutableListOf<TrainingLog>()
                val reader = BufferedReader(InputStreamReader(file.inputStream()))
                reader.readLine() // Skip CSV header
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    if (tokens.size >= 4) {
                        val log = TrainingLog(
                            date = tokens[0],
                            category = tokens[1],
                            step = tokens[2],
                            reps = tokens[3]
                        )
                        logList.add(log)
                    }
                }
                Log.d("TimerRepository", "Loaded ${logList.size} training logs.")
                logList
            } catch (e: Exception) {
                Log.e("TimerRepository", "Error loading training logs: ${e.message}")
                emptyList<TrainingLog>()
            }
        }
    }
}
