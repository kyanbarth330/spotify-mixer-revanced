# Installer architecture

`installer` is the sole user-facing APK. `payload` is a build-only Android module: its debug APK is opened by Gradle and only its `classes.dex` is embedded as `assets/mixer.dex`. Do not distribute the payload APK as a second application.

The installer has no network permission. Android's document picker supplies a user-selected URI; a background worker copies at most 300 MB into private cache. Package/version inspection and an exact SHA-256 gate precede patching. No external tool downloads are needed on the phone.

`PatchEngine` rewrites only `classes2.dex` with dexlib2. It requires exactly one `RETURN_VOID` in `SpotifyMainActivity.onResume()V`, inserts `invoke-static/range {p0}, MixerMenu.attach(Activity)` immediately before it, and leaves the register count unchanged. It adds the payload as `classes18.dex`. The byte-identical binary manifest and resource table remain, so no resource compiler or manifest editor is required. Original signature files and source-stamp marker are removed, while unrelated META-INF resources remain.

ZIP rebuilding stores native libraries with 16 KB data-offset alignment and resources.arsc with 4-byte alignment. This does not change ELF load-segment alignment or prove compatibility with a 16 KB-page Android device. The original APK includes libraries with 4 KB load alignment.

The device creates a persistent RSA signing key and self-signed certificate in AndroidKeyStore. apksig writes v1/v2 signatures and verifies the output before enabling installation. The local JVM round-trip test uses a disposable PKCS12 key; it does not exercise AndroidKeyStore.

A FileProvider grants read access only to the private output directory. Android 8+ unknown-source permission uses the per-app Settings screen. The install action opens Android's package installer with a temporary URI grant. The user must confirm installation. Launching that screen is not treated as proof that installation succeeded.

Interrupted/recreated flows require selecting and building again. Unsigned output is removed after each build attempt; failed signed output is removed. Selected input and completed output are private files; uninstalling or clearing app data removes them and the signing key. No automatic uninstall, silent installation or modification of installed Spotify occurs.

Runtime menu: add one tagged button to the Activity content root, avoid duplicates on resume, open an AlertDialog, save playlist text and ratio in Spotify-private preferences. This path still needs testing in Spotify's actual Android runtime. A Spotify Settings row and playback/playlist integration are separate unresolved work.
