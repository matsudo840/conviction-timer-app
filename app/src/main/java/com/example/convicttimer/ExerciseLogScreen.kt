package com.example.convicttimer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExerciseLogScreen(timerViewModel: TimerViewModel) {
    val uiState by timerViewModel.uiState.collectAsState()
    var isEditingMode by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<Pair<Int, TrainingLog>?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { isEditingMode = !isEditingMode }) {
                if (isEditingMode) {
                    Icon(Icons.Filled.Done, contentDescription = "Done Editing")
                } else {
                    Icon(Icons.Filled.Edit, contentDescription = "Enter Edit Mode")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn {
                itemsIndexed(uiState.trainingLogs) { index, log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEditingMode) {
                                editingLog = Pair(index, log)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Date: ${log.date}, Category: ${log.category}, Step: ${log.step}, Reps: ${log.reps}")
                    }
                }
            }
        }
    }

    editingLog?.let { (index, log) ->
        var updatedDate by remember { mutableStateOf(log.date) }
        var updatedCategory by remember { mutableStateOf(log.category) }
        var updatedStep by remember { mutableStateOf(log.step) }
        var updatedReps by remember { mutableStateOf(log.reps) }

        AlertDialog(
            onDismissRequest = { editingLog = null },
            title = { Text("Edit Log") },
            text = {
                Column {
                    TextField(value = updatedDate, onValueChange = { updatedDate = it }, label = { Text("Date") })
                    TextField(value = updatedCategory, onValueChange = { updatedCategory = it }, label = { Text("Category") })
                    TextField(value = updatedStep, onValueChange = { updatedStep = it }, label = { Text("Step") })
                    TextField(value = updatedReps, onValueChange = { updatedReps = it }, label = { Text("Reps") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updatedLog = TrainingLog(
                        date = updatedDate,
                        category = updatedCategory,
                        step = updatedStep,
                        reps = updatedReps
                    )
                    timerViewModel.updateLog(index, updatedLog)
                    editingLog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { editingLog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}