# Roadmap

## Launcher foundation
- [x] Compose launcher and navigation
- [x] SAF EXE picker and persistable URI/name
- [x] Device, ABI, heap, and OpenGL renderer diagnostics
- [x] Local profile preference
- [x] Runtime manifest and installer interface
- [x] SHA-256 verification helper (planned/in progress)
- [ ] Confirm CI build and lint green
- [ ] Test on real MediaTek device

## Compatibility runtime (not yet implemented)
- [ ] License/provenance review of Wine, Box64 and graphics components
- [ ] Integrate Wine/Box64 compatibility core and reproducible ARM64 build pipeline
- [x] Modular ZIP installer engine with HTTPS, size, SHA-256, safe extraction, and app-private staging
- [ ] Supply authenticated manifests and legally redistributable runtime packages
- [ ] Container creation/lifecycle and per-game settings
- [ ] Test Mali graphics backend on supported hardware
- [ ] Touch controls and gamepad mapping
- [ ] Multi-game persistent library

## Release gates
- Reproducible build with downloadable CI APK artifact
- Device test on documented chipset/GPU
- Do not claim Windows program execution until runtime integration is verified
- Verify licenses and redistribution rights for all bundled/downloaded runtime components
