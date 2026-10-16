# SpotEnhancer implementation

The `installer` module is the only distributed APK. The `runtime` module provides original screens/playlist selection code; all its DEX files are bundled into `mixer-runtime.zip`. The internal package names remain unchanged for continuity.

## Patch pipeline

1. Copy the selected original APK into private cache; verify exact SHA-256 and original signature.
2. Validate the bundled runtime helper and every runtime class definition.
3. Add one private, non-exported `org.example.spotifymixer.runtime.SpotEnhancerActivity` to the binary manifest. Preserve every original manifest string index and all original XML nodes. No Spotify resource IDs or permissions are changed.
4. Retain the original main-activity lifecycle call to `Mixer.attach(Activity)`; it now creates a fixed, reserved-space in-app navigation row instead of a floating control.
5. Hook the verified fragment state manager `p.av10.n()V` to call `Mixer.fragmentResumed(Object)`. The original `classes.dex` already has 65,536 method references. Move the patched class into an appended DEX to avoid overflowing its method table; every original class descriptor is preserved exactly once.
6. Append every original runtime DEX and the relocated fragment class. Preserve all other APK payloads, strip obsolete signature/source-stamp metadata, align STORED entries, and sign with the device's Android Keystore key using v2/v3 signatures.
7. Verify output signatures and open Android's installation confirmation through FileProvider. No silent install, root or automatic uninstall.

## Playlist integration

Exact-version inspection establishes `p.av10.c` as its fragment, `p.gs10.f` as fragment arguments and `p.gs10.e1` as its view. The bridge reads these fields, accepts only a valid Spotify playlist URI/link under known argument keys, and posts UI work after the lifecycle method completes.

A FrameLayout fragment root with a playlist identity receives an inline action row. Space is reserved using top padding; there is no window overlay or global floating button. Fragments without a recognized identity or compatible root are left untouched. Reflection/UI failures are caught and logged under `SpotEnhancer`. Detection and placement still need a device test on the user's playlist screen, especially because Spotify's header is largely Compose.

`Add to Mixer` carries the detected identity into the dedicated Activity. The user selects A or B. Playlist titles are used if available in arguments; otherwise the screen uses `Selected playlist`. The URI is stored privately, never entered manually. Slider changes are saved. Playback mixing is not implemented.

## Navigation

SpotEnhancer is a normal private Android Activity inside Spotify, not an AlertDialog. Home contains Playlist Mixer. The mixer screen has a route back to the enhancement home. `Back to Spotify` finishes the Activity and returns to the existing Spotify screen. Android back is also handled. All project-generated visible labels are English.

## Development signing

GitHub debug installer keys currently differ between builds. Removing the installer loses its device-local Spotify signing key. An older patched Spotify cannot then be updated with the newly generated certificate; reinstalling loses local data/downloads. This is a known limitation, not an automatic installer action.

## Version 0.5 changes

`Mixer.attach` now places the navigation row in Android's guaranteed `android.R.id.content` FrameLayout. It reserves top space and accounts for the status inset; it does not create a WindowManager overlay. This makes panel access independent of Spotify's playlist fragment/root type. A guarded layout observer updates the contextual button only when its state/text changes.

Playlist identity capture no longer rejects a valid playlist just because its fragment root is not a FrameLayout. Captured roots are held weakly, and only attached/shown roots are considered. Conflicting identities disable selection. URI values may be Strings or Android Uri objects; legacy Spotify user-playlist identities are normalized. Root classes/argument keys and resume counts are recorded in memory without playlist identities or names.

The selection buttons persist the chosen slot and navigate to the verified `spotify:collection:playlists` route inside Spotify. A detected playlist can be assigned from the in-app navigation row. The Activity remains private. There are no external launcher/share aliases, share sheets or installer-based panel entry points.

## Version 0.6 page parameters

The observed `p.bnk0` page uses a Parcelable under `parameters`; `page_key` names a provider class. The context reader handles this explicit payload and only accepts one unambiguous complete playlist identity from its non-static fields or the verified `p.jh71.a` URI wrapper. Diagnostics include parameter type/field metadata when unsupported, without playlist values. Fixed in-app navigation and A/B selection remain unchanged.
