# SpotEnhancer

An independent local Android installer for the exact user-supplied Spotify 9.1.90.2270 APK. No Spotify binary is distributed here.

## [Download SpotEnhancer 0.14](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38912224596/artifacts/12051395202)

Extract and install `installer-debug.apk`, select the original supported Spotify APK, build, and confirm Android installation. The app label is **SpotEnhancer Installer**.

## Version 0.14 — experimental native Playlist Creator

Open the green wrench -> Playlist Creator, enable it, and enter a name, genres/artists or search keywords and a requested track count (1–500). Open a normal Spotify playlist once and choose Refresh connection if services are waiting. Generate preview retrieves real Spotify search results with titles/artists; it is keyword search, not an AI model. Comma-separated artists/genres become up to 12 search combinations. Review/remove tracks, then confirm Create Playlist. Names use `SpotEnhancer-{name}`. Nothing autoplays.

The native creation request is followed by a full readback of name, count and track order. Failure or timeout keeps an unfinished-request record, blocks another write and offers read-only verification. Clear it only after checking your Spotify library; an unconfirmed request may already have created a playlist. No automatic write retries or automatic deletion. The feature switch defaults off and draft settings persist.

This is a new experimental integration with locally verified contracts and own-code protocol tests, **not confirmed account-side success**. Spotify server permissions/restrictions are respected. Supported input remains the exact **9.1.90.2270** APK; the supplied 9.1.92 APK is not yet supported. See [creator behavior and validation](docs/PLAYLIST_CREATOR.md). [0.14 build](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38912224596) and [own Android test evidence](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38912224614/artifacts/12050808096) pass; these do not establish live account-side creation.

## Version 0.13 development — Playlist Creator foundation

[Download creator draft installer](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38909775196/artifacts/12049368418) — build passes; Spotify generation remains unavailable.

The tools panel contains a default-off **Playlist Creator** with saved name, genre, artist, description and track-count preferences (1–500). Playlist names use `SpotEnhancer-{name}`. This is a draft form, **not working Spotify generation**: Generate preview is disabled in the shipped runtime because native search/create contracts have not been verified. No playlist is created by saving a draft, and no credentials or external URL workflow is used.

The original creator transaction and shared UI are tested with a clearly labelled own fixture: exact unique counts, proposal/removal, confirmation, one-shot writes and acknowledged name/count. That test is not Spotify recommendation, account creation, login or playback evidence. The original pinned APK is absent in this workspace and is needed to implement the native adapter. Existing 0.12 mixer behavior is retained. See [creator scope](docs/PLAYLIST_CREATOR.md).

## Version 0.12 — Add to Mixer on the playlist page

Recognized playlist pages now show a green **Add to Mixer** pill directly on the page, at the lower left above the player area. It is attached to that playlist fragment's FrameLayout and disappears outside a recognized playlist; it is not a restored global top bar or a native overflow-menu row. One tap durably adds the current playlist and opens the mixer. A pending source selection replaces that requested source instead. Existing playlists are not duplicated, and no audio starts automatically. The green wrench and panel remain available.

The action re-reads current arguments and checks that the fragment still owns this view before each click. Missing arguments, detached/replaced roots and ambiguous identities do not add a stale playlist. See validation for own-fixture test scope and real-device limits.

## Version 0.11 — concept-style tools panel and full mixes

A green wrench replaces the two top buttons and opens the new in-app panel: search, categories, Playlist Mixer, Mix Presets, Mix History and Diagnostics. The panel follows the approved visual concept with a dark translucent surface, compact cards, line icons and green controls. Abstract source artwork is original placeholder art, not fetched playlist covers.

The mixer supports multiple different playlists, automatic percentage balancing, replacement/removal and saved shuffle. **Load mix** reads the full included playlists; **Start Mix** appears after successful preparation. There is no 100-track output cap. Percentages guide the unshuffled order until sources run out; all their unique tracks are included. Spotify shuffle randomizes that order. Large native-player commands still require device verification and failures are reported without silently shortening the mix.

Presets store configurations. History records accepted requests and reloads current playlist contents, without autoplay. Old A/B selections migrate. See [behavior and limits](docs/NATIVE_MIXER.md) and [validation](docs/VALIDATION.md). Spotify queue restoration after restart and the native overflow-menu row remain unfinished.

## Version 0.10 — refreshed selections and saved shuffle

[Download installer 0.10](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38881099426/artifacts/12038190296). Add to Mixer reads the current fragment arguments instead of caching a playlist identity, and an already open mixer refreshes when saved sources change. **Shuffle mix** is saved and sent to Spotify with the next prepared mix; toggling it requires **Load mix → Start Mix** again. The home groups features under Playback and Tools; both apps share dark green surfaces and a fixed screen title.

