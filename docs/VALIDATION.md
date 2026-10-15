# Validation status

Work in progress: this file is updated with actual build and round-trip results before delivery. Do not interpret implemented code as proof of on-device behavior.

## Reproduce

```sh
gradle :installer:assembleDebug :installer:testDebugUnitTest --no-daemon
```

The normal unit run checks unsupported-input rejection and skips the proprietary-input integration test. CI never downloads Spotify.

For an authorized local copy of the exact input, generate a disposable test-only PKCS12 key and set:

```sh
keytool -genkeypair -alias test -keyalg RSA -keysize 2048 -dname 'CN=Local Test Only' -validity 365 -storetype PKCS12 -keystore /private/test.p12 -storepass test-only -keypass test-only
MIXER_TEST_APK=/private/input.apk MIXER_TEST_OUTPUT=/private/patch-test MIXER_TEST_KEYSTORE=/private/test.p12 gradle :payload:assembleDebug :installer:testDebugUnitTest --rerun-tasks
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
