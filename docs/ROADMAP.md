# Roadmap

## Launcher foundation
- [x] Compose launcher and navigation
- [x] SAF EXE picker and persistable URI/name
- [x] Device, ABI, heap, and OpenGL renderer diagnostics
- [x] Local profile preference
- [x] Runtime manifest and installer interface
- [x] SHA-256 verification helper
- [x] CI build, unit tests, and Android lint green (through run #56)
- [ ] Test on real MediaTek device

## Compatibility runtime (not yet implemented)
- [ ] License/provenance review of Wine, Box64 and graphics components
- [ ] Integrate Wine/Box64 compatibility core and reproducible ARM64 build pipeline
- [x] Modular ZIP installer engine with HTTPS, size, SHA-256, bounded extraction, safe paths, and app-private staging
- [x] Reject duplicate and conflicting ZIP archive entry paths
- [x] ECDSA P-256 signed-manifest verifier and fail-closed installer gate
- [x] Build-time public-key configuration and installer factory
- [x] Reject malformed, oversized, duplicate-ID, and non-HTTPS manifests before signature verification
- [x] ARM64 device/process ABI compatibility gate with unit tests
- [x] Android-advertised OpenGL ES, Mali string, and Vulkan feature diagnostics with unit tests
- [ ] Generate and protect the production release signing key; configure its public key in release builds
- [ ] Supply signed manifests and legally redistributable runtime packages
- [x] Validated declarative ARM64 container preference model with unit tests (does not create or launch containers)
- [x] Per-game graphics backend preference saved in the launcher library (declarative only)
- [x] Per-game resolution and memory-limit preferences saved with validated choices (declarative only)
- [x] Pure Kotlin mapper from saved game entries to validated container configuration, with safe deterministic IDs and unit tests (declarative only)
- [x] App-private persistent metadata registry with schema checks, atomic writes, safe IDs, and unit tests (metadata only; no prefix or mount)
- [x] Fail-closed runtime readiness evaluator and prerequisite status card with unit tests (reports evidence only; does not enable execution)
- [x] Read-only runtime component inventory with safe-path/symlink checks and explicit unverified state (does not prove file integrity or compatibility)
- [x] Synchronize saved game preferences into the metadata registry and prune stale game entries safely, with unit tests (metadata only)
- [ ] Apply graphics and memory preferences through a real container lifecycle
- [ ] Test Mali graphics backend on supported hardware
- [ ] Touch controls and gamepad mapping
- [ ] Multi-game persistent library

## Release gates
- Reproducible build with downloadable CI APK artifact
- Device test on documented chipset/GPU
- Do not claim Windows program execution until runtime integration is verified
- Verify licenses and redistribution rights for all bundled/downloaded runtime components
- Never ship a release with an unset or development-only manifest verification key


## Runtime inventory UI
- Connected the read-only runtime inventory report to the launcher Games page.
- The UI explicitly reports that no trusted release manifest is configured; it does not infer installation or readiness from files alone.
- Inventory remains read-only and does not launch or modify runtime files.
