# Application Specifications

This document provides a detailed breakdown of the Conviction Timer application's behavior and internal logic.

## 1. User Interface and Controls

The user interface is designed for simplicity and ease of use during a workout.

- **Exercise Selection Card:** A card at the top of the screen containing all controls for selecting an exercise.
    - **Category:** A dropdown to select the exercise category.
    - **Step:** A set of tabs to select the exercise step.
    - **Level:** A set of tabs to select the exercise level.
    - **Target Reps:** A display and controls (`+`/`-` buttons) for the target number of repetitions. This is located within the card, below the level selection.
- **Timer Display:** Shows the elapsed time in `MM:SS` format and the current repetition count.
- **Action Buttons:**
    - **`START` Button:** Begins the timer and the workout sequence.
    - **`PAUSE` Button:** Pauses the timer and audio. Pressing it again resumes the workout.
    - **`RESET` Button:** Stops the timer, resets the repetition count to 0, and clears the target reps setting.

## 2. Workout Flow

This section describes the step-by-step user experience.

1.  **Initial State:**
    - The user opens the app.
    - The app automatically loads the first exercise from `res/raw/exercises.csv`.
    - The **Category**, **Step**, **Level**, and **Target Reps** are pre-populated with the data from that first exercise.
    - The timer displays "00:00" and the rep counter shows "0."

2.  **Setup:**
    - The user can either proceed with the default exercise or select a different one using the controls in the Exercise Selection Card.
    - The user can also manually adjust the **Target Reps** using the `+` and `-` buttons.

3.  **Starting the Workout:**
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

## 3. Exercise Selection Flow

In addition to manually setting the target reps, the user can select a pre-defined exercise to automatically set the target repetitions.

1.  **Data Source:**
    - Exercise data is loaded from the `res/raw/exercises.csv` file included in the application.
    - This CSV file contains columns for `category`, `step`, `name`, `level`, `sets`, and `targetReps`.

2.  **Selection Process:**
    - The **Category**, **Step**, and **Level** controls are always visible.
    - The user selects a **Category** from a dropdown list.
    - Based on the chosen category, the available **Steps** are updated.
    - Upon selecting a step, the **Exercise Name** is automatically determined and displayed.
    - The available **Levels** for that exercise are then shown.
    - Once the user selects a **Level**, the **Target Reps** field is automatically populated with the value from the CSV data.

## 4. Timer and Audio Cue Logic

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
- **`targetReps`:** The target number of repetitions for the session.