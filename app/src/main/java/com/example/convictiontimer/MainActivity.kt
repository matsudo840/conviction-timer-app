package com.example.convictiontimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
        // 「-」ボタン (アウトライン)
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

        // 現在の回数表示
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

        // 「+」ボタン (塗りつぶし)
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
