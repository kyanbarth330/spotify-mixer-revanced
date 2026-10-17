# Validation — 2026-10-16

## Reported device failure and version 0.3 repair

The user reports that patched Spotify 0.2 closes immediately on opening; the installer itself works. Inspection of the **actual published installer artifact 12015022682** found that `assets/mixer.dex` is only 596 bytes and defines `org.example.spotifymixer.runtime.R`, with no `Mixer` helper. The previous local integration test used a separately compiled full runtime and therefore did not validate the published payload. An unresolved `Mixer.attach` invocation explains an immediate startup failure; a device stack trace has not been collected.

Version 0.3 packages every DEX from the runtime module, appends all of them to Spotify, and rejects a payload without a public static implemented `Mixer.attach(Activity)` method. The final installer artifact is checked in CI before upload. Two new JUnit regressions cover an R-only payload and a helper present in the second DEX (four Java tests total).

### Version 0.3 verification

[Build 38826272783](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38826272783) succeeds, including four Java tests and the final installer asset check. Downloaded artifact `12014794699` contains `mixer-runtime.zip`: `classes.dex` (596 bytes, R) and `classes2.dex` (6,912 bytes, Mixer and helpers), six classes total.

Local integration was repeated using **this exact downloaded runtime ZIP**, not a separately compiled helper. Both runtime DEX files are byte-identical in output `classes18.dex`/`classes19.dex`. Patch/sign/verification pass; 4,861 retained original entries remain unchanged. Independent verification reports valid v2/v3 signatures. Device confirmation of the repaired Spotify build is still pending.

The installer certificates of 0.2 and 0.3 differ because CI creates fresh debug signing keys. Updating 0.2 in place is therefore unsupported. Removing the old installer also loses its Android Keystore key, so the old patched Spotify cannot be updated with the new key. Reinstalling removes local data/downloads. Stable installer signing across CI runs remains a development limitation.

## Earlier confirmed Gradle build

[GitHub Actions run 38825522339](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38825522339) completed successfully: `:engine:test :installer:assembleDebug` on JDK 17 / Gradle 8.10.2 / Android SDK 35. The `spotify-mixer-installer` artifact contains the development installer APK. This confirms compilation, D8 packaging, runtime-DEX bundling and installer APK creation; it does not confirm Android execution.

## Confirmed locally

- Supplied APK: SHA-256 `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4`; complete ZIP CRC scan passes; all 17 DEX SHA-1/Adler-32 headers pass.
- Java engine compiled using JDK 17, dexlib2 2.5.2, Guava 27.1-android and apksig 8.7.3.
- Two JUnit tests pass: unknown-input rejection and synthetic DEX roundtrip proving the range call uses the unchanged `this` register (including a register number above 15) and preserves the return instruction.
- Two Python tests pass for short/long UTF-8 and UTF-16 string length decoding.
- Original runtime Java compiled against Android 35, converted to DEX with D8 8.7.18/min API 24.
- `LocalValidation` executed against the actual uploaded Spotify APK: original signature verification, exact hook matching, DEX reconstruction, runtime insertion, APK reconstruction, signing and output signature verification all pass.
- 4,861 retained ZIP entries were compared by uncompressed SHA-256 and were unchanged. This includes original manifest/resources, other DEX files and all 40 native libraries. Signature metadata, source-stamp hash and patched `classes2.dex` are excluded from that comparison.
- Independent apksig verification reports `verified=true`, `v1=false`, `v2=true`, `v3=true`, no errors.
- Resulting ZIP CRC passes; patched DEX SHA-1 passes; all 667 STORED entries remain 16 KiB aligned after signing. Native libraries are DEFLATED, preserving the original method.

The local test used a throwaway software PKCS12 key and a locally compiled 6,336-byte runtime DEX. It does **not** exercise Android Keystore or the exact Gradle-produced runtime. Patched Spotify and test keys are not published.

## Failures found and repaired

- Existing DEX search/magic and XML string-pool decoders were incorrect: fixed and regression-tested.
- Gradle engine compilation lacked Guava on the compile classpath: explicit Android Guava dependency added.
- An installation-result constant was not part of the public Android SDK: replaced with the public activity result code.
- AndroidX dependency checks failed: `android.useAndroidX=true` added.
- v1/JAR signing failed in the local integration run during an apksig decompression-size check. The implementation uses verified v2/v3 signing, supported by the target's minimum Android API 24. No claim is made that v1 signing works.
- Local Gradle dependency resolution could not reach the supplied proxy; full Gradle builds are performed in GitHub Actions instead. Local engine/runtime tests used explicitly downloaded dependencies, not a mocked patch.

## Still unverified

- Execution of the installer, RSA signing via Android Keystore, RAM use and installer permissions on a physical Android device.
- Android package installation completion/signature-conflict behavior on a physical device.
- Spotify startup, login, native libraries, playback, button placement, dialog/prefs and lifecycle behavior after re-signing.
- Other APK variants, split installs, Android versions/OEMs and future Spotify versions.

This is an experimental UI patch, not a complete security audit or a working audio/playlist mixing implementation. A successful build or cryptographic verification alone does not establish runtime success.

## Reproduce local integration

