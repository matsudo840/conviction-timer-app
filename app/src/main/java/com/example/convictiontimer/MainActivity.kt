package com.example.convictiontimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var totalRepsInput by remember { mutableStateOf("") }
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
        TextField(
            value = totalRepsInput,
            onValueChange = { newValue ->
                totalRepsInput = newValue.filter { it.isDigit() }
            },
            label = { Text("Total Repetitions") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = timerText,
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Rep: $currentRep",
            fontSize = 30.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Button(onClick = {
                val reps = totalRepsInput.toIntOrNull() ?: 0
                timerViewModel.startTimer(reps)
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

@Preview(showBackground = true)
@Composable
fun PreviewConvictionTimerScreen() {
    ConvictionTimerAppTheme {
        ConvictionTimerScreen()
    }
}