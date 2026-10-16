# SpotEnhancer — next session

## Current state: 0.12

0.12 adds a **page-scoped Add to Mixer pill** on recognized playlist FrameLayouts (lower left, above player area). It is separate from the global wrench and is not an overflow/header-menu injection. It adds and opens the mixer directly, honors a pending source index, and avoids duplicate sources without autoplay. The existing hook schedules attachment only if the captured root still has the same owner. Current arguments and the fragment's current view are checked again on every click. Missing identities/arguments remove or hide the action. Only direct children owned by that root are reused/removed; global-layout listeners are unregistered on detach. Identity-only visibility refresh avoids rescanning headings on every layout. No new proprietary-class hook or method reference is added to original classes2.dex. The supplied pinned root p.s3g1 was verified as FrameLayout; its addView ultimately delegates to ViewGroup. Private inspection remains outside the repository.

The main Android fixture now selects three sources using the page button. A dedicated flow adds directly without a pending request, taps again without creating a duplicate, and verifies the button is absent on library pages. Reused-argument coverage also clears arguments and expects no stale context. Final 0.12 build/evidence links are in README.md and VALIDATION.md.

## Retained 0.11 behavior

Continue on `codex/mixer-installer`, draft PR #1. Do not merge. The user approved the generated three-screen concept and asked to reproduce it, including presets, multiple weighted playlists, history and full playlists. All project UI remains English.

The two top buttons are replaced by one green wrench on the right of Spotify's main app surface. It does not reserve content padding or request system overlay permission. It opens a private translucent Activity with dimmed Spotify behind it. The home panel has working search/category filters, original line icons, compact cards and routes for Playlist Mixer, Mix Presets, Mix History and Diagnostics. Appearance is an empty future category, not a fake feature list. Abstract source artwork is original placeholder art, not real playlist covers.

Mixer supports two or more distinct sources, automatic percentage redistribution, source replace/remove, saved shuffle, named presets and history. Add playlist opens Spotify's library inside Spotify; opening a playlist and tapping the wrench completes the saved source-index selection. The panel also offers Add to Mixer for a recognized current playlist. Old A/B preferences migrate. Active configuration and presets/history commit before success/navigation.

## Full playlist semantics

FullMixPlanner emits every unique canonical track from included (nonzero) sources. No 100-output-track cap, 10,000-row guard or whole-playlist 90-second deadline remains. Pagination stays at 100 rows per call, with a 30-second call timeout and malformed/incomplete/changing-count rejection. Percentages control unshuffled ordering until a source runs out; including every track cannot force arbitrary final percentage totals. Spotify shuffle randomizes that order; the UI explains this. No silent truncation is used. Very large native-player commands need real-device verification and can fail visibly.

Load mix changes to Start Mix only after a real nonempty preparation. Missing native services disable Load mix and do not expose a fake Start action. Changing sources/balance/shuffle, reloading or leaving invalidates preparation. Playback keeps the existing verified native command path; the user confirmed 0.9 playback only, not full large-playlist or queue-restoration behavior.

Presets save source identities/titles, weights and shuffle. History keeps the latest 50 accepted request configurations, timestamps and prepared counts; acceptance is not proof of audible playback. Loading an entry does not autoplay and fetches current playlist contents on the next Load mix. It is not an immutable copy of an old queue. No credentials or track copies are stored in the library.

## Verification and repairs

See README.md, VALIDATION.md and EMULATOR_TESTS.md for final build/evidence links. Local engine/core tests cover three full sources, 12,501 output tracks, exact weighted prefixes, exhaustion, overlap/invalid/zero sources, percentage updates, duplicates, stale/cancelled preparation and failed reloads. Own-code Android fixtures test panel search, selection of three playlists, stored shuffle, preset save/load, history rendering, migration/corruption/deletion, resumed screen refresh, reused fragment arguments, signing and process-stop persistence of active configuration/presets/history.

Visual checking caught old glossy Holo sliders from the translucent theme; widgets use a Material wrapper and original thin-thumb/switch styling. An Android dialog's automatic uppercase label is selected by its stable button ID in tests. Stale UI references are re-resolved after navigation. The Activity theme must be registered in the manifest, not only set client-side: Android must keep the underlying Activity visible. ManifestEditor uses public style 0x01030010 (Theme.Translucent.NoTitleBar), checked against the compiled own runtime; the pinned input already maps the theme attribute at resource-map index 0. Verification checks the final manifest theme and preserved string indexes.

## Remaining work and constraints

- **Spotify queue restoration after restart is still not fixed or verified.** Changing saved sources does not itself replace the playing queue. Explicit Load mix -> Start Mix is required. There is no automatic audio restart. Investigate existing collection-context restoration without blindly replacing a working command contract. Native queue display, manual queued items, skip/Connect behavior and large native requests require device evidence.
- The genuine Add to Mixer row in Spotify's native overflow menu remains unfinished; current selection uses the new tools panel/wrench.
- Stable private installer signing remains unresolved. Fresh CI debug certificates prevent seamless updates. Do not automatically uninstall apps or commit a signing key.
- Original classes2.dex has 65,535 method references after existing hooks. New UI/helpers live in appended DEX; do not add more hooks without accounting/relocation.
- Use only this task's explicitly available files. The exact original APK was reattached to this workspace; do not search other conversations or Library.
- Never publish original/patched Spotify APKs, proprietary decompiled source or private keys. Only publish the own installer and original fixture screenshots/reports. Own emulators are not Spotify/login/audio end-to-end tests.
