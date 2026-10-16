# SpotEnhancer

An independent local Android installer for the exact user-supplied Spotify 9.1.90.2270 APK. No Spotify binary is distributed here.

## [Download SpotEnhancer 0.7](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38841700440/artifacts/12021403453)

Extract and install `installer-debug.apk`, select the original supported Spotify APK, build, and confirm Android installation. The app label is **SpotEnhancer Installer**.

## Version 0.7 — themed screens and title fallback

The installer and private enhancement screens now share a dark/green theme, rounded controls and cards, density-aware spacing and system-bar styling. Selection first uses explicit playlist title metadata, then one unambiguous accessibility heading inside the recognized playlist root. Ambiguous headings retain the honest `Selected playlist` fallback; actual Spotify title extraction still requires device confirmation.

A pure `MixPlanner` now plans deterministic, deduplicated queues with ratio quotas and source-shortage fallback. It is tested independently and **not connected to Spotify track retrieval or playback**. There is no Start Mix control yet.

The user confirmed selecting two distinct playlists in 0.6. See [virtual-device test scope and evidence](docs/EMULATOR_TESTS.md).

## Entirely inside Spotify

A fixed navigation row reserves space in Spotify's Activity content layout; it does not float over content. **SpotEnhancer** opens the private enhancement screen. **Back to Spotify** returns to Spotify.

For playlist selection: **SpotEnhancer → Playlist Mixer → Choose Playlist A/B** opens Spotify's own playlist library. Open a playlist and tap **Add to Mixer** in the in-app row to assign the chosen slot. There is no URL input, clipboard, share sheet, external shortcut or separate app workflow. All project labels are English.

The contextual action is enabled only when the hook captures an unambiguous visible playlist identity. If it remains disabled, open **SpotEnhancer → Diagnostics** after visiting the playlist. The report shows hook counts/view types/argument key names, not playlist names or URLs. The user confirmed panel operation and two distinct selections in 0.6. The desired native playlist overflow-menu row is still pending; the fixed navigation row remains the current entry point.

Playlist choices and ratio are saved. Actual playback/queue mixing is not implemented. The home screen is ready for additional enhancement features.

## Existing development installations

CI currently creates fresh debug installer certificates. Updating an older installer can require removing it; removing/clearing the installer loses its device-local Spotify signing key. Reinstalling Spotify then removes local data/downloads. Nothing is uninstalled automatically. Stable installer signing remains a development limitation.

## Supported input

Package `com.spotify.music`, version 9.1.90.2270, code 147078674, SHA-256 `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4`. Other APKs and split installations are rejected. Android API 24+, at least 1 GB free storage.

## Build

JDK 17, Gradle 8.10.2, Android SDK platform 35:

```sh
gradle :engine:test :runtime:testDebugUnitTest :installer:assembleDebug
python3 tools/check_installer_runtime.py installer/build/outputs/apk/debug/installer-debug.apk
```

The installer bundles every DEX from the original runtime module. See [validation evidence](docs/VALIDATION.md) and [implementation](docs/INSTALLER.md). No original/patched Spotify APK, proprietary source or private key is committed.

Not affiliated with Spotify or ReVanced. No premium, advertisement, DRM or authentication patches.
