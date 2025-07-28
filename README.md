# ForwardApp

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/phoenixway/forwardapp-android)

ForwardApp is a native Android application for managing goals and tasks, built with modern technologies. It's designed for effectively structuring your ideas, from short-term tasks to long-term strategic goals.

## ✨ Features

* **Hierarchical Lists:** Create nested lists to organize your projects and goals in detail.
* **Goal Management:** Add, edit, delete, and mark goals as complete.
* **Drag-and-Drop Reordering:** Easily change the priority of goals by dragging and dropping them within a list.
* **Gestures & Context Menus:** Use swipe gestures for quick actions (edit, delete) and context menus to manage your lists (rename, delete, add sublists).
* **Global Search:** Instantly find any goal across the entire app with a powerful global search feature.
* **Markdown Support:** Format your goal text with Markdown syntax for **bold**, *italic*, and ~~strikethrough~~ text.
* **Obsidian Integration:** Use wiki-links like `[[Note Name]]` to quickly jump to corresponding notes in your Obsidian vault.
* **Smart Icons:** Automatically assign icons (🔥, ⭐, 🔭, etc.) to your goals using special tags and markers (`#critical`, `!`, `~`) for visual classification.

## 🛠️ Tech Stack & Architecture

This project is built using modern tools and best practices for Android development.

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Asynchronicity:** Kotlin Coroutines & Flow
* **Database:** [Room](https://developer.android.com/training/data-storage/room)
* **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
* **Navigation:** Jetpack Navigation for Compose
* **Drag & Drop:** [compose-dnd](https://github.com/mohamedrejeb/compose-dnd) by Mohamed Rejeb

## 📲 Installation

You can install the app directly by downloading the APK from the latest release.

1.  **Download the APK**
    * Go to the [**Releases Page**](https://github.com/phoenixway/forwardapp-android/releases).
    * In the latest release, expand the **Assets** section and download the `.apk` file.

2.  **Enable Unknown Sources**
    * Before you can install the APK, you need to allow installations from unknown sources on your device.
    * On modern Android versions (8.0+), this is a per-app permission. When you open the downloaded `.apk` file, your file manager or browser will prompt you for permission to install apps. Grant it.
    * On older versions, you may need to go to `Settings > Security` and enable the `Unknown sources` option.

3.  **Install the App**
    * Open the downloaded `.apk` file from your device's notification shade or your file manager.
    * Tap **Install** to complete the installation.

## 🚀 Building the Project (for Developers)

1.  Clone the repository:
    ```bash
    git clone [https://github.com/phoenixway/forwardapp-android.git](https://github.com/phoenixway/forwardapp-android.git)
    ```
2.  Open the project in the latest stable version of [Android Studio](https://developer.android.com/studio).
3.  Let Gradle sync all the dependencies.
4.  Build and run the app on an emulator or a physical device.

## 🤝 Contributing

Contributions are welcome. If you have ideas for improvements or have found a bug, please open an issue. If you'd like to contribute code, please follow the standard Fork -> Create Branch -> Commit -> Create Pull Request workflow.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## ✍️ Author

**Roman Kozak (Pylypchuk)** - [phoenixway](https://github.com/phoenixway)
