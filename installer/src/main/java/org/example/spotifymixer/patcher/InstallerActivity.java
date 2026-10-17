package org.example.spotifymixer.patcher;

import android.app.Activity;
import org.example.spotifymixer.ui.EnhancerTheme;
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
    private TextView status,terminal;
    private ScrollView terminalScroll;
    private ProgressBar progress;
    private long logStart;
    private final java.util.ArrayDeque<String> lines=new java.util.ArrayDeque<>();
    private Button select, install;
    private File signed;
    private volatile boolean busy;
    public void onCreate(Bundle state) {
        setTheme(android.R.style.Theme_Material_NoActionBar);super.onCreate(state);
        signed = new File(new File(getFilesDir(), "output"), "spotify-mixer.apk");
        LinearLayout box=EnhancerTheme.page(this,"SpotEnhancer");
        EnhancerTheme.text(box,"INSTALLER · 0.17",13,true);
        EnhancerTheme.text(box,"Your music. Your experience.",22,true);
        LinearLayout card=EnhancerTheme.card(box);
        EnhancerTheme.text(card,"Build on your device",20,true);
        status=EnhancerTheme.text(card,"Select the supported Spotify 9.1.90.2270 APK. Your APK is processed locally. At least 1 GB of free storage is required. Native playback integration is experimental and needs device verification.",15,false);
        select=EnhancerTheme.button(box,"1. Select APK and build SpotEnhancer",true,()->{});
        select.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 101);
        });
        install=EnhancerTheme.button(box,"2. Install patched Spotify",false,()->{});install.setEnabled(false);
        install.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Install patched Spotify")
            .setMessage("The certificate differs from original Spotify. Android may reject an update. Uninstalling Spotify removes local data and downloads. This installer never uninstalls apps automatically.")
            .setPositiveButton("Continue", (d,w) -> launchInstall()).setNegativeButton("Cancel", null).show());
        LinearLayout logCard=EnhancerTheme.card(box);
        EnhancerTheme.text(logCard,"Patch terminal",18,true);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setVisibility(android.view.View.GONE);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(EnhancerTheme.GREEN));logCard.addView(progress);
        terminalScroll=new ScrollView(this);terminalScroll.setBackgroundColor(0xff080c09);
        terminal=new TextView(this);terminal.setTextColor(0xff89efa8);terminal.setTextSize(12);terminal.setTypeface(android.graphics.Typeface.MONOSPACE);terminal.setTextIsSelectable(true);int pad=EnhancerTheme.dp(this,12);terminal.setPadding(pad,pad,pad,pad);
        terminalScroll.addView(terminal);logCard.addView(terminalScroll,new LinearLayout.LayoutParams(-1,EnhancerTheme.dp(this,220)));
        EnhancerTheme.button(logCard,"Copy log",false,()->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SpotEnhancer patch log",terminal.getText()));Toast.makeText(this,"Log copied",Toast.LENGTH_SHORT).show();});
        logStart=android.os.SystemClock.elapsedRealtime();appendLog("[READY] Select an APK to begin. No patch has run yet.");
        EnhancerTheme.text(box,"Independent project · not affiliated with Spotify",12,false);
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
        lines.clear();terminal.setText("");logStart=android.os.SystemClock.elapsedRealtime();progress.setVisibility(android.view.View.VISIBLE);
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
            Patcher.patch(input,runtime,unsigned,this::appendLog);
            update("Signing and verifying output…");
            KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
            String alias = "spotify-mixer-v1";
            appendLog("[KEY] Preparing device-local Android Keystore signing key");
            if (!store.containsAlias(alias)) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");
                generator.initialize(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA512)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).build());
                generator.generateKeyPair();
            }
            signed.getParentFile().mkdirs();
            Patcher.sign(unsigned,signed,(PrivateKey)store.getKey(alias,null),(X509Certificate)store.getCertificate(alias),this::appendLog);
            String hash = Patcher.hash(signed);
            update("SpotEnhancer added; APK built and cryptographically verified.\nSHA-256: " + hash + "\nOpen Spotify after installation to verify the new screens.");
            runOnUiThread(() -> install.setEnabled(true));
        } catch (Exception | OutOfMemoryError ex) {
            signed.delete(); update("Build failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } finally {
            input.delete(); runtime.delete(); unsigned.delete(); busy=false;
            runOnUiThread(() -> {select.setEnabled(true);progress.setVisibility(android.view.View.GONE);});
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
    private void update(String text) { runOnUiThread(() -> status.setText(text));appendLog(text.startsWith("Build failed")?"[ERROR] "+text:"[INFO] "+text); }
    private void appendLog(String text){
        long elapsed=android.os.SystemClock.elapsedRealtime()-logStart;
        String line=String.format(java.util.Locale.ROOT,"[%02d:%02d] %s",elapsed/60000,(elapsed/1000)%60,text);
        runOnUiThread(()->{
            boolean follow=terminalScroll.getChildAt(0).getHeight()<=terminalScroll.getHeight()+terminalScroll.getScrollY()+EnhancerTheme.dp(this,24);
            lines.addLast(line);while(lines.size()>200)lines.removeFirst();
            StringBuilder content=new StringBuilder();for(String item:lines)content.append(item).append('\n');terminal.setText(content.toString());
            if(follow)terminalScroll.post(()->terminalScroll.fullScroll(android.view.View.FOCUS_DOWN));
        });
    }
    private static void copy(InputStream in, File file, long limit) throws IOException {
        if (in == null) throw new IOException("File is not readable");
        try (OutputStream out = new FileOutputStream(file)) { byte[] b = new byte[65536]; int n; long total=0;
            while ((n=in.read(b))!=-1) { total+=n; if(total>limit) throw new IOException("File is too large"); out.write(b,0,n); }
        }
    }
}
