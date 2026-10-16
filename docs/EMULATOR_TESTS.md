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

The expanded suite adds ambiguous-heading rejection/explicit-title precedence and the original installer's initial disabled-install state. [Run 38840384706](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38840384706) and [run 38840481673](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38840481673) both passed all **4 tests**. These runs still lost screenshots during fixture cleanup.

[Run 38840707147](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38840707147) exposed a screenshot-collection regression: UiAutomation does not interpret shell operators in a command string, so the chained mkdir/screencap/echo operation failed. Two tests failed at screenshot assertions, while title ambiguity and signing passed. Screenshot operations now use separate commands with a file-size assertion. Check the final rerun before claiming screenshots are preserved.


## Final verified rerun

[Run 38841185738](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38841185738) passed **4/4 instrumentation tests, 0 failures, 0 skipped** on Android 35. The downloaded JUnit XML records 18.797 seconds of test execution. [Evidence artifact 12020718592](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38841185738/artifacts/12020718592) contains six PNG screenshots, HTML/JUnit reports and logcat; retention is seven days.

All six screenshots were downloaded and visually inspected. They show the synthetic navigation entry, SpotEnhancer home, selected Alpha/Beta mixer cards and ratio, diagnostics, return to the fixture, and the actual own installer with installation disabled before a build. The mixer shot is scrolled and includes a transient selection toast; it is not an unobstructed whole-page design preview. Home and installer labels/controls render legibly with the shared dark/green theme. The navigation images explicitly identify the synthetic host; the diagnostics image comes from that same fixture. None depict Spotify execution.

Verified scope: same-app fixture navigation, Parcelable identity capture, two distinct titles from accessibility headings (no title argument), stored ratio, ambiguous-heading rejection and explicit metadata precedence, own installer initial state, Android Keystore signing/verifying the original fixture APK. This does not verify real Spotify titles, native playlist-menu insertion, login, track retrieval, playback, or actual Spotify installation.


Visual inspection also exposed Android 35's Bundle key-set object using an opaque default `toString`, so diagnostics showed `MapCollections$KeySet@…` instead of argument keys. Logging now copies keys to an explicit ArrayList, and the UI smoke test requires a `keys=[…]` report. This changes metadata formatting only; values remain excluded.


## Delivery verification after diagnostics repair

[Run 38841700530](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38841700530) passed **4/4 tests, 0 failures/errors/skips**, with 22.03 seconds of test execution. [Final evidence artifact 12021354163](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38841700530/artifacts/12021354163) was downloaded: six screenshots and the JUnit/HTML/logcat evidence are present. The new diagnostics screenshot was visually checked and now shows actual key names such as `[parameters, page_key]`, without values. Other UI behavior is covered by the same four tests described above.

The matching [installer build 38841700440](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38841700440) passes all 19 Java tests and payload validation. [Installer artifact 12021403453](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38841700440/artifacts/12021403453) was downloaded and independently checked: 3 runtime DEX files and 27 runtime/UI classes. These are the final download/test links for 0.7.

## 0.8 patch terminal

[Run 38874206894](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38874206894) passes 4 tests, 0 failures/errors/skips, in 31.52 seconds of test execution. [Evidence 12034527958](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38874206894/artifacts/12034527958) adds `07-installer-terminal-ready.png` to the existing screenshots/reports. It was downloaded and visually checked: readable green monospace terminal on black, explicit READY/no-patch-yet text, Copy log and disabled install action. Android Keystore signing now also asserts the real callback order from SIGN to verified OK. No fake successful patch log is inserted for screenshots. Actual Spotify patch logging remains untested here because the original APK is unavailable.

## 0.9 native mixer controls

[Run 38877505585](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38877505585) passes 4/4 tests. [Evidence artifact 12035604574](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38877505585/artifacts/12035604574) was downloaded; JUnit reports 0 failures/errors/skips and 39.581 seconds. The inspected mixer screenshot shows the experimental native service state plus disabled Load mix/Start Mix in the synthetic host. No fake native backend or simulated playback-success UI is installed in the fixture. Prepare/start transactions and protobuf schema/error behavior have separate pure Java tests; actual services are checked statically against the private input APK and still require a logged-in device trial.

## 0.10 — replacement and real process restart

[Run 38881099313](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38881099313) passes six connected tests; one phase-only persistence test is intentionally skipped in that suite. The fixture selects Alpha/Beta, then replaces both with Gamma/Delta while retaining shuffle. It also checks changed arguments on a reused fragment and a resumed mixer refreshing changed selections. Two explicit instrumentation phases write replacement preferences, force-stop the fixture, and read them in a new process. Both report OK (1 test).

[Evidence 12037323074](https://github.com/kyanbarth330/spotify-mixer-revanced/actions/runs/38881099313/artifacts/12037323074) contains eight screenshots and persistence-write/read.txt, retained seven days. The downloaded replacement screenshot shows Gamma/Delta and green Shuffle mix enabled. Earlier runs 38880257744 and 38880460837 reproduced lost asynchronous preference writes; production selection now waits for checked commit before reporting success. A separate immediate post-click assertion race was synchronized. See VALIDATION.md.

Only the original fixture/installer run here. Spotify login/audio, native queue display and queue restoration after a Spotify restart are not covered.