Keep the proprietary input and generated outputs outside the repository. Generate a throwaway PKCS12 test key with alias `test` and password `android`, then run:

```sh
gradle :engine:test :installer:assembleDebug
gradle :engine:validateLocalApk \
  -Papk=/private/spotify.apk \
  -PruntimeDex="$PWD/installer/build/generated/runtimeAssets/mixer-runtime.zip" \
  -PtestKey=/private/test.p12 \
  -PvalidationDir=/private/validation
```

The Android UI uses Android Keystore instead of this test key. Never commit APKs, test outputs or private keys.

## Version 0.4 — SpotEnhancer

The user confirms version 0.3 now starts, logs in and opens its menu. This is a user-reported device success, not an instrumented test.

Version 0.4 replaces the floating UI with a private Activity and introduces an exact-version fragment lifecycle hook for contextual playlist actions. Static inspection verified the fragment state-manager class/method and its arguments/view fields. Native button detection/placement and the new Activity are still pending device confirmation.

Local checks so far:

- Four engine tests and three playlist identity tests pass; two existing Python parser tests also pass.
- Actual supplied APK patch/sign integration passes, with 4,859 unmodified ZIP entries compared unchanged. The two additional intentional changes are `classes.dex` and `AndroidManifest.xml`.
- Binary manifest check confirms all original string indexes are preserved and exactly one private SpotEnhancer Activity is registered.
- All 102,449 original class descriptors remain present with no duplicates, including the relocated fragment state manager.
- No source or Spotify APK is uploaded for validation; the proprietary test output stays local.

The [version 0.4 build](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38827657914) succeeded, including the final installer payload check. Artifact `12015429833` was downloaded and inspected: it contains two runtime DEX files and 18 original runtime classes, including SpotEnhancerActivity and the playlist bridge. Local patch/sign/inventory verification was repeated using this exact downloaded runtime, with valid v2/v3 output signatures and the same 4,859 preserved entries. The private Activity registration and all original class descriptors were checked again.

## Version 0.5 — screenshot follow-up

The user's 0.4 screenshot shows no SpotEnhancer row. It does not identify whether the failure is the fragment hook, URI extraction, root compatibility or placement. Earlier static validation must not be treated as proof that this row worked on the device.

The final 0.5 implementation keeps all interaction inside Spotify: a fixed in-app navigation row provides panel access, Choose Playlist A/B opens Spotify's own playlist library, and a context-aware Add to Mixer action assigns the detected playlist. The fragment context bridge no longer requires a FrameLayout root. A diagnostics page records hook counts/view types/key names for a precise follow-up if detection still fails. No external share or launcher route is included.

Four engine tests and four playlist identity tests pass locally. Two Python parser tests pass. The Spotify playlist-library route string was verified in the supplied APK. Actual in-app placement/context selection still requires device confirmation; selection is disabled if no unambiguous visible playlist identity is found.

The final in-app-only [0.5 build 38828901876](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38828901876) succeeded. Its `spotenhancer-installer` artifact (12016326625) was downloaded and checked: two runtime DEX files, 25 original runtime classes and the required screen/context/diagnostic helpers. Local integration was repeated with this exact runtime and the current engine source. Patch/sign verification passed; 4,859 retained entries were unchanged. The output retained all 102,449 original class descriptors with no duplicates, registered one private SpotEnhancer Activity, and contained no external SpotEnhancer launcher/share aliases. These checks do not replace a device test of the navigation row or URI capture.

## Version 0.6 — evidence from the 0.5 diagnostic screenshot

The screenshot confirms that the in-app panel/diagnostics render and the fragment hook executes (25 resume calls). Its visible page fragment is `p.bnk0`, with root `p.s3g1` and argument keys `parameters`, `page_key`, and internal-referrer keys. No URI was recognized.

Targeted source inspection of the supplied DEX confirms:

- `p.qgc.a(Intent, p.jh71, p.o031)` writes the page provider's **class name** to `page_key` and a mapper-produced **Parcelable** to `parameters`.
- `p.lso0.invoke()` reads `parameters` with `Bundle.getParcelable`.
- `p.zof0.invoke()` reads `page_key` as a String for page-class lookup. It is not a playlist URI.
- The Spotify URI wrapper `p.jh71.a` has type `android.net.Uri`.

The old reader only considered direct String/Uri values under URI keys and nested Bundles, so it ignored this contract. Version 0.6 reads the explicit Parcelable page payload's non-static fields (at most 32), accepting only complete normalized playlist identities and the verified Spotify URI wrapper. It does not scan the Activity object graph, arbitrary collections, raw 22-character IDs or every string on screen. Multiple different candidates are rejected. Unrecognized parameters are described by class/field names and types in diagnostics, never their values. A stale context for the same view is cleared when extraction fails.

Six new unit tests cover opaque Parcelable fields, inheritance, conflicting identities, duplicate/static fields, invalid data and diagnostics without value leakage. Together with four existing URI tests and four engine tests, this is 14 Java tests; the two Python parser tests also pass. The exact parameter object's class/values on the user's device remain unseen, so this is a targeted contract fix requiring device confirmation, not a claim that selection has been observed working.

