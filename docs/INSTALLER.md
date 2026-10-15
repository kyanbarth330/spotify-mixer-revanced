# Installer implementation

`installer` is the only user-installed project output. `runtime` builds original menu code to `mixer.dex`; `engine` contains the shared Java patch/sign implementation.

## Local pipeline

- Android document picker → private cache, with a 300 MiB limit.
- Check exact SHA-256 and original APK signature using apksig.
- Read `classes2.dex`, require precisely one `SpotifyMainActivity.onResume()V`, and insert `Mixer.attach(Activity)` using `invoke-static/range` with the original `this` register. dexlib2 recalculates indexes, branches, exception regions and DEX checksums.
- Preserve the existing binary manifest/resources and all other DEX/native payloads. Add original runtime code as `classes18.dex`.
- Reconstruct ZIP, dropping obsolete JAR signature files and source-stamp certificate hash. Preserve compression methods; align STORED entries to 16 KiB using a valid ZIP extra field.
- Sign with a device-local RSA-2048 Android Keystore key, using APK Signature Schemes v2/v3 (Android 7+); verify the generated signature before enabling Install.
- Grant temporary read access to the private output using FileProvider. Request Android's package installation screen. No root, shell commands, network calls, automatic uninstall or silent installation.

Temporary input/runtime/unsigned files are removed after each attempt. Signed output stays in private app storage until a new session/build. Rotation is handled without recreating the build activity; process death requires selecting/building again.

## UI behavior

The hook posts button attachment to the decor view after the activity resumes. A tag prevents duplicate buttons. The dialog accepts playlist text fields and a 0–100 ratio and stores these in private Spotify preferences. It does not validate playlist ownership, query Spotify APIs or change playback. There is no claim that a native Spotify Settings row or mixer playback is working.

## Installation caveats

Original Spotify and the patched package have different certificates. Android must approve installation; signature mismatch can require manually removing the original installation, losing local data/downloads. Installer data/key loss changes the signing identity. Split installations are unsupported. OEM installer behavior, Android Keystore signing, memory use and UI execution still require a physical-device test.
