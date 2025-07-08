# Specifications

This document outlines the detailed specifications for the Conviction Timer application, covering both its user-facing behavior and its internal program logic.

## Application Specifications

This section describes the functional behavior of the app from a user's perspective.

### Timer Sequence

The timer operates on a 6-second cycle per repetition. The sequence is as follows:

- **Initial State:** The timer displays "00:00" and the rep counter is at "0."
- **Start:** The user sets the desired number of total reps and presses the START button.

#### Timing Details:
- **At 1 second:**
    - The app audibly says, "Ready."
- **At 7 seconds:**
    - The rep counter on the screen updates to "1."
    - The app audibly says, "One."
- **At 13 seconds:**
    - The rep counter on the screen updates to "2."
    - The app audibly says, "Two."
- **Subsequent Reps:**
    - This pattern continues every 6 seconds. For each new rep, the counter increments, and the corresponding number is announced.
- **Completion:**
    - Once the total number of reps is reached, the timer stops.
    - The screen displays the message, "Finish!"
    - The app audibly says, "Finish."

### User Controls

- **`+` / `-` Buttons:** Increment or decrement the total number of reps for the workout session.
- **`START` Button:** Begins the timer sequence.
- **`PAUSE` Button:** Halts the timer and audio cues. The timer can be resumed from where it left off.
- **`RESET` Button:** Stops the timer and resets the rep counter and total reps to zero.

## Program Specifications

This section details the internal logic and implementation of the timer, primarily managed by the `TimerViewModel`.

### Core Components

- **`TimerViewModel.kt`:** A lifecycle-aware ViewModel that holds and manages all timer-related state and logic.
- **`TextToSpeech` (TTS):** Used for all voice announcements ("Ready," "One," "Two," "Finish").
- **`SoundPool`:** Manages the playback of short audio cues (beeps and clicks) that mark intervals within each repetition.

### State Management

The ViewModel uses `LiveData` and `StateFlow` to expose the following states to the UI:

- **`timerText` (`LiveData<String>`):** The formatted time string (e.g., "00:07").
- **`currentRep` (`LiveData<Int>`):** The current repetition number displayed on the screen.
- **`isRunning` (`LiveData<Boolean>`):** Indicates whether the timer is currently active.
- **`totalReps` (`StateFlow<Int>`):** The target number of repetitions for the session.

### Timer Logic (`startTimer` function)

- The core of the timer is a `viewModelScope.launch` coroutine that runs a `while` loop.
- **`elapsedSeconds`:** A counter that increments every second to track the total time passed.
- **`repetitionDurationSeconds`:** A constant set to `6` seconds.
- **Modulus Operator (`%`):** The expression `elapsedSeconds % repetitionDurationSeconds` is used to determine the current position within a 6-second rep cycle.

#### Key Logic Points:

- **Voice and Rep Count Trigger:**
    - The condition `secondInRep == 1` (where `secondInRep` is the result of the modulus operation) is used to trigger actions at the 1s, 7s, 13s, etc., marks.
    - At `elapsedSeconds == 1`, the app says "Ready."
    - For subsequent triggers (`elapsedSeconds == 7`, `13`, etc.), the `repCycleIndex` (`elapsedSeconds / repetitionDurationSeconds`) is calculated to determine the correct rep number. The `currentRep` LiveData is updated, and the number is spoken.

- **Sound Effects:**
    - The `when (secondInRep)` block plays distinct sounds at different points in the 6-second cycle to help the user maintain a steady rhythm.

- **Completion Handling:**
    - When the `while` loop finishes (i.e., `elapsedSeconds` reaches `totalDuration`), the timer stops.
    - The UI is updated to a "Finish!" state, and the final audio cue is played.
