# Android virtual-device testing

The `testhost` module compiles the exact original SpotEnhancer runtime source into an independent synthetic Android application. It includes small original test doubles for the verified fragment field names, two fake playlist identities and a fake library screen. It contains no Spotify code, APK, account, credentials or tracks.

GitHub Actions workflow `android-smoke.yml` boots an Android 35 x86_64 emulator with KVM and runs Android instrumentation tests. Tests cover:

- Fixed in-app navigation opens the enhancement Activity.
- Choose Playlist A/B returns to the synthetic library in the same app.
- Parcelable page-parameter capture enables assignment and saves two different identities/titles.
- Ratio changes persist; diagnostics and Back to Spotify navigation work in the fixture.
- Real Android Keystore generates an RSA key and apksig signs/verifies the testhost's own APK with v2/v3 signatures.

The workflow stores screenshots, instrumentation reports and logcat from this **own-code fixture only**. It never uploads original/patched Spotify APKs or private keys. The test key and signed fixture copy are deleted by the test.

These tests do not establish that Spotify's native playlist menu has been patched, that its actual navigation supplies these fields in every case, that login works, or that playback/queue mixing works. Actual Spotify results must still be checked against the supplied APK on an authorized device. The local environment has no `/dev/kvm`; a fast local hardware-accelerated emulator is unavailable, so the runner provides the virtual device.

## Results — 2026-10-16

- [Initial run 38831245397](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38831245397): 2 tests, 1 failure. Keystore signing passed; UI navigation test failed at ActivityScenario cleanup after the original Activity was replaced. The evidence shell trap also ran before tests because emulator-runner executes each script line separately.
- [Repaired run 38840272067](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38840272067): **2 tests passed, 0 failures** on Android 35. Downloaded JUnit XML confirms selection/ratio/diagnostics/back navigation and Android Keystore v2/v3 signing. Screenshots were generated but removed by Gradle's fixture uninstall before adb collection, so this run has reports/logcat only.
- Screenshot preservation now uses shell-owned files under `/sdcard/Download/spotenhancer-evidence` on the clean, own-code-only emulator. A single shell script collects them after testing. No existing user device or account is involved.

The expanded suite adds ambiguous-heading rejection/explicit-title precedence and the original installer's initial disabled-install state. Final results and screenshots must be checked before claiming those tests passed.
