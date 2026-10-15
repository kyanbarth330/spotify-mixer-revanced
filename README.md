# Spotify Mixer Installer

One Android installer that accepts a **local user-selected APK**, injects an original Mixer UI, reconstructs and signs the APK on the device, and opens Android's installation confirmation. Spotify binaries are never included in this repository or CI artifacts.

## Version 0.3 fixes the reported startup crash

Version 0.2 omitted the DEX containing `Mixer`; Spotify's injected call therefore could not resolve its helper. Use [version 0.3](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38826272783/artifacts/12014794699) and rebuild from the original APK. The final installer asset is now checked for the actual helper class and the patch engine refuses incomplete runtime payloads. Physical-device confirmation remains pending.

## Get the installer

Open [the installer workflow](../../actions/workflows/build-installer.yml), select branch `codex/mixer-installer`, and download `spotify-mixer-installer` from a successful run. Extract and install `installer-debug.apk`. This is a development build.

1. Select the supplied standalone Spotify **9.1.90.2270**, version code **147078674** APK.
2. Wait for validation, DEX injection, reconstruction and signature verification.
3. Tap **Start Android-installatie**, allow this installer as an installation source if Android requests it, and confirm installation.
4. Open Spotify. The injected `Spotify Mixer` button opens playlist text fields and a ratio slider; Save stores these locally in Spotify's app data.

**Playback/queue mixing and playlist fetching are not implemented.** The button is attached to Spotify's main activity, rather than inserted as a native Settings row. On-device behavior is not yet verified. See [validation](docs/VALIDATION.md) for the actual test results and remaining work.

For an existing 0.2 installation: the 0.3 development installer has a different certificate. Remove the old installer and the crashing patched Spotify before this test, then install 0.3 and rebuild. This removes local app data/downloads; clearing the installer loses its old signing key. Fresh CI debug keys are a known development limitation.

## Compatibility

Only the exact supplied APK is supported (SHA-256 `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4`). Other variants and split APKs are rejected. Android 7/API 24+; reserve at least 1 GB free space. Devices with little memory may fail during DEX rewriting.

The patched APK keeps package `com.spotify.music` but has a different certificate. Android normally refuses to update original Spotify with it. The installer never uninstalls Spotify automatically. Manually uninstalling Spotify removes its local data/downloads. The new signing key belongs to Android Keystore on that device; uninstalling/clearing this installer can lose it and prevent future in-place updates to patched Spotify.

## Build

JDK 17, Gradle 8.10.2, Android SDK platform 35:

```sh
gradle :engine:test :installer:assembleDebug --no-daemon
```

The `runtime` module is an internal build input: all of its original UI DEX files are bundled as an installer asset. Only one installer APK is distributed. The engine contains no entitlement, advertisement, DRM or account-authentication patches. This is an independent project, not an official Spotify or ReVanced product.
