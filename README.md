# MTK Winlator

Android ARM64 launcher project targeting MediaTek Mali / Immortalis devices.

> Status: **launcher foundation only**. Wine/Box64 runtime and Mali graphics backends are not yet integrated. This repository does not currently provide a working Windows-game runtime.

## Goals
- Small native Android launcher (target launcher APK size: 30 MB or less; not guaranteed until measured)
- ARM64-only Android package
- Hardware information and GPU renderer detection
- Game library and SAF EXE selection
- Modular runtime installer architecture (runtime artifacts are not bundled yet)
- Diagnostics and per-game profiles planned

## Build
Requires Android Studio and JDK 17. Open this repository in Android Studio and allow Gradle sync, then build the debug APK.

## Provenance and licensing
Independent project; not an official Winlator release. Runtime components must be reviewed for license and redistribution compatibility before bundling.

## Current scope
UI/launcher scaffold with basic device diagnostics and file selection. Actual Wine/Box64 execution is future integration work.
