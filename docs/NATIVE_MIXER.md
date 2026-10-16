# Native Playlist Mixer — 0.12

Restricted to the SHA-pinned Spotify 9.1.90.2270 input. No Spotify binary or proprietary decompiled source is distributed.

## Flow

1. Open the green wrench button on Spotify's main app surface. It opens a private translucent Activity, with Spotify dimmed underneath. No system overlay permission is used and no content padding/top navigation row is added.
2. Open Playlist Mixer and choose **Add playlist**. Spotify's library opens inside the same app. Open a playlist and tap its page-level **Add to Mixer** pill to complete the pending selection; the wrench remains an alternative. The page pill can also add a playlist directly without a pending request. It appends that source, commits and opens the mixer, or opens the existing mix without duplicating an already included source. The home panel also retains Add to Mixer for a recognized current playlist.
3. Add two or more different sources. Adjusting one percentage redistributes the others so they sum to 100. Replace/remove actions are on each source's options menu. A 0% source is excluded.
4. **Load mix** paginates every included source in pages of 100 rows, using Spotify's existing authenticated playlist service. This page size is not an output limit. Real title metadata updates the source labels. The previous 100-output-track cap, 10,000-row guard and 90-second whole-playlist deadline are removed; the 30-second individual service-call timeout and malformed/incomplete-response checks remain.
5. FullMixPlanner includes every unique canonical Spotify track URI from the included sources. Episodes/local/invalid items are excluded. Weighted fair ordering follows the chosen percentages until a source runs out, then continues with the remaining sources. **Using every track cannot also force arbitrary final percentage counts when playlist lengths differ.** With Spotify shuffle enabled the player randomizes that prepared order; the UI explains this distinction.
6. The single green action changes to **Start Mix** only after a real nonempty preparation. It submits the complete explicit track list to Spotify's normal player command path. No silent truncation or simulated success is used. Large-command/device/service limits may produce an error; logged-in playback of large lists needs device verification.
7. An accepted request creates a history entry. An accepted command is not proof of audible playback. Back to Spotify returns to the native player/queue.

Changing the configuration, refreshing or leaving the mixer invalidates preparation. Loading after cancellation cannot publish an old result. Starting consumes its immutable snapshot. Spotify's ordinary skip/queue/Connect behavior, manual queued items and restoration after process restart remain unverified; automatic audio restart is not implemented.

## Presets and history

Presets save a name, playlist identities/titles, percentages and shuffle locally. They survive process restart. History retains the latest 50 **accepted request configurations**, timestamps and prepared track counts. Neither stores credentials or copies of playlist tracks. Loading a preset/history entry loads its configuration without starting audio; the next Load mix fetches the playlists' current contents, so history is not an immutable recording of the old track sequence. Deletions require confirmation in the UI. Corrupt stored JSON produces an error rather than silently clearing the library.

Old 0.10 A/B preferences migrate on first use. Active configuration and library writes use checked commit before success/navigation, preserving the process-stop repair. A pending selection is addressed by source index and is rejected when outside the current configuration.

## Pinned native contract

Existing initialized playlist/player services are captured at constructor returns. The adapter calls PlaylistDataService Get through the existing coroutine transport, with bounded protobuf decoding based on verified schema metadata. Playback uses Context.fromTrackUris, PlayOrigin, PlayCommand, normal options builders and the existing Rx Single result. Shuffle follows the stored snapshot; repeat remains off. No token extraction, new Web API login, URL input, share sheet, restriction bypass or separate audio engine is introduced.

NativeContractValidation checks the input hash, method/field signatures and 25 protobuf field constants locally. Original classes2.dex has 65,532 method references; the existing hooks yield 65,535. No additional proprietary-class hook is added by the new panel. ManifestEditor registers the private Activity with the public Android Theme.Translucent.NoTitleBar reference; setting only the client theme does not make the server keep the underlying Activity visible.

Own-code unit/emulator tests and local patch/sign checks do not establish Spotify/login/audio end-to-end success. See VALIDATION.md for actual results. The genuine native playlist overflow row and stable private CI installer signing remain separate unfinished work.
