# Supplied Spotify APK: research and injection design

## Exact input

The attached file was used directly; no replacement Spotify download was used.

| Property | Observed |
| --- | --- |
| SHA-256 | `5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4` |
| Bytes | 142,124,037 |
| Package/version | `com.spotify.music` / `9.1.90.2270` / code `147078674` |
| Minimum/target SDK | 24 / 37 |
| ZIP entries | 4,874; no duplicate names or CRC errors |
| DEX/classes | 17 / 102,449 |
| Native libraries | 40 across arm64-v8a, armeabi-v7a, x86, x86_64 |
| Original signatures | v2 and v3 verified with SDK 35 apksigner; source stamp verified |
| Certificate SHA-256 | `6505b181933344f93893d586e399b94616183f04349cb572a9e81a3335e28ffd` |

Every DEX header SHA-1 signature and Adler-32 checksum passed. Read-only ZIP CRC, DEX and native ELF inventory are reproducible with:

```sh
python3 tools/audit_apk.py /path/to/input.apk
```

ELF load-segment alignments include 4,096 and 16,384 bytes. ZIP alignment alone does not establish 16 KB-page device compatibility. The patch does not change native machine code.

This is a structural inventory of the entire archive and a targeted bytecode patch proof. It is not a complete semantic/security audit of 102,449 classes, native code, Spotify authentication, network behavior or playback.

## Investigated entry points

The binary manifest declares `com.spotify.music.SpotifyMainActivity` and aliases targeting it. The class is actually defined in `classes2.dex`, extends `Lp/ng71;`, and implements `onResume()V`. Traversal of the implementation finds exactly one normal return. The patch inserts one static call immediately before that return, passing the existing Activity receiver register without allocating additional registers.

`classes2.dex` originally contains 65,532 method IDs, close to the 65,536-entry limit. Adding the single `MixerMenu.attach(Activity)` reference produces 65,533 method IDs. The new UI implementation and its synthetic helper classes are placed in `classes18.dex`.

The resource `res/layout/settings_page.xml` exists and contains a RecyclerView. Settings-related defined classes found during earlier research include:

- `com.spotify.settings.settings.SettingsDelegate`
- `com.spotify.settings.settingsimpl.NativeSettings`
- `com.spotify.encoreconsumermobile.elements.settings.SettingsButton`
- `com.spotify.localfiles.settings.localfiles.impl.LocalFilesLibrarySettingsItemFactoryImpl`

Their names and method-ID references do not establish an insertion point in the live Settings list. A row inside Spotify Settings remains unresolved. The implemented entry point is a floating **Mixer** button in the main Activity.

The original metadata helper checked for literal `dex\\n`, rather than the newline in the DEX header; this is repaired. Its method list reports method-ID references grouped by type, not proof of declared implementations. The patch engine uses actual class definitions and method implementations.

## Rebuild strategy and verified output

The resource-free payload opens an AlertDialog in the existing Activity, so it needs no new manifest component or resource IDs. The binary manifest, resources and other DEX files are copied unchanged. Original signature entries and the source-stamp marker are removed before signing; unrelated META-INF data is preserved.

The real-input local round-trip test succeeded. An independent SHA-256 comparison found 4,872 original ZIP entries byte-identical, only `classes2.dex` modified, and `stamp-cert-sha256` removed. Added entries are `classes18.dex` and the new JAR signature files. All 18 output DEX checksums and the ZIP CRC test pass. SDK apksigner verifies the new v2 signature, and zipalign verifies 16 KB native-library data offsets and 4-byte resource alignment. See [VALIDATION.md](VALIDATION.md).

The menu stores two user-entered playlist labels/links and a ratio in private preferences. It does not query playlist contents, mix tracks, control playback, remove ads or bypass entitlements. Spotify startup, menu visibility and server-side behavior after resigning still require an actual patched-Spotify device test.

## Primary implementation references

- [dexlib2 instruction insertion](https://android.googlesource.com/platform/external/smali/+/4f26204/dexlib2/src/main/java/org/jf/dexlib2/builder/MutableMethodImplementation.java)
- [Android apksig](https://android.googlesource.com/platform/tools/apksig/)
- [AndroidKeyStore RSA private key](https://android.googlesource.com/platform/frameworks/base/+/f7167e8/keystore/java/android/security/keystore/AndroidKeyStoreRSAPrivateKey.java)

apksig is designed as a host library. The Android smoke test separately exercises it with AndroidKeyStore and a project-built fixture. That does not substitute for running modified Spotify.

No original or modified Spotify APK, extracted proprietary source or private signing key is published.
