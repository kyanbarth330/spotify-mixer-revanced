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