[Build 38829565864](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38829565864) succeeded with 14 Java tests and the final installer asset check. The downloaded `spotenhancer-installer` artifact (12016592186) contains two runtime DEX files and 26 runtime classes, including the new reader. The exact packaged runtime was used in another local patch/sign run: verification passed, 4,859 original entries were retained unchanged, all original class descriptors remain unique/present, and private Activity registration is valid. Runtime playlist recognition on the user's device remains pending.

## Version 0.7 — continuation

User evidence now confirms two distinct playlist selections in 0.6; the screen still showed generic titles. The 0.7 UI uses a shared original, resource-independent theme for both installer and injected runtime. The final installer checker now requires the shared theme and new title helper in the actual bundled payload, avoiding the earlier missing-class failure mode.

Title fallback uses only explicit metadata or a single accessibility heading in an already recognized playlist root, bounded to 256 views/12 levels. Conflicting or truncated candidates are rejected. Tests use original synthetic headings; the actual Spotify UI hierarchy remains unverified.

The original user APK is no longer present in this continuation's reset workspace. Consequently no new patch/sign/inventory run against Spotify can be claimed for 0.7. Earlier 0.6 integration evidence remains historical; current virtual-device tests use only original fixtures. No Spotify binary, proprietary decompiled source or private key is uploaded.

The pure queue planner is a separate implementation step, not playback integration. Tests cover requested quotas and determinism, exhausted sources, cross-source duplicates/invalid entries, ratio endpoints, rounding, empty inputs and invalid ratios. Track retrieval, availability checks, native overflow integration, stable installer signing and actual Spotify playback remain pending.

[0.7 build 38840481620](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38840481620) succeeded: 19 Java tests (9 engine, 10 runtime) and the final payload check. The downloaded installer artifact 12021047862 was checked again locally: 3 runtime DEX files, 27 runtime/UI classes including the required title/theme helpers. The two Python parser tests and whitespace/shell checks also pass. Android execution evidence is recorded separately in EMULATOR_TESTS.md.

The final [Android 35 run 38841185738](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38841185738) passed all four instrumentation tests. Its downloaded JUnit report and six screenshots were checked. See [EMULATOR_TESTS.md](EMULATOR_TESTS.md) for the exact own-code-only scope, evidence artifact and screenshot-collection failures repaired along the way. No Spotify execution claim follows from these tests.

Final delivery after the diagnostic key-format repair: installer build **38841700440**, artifact **12021403453**; downloaded payload validation passes again (3 DEX/27 runtime/UI classes). Android 35 run **38841700530** passes 4/4 tests and artifact **12021354163** contains six screenshots plus reports; the repaired diagnostic key names were visually checked. The README points to this final installer. All Spotify-specific limitations above remain in effect.

## Version 0.8 — device-reported runtime rejection

The user's 0.7 screenshot shows `Invalid or duplicate runtime class` for `ui/EnhancerTheme$$ExternalSyntheticLambda0`. The class is present, but Patcher.validateRuntime only permitted the runtime package and rejected the new original ui package. The error text conflated invalid namespace and duplicate detection. Earlier CI payload inspection confirmed presence, not acceptance by the patcher's validator; the fixture signed its own APK without running this payload acceptance step.

0.8 permits both exact original package prefixes (runtime/ and ui/), retaining duplicate and unrelated-package rejection. Regressions cover the theme, its D8 lambda, duplicates and unrelated packages. CI now extracts the actual final installer asset and passes it through Patcher.validateRuntime before publishing. This check needs no Spotify APK and is not a full Spotify patch/device test.

0.8 also adds the requested terminal-style patch log: real engine callbacks report runtime DEX checks, playlist/Activity hooks, manifest changes, retained original DEX files, added DEX files, signing and signature verification. Installer lines have elapsed timestamps, a bounded selectable monospace history, Copy log, and an indeterminate activity indicator while building. Initial state explicitly says no patch has run. No progress percentages or simulated successful operations are shown. Failed-input logging has a regression test; Android signing and installer terminal initial state are covered by the fixture suite. Full original-Spotify patch logs still require the unavailable input APK.

0.8 delivery: [build 38874206796](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38874206796) passes 23 Java tests, class-presence inspection and the exact Patcher.validateRuntime check on the delivered installer asset (3 DEX/27 unique classes). Artifact 12033969094 was downloaded and inspected again. [Android 35 run 38874206894](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38874206894) passes 4/4 tests (0 failures/errors/skips), including the terminal READY state and signing callback success sequence. Evidence artifact 12034527958 includes a seventh screenshot of the actual installer terminal, visually checked. Two Python tests and whitespace checks pass. This fixes the demonstrated namespace rejection; it does not establish that a full Spotify patch/install/playback succeeds on a device.

## Version 0.9 — native mixer integration

The user reattached the exact input APK (same pinned SHA). Local NativeContractValidation confirms the native service/model method/field signatures and all 25 protobuf field numbers consumed by the original adapter. Local service-hook reconstruction succeeds with 65,534 method references before the existing Activity hook. The new integration prepares a real paginated playlist request and a normal explicit-track-list PlayCommand; no fake playback-success control is added.

