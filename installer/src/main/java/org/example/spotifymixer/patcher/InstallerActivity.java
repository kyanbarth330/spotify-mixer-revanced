package org.example.spotifymixer.patcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import org.example.spotifymixer.engine.Patcher;

/** One local APK-in / signed APK-out flow. Android owns the install confirmation. */
public final class InstallerActivity extends Activity {
    private TextView status;
    private Button select, install;
    private File signed;
    private volatile boolean busy;
    public void onCreate(Bundle state) {
        super.onCreate(state);
        signed = new File(new File(getFilesDir(), "output"), "spotify-mixer.apk");
        LinearLayout box = new LinearLayout(this); box.setOrientation(1); box.setPadding(32,48,32,24);
        TextView title = new TextView(this); title.setText("SpotEnhancer Installer"); title.setTextSize(24); box.addView(title);
        status = new TextView(this);
        status.setText("Select the supported Spotify 9.1.90.2270 APK. Build SpotEnhancer locally, then install it. At least 1 GB of free storage is required. Playback mixing is not available yet."); box.addView(status);
        select = new Button(this); select.setText("1. Select APK and build SpotEnhancer"); box.addView(select);
        select.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 101);
        });
        install = new Button(this); install.setText("2. Install patched Spotify"); install.setEnabled(false); box.addView(install);
        install.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Install patched Spotify")
            .setMessage("The certificate differs from original Spotify. Android may reject an update. Uninstalling Spotify removes local data and downloads. This installer never uninstalls apps automatically.")
            .setPositiveButton("Continue", (d,w) -> launchInstall()).setNegativeButton("Cancel", null).show());
        setContentView(box);
        // Never offer an output left by an interrupted or previous session without verification.
        signed.delete();
    }
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req,result,data);
        if (req == 103) {
            update(result == RESULT_OK ? "Android reports installation completed. Open a playlist and look for SpotEnhancer."
                : "Installation was cancelled or failed. A certificate mismatch prevents updating an existing installation.");
            return;
        }
        if (req == 102) { if (android.os.Build.VERSION.SDK_INT >= 26 && getPackageManager().canRequestPackageInstalls()) launchInstall(); return; }
        if (req != 101 || result != RESULT_OK || data == null || data.getData() == null || busy) return;
        busy = true; select.setEnabled(false); install.setEnabled(false); signed.delete();
        Uri uri = data.getData();
        new Thread(() -> build(uri), "mixer-build").start();
    }
    private void build(Uri uri) {
        File input = new File(getCacheDir(),"input.apk");
        File runtime = new File(getCacheDir(),"mixer-runtime.zip");
        File unsigned = new File(getCacheDir(),"unsigned.apk");
        try {
            if (getFilesDir().getUsableSpace() < 1024L*1024*1024) throw new IOException("At least 1 GB of free storage is required");
            update("Copying and checking APK…");
            try (InputStream in = getContentResolver().openInputStream(uri)) { copy(in,input,300L*1024*1024); }
            try (InputStream in = getAssets().open("mixer-runtime.zip")) { copy(in,runtime,16L*1024*1024); }
            update("Verifying input and building SpotEnhancer…");
            Patcher.patch(input,runtime,unsigned);
            update("Signing and verifying output…");
            KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
            String alias = "spotify-mixer-v1";
            if (!store.containsAlias(alias)) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");
                generator.initialize(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA512)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).build());
                generator.generateKeyPair();
            }
            signed.getParentFile().mkdirs();
            Patcher.sign(unsigned,signed,(PrivateKey)store.getKey(alias,null),(X509Certificate)store.getCertificate(alias));
            String hash = Patcher.hash(signed);
            update("SpotEnhancer added; APK built and cryptographically verified.\nSHA-256: " + hash + "\nOpen Spotify after installation to verify the new screens.");
            runOnUiThread(() -> install.setEnabled(true));
        } catch (Exception | OutOfMemoryError ex) {
            signed.delete(); update("Build failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } finally {
            input.delete(); runtime.delete(); unsigned.delete(); busy=false;
            runOnUiThread(() -> select.setEnabled(true));
        }
    }
    private void launchInstall() {
        try {
            if (!signed.isFile()) throw new IOException("Build an APK first");
            if (android.os.Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())),102); return;
            }
            Uri uri = FileProvider.getUriForFile(this,getPackageName()+".files",signed);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_RETURN_RESULT,true);
            startActivityForResult(intent,103);
        } catch (Exception ex) { update("Could not start installation: " + ex.getMessage()); }
    }
    private void update(String text) { runOnUiThread(() -> status.setText(text)); }
    private static void copy(InputStream in, File file, long limit) throws IOException {
        if (in == null) throw new IOException("File is not readable");
        try (OutputStream out = new FileOutputStream(file)) { byte[] b = new byte[65536]; int n; long total=0;
            while ((n=in.read(b))!=-1) { total+=n; if(total>limit) throw new IOException("File is too large"); out.write(b,0,n); }
        }
    }
}
