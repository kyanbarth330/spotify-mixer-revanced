# Spotify 9.1.90.2270 — UI-only reverse engineering

## Verified from supplied APK
- 4,874 ZIP entries; 17 DEX files.
- Binary AndroidManifest.xml exists.
- res/layout/settings_page.xml exists (compiled XML).
- This does not prove it is responsible for the visible Settings screen.

## Unresolved
- Identify the real Settings entry-point and list rendering mechanism.
- Identify a stable insertion point for a new Spotify Mixer row.
- Integrate and register a UI Activity or equivalent screen.
- Patch, sign, and run-test the APK.

## Scope
UI only: Playlist A/B placeholder buttons, 0–100% slider, Start Mixer placeholder. No queue changes, API calls or playback control.

## Local analysis
`python3 tools/inspect_apk.py path/to/spotify.apk`

Do not upload the Spotify APK or decompiled proprietary sources to this public repository.

## Additional binary inspection (2026-10-16)

The user-supplied 9.1.90.2270 APK was inspected locally, without publishing its bytes. Plain ASCII scans found:

- `SettingsPage` in `classes.dex` and `classes2.dex`.
- `settings_page` in `classes2.dex`.
- `SettingsActivity` in `classes3.dex`.

These are **search leads, not verified classes/hooks**. DEX binary offsets are not a patch mechanism. The compiled settings layout may not control the live settings navigation. No decompilation, injection, signing or on-device install pipeline has been validated.

The installer now verifies the selected APK package name `com.spotify.music`, exact version `9.1.90.2270`, DEX/manifest presence, and computes SHA-256. It intentionally disables patch/install until a working patch exists.

## Confirmed class definitions from all 17 DEX files

A local parser traversed the DEX type/class tables (not merely ASCII searches) in the supplied 9.1.90.2270 APK. The following are **actual defined classes**:

- `classes2.dex`: `Lcom/spotify/settings/settings/SettingsDelegate;`
- `classes2.dex`: `Lcom/spotify/settings/settingsimpl/NativeSettings;`
- `classes6.dex`: `Lcom/spotify/encoreconsumermobile/elements/settings/SettingsButton;`
- `classes7.dex`: `Lcom/spotify/localfiles/settings/localfiles/impl/LocalFilesLibrarySettingsItemFactoryImpl;`
- `classes8.dex`: `Lcom/spotify/localfiles/settings/localfiles/api/LocalFilesLibrarySettingsItemFactory;`
- `classes8.dex`: `Lcom/spotify/localfiles/settings/localfiles/impl/LocalFilesSettingsItemModule;`

These names are confirmed DEX class descriptors, not guessed. They narrow the investigation to the settings item factory/navigation implementation. A settings page RecyclerView is separately confirmed in the binary layout. **No injection target or tested patch is yet established.**

## DEX method-table inspection

Read-only method-table parsing of the supplied APK confirms:

- `Lcom/spotify/settings/settings/SettingsDelegate;` declares `onIncognitoModeDisabledByTimer`.
- `Lcom/spotify/settings/settingsimpl/NativeSettings;` declares `create`, `destroy`, `enterAuthenticatedScope`, `exitAuthenticatedScope`, and `setDelegate`.
- `Lcom/spotify/encoreconsumermobile/elements/settings/SettingsButton;` declares constructors and obfuscated methods `d`, `m`.
- `Lcom/spotify/localfiles/settings/localfiles/impl/LocalFilesLibrarySettingsItemFactoryImpl;` declares `create` methods.

This establishes that the named settings classes are not yet a proven global settings list injection point. `LocalFilesLibrarySettingsItemFactoryImpl` appears specific to local-files settings. A full call-graph/decompilation of the obfuscated settings screen is still required before producing a real patch.

## Concrete patch design (2026-10-16)

The attached file, not a replacement download, is the exact input:

| Property | Observed |
| --- | --- |
| SHA-256 | `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4` |
| Bytes | 142,124,037 |
| Package/version | `com.spotify.music` / `9.1.90.2270` / code `147078674` |
| Minimum/target SDK | 24 / 37 |
| ZIP entries | 4,874 |
| DEX/classes | 17 / 102,449 |
| Native libraries | 40 across arm64-v8a, armeabi-v7a, x86, x86_64 |
| Original signatures | v2 and v3 verified with SDK 35 apksigner |
| Certificate SHA-256 | `6505b181933344f93893d586e399b94616183f04349cb572a9e81a3335e28ffd` |

All DEX header SHA-1 signatures and Adler-32 checksums passed. Read-only ZIP CRC and ELF inventory are reproducible with `python3 tools/audit_apk.py /path/to/input.apk`. Native ELF load alignments include 4,096 and 16,384 bytes. ZIP alignment alone does not establish 16 KB device compatibility.

The manifest declares `com.spotify.music.SpotifyMainActivity` and aliases targeting it. That class is defined in `classes2.dex` and has an `onResume()V` implementation. This provides a narrower injection route than reverse engineering the entire obfuscated Settings renderer. `classes2.dex` starts with 65,532 method IDs: code must stay out of this nearly full DEX; the hook adds one reference and the payload goes into DEX 18.

The existing Python metadata parser incorrectly checked for the literal characters `dex\\n`; it now checks the newline in the DEX magic. Its `methods` list is method-ID references grouped by defining type, not proof of declared implementations. The patch engine traverses actual method implementations instead.

Earlier sections describe the prior prototype and historical research. They are not evidence that the current code cannot patch; see VALIDATION.md for current measured results. This is a targeted structural investigation and patch proof, not a complete semantic/security audit of 102,449 classes. Spotify authentication, network behavior, anti-tamper reactions and playback after resigning are not established by static analysis or signature verification.

Primary implementation references: [dexlib2 MutableMethodImplementation](https://android.googlesource.com/platform/external/smali/+/4f26204/dexlib2/src/main/java/org/jf/dexlib2/builder/MutableMethodImplementation.java), [Android apksig](https://android.googlesource.com/platform/tools/apksig/), [AndroidKeyStore RSA key](https://android.googlesource.com/platform/frameworks/base/+/f7167e8/keystore/java/android/security/keystore/AndroidKeyStoreRSAPrivateKey.java). apksig is designed as a host library; using it on Android requires device testing in addition to successful compilation and host signing.
