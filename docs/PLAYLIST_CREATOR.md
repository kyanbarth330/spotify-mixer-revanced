# Playlist Creator — current scope

## Available in production

Open SpotEnhancer -> Tools -> Playlist Creator. Enable the default-off switch, fill a name and at least one of genres/artists/music preferences, choose 1–500 tracks, and Save draft. The draft and feature switch commit to device storage. Switching off disables the form and invalidates any current proposal. Saved preferences are retained. Draft saving does not create a playlist.

Production has **no backend**: Generate preview is disabled and there is no Create Playlist action. The panel states this explicitly. No source-playlist mixing is presented as genre recommendation and no text-to-music AI is claimed. Version 0.12 remains the last fully evidenced mixer delivery until the new build evidence is recorded.

## Implemented transaction and original fixture

CreatorSession accepts a typed request and backend interface. A backend must supply unique canonical Spotify track identities with names/artists; duplicate IDs are collapsed. Fewer matches than requested is an error, not silent short output. Users can remove proposed tracks; confirmation uses the actual remaining count. The final track cannot be removed. A preview is immutable and replacement/removal/invalidation makes an old preview unusable.

Only explicit confirmation invokes backend.create. A preview can be submitted once; it is consumed before I/O. No failed or ambiguous write is retried automatically. A receipt must identify a canonical playlist URI and confirm the exact `SpotEnhancer-` name and reviewed count. A missing/mismatched receipt is an unconfirmed outcome. The future native adapter must verify creation AND ordered item insertion before returning that receipt, handle partial failure and reconcile operation IDs. A failure can leave a partially created playlist; UI tells users to check Spotify before retrying. An already dispatched remote write cannot be undone by closing the panel.

CreatorFixtureActivity is testhost-only deterministic data, labelled SYNTHETIC FIXTURE — NOT SPOTIFY. It reuses CreatorPanel with an own backend, stores no credentials, calls no network and creates no Spotify playlist. It is not included in installer runtime DEX. No production intent extra can enable this fixture backend.

## Missing native integration

The exact user-supplied Spotify APK is not present in the current workspace. Existing NativeBridge supports known-playlist reads and playback only. Search/ranking and write schemas cannot be safely guessed. The next step needs the pinned original APK, then local-only verification of native search/results, preference interpretation, account-owned creation, item addition, pagination, availability, count limits, permissions/restrictions and readback. Account/device evidence is needed after build/own-fixture checks. Do not upload Spotify binaries, decompiled source or keys to GitHub or public emulator runners.

## Feature backlog

The selected enhancements in NEXT_SESSION.md remain requirements; they are not implemented by this creator foundation. Each needs an independent persisted toggle, disabled by default, and an honest unavailable state until its native behavior is verified.
