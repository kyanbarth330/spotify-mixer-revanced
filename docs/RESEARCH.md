# Spotify 9.1.90.2270 — UI-only reverse engineering

## Verified from supplied APK
- 4,874 ZIP entries; 17 DEX files.
- Binary AndroidManifest.xml exists.
- res/layout/settings_page.xml exists (compiled XML).
- This does not prove it is responsible for the visible Settings screen.

## Unresolved
- Identify the real Settings entry-point and list rendering mechanism.
- Identify a stable insertion point for a new Spotify Mixer row.
- Integrate and register a UI Activity or equivalent screen.
- Patch, sign, and run-test the APK.

## Scope
UI only: Playlist A/B placeholder buttons, 0–100% slider, Start Mixer placeholder. No queue changes, API calls or playback control.

## Local analysis
`python3 tools/inspect_apk.py path/to/spotify.apk`

Do not upload the Spotify APK or decompiled proprietary sources to this public repository.
