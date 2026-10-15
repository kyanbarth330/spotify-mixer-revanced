# Spotify Mixer Installer (experimental)

One Android installer selects **your own exact Spotify 9.1.90.2270 APK**, injects a resource-free Mixer menu, rebuilds and signs the APK locally, and opens Android's installation confirmation.

The menu contains two editable playlist labels/links and a saved 0–100% ratio. **It does not select Spotify playlists, mix playback, change the queue, or implement premium features.** The entry is a floating Mixer button in `SpotifyMainActivity`, not a row in Spotify Settings.

## Build

JDK 17, Gradle 8.10.2, Android SDK platform 35 and build-tools 35.0.0:

```sh
gradle :installer:assembleDebug :installer:testDebugUnitTest --no-daemon
```

Or run [Build Android Mixer installer](../../actions/workflows/build-installer.yml) and download `spotify-mixer-installer-experimental`. Install `installer-debug.apk`; it bundles the project's menu DEX and patch libraries, **no Spotify binary**. Keep the same installer signing key for updates; fresh CI debug keys can require reinstalling the installer, which also deletes its local Spotify signing key.

## Use

1. Install the installer on Android 7.0+ and select the original APK through the document picker.
2. Tap **Bouw en onderteken**. The installer checks the exact input hash, injects one call into the main Activity, appends `classes18.dex`, rebuilds with aligned native libraries, and verifies its new v2 signature.
3. Tap **Open Android-installatie**. Grant this installer permission to install apps if Android requests it, then confirm the system installation screen.
4. Open Spotify and look for **Mixer**. Menu visibility, login and playback need device validation; see [test status](docs/VALIDATION.md).

An official Spotify install uses a different signing certificate and cannot be updated with this output. Removing Spotify can delete local downloads. This installer never automatically removes Spotify. The signing key stays in AndroidKeyStore; removing the installer or clearing its data loses that key and prevents signing compatible future Spotify updates.

Only SHA-256 `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4` is accepted. Other builds, split APKs and already-patched inputs are unsupported. Allow at least 700 MB free storage. Low-memory devices may fail during DEX rebuilding.

## Evidence and limitations

- [APK research and patch design](docs/RESEARCH.md)
- [Validation results and repeatable tests](docs/VALIDATION.md)
- [On-device installer architecture](docs/INSTALLER.md)

No Spotify APK, extracted proprietary code, or private signing key belongs in this repository or CI artifacts. Not affiliated with Spotify or ReVanced; this implementation uses dexlib2 and Android apksig directly, not a ReVanced patch bundle.
