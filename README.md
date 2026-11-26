# MTK Winlator

Independent Android ARM64 launcher project focused on MediaTek Mali / Immortalis devices.

**Current state:** launcher foundation only. The app currently provides a basic Compose launcher, saved EXE selection, device information, and profile preference. It does **not** execute Windows programs yet. Wine, Box64, container management, and a Mali-specific compatibility backend remain unintegrated. A generic ZIP runtime installer engine exists, but no runtime package is configured or downloadable yet.

## Goals
- Native Android UI and launcher APK kept small
- ARM64 target
- Device, ABI, memory, and OpenGL renderer diagnostics
- File picker for executable selection (selection only)
- Local profile preferences
- Modular runtime package installer with HTTPS, size, SHA-256, and safe ZIP extraction checks
- MediaTek/Mali-specific compatibility work after runtime and licensing review

## Build
- JDK 17
- Android SDK platform 35
- Gradle 8.11.1

Open the repository in Android Studio or run `gradle assembleDebug` in an environment with Android SDK configured. GitHub Actions workflow `Android build and validation` builds the debug APK and runs lint.

## License/provenance
This project is independent and not an official Winlator build. Before incorporating Wine, Box64, graphics drivers, or other runtime packages, review upstream licenses and redistribution rules. No third-party runtime binaries are currently bundled.