24 local Java tests pass (engine/shared-core). They cover the new service hooks, protobuf decoding/errors, prepare/start invalidation/cancellation, shared-track quota allocation and previous patch/sign-input regressions. Ten runtime unit tests remain in CI. The first CI attempt failed on a missing line break in the Gradle dependency declaration for shared-core; repaired. The shared planner is a Java module consumed by engine/runtime/testhost so they execute the same implementation.

See NATIVE_MIXER.md for the experimental user flow and specific unverified queue/audio behavior. No Spotify APK, proprietary source or test key is published.

0.9 [build 38877505652](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38877505652) passes **34 Java tests** (24 engine/shared-core + 10 runtime), final payload presence checks and the exact validator on its packaged asset. Downloaded artifact **12036475207** contains 4 runtime DEX files and 58 original runtime/UI classes. Local patch/sign validation against the user-supplied APK used this exact downloaded payload: valid original/output signatures, 4,859 retained entries unchanged. Independent structure validation retains all 102,449 original class descriptors without duplicates, preserves original manifest string indexes, registers one private enhancement Activity and confirms the relocated fragment hook. Modified classes2.dex has 65,535 method references. The test output APK and PKCS12 key remain local and are not deliverables.

[Android 35 run 38877505585](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38877505585) passes **4/4 instrumentation tests** (0 failures/errors/skips, 39.581 seconds). Downloaded evidence artifact **12035604574** contains the own-code screenshots/reports. The mixer screenshot was visually inspected: native-service indicators show waiting and both Load mix/Start Mix remain disabled in the fixture, which contains no real Spotify services. This is deliberately not proof of actual track retrieval, audible playback or Spotify queue presentation. User/device confirmation of native service capture, requests, queue/manual-queue interactions and playback is still needed.

## 0.10 selection persistence, shuffle and UI

The user confirmed 0.9 playback works, then reported stale selections and strange Spotify queue behavior after restart. These are separate verification targets.

A new own-code Android 35 write/force-stop/read test reproduced loss of replacements with SharedPreferences.apply: run 38880257744 read the original Alpha URI after the write phase selected Gamma/Delta. The write phase had returned before the asynchronous disk write completed. MixerPreferences.select now uses checked commit before success/navigation. Pending-slot requests, shuffle toggles and ratio gesture completion also commit; failures show an English error. These small explicit settings writes run synchronously so success has a durable meaning. This is a limited UI-thread I/O tradeoff, not a general permission to block on playlist/network work.

The Add to Mixer bridge re-reads fragment arguments instead of storing the first URI for a reused root. Offscreen roots are excluded. On resume, the mixer invalidates an old preparation when saved source identities/settings changed. Metadata callbacks only write titles for their original identities. A prepared transaction snapshots shuffle and rejects an obsolete preparation. The Android fixture now exercises Alpha/Beta followed by Gamma/Delta, stored shuffle, resumed-screen refresh, and a reused fragment whose arguments change without a new resume hook.

A saved Shuffle mix switch supplies the native shufflingContext override on the next explicit Start Mix. Repeat overrides remain off. No audio starts automatically after restart. Shared UI uses dark forest surfaces, rounded cards and a fixed title; home groups existing features under Playback and Tools.

Initial CI build 38879952975 passed packaged-payload validation (4 DEX/59 classes); initial own-code run 38879952999 passed 5/5 tests. A later test exposed an immediate post-click assertion race; it now waits for the checked state and UI idle. The process-loss counterexample above was retained and drove the production persistence fix. Final result links are recorded below after verification.

Spotify queue restoration is still unverified and not claimed fixed. The runtime submits explicit track pages using a collection context; changing saved selections does not itself replace Spotify's currently playing queue. Use Load mix -> Start Mix after choosing new sources. There is no verified automatic restoration after Spotify is killed. Emulator screenshots are exclusively the original fixture/installer; no Spotify login, account, audio or native queue restoration was tested there.

### Final 0.10 delivery

- [Installer build 38881099426](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38881099426) succeeds, including engine/runtime unit tests, payload presence and actual Patcher acceptance. [Installer artifact 12038190296](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38881099426/artifacts/12038190296), source c4f81578c435c100176809ad6a91bc473f04348f. Delivered payload has 4 DEX/60 original runtime/UI classes. Local 25 core/engine tests also pass.
- Local patch/sign roundtrip uses this exact downloaded payload. v2/v3 verification passes; 4,859 retained ZIP entries are unchanged, all 102,449 original classes remain uniquely present, manifest strings/private Activity and relocated fragment hook validate. No proprietary output is uploaded.
- [Android 35 run 38881099313](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38881099313) succeeds. Connected suite: 7 reported, 0 failures/errors, 1 intentionally skipped phase-only test (6 executed), 66.944 seconds. Separate write and read instrumentation runs each report `OK (1 test)`, with an actual `am force-stop` between them. The read verifies Gamma/Delta identities/titles, ratio 40 and shuffle true.
- [Evidence artifact 12037323074](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38881099313/artifacts/12037323074) contains eight own-code screenshots, XML/reports/logcat and persistence-write/read.txt. Downloaded reports and replacement/shuffle screenshot were inspected. Evidence retention is seven days. This tests own code and settings persistence, not Spotify audio or queue restoration.

## 0.11 — approved concept, presets, multiple sources and full playlists

