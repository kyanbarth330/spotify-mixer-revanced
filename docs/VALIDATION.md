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
