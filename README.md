# Spotify Mixer Patcher

One Android installer app, built with GitHub Actions.

## Build and install on Android

1. Open [Build Android patcher prototype](../../actions/workflows/build-installer.yml).
2. Run the workflow and wait for a successful result.
3. Download the `spotify-mixer-patcher-prototype` artifact ZIP.
4. Extract and install `installer-debug.apk`.

## Current functionality

- Select a local APK using Android's system document picker.
- Verify the APK package name, version, DEX and manifest presence.
- Compute SHA-256 for the selected APK.
- Preview the intended Spotify Mixer interface **inside the patcher** (two playlist placeholders, ratio slider, start placeholder).

## Not implemented yet

- Inject a Settings menu entry into Spotify.
- Patch Spotify bytecode/resources, rebuild, align and sign the modified APK.
- Install patched Spotify.

**The patch and install controls remain disabled. This is not yet a working Spotify modification.** A normal Android app cannot modify the already-installed Spotify package in place. Any eventual APK installation requires Android's confirmation and may require uninstalling a differently signed Spotify build.

## Research

See [docs/RESEARCH.md](docs/RESEARCH.md). The repository contains original project source, not Spotify binaries. Do not commit proprietary Spotify APKs or signing keys.

Not affiliated with Spotify or ReVanced.
