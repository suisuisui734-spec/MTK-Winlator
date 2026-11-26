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
- [ ] Apply graphics preferences through a real container lifecycle
- [ ] Test Mali graphics backend on supported hardware
- [ ] Touch controls and gamepad mapping
- [ ] Multi-game persistent library

## Release gates
- Reproducible build with downloadable CI APK artifact
- Device test on documented chipset/GPU
- Do not claim Windows program execution until runtime integration is verified
- Verify licenses and redistribution rights for all bundled/downloaded runtime components
- Never ship a release with an unset or development-only manifest verification key
