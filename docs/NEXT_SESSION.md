# SpotEnhancer — next session

## User decisions to preserve

- Product name: **SpotEnhancer**. Keep all project-generated UI text in **English**.
- Keep the flow entirely inside Spotify: no URL input, clipboard, external share sheet or separate-app playlist selection.
- Use a dedicated enhancement screen with **Back to Spotify**, and a Playlist Mixer feature screen. More enhancement options will be added later.
- Add **Add to Mixer** as a genuine row in Spotify's playlist overflow/bottom-sheet menu, alongside the native actions shown in the user's reference screenshot (Share, Add to this playlist, Edit playlist, etc.). The existing reserved-space navigation row is not the final desired placement for this action.
- Fully restyle both **SpotEnhancer Installer** and the **in-Spotify enhancement screens** with a consistent Spotify-inspired visual theme: dark backgrounds/surfaces, Spotify green accents and primary actions, rounded cards/buttons, clear hierarchy, native-looking icons and spacing. The requested image is a design concept, not a completed implementation.

## Next functional work

1. Display the real titles of selected playlists instead of `Selected playlist`. Preserve playlist identity internally.
2. Retrieve tracks from selected playlists A and B through an appropriate, verified integration.
3. Define and implement mixing according to the selected A/B ratio, including edge cases such as empty playlists, duplicates, unavailable tracks and exhausted sources.
4. Add a genuinely functional **Start Mix** action and verified Spotify playback/queue integration. Do not present a simulated or placeholder button as working.
5. Retain simple in-app selection and a reliable return path to Spotify.

## Evidence as of the latest user messages

- User confirmed version 0.3 startup, login and initial menu operation.
- 0.4 contextual action row did not appear in the supplied screenshot.
- 0.5 in-app panel and fragment diagnostics worked; parameters use a Parcelable page payload.
- 0.6 diagnostic screenshot reports `Playlist context captured` for a page. Another page's parameters were `p.e041` with field `a:p.ks31`; do not assume every parameter object contains a playlist URI.
- User reports selecting **two different playlists** in 0.6. The screenshot shows A/B selections and a 48/52 ratio, but both titles remain generic.
- **Playback/queue mixing is not implemented.** Selection and ratio are saved only. This limitation was explicitly explained to the user.
- Fresh CI debug installer certificates still prevent seamless development updates; removing the installer loses its device signing key and reinstalling Spotify loses local data/downloads. Resolve stable installer signing before repeated production-style releases.

## Delivery constraints

Do not publish original or patched Spotify APKs, private keys or proprietary decompiled source. Build/test installer changes and document actual device results separately from static/build/signature checks. Do not infer runtime success from a successful build.

## Background work authorization

The user explicitly asked to continue while they sleep and to test on a virtual device between changes. A one-time background continuation is scheduled for this project. Continue within the existing draft PR; do not merge. An Android 35 emulator workflow and original testhost were added: see docs/EMULATOR_TESTS.md and `.github/workflows/android-smoke.yml`. Check their latest results before proceeding, fix demonstrable failures, and record screenshots/results. The fixture tests only the original SpotEnhancer runtime and synthetic parameters; never describe it as a real Spotify/login/playback test. Never upload the proprietary APK to the public runner/artifacts.

## Continuation on 2026-10-16

Version 0.7 adds a shared resource-independent dark/green UI helper for installer/runtime, card-based playlist selections and rounded controls. Playlist titles use explicit metadata first, then a bounded scan for one unambiguous accessibility heading in a root with an already verified playlist identity. The synthetic fixture no longer supplies a title argument, so its two-playlist flow exercises the heading fallback. Real Spotify behavior is not inferred from this fixture.

`engine/MixPlanner` is an original pure queue planner with ratio quotas, deterministic ordering, global deduplication, invalid-entry rejection and fallback when a source runs out. It does not retrieve tracks, determine Spotify availability or call playback. No fake Start Mix action was added.

The first emulator run failed during ActivityScenario cleanup after navigation had replaced the initial Activity. Its Android Keystore signing test passed. The workflow also ran its EXIT trap in a separate shell before tests, losing screenshots. The test now launches the navigation task directly and the shell script keeps build/evidence collection in one shell. Subsequent results belong in EMULATOR_TESTS.md.

The transient task workspace was reset before this continuation: the repository was cloned again, but the originally attached Spotify APK and local SDK/research files were no longer present. Do not search unrelated conversations/Library. Exact native overflow integration and deeper playlist title/track model inspection therefore remain blocked until the explicitly supplied APK is available again. Stable private CI signing material is also still unavailable; never commit a key as a workaround. Keep the PR draft.


