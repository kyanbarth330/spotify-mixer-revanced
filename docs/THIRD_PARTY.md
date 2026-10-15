# Third-party components

The installer bundles these open-source components; Spotify itself is never bundled:

| Component | Pinned version | Upstream |
| --- | --- | --- |
| dexlib2 | 2.5.2 | https://github.com/JesusFreke/smali |
| Guava (Android) | 33.3.1-android | https://github.com/google/guava |
| Android apksig | 8.7.3 | https://android.googlesource.com/platform/tools/apksig/ |
| AndroidX Core | 1.15.0 | https://android.googlesource.com/platform/frameworks/support/ |

Gradle resolves their transitive dependencies. Preserve upstream copyright/license notices when redistributing the installer. Test libraries and build tools are not the Spotify payload. No ReVanced engine or patch bundle is used.
