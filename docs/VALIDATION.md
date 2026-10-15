# Validation status

Measured locally on 2026-10-16 (Europe/Amsterdam), using the attached APK and JDK 17 / Gradle 8.10.2 / Android build-tools 35.0.0:

| Check | Result |
| --- | --- |
| Installer and payload build | PASS (`:installer:assembleDebug`) |
| JVM unit/integration tests | PASS: 3 tests, 0 failed, 0 skipped |
| Wrong-input rejection | PASS |
| Bundled payload contains `MixerMenu.attach(Activity)` | PASS; 6,228-byte DEX matches generated asset |
| Real supplied APK patch/sign round trip | PASS; exactly 1 injected call, 18 output DEX files |
| classes2.dex method IDs | 65,532 → 65,533 |
| Original ZIP content SHA-256 comparison | 4,872 entries byte-identical; only classes2.dex changed; source-stamp marker removed |
| Binary manifest comparison | PASS; byte-identical |
| Output ZIP CRC and all DEX checksums | PASS |
| Independent SDK apksigner verification | PASS; v2 verified |
| zipalign `-c -P 16 -v 4` | PASS for unsigned and signed output |
| AndroidKeyStore/FileProvider emulator smoke | Running in GitHub Actions; not yet confirmed |
| Patched Spotify launch/menu/login/playback | NOT TESTED on an Android device |

These checks demonstrate a structurally patched and correctly signed APK. They do not prove Spotify launches or accepts the new signer.

The local installer is 3,910,881 bytes with SHA-256 `1459130edbcf775fb46aa063f507e162fea72e170c65603b74626e09d2615ee6`. Debug/CI builds can differ because signing keys and build metadata differ.

## Failures found and repaired

- Missing Guava compile dependency required by dexlib2 immutable API overloads: added an explicit Android Guava dependency.
- Debug payload had `R` in classes.dex and the actual menu in classes2.dex: switched to an unsigned release payload, required exactly one DEX during packaging, and added a CI test for the actual bundled menu entry point.
- The inherited DEX parser checked literal `dex\\n`: repaired DEX magic handling and clarified that its method table reports references, not declared implementations.
- Local tooling had an incomplete JDK and unavailable default temporary directory/proxy configuration: supplied a full JDK and isolated build configuration. These are environment repairs, not runtime success evidence.

## Reproduce

```sh
gradle :installer:assembleDebug :installer:testDebugUnitTest --no-daemon
```

The normal unit run checks unsupported-input rejection and skips the proprietary-input integration test. CI never downloads Spotify.

For an authorized local copy of the exact input, generate a disposable test-only PKCS12 key and set:

```sh
keytool -genkeypair -alias test -keyalg RSA -keysize 2048 -dname 'CN=Local Test Only' -validity 365 -storetype PKCS12 -keystore /private/test.p12 -storepass test-only -keypass test-only
MIXER_TEST_APK=/private/input.apk MIXER_TEST_OUTPUT=/private/patch-test MIXER_TEST_KEYSTORE=/private/test.p12 gradle :payload:assembleRelease :installer:testDebugUnitTest --rerun-tasks
```

The integration test rewrites the actual input, reopens the output, checks the injected method reference, verifies the manifest is byte-identical and checks unchanged entry CRCs. It then signs with the disposable key and requires successful v2 verification. Output Spotify APKs stay local; do not upload them as artifacts.

Android signing smoke test, using only a source-built fixture:

```sh
gradle :installer:connectedDebugAndroidTest
```

This exercises AndroidKeyStore, apksig signature verification, repeat signing with the same key, and FileProvider readability. It does not establish that Spotify starts after patching.

## Device acceptance checklist (not yet executed)

- Select the exact APK; reject different versions/hashes and malformed files.
- Complete patch/sign on a phone, including limited storage/memory failure paths.
- Deny/grant unknown-source permission and retry installation.
- Confirm or cancel the Android install screen; do not count opening it as success.
- Check differently signed existing Spotify conflict and clean installation.
- Launch Spotify, authenticate if required, inspect logcat, verify Mixer visibility and single-button behavior across pause/resume.
- Save playlist text/ratio, close/reopen menu and restart Spotify; verify persistence.
- Check regular playback and return navigation separately. Playlist mixing is not implemented.
- Test Android 7, 8, 10 and a recent Android release; test 16 KB devices separately.
