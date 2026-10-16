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
