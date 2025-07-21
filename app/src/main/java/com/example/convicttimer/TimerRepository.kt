package com.example.convicttimer

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class TimerRepository(private val application: Application) {

    private var exercises: List<Exercise> = emptyList()
    private val sharedPreferences = application.getSharedPreferences("user_progress", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun loadExercises() {
        // Avoid reloading data if it's already there
        if (exercises.isNotEmpty()) return

        withContext(Dispatchers.IO) {
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
        sharedPreferences.edit().putString("selection_state_$category", json).apply()
    }

    fun getCategorySelectionState(category: String): CategorySelectionState? {
        val json = sharedPreferences.getString("selection_state_$category", null)
        return gson.fromJson(json, CategorySelectionState::class.java)
    }
}