Implements the green wrench entry, private translucent panel, working feature search/categories, compact source cards, named presets, accepted-request history and full-list multi-source preparation. Checked local configuration writes preserve the previous process-stop fix. Old A/B state migrates. FullMixPlanner's tests emit 12,501 unique tracks across three sources and verify 50/30/20 weighted prefixes before exhaustion, complete tails, global deduplication, invalid/zero sources and percentage redistribution. Session regressions check three full sources, explicit start only, stale preparations, cancellation and failed reloads. Local engine/core suite: 30 tests pass. Runtime's existing 10 identity/parameter tests remain in the CI build.

Full-list semantics are documented in NATIVE_MIXER.md: percentages guide the unshuffled order, not the final counts of an entire unequal-sized union. Native shuffle randomizes that order. No 100-track cap or silent shortening remains. The 100-row request page size and per-call timeout remain. Large native player requests, Spotify queue restoration and audio are not verified by the original fixture.

Repairs during implementation: a local name collision in a UI callback prevented one intermediate compilation; it was renamed. First UI navigation test held an obsolete accessibility node, so locators are re-resolved after transitions and the test waits for the new Sources screen. Dialog confirmation uses Android's stable button ID because the theme uppercases Save. Visual inspection found client-only translucency hid the underlying Activity and old Holo slider/switch visuals; the manifest now declares the public translucent theme and widgets use Material/custom drawables. Original manifest theme-attribute mapping and public style constant were checked locally, with the final verifier asserting the reference.

Presets/history store configurations, not immutable old track queues. A history request is recorded only after native command acceptance; device audio success is not inferred. Fixture-created history entries are explicitly named as fixtures. Only original UI/code runs on the public emulator; no Spotify binaries or account credentials are uploaded.

### Final 0.11 delivery

- [Build 38885593406](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38885593406) succeeds, including engine/runtime unit tests (40 source tests total), final payload presence and actual Patcher acceptance. [Installer artifact 12039133945](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38885593406/artifacts/12039133945), source f70417c5b4acfeab7029fcac7eee45bd2f249c7c. Downloaded payload: 4 DEX/81 original runtime/UI classes.
- Full private local patch/sign roundtrip uses that exact delivered payload. v2/v3 verification passes; 4,859 retained original ZIP entries are unchanged, all 102,449 original classes remain uniquely present, original manifest string indexes and relocated fragment hook validate, and the new private Activity declares public translucent theme 0x01030010. No proprietary output or key is published.
- [Android 35 run 38885593569](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38885593569) succeeds: connected XML reports 8 tests, 0 failures/errors, 1 intentional skip (7 executed), 62.22 seconds. Separate write/read instrumentation phases each report OK (1 test), with an actual fixture force-stop between them. The second process verifies three sources, 50% first-source weight, shuffle, the saved preset and a 12,501-count fixture history entry.
- [Evidence artifact 12039159723](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38885593569/artifacts/12039159723) includes eight own-code PNGs, reports/logcat and persistence-write/read.txt (seven-day retention). Downloaded XML, phase logs, feature panel, thin-slider/custom-switch mixer, saved preset and floating wrench screenshots were inspected. The feature panel visibly dims the original fixture underneath. These are real rendered own-code screens, not the generated concept image.
- No logged-in Spotify playback, huge native command acceptance, audible output or Spotify queue restoration is claimed from these tests. Existing 0.9 audio success is user-reported and predates the full-list change.

## 0.12 — page-level Add to Mixer

Adds a green pill directly to recognized playlist FrameLayouts, independently of the global wrench/panel. The pinned p.s3g1 root was inspected locally: it extends FrameLayout and its addView override ultimately delegates to ViewGroup. No new proprietary-class hook or original DEX method reference was added. Unsupported roots are left unchanged. This is a page-scoped floating child, not an insertion into Spotify's native header action row or overflow menu.

The action re-reads current arguments and verifies the fragment still owns the same attached visible view before selecting. Posted attachment checks ownership again, null/non-playlist arguments clear the action, observers are removed on detach, and visibility checks avoid title scans. Selection commits before navigation, honors pending source indexes and rejects duplicate additions without autoplay. Direct clicking an already included playlist opens the current mix without appending it.

The own fixture tests three-source selection via the page button, direct addition without a pending request, duplicate prevention, absence in the library and argument loss. One run passed; a later run failed during the initial global-wrench lookup before entering the new flow, while its dedicated page-action test passed. The test now waits for the initial library window/UI idle, uses a longer locator budget and preserves a failure screenshot if the wrench is absent. Final evidence below belongs to the repeated complete suite, not a claim of native Spotify/UI/audio end-to-end coverage.

### Final 0.12 delivery

[Installer build 38889340556](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38889340556) succeeds with engine/runtime tests and actual packaged-runtime validation. [Installer artifact 12040699354](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38889340556/artifacts/12040699354), product source 86aca59a5e9d14de5200e564be0b1d53c3912f67, contains 4 DEX/86 original runtime/UI classes. Local 30 core/engine tests pass. Full private patch/sign testing with this exact downloaded payload passes v2/v3 verification, preserves 4,859 retained ZIP entries and all 102,449 original classes uniquely, and validates private/translucent Activity registration and relocated hook. No proprietary APK or key is uploaded.

