# Experimental native Playlist Mixer — 0.9

This integration is restricted to the exact SHA-pinned 9.1.90.2270 APK. No original/patched Spotify binary or decompiled source is distributed.

## User flow

1. Select two different playlists with the existing in-app Add to Mixer flow.
2. Open Playlist Mixer. Both native service indicators must be ready. If they are waiting, visit a playlist and start a normal Spotify track once, then return/refresh.
3. Set the A/B ratio and tap **Load mix**. Both playlists are fetched in pages of 100 items. Real playlist metadata supplies their titles. Episodes, local URIs and non-track placeholders are excluded from this track-only mix.
4. The screen shows the prepared size and actual source counts. The mix contains up to 100 unique canonical track URIs. Duplicate tracks are assigned without unnecessarily starving the other source. Insufficient source capacity changes the actual ratio, which the screen reports.
5. **Start Mix** submits that prepared list to Spotify's normal player command path. Shuffle and repeat overrides are disabled for the request. A successful command response is described as an accepted request, not proof of audible playback.
6. **Back to Spotify** returns to the normal app. Spotify's ordinary next/previous and queue UI are the intended controls. Existing manually queued items, queue presentation, Connect-device targeting and account-specific restrictions still need real-device confirmation. The patch does not implement its own audio engine or bypass restrictions.

Changing the ratio, choosing other playlists, refreshing the page or a failed reload invalidates the prepared mix. Starting consumes the prepared snapshot, preventing accidental repeat submissions; load again to retry.

## Verified metadata contract

Original code captures fully initialized existing playlist and player service objects at their constructor returns. The playlist adapter uses the existing authenticated coroutine transport and its PlaylistDataService Get method, with a bounded protobuf wire adapter built from the pinned schema's field-number metadata. The player adapter uses Context.fromTrackUris, PlayOrigin, PlayCommand, normal prepare/options builders, and the existing player service's Rx Single result.

NativeContractValidation is a local-only helper that checks the input SHA, reflection method/field signatures and 25 protobuf field constants against the supplied APK. ServiceHooks regressions check the unchanged this register (including values above 15), return placement and fail-closed matching. The original classes2.dex has 65,532 method references; the two capture references plus the existing Activity reference bring the modified DEX to 65,535. Future hook additions must respect the 65,536-reference limit or relocate classes.

No tokens are extracted. No external Web API login, URL entry, Android share route, payment/ad/DRM patch or private-key export is introduced. A transport's synchronous stub was deliberately avoided; the implementation handles its actual asynchronous continuation response.

## Limits

- Native service availability, network/authentication errors, loading playlists, malformed responses and timeouts stop preparation. Start Mix stays disabled until a real load produces a nonempty mix.
- Pagination rejects advertised incomplete totals, changing counts and more than 10,000 rows. It does not guarantee an atomic snapshot if a playlist is edited without changing its count during pagination.
- URI validation does not establish per-track playability. Spotify's normal service and player still enforce availability and account restrictions.
- The current selection entry remains the fixed navigation row. A genuine native playlist overflow row is separate unfinished work.
- Own-code unit/emulator tests and local patch/sign verification are not logged-in Spotify playback tests. Actual native response handling, audible playback and queue appearance must be confirmed on a real installation.