Final continuation evidence: installer 0.7 build 38840481620/artifact 12021047862 passes 19 Java tests and actual payload checks; downloaded payload has 3 DEX/27 runtime/UI classes. Android 35 run 38841185738 passes 4/4 tests and artifact 12020718592 preserves six inspected screenshots plus reports/logcat (seven-day retention). Two Python tests pass. See EMULATOR_TESTS.md for repaired lifecycle/screenshot issues. Current source changes are reviewable in draft PR #1; nothing merged. Native playlist overflow Add to Mixer, verified real Spotify names/track retrieval/playback and stable CI signing remain unfinished.

Final superseding links after fixing Android 35 diagnostic key formatting: installer build 38841700440/artifact 12021403453, emulator run 38841700530/evidence artifact 12021354163. The former passes 19 Java tests and downloaded-payload checks; the latter passes 4/4 tests with six screenshots/report files present. The repaired diagnostics screenshot shows argument key names correctly. Use these links for the delivered 0.7 build.

## Version 0.8 — user-reported build failure repaired

The user supplied a physical-device screenshot of 0.7 failing on `ui/EnhancerTheme$$ExternalSyntheticLambda0`. Patcher.validateRuntime incorrectly allowed only runtime/ classes. Fixed to allow the two original runtime/ and ui/ prefixes, retaining duplicate/unrelated-package rejection. CI now invokes this exact validator on the asset extracted from the delivered installer. Do not treat 0.7's successful compilation/presence check as a successful on-device patch.

User additionally requested terminal-style logging. Added real engine callbacks, elapsed timestamps, selectable bounded monospace history, Copy log and a busy indicator. Logs distinguish checked, patched, preserved and added DEX files plus manifest/signing/verification stages. No fake progress. Build 38874206796/artifact 12033969094 passes 23 Java tests, Python payload checks and the new real-validator packaged-asset check (3 DEX/27 unique classes). User confirmation of a complete Spotify patch/install remains pending; the original Spotify APK is still unavailable here. PR remains draft.

0.8 emulator evidence: run 38874206894 passed 4/4 tests; artifact 12034527958 includes the new inspected terminal screenshot plus reports (7-day retention). Terminal callbacks were validated for real runtime acceptance, failed-input handling and actual Android Keystore signing of the own fixture. Do not claim a complete Spotify patch succeeded until user confirmation.

## Current delivery — 0.9 native mixer (supersedes previous missing-APK blocker)

The user reattached the exact original APK; it is currently in this task's upload directory. User confirms 0.8 works. Native metadata/service research and new 0.9 integration are implemented: capture p.xta0 and p.gay at constructor returns; paginate authenticated PlaylistDataService Get; decode pinned protobuf fields; load titles and track URI rows; prepare up to 100 unique tracks with the shared planner; send normal PlayCommand with explicit Context tracks and shuffle/repeat overrides disabled. UI uses Load mix -> Start Mix -> Back to Spotify and reports accepted command separately from audio success. Native services must be ready; otherwise load/start are disabled. See docs/NATIVE_MIXER.md.

Delivery: installer build **38877505652**, artifact **12036475207** (34 Java tests and actual packaged-runtime validation pass). Downloaded asset: 4 DEX/58 runtime/UI classes. Full local original-APK patch/sign roundtrip with that payload passes; 4,859 retained entries unchanged, 102,449 original classes retained uniquely, private Activity manifest valid. classes2.dex now has 65,535/65,536 method references, so future hooks need careful reference accounting/relocation. NativeContractValidation checks methods/fields and 25 protobuf field constants against the pinned input.

Android 35 own-code run **38877505585** passes 4/4 tests; evidence **12035604574** includes the inspected new mixer UI with native services waiting and Load/Start disabled, plus reports. This does not test actual native Spotify requests/audio/queue. Device feedback is the next requirement, especially service capture, returned schema, accepted PlayCommand, normal next/previous controls, manual queued items, Connect targeting and queue display. Native playlist overflow Add to Mixer and stable CI installer signing are still unfinished. Do not claim audio works from static/signature/fixture success. Never publish Spotify APKs, proprietary decompiled source or keys. PR remains draft; nothing merged.

## 0.10 work — selection refresh and shuffle

User reports 0.9 playback works, but replacing both playlists and restarting produces stale selections/queue behavior. Cached playlist identities were replaced by weak references to fragments; Add to Mixer now resolves current arguments at click time and excludes offscreen roots. This addresses reused-fragment stale identities; it is not proof of a fix for Spotify's persisted queue. A saved Shuffle mix switch is snapshotted into the prepared transaction and sent as shufflingContext on explicit Start Mix. Changing it invalidates the prepared mix. No autoplay on restart.

Shared styling now uses forest-black surfaces, larger rounded cards and a fixed screen title; the home groups actual features under Playback and Tools. Full UI refinement remains iterative. New tests cover replacement arguments without another resume hook and stale prepared transaction rejection, plus shuffle propagation. CI results must be recorded after execution. Spotify queue restoration still requires real-device reproduction; current player context uses spotify:collection:tracks with explicit pages, which may be relevant but is not proven causal. Do not change that contract blindly.
