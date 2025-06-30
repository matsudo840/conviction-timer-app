# Conviction Timer App

This is an Android application designed to serve as a specialized timer for Conviction Training.

## Features

- **Customizable Repetition Timer:** Configurable timing for each phase of a repetition:
    - 2 seconds for the eccentric (bottom position) phase.
    - 1 second hold at the bottom position.
    - 2 seconds for the concentric (top position) phase.
    - 1 second pause before the next repetition.
- **Repetition Counter:** Counts repetitions in English (e.g., "one", "two", "three") upon reaching the top position.
- **Configurable Total Reps:** Users can input the desired total number of repetitions via increment/decrement buttons.
- **Accurate Timer:** Implemented with a focus on precise timing to minimize drift.

## Technology Stack

- **Kotlin**
- **Jetpack Compose** for UI development
- **Android** platform

## Getting Started

Follow these steps to set up the development environment and run the app:

### 1. Open Project in Android Studio

1.  Launch Android Studio.
2.  Select "Open an Existing Project" (or "Open").
3.  Navigate to the project's root directory (`/Users/matsudo840/develop/conviction-timer-app`) and click "Open".
4.  Allow Android Studio to perform a Gradle Sync. This may take a few minutes.

### 2. Configure Gradle Properties

Ensure your `gradle.properties` file (located in the project root) contains the following lines to enable AndroidX and Jetifier, which are required for this project:

```properties
android.useAndroidX=true
android.enableJetifier=true
```

### 3. Set Minimum SDK Version

The project requires a minimum SDK version of 26 due to the use of adaptive icons. This is configured in `app/build.gradle.kts`:

```kotlin
minSdk = 26
```

### 4. Prepare Beep Sound (Optional)

For the optional "beep" sound functionality (currently disabled in code), you will need to place a short `.ogg` audio file named `beep.ogg` in the following directory:

`app/src/main/res/raw/beep.ogg`

If the `raw` directory does not exist, create it.

### 5. Create an Android Emulator or Connect a Device

1.  In Android Studio, go to `Tools > AVD Manager` to create a new virtual device (emulator).
2.  Alternatively, connect a physical Android device with USB debugging enabled.

### 6. Run the Application

1.  Select your desired emulator or connected device from the device dropdown in the Android Studio toolbar.
2.  Click the green "Run 'app'" button (▶) to build and deploy the application.

## Troubleshooting

### No Sound from Text-to-Speech (TTS)

If you don't hear the repetition announcements:

1.  **Check Device Volume:** Ensure your emulator or physical device's media volume is turned up.
2.  **Check TTS Engine Settings:**
    *   On your Android device, go to `Settings > System > Languages & input > Text-to-speech output`.
    *   Verify that "Google Text-to-speech Engine" is selected as the preferred engine.
    *   Tap the settings icon next to the engine and ensure that the English (US) voice data is installed.
3.  **Check Logcat:** In Android Studio's Logcat window, filter by tag `TTS`. Look for messages like `Speaking repetition: X` to confirm the app is attempting to play sound. If you see `TTS Initialization failed.` or language-related errors, address those first.

### Build Errors

*   **`Unresolved reference: KeyboardOptions` or `KeyboardType`**: Ensure `MainActivity.kt` has the following imports:
    ```kotlin
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.ui.text.input.KeyboardType
    ```
*   **`Unresolved reference: raw`**: This typically means the `beep.ogg` file is missing or incorrectly named in `app/src/main/res/raw/`. Even if the beep sound is disabled in code, the reference might still cause issues if the file is expected by the build system. Clean and rebuild your project (`Build > Clean Project`, then `Build > Rebuild Project`).
