# Validation — 2026-10-16

## Confirmed locally

- Supplied APK: SHA-256 `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4`; complete ZIP CRC scan passes; all 17 DEX SHA-1/Adler-32 headers pass.
- Java engine compiled using JDK 17, dexlib2 2.5.2, Guava 27.1-android and apksig 8.7.3.
- Two JUnit tests pass: unknown-input rejection and synthetic DEX roundtrip proving the range call uses the unchanged `this` register (including a register number above 15) and preserves the return instruction.
- Two Python tests pass for short/long UTF-8 and UTF-16 string length decoding.
- Original runtime Java compiled against Android 35, converted to DEX with D8 8.7.18/min API 24.
- `LocalValidation` executed against the actual uploaded Spotify APK: original signature verification, exact hook matching, DEX reconstruction, runtime insertion, APK reconstruction, signing and output signature verification all pass.
- 4,861 retained ZIP entries were compared by uncompressed SHA-256 and were unchanged. This includes original manifest/resources, other DEX files and all 40 native libraries. Signature metadata, source-stamp hash and patched `classes2.dex` are excluded from that comparison.
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
  -PruntimeDex="$PWD/installer/build/generated/runtimeAssets/mixer.dex" \
  -PtestKey=/private/test.p12 \
  -PvalidationDir=/private/validation
```

The Android UI uses Android Keystore instead of this test key. Never commit APKs, test outputs or private keys.
