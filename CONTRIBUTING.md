# Contributing to OpenTrackEditor

Thanks for your interest in contributing!  
This project is built with **Android + Kotlin**. Please follow these guidelines when contributing.

---

## Getting Started

1. **Fork** the repository.
2. **Clone** your fork locally.
3. Open the project in **Android Studio** (latest stable version recommended).
4. Make sure you can build and run the project before making changes.

---

## Code Style

- Use **Kotlin** best practices and follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Format code with **Android Studio’s built-in formatter** before committing.
- Keep code clean, readable, and well-documented.

---

## Architecture

This project follows **Clean Architecture** principles:

- **Domain layer**: Business logic, use cases, entities.
- **Data layer**: Repositories, API services, local storage.
- **Presentation layer**: UI (Activities, Fragments, Compose, ViewModels).

When adding new features, please respect this separation of concerns.

More details are available on [our website](https://olivermineau.com/opentrackeditor/dev).

---

## Making Changes

1. Create a branch from `main`:
   - `feature/your-feature-name` for new features
   - `bugfix/issue-123` for bug fixes
2. Make small, focused commits with descriptive messages.
3. Test your changes on an emulator or device.

---

## Submitting Pull Requests

- Push your branch and open a **Pull Request** against `main`.
- Clearly describe your changes and reference any related issue (e.g., `Fixes #123`).
- Ensure the project builds and all tests pass before submitting.

---

## Reporting Issues

- Use the **Issues** tab.
- Include steps to reproduce, expected behavior, and screenshots/logs if helpful.
- Use labels (`bug`, `enhancement`, etc.) if possible.

---


Thanks for helping improve this project!
