# Conviction Timer App

A simple, focused timer for bodyweight strength training, inspired by "Conviction Training." It helps you maintain a slow, consistent pace for each repetition, ensuring perfect form and maximum results.

## Why This App?

In training methods that emphasize slow, controlled movements, maintaining a precise tempo is crucial. This app replaces the need for a stopwatch and mental counting with clear, intuitive audio cues, allowing you to focus entirely on your workout.

## Key Features

- **Paced Repetitions:** Guides you through a 6-second repetition cycle (e.g., 2 seconds down, 2 seconds hold, 2 seconds up) with distinct sound cues.
- **Automatic Rep Counting:** Counts your reps aloud in English, so you never lose track.
- **Simple Interface:** A clean, minimalist UI with just the essential controls: set your reps, start, pause, and reset.

## For Developers

This project serves as a great example of a modern Android application built with Kotlin and Jetpack Compose.

- **Architecture:** Follows the MVVM (Model-View-ViewModel) pattern.
- **Core Technologies:**
    - **UI:** Jetpack Compose for a declarative and modern UI.
    - **State Management:** `StateFlow` and `LiveData` for reactive state handling.
    - **Asynchronous Operations:** Kotlin Coroutines for managing the timer logic without blocking the main thread.
- **Project Structure:**
    - `MainActivity.kt`: The main entry point, responsible for setting up the UI.
    - `TimerViewModel.kt`: Contains all the business logic for the timer, state management, and audio cues.
    - `res/raw/`: Contains audio assets (`.mp3`) for timer cues.

## Getting Started

1.  Clone this repository.
2.  Open the project in Android Studio.
3.  The minimum required SDK version is 26.
4.  Run the app on an Android emulator or a physical device.
