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
