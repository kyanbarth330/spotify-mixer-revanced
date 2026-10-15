# Spotify Mixer ReVanced

Experimental open-source prototype targeting Spotify Android 9.1.90.2270.

**Status:** source prototype only. This is not yet a functional ReVanced patch or an installable modified Spotify APK.

## Intended features
- Spotify Settings > Spotify Mixer (not integrated yet)
- Select Playlist A and B (placeholders)
- Mix ratio slider 0–100%
- Start Mixer (placeholder)

The Android Activity prototype is in `prototype/MixerActivity.java`. It cannot be injected into Spotify without first identifying a compatible settings navigation hook and building an actual patch.

## Roadmap
1. Reverse engineer the settings entry and navigation in a compatible APK.
2. Build and test a ReVanced patch for that version.
3. Set up reproducible patch builds and installation instructions.
4. Explore actual playback queue mixing without the Spotify Web API.

Do not commit Spotify APK binaries, extracted proprietary code, or signing keys. This project is not affiliated with Spotify or ReVanced.
