# Enhancements — 0.17

All new tools are in the in-Spotify wrench panel, searchable and grouped under Playback, Appearance and Tools. Every feature has its own saved switch, off by default. Settings Profiles itself stays separately enabled when a profile is applied. English UI only. No URL input, external share route, downloaded artwork, account tokens or additional Android permissions.

## Actual scope

| Feature | What this build does | Limits |
| --- | --- | --- |
| Sleep Timer | User minutes (1–1440), local pause request, cancellation, original-session guard | In-process; background alarm delivery can be delayed. Does not resume after process death. Request acceptance is not audible-stop proof. |
| Playlist Check | Read-only full-playlist canonical track-ID duplicate scan | Choose current playlist/mixer sources/presets/history. Different recordings with different IDs stay separate. First 100 duplicate IDs rendered; all rows checked. No delete. |
| Cinema Player | Published title/artist/cover, local play/pause/skip, fullscreen flag, Back to Spotify | Own in-Spotify Activity. Native capability flags determine enabled controls. Remote sessions read-only; no downloaded artwork. |
| Live Artwork | Dark animated colors sampled from artwork in Cinema | Only Cinema background; does not animate original Spotify pages or alter artwork pixels. Stops on leaving the screen. |
| Find in Playlists | Finds current canonical track in complete known mixer-source/preset/history playlists | Not an account-wide Spotify library scan. Exact URI matches; no fuzzy recording matching. |
| Rediscover | Up to 1,000 unique playing-track events observed after enable, oldest-first age filter | No historic Spotify account import; not proof of a completed listen. First 100 matching rows rendered. Local history can be cleared. |
| AMOLED Mode | True-black supported native background conversion, preserving preset accents | Existing Theme Studio coverage; custom rendering/cached colors can retain originals. Reopen screen if needed. |
| Compact Lists | Reversible supported Android RecyclerView row/padding reduction, 48dp minimum target | Native Compose/custom-drawn lists untouched. Own queue rows also use smaller text. |
| Queue Search | Search all Android-published items by title/artist; jump if supported | May be a partial queue window, never claimed the full Spotify queue. First 100 matches shown; refine search. |
| Queue Undo | Explicit durable snapshot of published canonical track items; confirmed replay via existing native play adapter | Manual snapshot, not automatic undo. Replays from start, unshuffled; cannot retain positions/manual-context partitions or remote state. Does not claim complete Spotify queue capture. |
| Clean Share | Copies canonical current-track/current-playlist link to clipboard | Own tool only; Spotify's native Share menu is not intercepted. No external share sheet or URL input. |
| Volume Steps | 1–10 music-stream steps per foreground volume key press | Android music volume is shared by local apps. No background/headset or remote Connect interception. |
| Playback Rules | Opt-in pause on last headphone disconnect; separately opt-in resume on new connection | Wired/USB/Bluetooth output callbacks, initial devices ignored. Local session/capability required. Device/route behavior needs live testing. |
| Session Guard | Confirms our Start Mix / restore / Rediscover replacements | Native Spotify play buttons and Connect commands are not intercepted. Queue restore/clear always retain their explicit confirmation. |
| Quick Search | Text query opens native Spotify search inside the same package | Routing request, no external browser/API. Exact supported input's routes still need device confirmation. |
| Seek Buttons | ±10/±30 seconds in Cinema using current position/speed/time and duration clamp | Local session with seek capability required. No original native-player control injection. |
| Confirm Queue Clear | Review native upcoming count; confirm one revision-bound SetQueue request preserving previous models; read back current identity, preserved previous items and empty upcoming window | Own action only; native clear/reorder buttons not intercepted. Context/autoplay can refill, in which case no success is claimed. Uncertain writes block another request across restart; verify is read-only, manual forget requires checking Spotify. |
| Always Show Track Info | Up to three lines in Android TextViews matching the current title/artist | No native Compose/custom text replacement or recovery of unpublished/truncated strings. Reversible tracked properties. |
| Start Page | Home / Your playlists / Search on first launcher MainActivity resume per process | Requires MAIN + LAUNCHER with no data URI; explicit deep links preserved. No forced restart. |
| No Accidental Skip | Second same-direction press within 1.5s for Cinema and foreground next/previous key events | Native Spotify on-screen skip, background headset events and Connect not intercepted. |
| Settings Profiles | Named saved allowlisted enhancer flags/options, apply and delete with confirmation | Excludes accounts, creator recovery, mixer sources/history, queue snapshots/recovery, listening data and running timers. |

The genuine native playlist overflow-menu Add to Mixer row is still unfinished; the existing page-level pill remains. Global interception of native Share/play/skip/clear controls, complete native queue restoration, account-wide playlist membership and pre-existing listening-history import are not implemented. These limits are displayed in the relevant feature screens rather than represented by fake working controls.

## Native integration and validation

The exact original `p.bqf0` media-session wrapper is relocated whole from classes.dex alongside existing relocated hooks; both verified constructor returns capture its Android MediaSession. Controllers are restricted to our package; no notification-listener/media-control permission is requested. Captured framework sessions provide metadata, controls and published queue; capability/local-route checks gate commands. Lifecycle callbacks release observers and animation when screens close.

Confirm Queue Clear reuses the captured native player client's `p.uay` queue adapter, existing `GetQueue` Flowable's first element and `SetQueueCommand`. Exact field/method contracts are asserted locally against the pinned APK. A current revision/current-track identity must match the confirmed preview, the pending record commits before a single mutation, and readback must show the same current identity, preserved previous items and empty upcoming items. Server refusal, timeout/refill or changed session leaves explicit recovery state; never automatically retries a write. Protocol fixtures are original handwritten doubles, not Spotify source or real server results.

67 local core/engine tests pass, including link normalization, duplicate identity counts, seek/volume bounds, skip timing, wrapper hook relocation and revision-bound queue-clear uncertainty/read-only recovery. Own Android tests exercise original media sessions, synthetic transformed DEX execution, real framework transport callbacks, settings/profile exclusions, reversible rows, own playlist/queue protocol doubles, plus process-stop persistence. Build, emulator and exact delivered-payload private patch/sign/inventory results will be recorded in VALIDATION.md after completion. No live Spotify/login/audible playback result is claimed.
