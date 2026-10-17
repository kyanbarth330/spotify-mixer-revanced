# Playlist Creator — 0.18 intent-aware search

## Current flow

Green wrench -> Playlist Creator -> Enable -> name + genres/artists/search keywords + 1–500 tracks -> Generate preview -> review/remove -> Create Playlist -> confirm. Names start `SpotEnhancer-`. Open a normal playlist and Refresh connection if the search/playlist services are waiting. Everything remains in Spotify. No autoplay, URL entry, OAuth handoff, token extraction or external AI service.

Genres and artists retain quoted Spotify filters. Local rules recognize Dutch party / Nederlandse feestmuziek (including in the genre field), Dutch acoustic/chill, and rock/metal workout descriptions. “Dutch party songs” expands to six searches including carnaval, levenslied and Dutch-pop feest, Nederlandse feestmuziek and Hollandse meezingers. Extra Dutch-party keywords are retained; 80s/90s/00s become year ranges. Other phrases remain literal searches. This is a transparent heuristic, not AI, a language detector or a guarantee of mood relevance. Use **Preview search plan** before searching; the plan is also shown with the proposal.

Queries stay bounded to 12, interleaved across explicit genre/artist selectors. Up to 48 native search calls total, at most 20 pages/query and 50 results/page collect a larger candidate pool (requested count plus at least 50, capped at 1,000 target candidates). Each call retains its 30-second timeout. Clearly labelled karaoke/tribute artists or versions are excluded unless requested. Results keep within-artist search relevance while round-robin spreading exact normalized artist credits; this does not merge alternate spellings/collaborations or alternate recordings. Canonical track identities remain deduplicated. It does not use invented popularity, audio features or language scores.

Insufficient unique results fail visibly; no unrelated automatic fallback or lower count. The exact proposal remains removable/reviewable before any account write. Live Dutch-party relevance still requires user review; fixture tests cannot establish it. Removing preview tracks explicitly lowers the confirmed count. Server restrictions/errors stay in effect.

## Native creation and recovery

The original initialized authenticated RetrofitMaker supplies the search API; existing captured playlist service supplies CoroutineTransport. A single native RootlistModificationService.Modify creates the playlist with reviewed track URIs. Name/count/order are read back through the existing native playlist loader before success is displayed. Only reads retry (three attempts); creation is never retried automatically. No deletion or rollback is attempted.

Before writing, an unfinished record (operation ID, name, track IDs) commits locally. The acknowledged playlist identity is then committed. If any response or readback is uncertain, the record remains and blocks new generation, including across process restart. Check previous creation performs reads only when an identity is available. With no identity, users must inspect their Spotify library. Clear unfinished request requires explicit acknowledgement and permits another write, which can produce duplicates if a prior playlist already exists. Existing playlists are never deleted. An already dispatched remote write cannot be undone by closing the panel. Turning the feature off prevents new generation/creation and retains drafts/recovery state.

## Supported input and test limits

Exact 9.1.90.2270 SHA remains pinned. Its methods, field numbers and creation call pattern were inspected privately. Original classes.dex is full, so RetrofitMaker is relocated alongside the existing fragment manager and its four constructor returns capture the initialized factory; classes2 receives no new method reference. The supplied 9.1.92 APK is not supported and is not silently accepted.

CreatorSession unit tests cover exact counts, immutable review, stale/consumed previews, confirmation and ambiguous writes. CreatorWire tests cover nested track metadata, filtering, malformed protobuf, native create envelope/status/identity and queries. Handwritten testhost protocol doubles exercise the actual NativeCreator reflection path, creator serialization and name/count/order readback plus pending recovery, without Spotify code, credentials or network. They are confined to testhost and not bundled in installer runtime. These tests do not prove a live Spotify account playlist was created. Only own installer and own fixture evidence may be published.

## Historical 0.13

0.13 provided the saved draft form and fixture transaction only, with production generation disabled. It is superseded by the native code above; final 0.14 build/device evidence belongs in VALIDATION.md. No other requested enhancement is included in this release.
