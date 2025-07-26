# Application Specifications

This document provides a detailed breakdown of the Conviction Timer application's features, behavior, and internal logic.

## 1. Overview

The Conviction Timer is a specialized tool for bodyweight strength training. Its primary purpose is to guide the user through repetitions at a fixed, slow pace using audio cues, eliminating the need for manual counting or a stopwatch. The application is built around a 6-second repetition cycle.

## 2. User Interface (UI)

The UI is designed for simplicity and minimal interaction during a workout. It consists of two main screens accessible via a bottom navigation bar.

-   **Timer Screen:** The primary screen for selecting and performing exercises.
-   **Log Screen:** A screen that displays a history of completed workouts.

### 2.1. Timer Screen

-   **Exercise Selection Card:** A card at the top of the screen that houses all controls for selecting a workout routine.
    -   **Category:** A scrollable tab row (`ScrollableTabRow`) to select the main exercise group (e.g., "Push-up", "Squat").
    -   **Step:** A scrollable tab row to select the specific exercise progression.
    -   **Level:** A set of tabs to select the difficulty (e.g., "Beginner", "Intermediate").
    -   **Target Reps:** A display showing the number of repetitions for the selected level, with `+` and `-` buttons for manual adjustment.
    -   **Sets:** A display showing the number of sets for the selected routine.
-   **Timer Display:** A central text element showing the elapsed time in `MM:SS` format and the current repetition count.
-   **Control Buttons:**
    -   **`START` / `STOP`:** A single button to begin or end the timer sequence.

### 2.2. Log Screen

-   **Workout History:** Displays a chronological list of all completed training sessions. Each entry shows:
    -   Date
    -   Exercise Category & Step
    -   Number of Reps completed
-   **Editing Mode:** A floating action button allows the user to enter an editing mode. In this mode, tapping on a log entry opens a dialog to modify the date, category, step, and reps.

## 3. Core Logic & State Management

All business logic is centralized in the `TimerViewModel` to create a reactive and maintainable architecture.

### 3.1. ViewModel

The `TimerViewModel` is responsible for:
-   Managing the timer's state (`running`, `paused`, `stopped`).
-   Holding and updating UI-related data (timer text, rep count, selected exercise).
-   Loading exercise data from the `TimerRepository`.
-   Handling all user interactions (starting/stopping the timer, selecting exercises).
-   Controlling audio output (`TextToSpeech` and `SoundPool`).

### 3.2. Data Flow

1.  On initialization, `TimerViewModel` requests `TimerRepository` to load exercise data.
2.  `TimerRepository` reads the `res/raw/exercises.csv` file into a list of `Exercise` data objects.
3.  The ViewModel exposes this data to the UI through `StateFlow` and `LiveData`.
4.  User selections in the UI (e.g., choosing a category) trigger methods in the ViewModel, which then filters the exercise list and updates the UI state accordingly.

### 3.3. State Management

The application uses a combination of `StateFlow` and `LiveData` to manage state:

-   **`timerText`, `currentRep`, `isRunning`:** Exposed as `LiveData` for observing simple, lifecycle-aware UI updates.
-   **Exercise Data (`categories`, `steps`, `levels`, etc.) and Training Logs:** Exposed as `StateFlow` to handle streams of data and more complex state transformations related to exercise selection and log updates.

### 3.4. Exercise Selection Logic

The selection controls are cascaded. The choice in one control filters the options available in the next:

1.  **Select Category:** Updates the list of available `Steps`.
2.  **Select Step:** Determines the `Exercise Name` and updates the list of available `Levels`.
3.  **Select Level:** Automatically populates the `Target Reps` and `Sets` from the corresponding data in the CSV file.

### 3.5. State Persistence

To enhance user experience, the app remembers the user's last selected `step` and `level` for each **category**.
-   This is achieved using `SharedPreferences`.
-   When a user selects a step or level, the `TimerViewModel` saves a `CategorySelectionState` object (containing the selected step and level) to storage, keyed by the category name.
-   When the user returns to a category, the ViewModel retrieves this saved state and automatically applies the previous selections.

## 4. Timer and Audio Sequence

### 4.1. 6-Second Repetition Cycle

The core of the app is a precise 6-second timer loop for each repetition. The sequence begins after the user presses `START`.

| Elapsed Time (Seconds) | Second in Rep | On-Screen Rep Count | Voice Prompt (TTS) | Sound Effect (SoundPool) |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0 | 0 | "Ready" | Interval Sound |
| 1 | 1 | 0 | - | Interval Sound |
| 2 | 2 | 0 | - | Count Sound |
| 3 | 3 | 0 | - | Interval Sound |
| 4 | 4 | 0 | - | Interval Sound |
| 5 | 5 | 0 | - | Count Sound |
| 6 | 0 | 1 | "One" | Interval Sound |
| 7 | 1 | 1 | - | Interval Sound |
| 8 | 2 | 1 | - | Count Sound |
| ... | ... | ... | ... | ... |

-   **Completion:** When the `currentRep` reaches the `targetReps`, the timer stops, a "Finish!" message is displayed, and a "Finish" voice prompt is played. A log of the completed workout is then saved automatically.

### 4.2. Audio Output

The app uses two different Android APIs for audio feedback, chosen for their specific strengths:

-   **`TextToSpeech` (TTS):** Used for voice prompts ("Ready", "One", "Two", "Finish"). Ideal for speaking dynamic text.
-   **`SoundPool`:** Used for low-latency sound effects (`count.mp3`, `interval.mp3`). It's optimized for playing short, frequently used audio clips with minimal delay.

## 5. Data Source

All workout routines are defined in a single CSV file.

-   **File Path:** `app/src/main/res/raw/exercises.csv`
-   **Structure:** The file contains the following columns:
    -   `Category`: The main exercise group (e.g., `Push-up`).
    -   `Step`: The progression number within the category (e.g., `1`, `2`).
    -   `Exercise`: The specific name of the exercise (e.g., `Wall Push-ups`).
    -   `Level`: The difficulty level (`Beginner`, `Intermediate`, `Advanced`).
    -   `Reps`: The target number of repetitions for that level.
    -   `Sets`: The recommended number of sets for that level.
