# Roadmap

## Launcher foundation
- [x] Compose Android launcher and navigation
- [x] SAF executable picker with persisted URI/name
- [x] Device/ABI/RAM diagnostics
- [x] OpenGL renderer query using temporary EGL context
- [x] Local profile preference persistence
- [x] Runtime installer interface and manifest model
- [ ] Compile in CI and test on real device

## Runtime integration (not yet implemented)
- [ ] Review licenses and provenance of Wine/Box64 and graphics components
- [ ] Integrate compatibility core
- [ ] Modular runtime download/install with checksum verification
- [ ] Container creation, lifecycle, and per-game configuration
- [ ] Validate renderer/backend on target Mali GPUs
- [ ] Touch controls and gamepad mapping
- [ ] Persisted game library, multi-game management

## Release gates
- Reproducible CI build and artifact
- Device testing on specific MediaTek models with GPU documented
- No claim of Windows-game execution before validated runtime integration
- License/redistribution review for every runtime package
