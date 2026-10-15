# Compiled Spotify settings layout inspection

Target APK: Spotify Android 9.1.90.2270.

Direct inspection of `res/layout/settings_page.xml` shows the compiled XML string pool contains:

- `FrameLayout`
- `androidx.constraintlayout.widget.ConstraintLayout`
- `androidx.recyclerview.widget.RecyclerView`
- `clipToPadding` and layout constraint attributes

**Interpretation:** the Settings screen uses a RecyclerView; its entries are likely populated in code, so changing the XML alone will not create a new Settings item. This is a research lead, not proof of a specific adapter class.

To reproduce locally: `python3 tools/inspect_axml_strings.py spotify.apk --resource res/layout/settings_page.xml`

**Next blocker:** identify the adapter / view binding / navigation code that fills the RecyclerView, then construct a version-specific patch. The actual UI injection, rebuilding, signing, and installing remain unimplemented.
