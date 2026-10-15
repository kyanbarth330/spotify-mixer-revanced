# Spotify 9.1.90.2270 investigation

## Supplied artifact

SHA-256: `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4`.

142,124,037 bytes; 4,874 ZIP entries; no duplicate names or failing ZIP CRCs. All 17 DEX files have valid SHA-1 and Adler-32 headers. Their class tables contain 102,449 definitions; their method tables contain 701,029 entries (including references, not 701,029 declared methods). There are 40 compressed native libraries across arm64-v8a, armeabi-v7a, x86 and x86_64. See `APK_INVENTORY.json` for metadata.

Parsed manifest attributes: package `com.spotify.music`, version `9.1.90.2270`, code `147078674`, minimum SDK 24, target/compile SDK 37. `com.spotify.music.MainActivity` is a launcher alias targeting `com.spotify.music.SpotifyMainActivity`; the application class is `com.spotify.music.SpotifyApplication`.

## Implemented hook

The exact `Lcom/spotify/music/SpotifyMainActivity;` definition is in `classes2.dex`. The patch engine finds its actual declared `onResume()V` implementation, requires exactly one match, preserves its register count, and inserts one static range call at entry using `registerCount - 1` as `this`.

An original helper added in `classes18.dex` posts UI attachment to the main window. No new manifest component or resource IDs are necessary. The dialog stores two playlist text fields and a ratio. No login, payment, advertisements, DRM, playback or queue code is changed.

## Settings investigation

The APK contains `res/layout/settings_page.xml` and settings-related class names. Neither resource presence nor a matching string establishes the live Settings list hook. This implementation deliberately uses the verified main-activity lifecycle method. A native Spotify Settings-row patch remains unimplemented.

## Corrections to previous research tools

The old `dex_settings_classes.py` compared against a literal backslash-n rather than the DEX newline magic. The old search regex also overescaped the DEX extension. XML variable-length parsers returned incorrect offsets for short strings; UTF-8 long lengths used a 7-bit rather than 8-bit shift. Those errors are fixed. Earlier name/string observations alone must not be treated as evidence of executable hooks.

## Boundaries of the investigation

This is a complete ZIP/DEX structural inventory and a focused injection investigation, **not** a semantic audit of every one of the 102,449 classes or Spotify's native code. Static patch/sign tests do not establish that Spotify starts, authentication works, UI is unobstructed, or playback survives its changed certificate. Device results are tracked separately in `VALIDATION.md`.

All proprietary analysis and patched Spotify outputs stay local. Repository/CI contain original project sources and metadata only.