[Own Android 35 run 38890014426](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38890014426) passes: XML reports 9 tests, 0 failures/errors, 1 intentional phase-only skip (8 executed), 68.546 seconds. Separate write and read instrumentation phases each pass one test across a real fixture force-stop. Source 94f4313 only adds test startup synchronization/failure capture to the product source above. [Evidence artifact 12041477121](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38890014426/artifacts/12041477121) contains nine own-code screenshots, reports/logcat and phase logs, retained seven days. Downloaded XML/phase logs and the page-level button screenshot were inspected. No logged-in Spotify or native Spotify screen was run on the public emulator; real Spotify placement/touch behavior still needs device confirmation. Native overflow/header integration is not claimed.

## 0.13 creator foundation — verification in progress

Production has a default-off creator draft form and no native generation/write adapter. The supplied original Spotify APK is absent in this continuation; no new original-Spotify patch/sign or native create success is claimed. Local Java 17 compilation of CreatorSession succeeds. CI unit tests and own Android UI tests will be recorded after completion. Own fixture preview/receipts are synthetic and cannot establish Spotify search, account write or audio behavior.

### 0.13 verified functional results

[Installer build 38908816863](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38908816863) succeeds for source 67b85c4, including engine/runtime tests, 4 DEX/114 unique runtime classes and exact packaged-runtime Patcher acceptance. [Own installer artifact 12049900391](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38908816863/artifacts/12049900391). No proprietary input/output APK or key was published.

[Own Android 35 run 38908817109](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38908817109) passes: downloaded XML reports 11 tests, 0 failures/errors, 1 phase-only skip (10 executed), 131.725 seconds. Both creator tests pass: production remains unable to generate, draft and toggle survive Activity recreation, fixture proposal removal/cancel/confirmation writes exactly once with the reviewed count. [Evidence artifact 12049676999](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38908817109/artifacts/12049676999) contains the own-code screenshots/reports (seven-day retention). Visual inspection found a white platform input background with low-contrast text; a subsequent explicit dark rounded input background fixes it. These screenshots precede that color correction. Native Spotify generation/account creation is still blocked and not claimed.

Final creator input-contrast correction source 15bcd59998f28c33a3606f6d4711f648c939e9af passes [build 38909775196](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38909775196), including engine/runtime tests and packaged-runtime acceptance. [Own installer 12049368418](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38909775196/artifacts/12049368418). Earlier complete functional emulator evidence above remains valid for the unchanged behavior; color-correction emulator rerun 38909775289 was still running at handoff and is not claimed passed. No new Spotify APK validation or account creation is claimed.

## 0.14 native creator — in progress

Original 9.1.90 APK reattached and matches pinned SHA. Private metadata checks pass for 20 new creator wire fields/methods and previous 25 playlist fields. 46 local engine/core tests pass. Original DEX hook rewrite relocates RetrofitMaker and av10 together; original classes.dex has 65,486 method refs, relocated DEX has 185. No new classes2 reference is needed. Search/create are real original client calls, not fixture-generated production data. Live Spotify account success is not claimed. The additional attached 9.1.92 APK remains unsupported. CI/delivered-payload roundtrip and own protocol/device tests pending.

### Final 0.14 delivery

[Build 38912224596](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38912224596) succeeds, including engine/runtime unit tests and actual delivered-payload Patcher acceptance: 4 runtime DEX / 128 original runtime/UI classes. [Own installer artifact 12051395202](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38912224596/artifacts/12051395202), source 4d2d94d15983456e2c73e83d151a5bd6a9cc64bf. Exact downloaded payload privately patches/signs the supported original APK with valid v2/v3 signatures, 4,859 retained entries unchanged, all 102,449 original classes uniquely retained, private/translucent Activity and original manifest string indexes verified. Relocated DEX contains av10 and RetrofitMaker. Private output/key are not published.

[Own Android 35 run 38912224614](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38912224614) succeeds. Downloaded XML: 12 reported tests, 0 failures/errors, 1 phase-only skip (11 executed), 162.374 seconds. All three creator tests pass: default-off saved draft/unavailable-service controls, original synthetic review/remove/confirmation, and actual NativeCreator reflection/serialization/readback against handwritten in-memory protocol doubles. The latter checks real titles, reviewed count/order, one mutation, wrong-name readback leaving durable recovery state, blocked new generation, and read-only recovery without another mutation. Separate persistence write/force-stop/read phases each pass one test. [Evidence artifact 12050808096](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38912224614/artifacts/12050808096) contains only own-code screenshots/reports/logs (seven-day retention); downloaded reports and dark-input creator screenshot were checked.

This proves compilation, packaging, original-APK patch/sign structure and own adapter/transaction behavior. No Spotify binary/account runs on the public emulator. Live account creation, search relevance, large server requests and 9.1.92 support remain unverified; only pinned 9.1.90 is accepted.

## 0.15 Theme Studio — in progress

User confirms 0.14 native Playlist Creator works on-device. New theme tests: 51 local engine/core tests pass (5 added theme regressions), including readable palette pairs and DEX return-register preservation. Actual pinned classes4 rewrite succeeds with 65,470 method refs; merged relocated hook DEX has three original classes / 486 refs. New own Android tests cover theme UI, tracked native-view restoration, unchanged bitmap drawable and saved preset across process stop. Build/emulator/delivered-payload roundtrip are pending; no native Spotify visual success is claimed yet.

