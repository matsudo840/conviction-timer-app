package com.example.convictiontimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.convictiontimer.ui.theme.ConvictionTimerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConvictionTimerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConvictionTimerScreen()
                }
            }
        }
    }
}

@Composable
fun ConvictionTimerScreen(timerViewModel: TimerViewModel = viewModel()) {
    val totalReps by timerViewModel.totalReps.collectAsState()
    val timerText by timerViewModel.timerText.observeAsState("00:00")
    val currentRep by timerViewModel.currentRep.observeAsState(0)
    val isRunning by timerViewModel.isRunning.observeAsState(false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ExerciseSelection(timerViewModel = timerViewModel)
        Spacer(modifier = Modifier.height(32.dp))
        RepsAdjustmentControls(
            totalReps = totalReps,
            onIncrement = { timerViewModel.incrementTotalReps() },
            onDecrement = { timerViewModel.decrementTotalReps() }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = timerText,
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Rep: $currentRep",
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Button(onClick = {
                timerViewModel.startTimer()
            }, enabled = !isRunning) {
                Text("Start")
            }
            Button(onClick = { timerViewModel.pauseTimer() }, enabled = isRunning) {
                Text("Pause")
            }
            Button(onClick = { timerViewModel.resetTimer() }) {
                Text("Reset")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelection(timerViewModel: TimerViewModel) {
    val categories by timerViewModel.categories.collectAsState()
    val selectedCategory by timerViewModel.selectedCategory.collectAsState()

    val steps by timerViewModel.steps.collectAsState()
    val selectedStep by timerViewModel.selectedStep.collectAsState()

    val selectedExercise by timerViewModel.selectedExercise.collectAsState()

    val levels by timerViewModel.levels.collectAsState()
    val selectedLevel by timerViewModel.selectedLevel.collectAsState()

    var categoryExpanded by remember { mutableStateOf(false) }
    var stepExpanded by remember { mutableStateOf(false) }
    var levelExpanded by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            TextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            timerViewModel.onCategorySelected(category)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = stepExpanded,
            onExpandedChange = { stepExpanded = !stepExpanded }
        ) {
            TextField(
                value = selectedStep,
                onValueChange = {},
                readOnly = true,
                label = { Text("Step") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stepExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                enabled = selectedCategory.isNotEmpty()
            )
            ExposedDropdownMenu(
                expanded = stepExpanded,
                onDismissRequest = { stepExpanded = false }
            ) {
                steps.forEach { step ->
                    DropdownMenuItem(
                        text = { Text(step) },
                        onClick = {
                            timerViewModel.onStepSelected(step)
                            stepExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedExercise.isNotEmpty()) {
            Text(
                text = "Exercise: $selectedExercise",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = levelExpanded,
            onExpandedChange = { levelExpanded = !levelExpanded }
        ) {
            TextField(
                value = selectedLevel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Level") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                enabled = selectedExercise.isNotEmpty()
            )
            ExposedDropdownMenu(
                expanded = levelExpanded,
                onDismissRequest = { levelExpanded = false }
            ) {
                levels.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level) },
                        onClick = {
                            timerViewModel.onLevelSelected(level)
                            levelExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RepsAdjustmentControls(
    totalReps: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        OutlinedIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDecrement()
            },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            enabled = (totalReps > 0)
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = "回数を減らす",
                modifier = Modifier.size(36.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(120.dp)
        ) {
            Text(
                text = "Total Reps",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = totalReps.toString(),
                fontSize = 26.sp,
            )
        }

        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onIncrement()
            },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "回数を増やす",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewConvictionTimerScreen() {
    ConvictionTimerAppTheme {
        ConvictionTimerScreen()
    }
}