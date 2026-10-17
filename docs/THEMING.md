# Theme Studio — 0.15

Five original palettes: AMOLED Green, Midnight Ocean, Velvet Purple, Rose Gold and Arctic Mint. Feature defaults off. Choices/settings commit before success. Previewing a preset does not apply it; Apply stores it. Disabling/Restore defaults turns off native conversion and restores tracked view properties, retaining other settings. Reopen screen recreates Spotify MainActivity, not the player service. It never clears data or force-kills the process. Cached screens may require manually restarting Spotify for a full refresh.

## Native integration and limits

Native p.k5k1.K(J)I is Spotify's pinned Compose packed-color -> ARGB conversion. Its original code still runs; a pure own helper maps the actual return value. The class is relocated from full classes4.dex to the existing final native-hook DEX; original classes/descriptors are preserved. No hook is added to full classes2.dex. Mapping is no-op when off and allocates no per-call arrays.

Known neutral UI shades and Spotify green accents map to the preset; alpha and unrelated hues are preserved. Already themed colors are stable across repeated mapping. The hook also affects vector/icon colors through this native path. Bitmap/video pixels are not filtered. It is not guaranteed that every native surface uses this conversion. Custom canvas code, gradients rendered by other paths and some dialogs may retain original colors.

A lifecycle-bound own observer supplements traditional Android views: bounded 2,000-view scan, at most every 250 ms for layout changes; applied properties are cached to avoid repeated background assignment/layout loops. It styles solid/gradient drawable colors and nonstateful text/icon/background tints, preserving drawable shape. Arbitrary stateful selectors are deliberately left intact to preserve custom disabled/error/focus semantics. No View text, hierarchy, padding, fonts, media, playback or playlist data is changed. No screenshot/pixel capture, network, accessibility service or system overlay permission is needed.

Saved property ownership prevents restoring over unrelated native recycled-view updates. Observers unregister on Activity destruction; Activity/root references are weak. On process restart, settings load from the existing hook. Off restores properties that were tracked, while native values cached during enabled creation may require reopening/restarting the screen.

This is global color styling, not a promise that every Spotify screen, font or layout changes. Native visual results must be confirmed on the user's device. Testhost screenshots contain only original code; they are not Spotify login/native-screen evidence.

## Verification

Pure palette tests check opacity/alpha, unrelated colors, repeat mapping and contrast (text/background >=7, muted/surface and accent/button pairs >=4.5). Synthetic DEX tests check relocation and mapping the actual return register without changing the method register count. Local actual-APK rewrite checks DEX limits; final delivered runtime must be used for patch/sign and original-class inventory. Own Android tests cover applying/saving/restoring, original view colors, unchanged artwork, and settings surviving force-stop. Results are in VALIDATION.md.
