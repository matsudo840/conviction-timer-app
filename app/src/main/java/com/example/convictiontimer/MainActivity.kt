package com.example.convictiontimer

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
fun ConvictionTimerScreen(timerViewModel: TimerViewModel = viewModel(factory = TimerViewModelFactory(
    LocalContext.current.applicationContext as Application,
    TimerRepository(LocalContext.current.applicationContext as Application)
))) {
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer to push content down a bit from the top bar
            Spacer(modifier = Modifier.height(16.dp))

            ExerciseSelectionCard(timerViewModel = timerViewModel)

            // Vertically center the timer display
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TimerDisplay(timerText = timerText, currentRep = currentRep)
            }

            TimerControls(
                isRunning = isRunning,
                onStartPause = {
                    if (isRunning) timerViewModel.pauseTimer() else timerViewModel.startTimer()
                },
                onReset = { timerViewModel.resetTimer() }
            )

            // Spacer at the bottom for padding
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ExerciseSelectionCard(timerViewModel: TimerViewModel) {
    val totalReps by timerViewModel.totalReps.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExerciseSelection(timerViewModel = timerViewModel)
            Spacer(modifier = Modifier.height(24.dp))
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

    Column {
        if (categories.isNotEmpty()) {
            SelectionRow(
                label = "Category",
                items = categories,
                selectedValue = selectedCategory,
                onValueSelected = { timerViewModel.onCategorySelected(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (steps.isNotEmpty()) {
            SelectionRow(
                label = "Step",
                items = steps,
                selectedValue = selectedStep,
                onValueSelected = { timerViewModel.onStepSelected(it) }
            )
        }

        if (selectedExercise.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Exercise: $selectedExercise",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (levels.isNotEmpty()) {
            SelectionRow(
                label = "Level",
                items = levels,
                selectedValue = selectedLevel,
                onValueSelected = { timerViewModel.onLevelSelected(it) },
                scrollable = false
            )
        }
    }
}

@Composable
fun SelectionRow(
    label: String,
    items: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    scrollable: Boolean = true
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val selectedIndex = items.indexOf(selectedValue).coerceAtLeast(0)

        if (scrollable) {
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 0.dp
            ) {
                items.forEachIndexed { index, item ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { onValueSelected(item) },
                        text = { Text(item) }
                    )
                }
            }
        } else {
            TabRow(
                selectedTabIndex = selectedIndex
            ) {
                items.forEachIndexed { index, item ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { onValueSelected(item) },
                        text = { Text(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimerDisplay(timerText: String, currentRep: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = timerText,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Rep: $currentRep",
            style = MaterialTheme.typography.headlineMedium,
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
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        FilledTonalIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDecrement()
            },
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            enabled = (totalReps > 0)
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = "回数を減らす",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(100.dp)
        ) {
            Text(
                text = "Target Reps",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = totalReps.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onIncrement()
            },
            modifier = Modifier.size(56.dp),
            shape = CircleShape
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "回数を増やす",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun TimerControls(
    isRunning: Boolean,
    onStartPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f)
        ) {
            Text("Reset")
        }
        Button(
            onClick = onStartPause,
            modifier = Modifier
                .weight(2f)
                .height(48.dp)
        ) {
            val icon = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow
            val text = if (isRunning) "Pause" else "Start"

            Icon(imageVector = icon, contentDescription = text)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text)
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