# Installer prototype v0.1

The Android app can select a local APK using the system file picker and check for basic APK ZIP entries.

**It does not yet patch or install Spotify.** The patch/install buttons are deliberately disabled. ZIP structure validation does not establish APK authenticity or Spotify package identity.

To build: GitHub Actions > Build Android patcher prototype > Run workflow; download the artifact ZIP and extract the debug APK.

Future work:
- Verify the package identity and target version safely.
- Implement a compatible UI-only bytecode/resource patch.
- Rebuild, zipalign and sign locally on device.
- Request Android package installation with explicit user approval.
- Handle signature mismatch and existing installation safely.

Never distribute Spotify's original APK or private signing keys in this repository.