### Final 0.15 delivery

[Installer build 38916246028](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38916246028) passes engine/runtime unit tests and actual packaged-runtime acceptance. [Own installer artifact 12052692365](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38916246028/artifacts/12052692365), product source b46255ed4ff4842641267c20636e48a2897a3a04. Exact downloaded runtime privately patches/signs pinned Spotify 9.1.90 with valid v2/v3 signatures; 4,858 retained ZIP entries are unchanged (classes4 is now intentionally rewritten), all 102,449 original classes remain uniquely present, original manifest indexes and private/translucent Activity verify. Final relocated DEX contains av10, RetrofitMaker and k5k1; no proprietary binary/source/key is published.

[Final own Android35 run 38916513146](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38916513146) passes: downloaded XML has 15 reported tests, 0 failures/errors, 1 intentional phase-only skip (14 executed), 176.526 seconds. All three theme tests pass: preset UI/apply/restore, tracked native-view recolor and exact restoration with bitmap drawable/filter unchanged, and execution of an own synthetic DEX transformed by the actual ThemeHook using DexClassLoader on Android 35. The last test verifies the injected conversion returns the ocean background when on and original color when off. No proprietary DEX is loaded on the public runner. Source 861d45d adds only this test to product source above. Separate settings write/force-stop/read runs each pass one test, including enabled ocean preference.

[Evidence artifact 12052823312](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38916513146/artifacts/12052823312) contains own fixture screenshots/reports/logcat/phase logs (seven-day retention); downloaded XML and phase logs were inspected. Initial full suite 38916246035 also passed 13 executed tests. Its Theme Studio ocean and blue native-view fixture screenshots were visually checked; the final run uses identical product code. Stateful native controls can retain original styles (visible in the fixture), as documented. No logged-in Spotify theming or universal native visual coverage is claimed; user confirmation remains needed. Creator account-side success is separately user-reported.

## 0.16 — session-scoped Equalizer

[Installer build 38983051463](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38983051463) passes engine/runtime unit tests and final packaged-runtime validation. [Own installer 12082135858](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38983051463/artifacts/12082135858), product source 84a6184d0bcf3c4118c5ef9da21459678942a837, contains 4 runtime DEX / 161 original runtime/UI classes. All 56 local core/engine tests pass, including five new equalizer lifecycle/curve regressions. Source parser tests pass. Exact native audio renderer/listener/adapter method contracts were checked against the pinned input; original MODIFY_AUDIO_SETTINGS declaration was verified. No new proprietary DEX hook/reference or permission is injected.

Exact downloaded payload privately patches/signs original 9.1.90 with valid v2/v3 signatures, 4,858 retained ZIP entries unchanged and all 102,449 original classes uniquely retained. Original manifest indexes/private translucent Activity and existing relocated hooks verify. No proprietary input/output or key is published.

Initial Android run 38983051473 failed one UI assertion immediately after Apply preset (saved value was not yet visible to instrumentation); earlier run 38983016023 reached an active real Android Equalizer and captured its rendered frequency bands, then failed an immediate reset assertion and a fixture window startup check. These are failed suites, not passing evidence. Test-only source 50ffde1 waits for actual persisted preferences/initial windows, avoids attempting a long scroll before any scroll view exists, and preserves a screenshot on missing UI. [Final rerun 38983771883](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38983771883) passes: downloaded XML reports 16 tests, 0 failures/errors, 1 intentional phase-only skip (15 executed), 173.842 seconds. Equalizer UI/native Android test passes, including presets, manual custom bands, reset and disable/release. Separate write/force-stop/read phases each report OK (1 test), including enabled/custom/60 Hz preference persistence. [Own-code evidence 12082462688](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38983771883/artifacts/12082462688) includes screenshots/reports/logs (seven-day retention). Downloaded XML, both phase logs and rendered equalizer screenshot were inspected. No production behavior is changed by that test-only commit.

Own audio test uses a real, non-playing AudioTrack and Android Equalizer plus an original handwritten event bus. It is not a Spotify/login/audible playback test. Live audio, headphones/Bluetooth/offload behavior and remote route transitions remain unverified. Spotify Connect/remote playback is not equalized. See EQUALIZER.md.

## 0.17 startup regression correction (2026-10-17)

User reports that the equalizer release opens successfully, while installer build #58
(commit 4bbadfc) produces a Spotify app that crashes immediately. A green installer
build and own-fixture tests did not establish native Spotify startup success.

Inspection of the explicitly supplied 9.1.90 APK identifies a definite invalid
injection: both media-wrapper constructors reuse their incoming `this` register
for integer flags before returning. The old return-site capture passed that
integer to an Object parameter, violating DEX verification types. Capture now
runs immediately after the exact MediaSession field assignment, passing its
object source register to `captureSession(MediaSession)`. Unexpected assignment
counts abort patching instead of guessing. No extra registers are allocated.

The JVM regression fixture now deliberately overwrites `this` with an integer;
all 67 core/engine tests pass. The Android transformed-DEX fixture reproduces
that reuse too. Android CI and actual Spotify startup confirmation remain
required; no device crash stack trace was supplied, so other crash causes
cannot yet be ruled out. No Spotify binaries or implementation were published.


