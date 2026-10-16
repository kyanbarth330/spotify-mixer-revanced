# SpotEnhancer — next session

## Current state: 0.10

Work on `codex/mixer-installer` and draft PR #1. Do not merge. The user confirmed that 0.9 playback works, then reported old selections and strange queue behavior after restart. Do not interpret that feedback as full queue/Connect verification.

0.10 adds persisted **Shuffle mix**, passes its snapshot to Spotify on explicit Start Mix, re-reads playlist arguments from reused fragments, and refreshes an already open mixer when saved sources change. It retains the ratio planner, paginated native track/title retrieval, actual service readiness gating and explicit Load mix -> Start Mix workflow. Shared UI has dark forest surfaces, rounded cards and a fixed title; home groups existing features under Playback and Tools for expansion. Further visual refinement remains possible.

An own-code force-stop regression reproduced lost SharedPreferences.apply writes: the second process read the original Alpha selection after the first selected Gamma/Delta. Production `MixerPreferences.select` now checks commit before success/navigation. Pending-slot requests, shuffle and ratio gesture completion also commit. These small explicit writes deliberately trade some UI-thread I/O for a durable success signal; do not move playlist/network work to the UI thread.

## Decisions and constraints

- All project-generated UI is English; product name is SpotEnhancer.
- Keep selection/playback inside Spotify: no URL input, clipboard or external share route.
- Keep a dedicated enhancement screen and Back to Spotify.
- The requested genuine **Add to Mixer** row in Spotify's native playlist overflow menu remains unfinished. The reserved-space in-app navigation row is the current implementation.
- Never publish original/patched Spotify APKs, proprietary decompiled source or private keys. Only publish the own installer and original testhost reports/screenshots.
- Use only files explicitly available in this task. The exact original APK was reattached and is in this workspace's upload directory; do not search other conversations or Library.
- Fresh CI debug signing certificates remain an update limitation. Do not automatically uninstall either app or commit a signing key.

## Remaining work

1. **Spotify queue restoration after restart is not fixed or verified.** Changing saved selections does not change a currently playing queue; explicit Load mix -> Start Mix is required. No automatic audio restart is implemented. Investigate the explicit track pages under the existing collection context without blindly changing a working playback contract. Real Spotify process restart, manual queued items, queue display, Connect and skip behavior require device evidence.
2. Implement the native playlist overflow row using the pinned APK, without increasing the nearly full original DEX beyond its limit. Patched classes2.dex has 65,535 method references.
3. Continue the UI refinement while preserving future feature categories and working playback.
4. Resolve stable installer signing through private CI configuration when account access is available.

## Verification scope

Local delivered-payload checks must use the actual installer asset, not a separately compiled helper. Full local original-APK patch/sign checks remain private. Own Android 35 fixtures are not Spotify/login/playback end-to-end tests.

Final 0.10 build and emulator evidence links are in README.md and docs/VALIDATION.md. The emulator suite covers repeated A/B replacement, reused arguments, resumed mixer refresh, stored shuffle, safe unavailable-service controls, signing and navigation. Its separate write/force-stop/read phases verify settings across actual processes. Keep any failure and repaired result clearly distinguished.
