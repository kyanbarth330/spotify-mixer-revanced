# SpotEnhancer — next session

## Current work: 0.17 remaining enhancements

User requested all remaining selected features. 21 independent default-off tools/scoped tweaks added. Read ENHANCEMENTS.md for exact scope: several native/global behaviors remain limited, never claim full Spotify UI interception or complete queue capture. Native queue clearing now has a real revision-bound adapter, confirmation, durable uncertainty record and read-only verification. MediaHook relocates both constructors of exact p.bqf0 from original classes.dex. 65 local core/engine tests and native metadata contracts pass. Own media-session/transport/protocol/DEX/row/profile/persistence Android tests are added. CI/private delivered payload checks pending. Keep PR draft; only own installer/evidence may be public. Historical scopes below are retained.


## Current work: 0.16 Equalizer

User narrowed all-feature request to Equalizer first. Adds independent default-off persisted switch, five presets/custom frequency bands, device gain limits, local positive-session Android effect and native audio listener reflection (no new DEX hook). See EQUALIZER.md. 56 local core/engine tests pass (5 equalizer regressions). Build 38983051463 / own installer 12082135858 passes; exact delivered runtime privately patches/signs the original with all 102,449 originals unique and 4,858 retained entries unchanged. Final own Android run 38983771883 / evidence 12082462688 passes 15 executed tests, zero failures/errors, plus write/force-stop/read persistence. Test-only source 50ffde1 fixes asynchronous preference assertions and startup synchronization after initial failed suites (see VALIDATION.md). Downloaded XML/phase logs and own equalizer screenshot checked. No live Spotify audio success claimed. Previous 0.15 scope text below is historical.


## Current work: 0.15 Theme Studio

User explicitly confirmed Playlist Creator works on their logged-in device, then requested global theming with presets. Current scope is Theme Studio only, not the other enhancement backlog. Creator and mixer stay functional.

Theme Studio has a default-off persisted switch, five original dark palettes (AMOLED Green, Midnight Ocean, Velvet Purple, Rose Gold, Arctic Mint), preview, Apply, reopen screen and Restore defaults. Runtime maps neutral/Spotify-green UI colors preserving alpha and unrelated hues. Native Compose path is the verified p.k5k1.K(J)I ARGB conversion. ThemeHook relocates its complete class from full classes4.dex into the existing appended native-hook DEX with av10/RetrofitMaker. Local actual rewrite: classes4 65,470 refs, merged hook DEX 486 refs. All old classes must remain unique/present.

ThemeRuntime loads preferences from the existing MainActivity resume hook and observes subsequent Activities with lifecycle callbacks. A throttled/bounded layout pass styles solid/gradient view backgrounds, nonstateful text/background/icon tints and system bars; it leaves image/bitmap data and arbitrary stateful selectors alone. Original properties are restored only if still owned by the theme, avoiding clobbering recycled-view native updates. No bitmap filters, system overlay permission, network or automatic process kill. Reopen screens/restart Spotify may be needed for cached colors. This is global color theming, not a wholesale layout/font replacement; custom canvas/gradient/dialog coverage is not universal.

51 local engine/core tests pass, including contrast/idempotence/alpha, actual return-register hook/relocation and previous creator/mixer regressions. Own Android fixture tests added for preset UI, native-view recolor/restore, artwork unchanged, and process-stop preference persistence. Final build 38916246028 / own installer 12052692365 passes; exact payload private patch/sign/inventory checks retain all 102,449 originals and 4,858 unchanged entries. Final Android35 run 38916513146 / evidence 12052823312 passes 14 executed tests with no failures/errors, including own transformed DEX execution and write/force-stop/read theme preference checks. See VALIDATION.md. Do not claim native Spotify visual coverage from own fixture screenshots.

## Retained 0.14 native Playlist Creator

User restricted current work to Playlist Creator only. Original pinned 9.1.90 APK was reattached and SHA validated; a newer 9.1.92 APK was also supplied but **is not supported** by the exact-input patcher. Do not silently accept it or claim equivalent obfuscated contracts. No other enhancement has shipped.

NativeCreator now uses an initialized RetrofitMaker plus captured playlist service. PlaylistHook relocates RetrofitMaker alongside av10 into the final appended DEX, hooking its four constructor returns; original classes.dex stays below the reference ceiling (local rewrite: 65,486 refs). No new refs were added to the full original classes2.dex.

Search uses the existing p.sy11 API, searchview/v3/search, with native query parameters and parsed titles/artists/page tokens. Genres/artists build quoted search selectors; the description is keyword search, **not AI recommendation**. Multiple combinations are interleaved and identities deduplicated; shortages fail visibly. Up to 500 requested tracks, 50 per page, 20 rounds/query, max12 combinations. Existing CreatorSession reviews/removes/confirms.

Creation sends one RootlistModificationService.Modify request (operation=create, before=start, playlist=true, name and URI list). An acknowledged URI alone is insufficient: NativeBridge reads the new playlist and checks title, count and complete ordered list. Pending operation/name/URIs and acknowledged identity are committed before/after the single write; failed/uncertain requests block new generation across process restart. UI can verify by reads only, or clear the record after explicit check-Spotify acknowledgement (never deletes a playlist). Reads retry at most three times; writes never auto-retry. Already dispatched writes cannot be cancelled by leaving the panel. Private operation state is device-local.

46 local engine/core tests pass. Local metadata validation asserts 20 creator wire fields plus the previous 25 playlist fields and required methods; real hook rewrite preserves both relocated classes. Final source 4d2d94d passes build 38912224596 (installer 12051395202) and own Android35 run 38912224614 (evidence 12050808096: 11 executed tests, 0 failures/errors; separate persistence phases pass). Exact downloaded payload passes private patch/sign/full-original-class inventory checks; see VALIDATION.md. User has now confirmed native Playlist Creator works on their logged-in device (after the 0.14 delivery). Own protocol doubles exercise the actual reflection adapter but cannot prove Spotify server/account behavior. Keep PR #1 draft and never publish proprietary APKs, decompiled code or keys.

Backlog (not current scope): every other selected enhancement from the conversation, each with independent default-off toggle. Existing 0.12 mixer is retained.

## Retained state: 0.12

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