Changing a saved selection does not change the queue already playing in Spotify. Choose the new A/B, then **Load mix → Start Mix** to submit it. The user confirmed 0.9 playback, and subsequently reported stale choices and unusual queue behavior after restart. Selection fixes and own-code persistence tests do **not** establish that Spotify's queue restoration is repaired. Automatic restoration of the mix after Spotify is killed is not implemented; explicitly load/start again. See the validation report for exact test scope.

## Version 0.9 — experimental native mixer

Select Playlist A/B, set the ratio and choose **Load mix → Start Mix → Back to Spotify**. Spotify's native playlist service supplies paginated tracks and real titles. The shared planner prepares up to 100 unique track URIs and reports actual A/B counts. Start Mix uses the existing Spotify player command path, with shuffle/repeat disabled for the request. Unsupported items are skipped, source shortages are reported, and errors never become fake playback-success messages.

If service indicators are waiting, open a playlist and play a normal Spotify track once, then return and refresh. **Start Mix stays disabled until an actual playlist load prepares a nonempty queue.** An accepted command is not proof of audible playback. Queue presentation, manual queue interactions and real audio still need device confirmation; these were not tested with Spotify on the public emulator.

See [native mixer behavior and limits](docs/NATIVE_MIXER.md). The native playlist overflow row remains unfinished; Add to Mixer still uses the fixed in-app row.

## Version 0.8 — device build repair and patch terminal

Fixes the on-device `Invalid or duplicate runtime class` failure for the shared theme/D8 lambda introduced in 0.7. CI now validates the actual packaged runtime using the patcher's own acceptance rules, in addition to checking class presence.

The installer has a selectable, scrollable terminal log with elapsed timestamps and **Copy log**. Real callbacks report runtime DEX validation, playlist/Activity hooks, manifest changes, retained and added `classes*.dex`, signing and signature verification. No patch operations are simulated. A failed step retains its history for diagnosis.

## Version 0.7 — themed screens and title fallback

The installer and private enhancement screens now share a dark/green theme, rounded controls and cards, density-aware spacing and system-bar styling. Selection first uses explicit playlist title metadata, then one unambiguous accessibility heading inside the recognized playlist root. Ambiguous headings retain the honest `Selected playlist` fallback; actual Spotify title extraction still requires device confirmation.

A pure `MixPlanner` now plans deterministic, deduplicated queues with ratio quotas and source-shortage fallback. It is tested independently. Version 0.9 connects it to native playlist retrieval and an experimental Start Mix command; see the current flow above.

The user confirmed selecting two distinct playlists in 0.6. See [virtual-device test scope and evidence](docs/EMULATOR_TESTS.md).

## Entirely inside Spotify

A fixed navigation row reserves space in Spotify's Activity content layout; it does not float over content. **SpotEnhancer** opens the private enhancement screen. **Back to Spotify** returns to Spotify.

For playlist selection: **SpotEnhancer → Playlist Mixer → Choose Playlist A/B** opens Spotify's own playlist library. Open a playlist and tap **Add to Mixer** in the in-app row to assign the chosen slot. There is no URL input, clipboard, share sheet, external shortcut or separate app workflow. All project labels are English.

The contextual action is enabled only when the hook captures an unambiguous visible playlist identity. If it remains disabled, open **SpotEnhancer → Diagnostics** after visiting the playlist. The report shows hook counts/view types/argument key names, not playlist names or URLs. The user confirmed panel operation and two distinct selections in 0.6. The desired native playlist overflow-menu row is still pending; the fixed navigation row remains the current entry point.

Playlist choices and ratio are saved. Version 0.9 connects an experimental native preparation/playback path; logged-in device confirmation is still required. The home screen is ready for additional enhancement features.

## Existing development installations

CI currently creates fresh debug installer certificates. Updating an older installer can require removing it; removing/clearing the installer loses its device-local Spotify signing key. Reinstalling Spotify then removes local data/downloads. Nothing is uninstalled automatically. Stable installer signing remains a development limitation.

## Supported input

Package `com.spotify.music`, version 9.1.90.2270, code 147078674, SHA-256 `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4`. Other APKs and split installations are rejected. Android API 24+, at least 1 GB free storage.

## Build

JDK 17, Gradle 8.10.2, Android SDK platform 35:

```sh
gradle :engine:test :runtime:testDebugUnitTest :installer:assembleDebug
python3 tools/check_installer_runtime.py installer/build/outputs/apk/debug/installer-debug.apk
```

The installer bundles every DEX from the original runtime module. See [validation evidence](docs/VALIDATION.md) and [implementation](docs/INSTALLER.md). No original/patched Spotify APK, proprietary source or private key is committed.

Not affiliated with Spotify or ReVanced. No premium, advertisement, DRM or authentication patches.
