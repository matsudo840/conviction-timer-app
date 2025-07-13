package com.example.convictiontimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvictionTimerScreen(timerViewModel: TimerViewModel = viewModel()) {
    val totalReps by timerViewModel.totalReps.collectAsState()
    val timerText by timerViewModel.timerText.observeAsState("00:00")
    val currentRep by timerViewModel.currentRep.observeAsState(0)
    val isRunning by timerViewModel.isRunning.observeAsState(false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conviction Timer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ExerciseSelectionCard(timerViewModel = timerViewModel)

            TimerDisplay(timerText = timerText, currentRep = currentRep)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimerControls(
                    isRunning = isRunning,
                    onStart = { timerViewModel.startTimer() },
                    onPause = { timerViewModel.pauseTimer() },
                    onReset = { timerViewModel.resetTimer() }
                )
            }
        }
    }
}

@Composable
fun ExerciseSelectionCard(timerViewModel: TimerViewModel) {
    val totalReps by timerViewModel.totalReps.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExerciseSelection(timerViewModel = timerViewModel)
            Spacer(modifier = Modifier.height(16.dp))
            RepsAdjustmentControls(
                totalReps = totalReps,
                onIncrement = { timerViewModel.incrementTotalReps() },
                onDecrement = { timerViewModel.decrementTotalReps() }
            )
        }
    }
}


@Composable
fun ExerciseSelection(timerViewModel: TimerViewModel) {
    val categories by timerViewModel.categories.collectAsState()
    val selectedCategory by timerViewModel.selectedCategory.collectAsState()

    val steps by timerViewModel.steps.collectAsState()
    val selectedStep by timerViewModel.selectedStep.collectAsState()

    val selectedExercise by timerViewModel.selectedExercise.collectAsState()

    val levels by timerViewModel.levels.collectAsState()
    val selectedLevel by timerViewModel.selectedLevel.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Dropdown(
            label = "Category",
            selectedValue = selectedCategory,
            options = categories,
            onSelected = { timerViewModel.onCategorySelected(it) },
            enabled = true
        )

        if (steps.isNotEmpty()) {
            Text(
                text = "Step",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            val selectedTabIndex = steps.indexOf(selectedStep).coerceAtLeast(0)
            ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                steps.forEachIndexed { index, step ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { timerViewModel.onStepSelected(step) },
                        text = { Text(step) }
                    )
                }
            }
        }

        if (selectedExercise.isNotEmpty()) {
            Text(
                text = "Exercise: $selectedExercise",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (levels.isNotEmpty()) {
            Text(
                text = "Level",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            val selectedTabIndex = levels.indexOf(selectedLevel).coerceAtLeast(0)
            ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                levels.forEachIndexed { index, level ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { timerViewModel.onLevelSelected(level) },
                        text = { Text(level) }
                    )
                }
            }
        }
    }
}

@Composable
fun Dropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown",
                    Modifier.clickable(enabled) { expanded = true }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TimerDisplay(timerText: String, currentRep: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Text(
            text = timerText,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Rep: $currentRep",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.secondary
        )
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
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Total Reps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = totalReps.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
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

@Composable
fun TimerControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        Button(onClick = onStart, enabled = !isRunning, modifier = Modifier.weight(1f)) {
            Text("Start")
        }
        Button(onClick = onPause, enabled = isRunning, modifier = Modifier.weight(1f)) {
            Text("Pause")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text("Reset")
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