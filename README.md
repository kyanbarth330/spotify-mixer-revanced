# SpotEnhancer

An independent local Android installer for the exact user-supplied Spotify 9.1.90.2270 APK. No Spotify binary is distributed here.

## [Download SpotEnhancer 0.5](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38828901876/artifacts/12016326625)

Extract and install `installer-debug.apk`, select the original supported Spotify APK, build, and confirm Android installation. The app label is **SpotEnhancer Installer**.

## Entirely inside Spotify

A fixed navigation row reserves space in Spotify's Activity content layout; it does not float over content. **SpotEnhancer** opens the private enhancement screen. **Back to Spotify** returns to Spotify.

For playlist selection: **SpotEnhancer → Playlist Mixer → Choose Playlist A/B** opens Spotify's own playlist library. Open a playlist and tap **Add to Mixer** in the in-app row to assign the chosen slot. There is no URL input, clipboard, share sheet, external shortcut or separate app workflow. All project labels are English.

The contextual action is enabled only when the hook captures an unambiguous visible playlist identity. If it remains disabled, open **SpotEnhancer → Diagnostics** after visiting the playlist. The report shows hook counts/view types/argument key names, not playlist names or URLs. Device confirmation of 0.5's row and context detection is still pending. The user's 0.4 screenshot showed the previous contextual row missing; this is not claimed to have been proven fixed by a build.

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
