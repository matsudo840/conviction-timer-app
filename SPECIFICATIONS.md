# Application Specifications

This document provides a detailed breakdown of the Conviction Timer application's behavior and internal logic.

## 1. User Interface and Controls

The user interface is designed for simplicity and ease of use during a workout.

- **Repetition Display:** Shows the current repetition count.
- **Timer Display:** Shows the elapsed time in `MM:SS` format.
- **Total Reps Setting:**
    - **`+` Button:** Increments the target number of repetitions.
    - **`-` Button:** Decrements the target number of repetitions.
- **Action Buttons:**
    - **`START` Button:** Begins the timer and the workout sequence.
    - **`PAUSE` Button:** Pauses the timer and audio. Pressing it again resumes the workout.
    - **`RESET` Button:** Stops the timer, resets the repetition count to 0, and clears the total reps setting.

## 2. Workout Flow

This section describes the step-by-step user experience.

1.  **Setup:**
    - The user opens the app.
    - The timer displays "00:00" and the rep counter shows "0."
    - The user sets the desired number of total reps using the `+` and `-` buttons.

2.  **Starting the Workout:**
    - The user presses the **START** button.
    - The timer begins, and a "Ready" voice prompt is played.

3.  **Performing Repetitions:**
    - The app guides the user through a 6-second cycle for each repetition, accompanied by audio cues.
    - At the beginning of each new repetition, the rep counter on the screen updates, and the number is announced aloud (e.g., "One," "Two").

4.  **Pausing and Resuming:**
    - The user can press the **PAUSE** button at any time to halt the timer and audio.
    - Pressing the button again (it will now read **RESUME**) continues the workout from where it was paused.

5.  **Completion:**
    - Once the target number of reps is reached, the timer stops automatically.
    - The screen displays the message "Finish!"
    - A "Finish" voice prompt is played.

6.  **Resetting:**
    - The user can press the **RESET** button at any time to end the current session and return the app to its initial state.

## 3. Timer and Audio Cue Logic

All timing and audio events are managed by the `TimerViewModel`.

### Timer Sequence (6-Second Cycle)

The core of the app is a 6-second timer that dictates the pace of each repetition. Here is the sequence of events, starting from when the user presses **START**:

| Elapsed Time | On-Screen Rep Count | Voice Prompt      | Sound Effect      |
| :----------- | :------------------ | :---------------- | :---------------- |
| **1s**       | 0                   | "Ready"           | -                 |
| **2s**       | 0                   | -                 | Interval Sound    |
| **4s**       | 0                   | -                 | Interval Sound    |
| **7s**       | 1                   | "One"             | Rep Count Sound   |
| **8s**       | 1                   | -                 | Interval Sound    |
| **10s**      | 1                   | -                 | Interval Sound    |
| **13s**      | 2                   | "Two"             | Rep Count Sound   |
| ...          | ...                 | ...               | ...               |

- **Repetition Duration:** Each rep takes exactly **6 seconds**.
- **Voice Prompts:** Managed by Android's `TextToSpeech` engine.
- **Sound Effects:** Short beeps (`count.mp3`, `interval.mp3`) are played using `SoundPool` for low-latency audio feedback.

### State Management

The `TimerViewModel` uses `StateFlow` and `LiveData` to expose the following states to the UI, ensuring the interface is always in sync with the timer's logic:

- **`timerText`:** The formatted time string (e.g., "00:07").
- **`currentRep`:** The current repetition number.
- **`isRunning`:** A boolean indicating if the timer is active.
- **`totalReps`:** The target number of repetitions for the session.