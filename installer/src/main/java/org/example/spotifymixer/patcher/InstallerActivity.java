package org.example.spotifymixer.patcher;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

public class InstallerActivity extends Activity {
    private static final int REQUEST_APK = 101;
    private TextView status;
    private Uri apkUri;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setPadding(28, 48, 28, 28);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18, 18, 18));
        TextView heading = new TextView(this);
        heading.setText("Spotify Mixer Patcher");
        heading.setTextSize(24);
        heading.setTextColor(Color.WHITE);
        root.addView(heading);
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setText("Selecteer een originele Spotify APK om de bestandsstructuur te controleren.");
        root.addView(status);
        Button choose = new Button(this);
        choose.setText("1. Selecteer APK");
        choose.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_APK);
        });
        root.addView(choose);
        Button patch = new Button(this);
        patch.setText("2. UI-patch toepassen (nog niet beschikbaar)");
        patch.setEnabled(false);
        root.addView(patch);
        Button install = new Button(this);
        install.setText("3. Installeren (nog niet beschikbaar)");
        install.setEnabled(false);
        root.addView(install);
        setContentView(root);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_APK || resultCode != RESULT_OK || data == null) return;
        apkUri = data.getData();
        if (apkUri == null) return;
        status.setText("APK geselecteerd. Bestandsstructuur controleren...");
        new Thread(() -> {
            boolean manifest = false, dex = false;
            try (InputStream in = getContentResolver().openInputStream(apkUri);
                 ZipInputStream zip = new ZipInputStream(in)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if ("AndroidManifest.xml".equals(name)) manifest = true;
                    if ("classes.dex".equals(name)) dex = true;
                    if (manifest && dex) break;
                }
                boolean valid = manifest && dex;
                runOnUiThread(() -> status.setText(valid
                    ? "Geldige APK-structuur gevonden. Spotify-identiteit en versie zijn nog NIET gecontroleerd. Patchmotor nog niet beschikbaar."
                    : "Bestand bevat niet de verwachte APK-structuur."));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("APK kon niet worden gelezen: " + e.getClass().getSimpleName()));
            }
        }).start();
    }
}
