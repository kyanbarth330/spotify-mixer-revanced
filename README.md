# SpotEnhancer

An independent, local Android installer for enhancements to the exact user-supplied Spotify 9.1.90.2270 APK. Spotify binaries are never distributed in this repository or CI artifacts.

## [Download SpotEnhancer 0.4](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38827657914/artifacts/12015429833)

- A dedicated **SpotEnhancer** Activity with **Back to Spotify**, replacing the floating Mixer button/dialog.
- A **Playlist Mixer** screen inside SpotEnhancer, ready for additional features on the home screen.
- Contextual **Add to Mixer** and **SpotEnhancer** actions on supported playlist fragment views. Add to Mixer carries the playlist identity from Spotify's fragment arguments; choose Playlist A or B without typing/pasting a URL.
- Saved playlist selections and ratio. All project UI labels are English.

Playback/queue mixing remains unimplemented. Version 0.4's new playlist integration and Activity still need device confirmation. Version 0.3 startup, login and menu opening were confirmed working by the user.

## Install

Download `spotify-mixer-installer` from a successful [installer workflow](../../actions/workflows/build-installer.yml) run on branch `codex/mixer-installer`. The historical artifact name/package ID remain unchanged; the app label is **SpotEnhancer Installer**. Extract and install `installer-debug.apk`, select the original supported APK, build, then start Android installation.

Open a playlist in Spotify. The inline row has **Add to Mixer** and **SpotEnhancer**. The former opens playlist assignment; the latter opens the enhancement home. **Back to Spotify** finishes the enhancement Activity and returns to Spotify's existing screen.

CI currently produces fresh debug installer certificates. Updating an older installer may require removing it; this loses its Android Keystore signing key, so the previously patched Spotify cannot be updated with a newly generated key. Reinstalling Spotify removes local data/downloads. Nothing is uninstalled automatically. Stable installer signing is still a development limitation.

## Supported input

Package `com.spotify.music`, version 9.1.90.2270, code 147078674, exact SHA-256 `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4`. Other APKs and split installs are rejected. Android API 24+; reserve at least 1 GB free storage.

## Build and validation

JDK 17, Gradle 8.10.2, Android SDK platform 35:

```sh
gradle :engine:test :runtime:testDebugUnitTest :installer:assembleDebug
python3 tools/check_installer_runtime.py installer/build/outputs/apk/debug/installer-debug.apk
```

See [validation evidence](docs/VALIDATION.md) and [implementation](docs/INSTALLER.md). The installer bundles every DEX from our original runtime module and verifies that the menu helper exists. No original or patched Spotify APK, private key or proprietary source is committed.

Not affiliated with Spotify or ReVanced. There are no premium, advertisement, DRM or authentication patches.
