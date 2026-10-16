# Equalizer (0.16)

Green wrench -> Equalizer -> Enable equalizer. Start or pause/resume a local track, return to Equalizer and Refresh connection. Presets can be saved before playback: Flat, Bass Boost, Vocal, Treble Boost, Warm and your Custom settings. Apply preset saves/applies the chosen curve; Reset to Flat keeps the enabled switch unchanged. Disable releases our Android effect. All UI is English.

Frequency bands and gain limits come from the actual Android equalizer attached to the current positive session ID. Manual changes commit on slider release. Preset gains interpolate across logarithmic frequency and clamp to device limits. Custom levels are stored by center frequency; a different band's frequency defaults to flat. Settings persist, but effect objects/session IDs never persist across process restart. There is no playback start, volume change or background service.

The adapter uses the pinned input's AudioRenderer.addListener(AudioDriverListener) and AudioTrackAdapter.getAudioSessionId contracts. Created/destroyed events attach/release; resume events allow recovering a session created before the listener was registered. Native callbacks post to the main thread, with errors contained; session zero is never used. No additional original DEX hook/reference or manifest permission is needed: the supplied original already declares MODIFY_AUDIO_SETTINGS. Our testhost declares it separately. The original sessionId() is an allocating counter, **not an active playback session**, and must not be called to discover audio.

Only local audio is affected. Spotify Connect/remote speakers are not supported. Hardware/offload paths and competing system equalizers may reject or bypass effects. UI reports waiting/unavailable; an accepted Android effect does not prove audible processing. Use one equalizer at a time. Boosts can distort loud recordings. No automatic disabling of another app's equalizer, no global output effect, no audio recording or proprietary assets.

## Validation

Five original lifecycle/curve tests pass locally (allocation guard, range clamp, session replacement, stale close, disable/release, failure/retry and control-loss ownership). Exact APK metadata verifies the reflection methods. Own Android fixture test uses a real non-playing AudioTrack and Android Equalizer with our handwritten event bus; no Spotify binary, login or sound playback is used. CI results and installer link will be recorded after completion. Live Spotify audio/route switching still needs user testing.
