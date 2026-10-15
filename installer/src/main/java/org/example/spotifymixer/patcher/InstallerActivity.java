package org.example.spotifymixer.patcher;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.app.AlertDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * On-device Spotify APK preflight. Does not modify, resign or install Spotify.
 * Only operates on a document explicitly selected by the user.
 */
public class InstallerActivity extends Activity {
    private static final int REQUEST_APK = 101;
    private static final long MAX_APK_BYTES = 300L * 1024 * 1024;
    private TextView status;
    private Button selectButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setPadding(32, 48, 32, 32);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18, 18, 18));
        TextView heading = new TextView(this);
        heading.setText("Spotify Mixer Patcher");
        heading.setTextSize(24);
        heading.setTextColor(Color.WHITE);
        root.addView(heading);
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setText("Kies je eigen Spotify APK. De app controleert pakketnaam, versie en bestandsstructuur. Er wordt nog niets gepatcht.");
        root.addView(status);
        selectButton = new Button(this);
        selectButton.setText("1. Selecteer Spotify APK");
        selectButton.setOnClickListener(v -> {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            startActivityForResult(picker, REQUEST_APK);
        });
        root.addView(selectButton);
        Button installed = new Button(this);
        installed.setText("Zoek geïnstalleerde Spotify");
        installed.setOnClickListener(v -> detectInstalledSpotify());
        root.addView(installed);
        Button preview = new Button(this);
        preview.setText("Bekijk Spotify Mixer UI");
        preview.setOnClickListener(v -> showMixerPreview());
        root.addView(preview);
        Button patch = new Button(this);
        patch.setText("2. Patch Spotify UI (in ontwikkeling)");
        patch.setEnabled(false);
        root.addView(patch);
        Button install = new Button(this);
        install.setText("3. Installeer aangepaste Spotify (in ontwikkeling)");
        install.setEnabled(false);
        root.addView(install);
        setContentView(root);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_APK || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        selectButton.setEnabled(false);
        status.setText("APK wordt gecontroleerd...");
        new Thread(() -> inspect(uri)).start();
    }

    private void detectInstalledSpotify() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.spotify.music", 0);
            ApplicationInfo app = getPackageManager().getApplicationInfo("com.spotify.music", 0);
            String version = info.versionName == null ? "onbekend" : info.versionName;
            boolean split = app.splitSourceDirs != null && app.splitSourceDirs.length > 0;
            File base = new File(app.sourceDir);
            String message = "Geïnstalleerde Spotify gevonden!"
                + "\\nVersie: " + version
                + "\\nBasis-APK leesbaar: " + (base.isFile() && base.canRead() ? "ja" : "nee")
                + "\\nSplit APK's: " + (split ? app.splitSourceDirs.length : 0)
                + "\\n\\n" + (split
                    ? "Dit is een split-installatie. Alleen de basis-APK patchen is niet voldoende."
                    : "Een geïnstalleerde APK kan worden onderzocht, maar nog niet gepatcht.")
                + "\\nDe huidige installatie wordt niet gewijzigd.";
            status.setText(message.replace("\\n", "\n"));
        } catch (PackageManager.NameNotFoundException e) {
            status.setText("Spotify niet gevonden. Selecteer een originele Spotify-APK via de bestandskiezer.");
        } catch (Exception e) {
            status.setText("Spotify detecteren mislukt: " + e.getMessage());
        }
    }

    private void showMixerPreview() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 8);
        TextView ratio = new TextView(this);
        ratio.setText("Playlist A: 70% · Playlist B: 30%");
        Button a = new Button(this);
        a.setText("Selecteer Playlist A");
        a.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setMessage("Playlistselectie is nog niet beschikbaar.")
            .setPositiveButton("OK", null).show());
        Button b = new Button(this);
        b.setText("Selecteer Playlist B");
        b.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setMessage("Playlistselectie is nog niet beschikbaar.")
            .setPositiveButton("OK", null).show());
        SeekBar slider = new SeekBar(this);
        slider.setMax(100);
        slider.setProgress(70);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ratio.setText("Playlist A: " + progress + "% · Playlist B: " + (100-progress) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        Button start = new Button(this);
        start.setText("Start Mixer");
        start.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setMessage("Dit is een UI-voorbeeld. Spotify wordt nog niet aangepast.")
            .setPositiveButton("OK", null).show());
        layout.addView(a);
        layout.addView(b);
        layout.addView(ratio);
        layout.addView(slider);
        layout.addView(start);
        new AlertDialog.Builder(this).setTitle("Spotify Mixer — UI-voorbeeld")
            .setView(layout).setPositiveButton("Sluiten", null).show();
    }

    private void inspect(Uri uri) {
        File apk = new File(getCacheDir(), "selected-spotify.apk");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long count = 0;
            try (InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(apk)) {
                if (input == null) throw new IllegalArgumentException("Bestand kan niet worden geopend");
                byte[] buffer = new byte[65536];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    count += read;
                    if (count > MAX_APK_BYTES) throw new IllegalArgumentException("APK is groter dan 300 MB");
                    output.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                }
            }
            boolean manifest = false, dex = false, settingsLayout = false;
            int dexCount = 0;
            try (ZipFile zip = new ZipFile(apk)) {
                for (java.util.Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                    String name = e.nextElement().getName();
                    if ("AndroidManifest.xml".equals(name)) manifest = true;
                    if (name.matches("classes([0-9]+)?\\.dex")) { dex = true; dexCount++; }
                    if ("res/layout/settings_page.xml".equals(name)) settingsLayout = true;
                }
            }
            PackageInfo info = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            String pkg = info == null ? "" : info.packageName;
            String version = info == null ? "onbekend" : info.versionName;
            boolean spotify = "com.spotify.music".equals(pkg);
            boolean supported = spotify && "9.1.90.2270".equals(version);
            String hash = hex(digest.digest());
            String message = "Pakket: " + (pkg.isEmpty() ? "niet herkend" : pkg)
                + "\\nVersie: " + version
                + "\\nDEX-bestanden: " + dexCount
                + "\\nInstellingenlayout aanwezig: " + (settingsLayout ? "ja" : "nee")
                + "\\nSHA-256: " + hash
                + "\\n\\n" + (!manifest || !dex ? "Ongeldige APK-structuur."
                   : !spotify ? "Dit is geen Spotify-APK."
                   : !supported ? "Spotify gevonden, maar deze versie is niet het onderzochte doelwit (9.1.90.2270)."
                   : "Spotify-doelversie herkend. UI-patchmotor nog niet geïmplementeerd.");
            runOnUiThread(() -> status.setText(message.replace("\\n", "\n")));
        } catch (Exception ex) {
            runOnUiThread(() -> status.setText("Controle mislukt: " + ex.getMessage()));
        } finally {
            // Keep no third-party APK copy on the device.
            apk.delete();
            runOnUiThread(() -> selectButton.setEnabled(true));
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder b = new StringBuilder();
        for (byte value : bytes) b.append(String.format(Locale.US, "%02x", value & 0xff));
        return b.toString();
    }
}