## 0.18 — smarter Creator and five music tools (2026-10-17)

[Installer build 39023119583](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/39023119583) passes engine/runtime tests and final payload checks. [Own installer 12096504800](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/39023119583/artifacts/12096504800), product source fe7107e0158e1fb49dd48d5864832b86e38c8036. 80 local core/engine tests pass, including nine Creator intent/diversity/filter regressions and four full-list comparison tests. No new native hooks or permissions; the 0.17 startup register-reuse correction is retained.

Exact downloaded payload: 4 DEX / 281 original runtime/UI classes. Private supported-input patch/sign roundtrip passes v2/v3, 4,858 retained ZIP entries unchanged, all 102,449 original classes uniquely present, private/translucent Activity and original manifest string indexes verified. No Spotify APK, proprietary implementation or signing key was published.

Own Android run 39022478119 failed a test assertion: the new test opened Track Details without enabling its independent switch. Test-only source 758053e enables it explicitly. This failed run is not passing device evidence. [Final own Android run 39023119454](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/39023119454) passes; new test coverage includes bookmark seek against an own framework session, saved search persistence/profile exclusion, complete protocol-fixture comparison without writes, Cinema keep-screen-on cleanup, process-stop storage and visible Dutch-party query plan before any account write. These fixtures cannot establish live Spotify relevance, startup/login or audible playback. See PLAYLIST_CREATOR.md and ENHANCEMENTS.md for limits.


## 0.19 accounts and mixer metadata — verified own-code delivery

Own PHP/MySQL service and Android server-verified tier UI are implemented; no live domain deployment or login is claimed. The owner selected PHP/MySQL but supplied no server access. Six of 32 tools are Free; 26 Premium, account/diagnostics always available. Own fixtures set membership through androidTest-only reflection, not a shipped bypass. Tests cover Free locks, token Keystore encryption and one-row playlist metadata/own artwork before track preparation. Backend HTTP/MySQL tests exercise real Argon2id, ignored client upgrade claims, operator grants, session verification, password-change/logout revocation and rate limits. Final evidence is recorded below.

85 local core/engine tests pass. Native metadata fields match the supported original input: picture decoration policy12, PlaylistMetadata.pictures8, ImageGroup standard/small link1/2. No new proprietary hook is added. Mixer metadata uses a separate one-row request; covers are bounded and loaded only from supported Spotify HTTPS sources or canonical image/mosaic links. Unavailable metadata/artwork is shown as unavailable. See server/README.md for deployment and security limits.


### Final 0.19 results

- Product source 26757d0390c62a0132490606d68d98cd6e4c8d62 passes [installer build 39024353807](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/39024353807). [Own installer 12097599312](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/39024353807/artifacts/12097599312) contains 4 runtime DEX / 315 own runtime/UI classes. The exact downloaded payload privately patches/signs the supported input with valid v2/v3 signatures, 4,858 retained ZIP entries unchanged, all 102,449 original classes uniquely retained and private/translucent Activity/original manifest string indexes verified. Original INTERNET permission is present. No original/patched Spotify binary or private key is uploaded.
- [PHP/MySQL run 39024412997](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/39024412997) passes PHP lint and a real HTTP lifecycle test against disposable MySQL (Ran 1 test, OK). It covers ignored client plan/expiry upgrade claims, Argon2id/token-hash storage, bad/missing tokens, login/registration validation, operator Premium/free changes, logout, password-change revocation, logout-all, expired session rejection and rate limiting. It does not contact the live domain. Source 279d7cdf0797893bfbc0b2392c1b9f0fb32ecbbb only adds tests to the product code.
- [Final own Android35 run 39024412933](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/39024412933) passes. Downloaded XML: 30 reported tests, 0 failures/errors, 1 intentional phase-only skip (29 executed), 287.731 seconds. All four AccountAndMetadataTest methods pass: Free lock/account access, Keystore encryption without plaintext persistence, stale Premium controls unable to save after expiry, and actual mixer name/own cover rendering from a one-row native-protocol fixture before track preparation. Separate process-stop persistence write/read phases each report OK (1 test).
- [Evidence 12097154810](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/39024412933/artifacts/12097154810) contains only original test-host screenshots, XML/logcat and phase logs (seven-day retention). Downloaded XML and both phase logs were inspected. Initial complete run 39024211320 also passes 28 executed tests; its account and metadata/blue-own-cover screenshots were visually inspected. The final product changes add lifetime guards and correct password-length wording, retaining that layout.
- Intermediate Android run 39024353809 failed before tests because Maven dependencies (dexlib2 and annotation artifacts) could not be resolved. That is failed CI, not passing evidence; the final complete run above resolves dependencies and passes.

The Android membership states are set through reflection in androidTest-only code; no production bypass is shipped. Backend and Android tests are separate, not a live TLS login/client/server integration test. Live domain installation/configuration, actual Spotify metadata/artwork delivery and real device Spotify startup/login/audio still require owner/device confirmation. The server README gives the exact public-root/config/schema/administrator steps. PR #1 remains draft; no merge or live deployment was performed.
