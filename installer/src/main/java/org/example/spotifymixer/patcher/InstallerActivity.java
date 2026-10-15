package org.example.spotifymixer.patcher;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;

/** User-selected input, offline patch/sign, then Android's confirmation UI. */
public class InstallerActivity extends Activity {
    private static final int PICK=101, PERMISSION=102;
    private TextView status;
    private Button select, patch, install;
    private File input, output;
    private boolean busy;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        input=new File(getCacheDir(),"selected.apk");output=new File(getFilesDir(),"output/spotify-mixer.apk");
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(32,48,32,32);
        TextView title=new TextView(this);title.setText("Spotify Mixer Installer");title.setTextSize(24);root.addView(title);
        status=new TextView(this);status.setText("Selecteer je eigen Spotify 9.1.90.2270 APK. De Mixer-knop opent een menu met opgeslagen playlistvelden en verhouding; afspelen is nog niet gekoppeld. Patchen en ondertekenen gebeuren op dit apparaat.");root.addView(status);
        select=new Button(this);select.setText("1. Selecteer Spotify APK");root.addView(select);
        select.setOnClickListener(v -> {Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK);});
        patch=new Button(this);patch.setText("2. Bouw en onderteken");patch.setEnabled(false);root.addView(patch);patch.setOnClickListener(v -> build());
        install=new Button(this);install.setText("3. Open Android-installatie");install.setEnabled(false);root.addView(install);install.setOnClickListener(v -> install());
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        // Never trust a stale output after process recreation; select/build again.
        if(saved!=null){input.delete();output.delete();}
    }
    private void controls(boolean active) {busy=!active;select.setEnabled(active);patch.setEnabled(active&&input.isFile());install.setEnabled(active&&output.isFile());}
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==PERMISSION){if(getPackageManager().canRequestPackageInstalls())install();else status.setText("Installatietoestemming niet verleend. Je kunt opnieuw op installeren tikken.");return;}
        if(request!=PICK||result!=RESULT_OK||data==null||data.getData()==null||busy)return;
        Uri uri=data.getData();output.delete();input.delete();controls(false);status.setText("APK lokaal kopiëren en exact controleren…");
        new Thread(() -> {
            try {
                try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(input)) {
                    if(in==null)throw new IOException("Bestand niet leesbaar");
                    byte[] buffer=new byte[65536];int n;long count=0;
                    while((n=in.read(buffer))!=-1){count+=n;if(count>300L*1024*1024)throw new IOException("Bestand groter dan 300 MB");out.write(buffer,0,n);}
                }
                PackageInfo info=getPackageManager().getPackageArchiveInfo(input.getPath(),0);
                if(info==null||!"com.spotify.music".equals(info.packageName)||!"9.1.90.2270".equals(info.versionName))throw new IOException("Pakket of versie niet ondersteund");
                if(!PatchEngine.INPUT_SHA256.equals(PatchEngine.sha256(input)))throw new IOException("SHA-256 wijkt af van de onderzochte APK");
                message("Exacte doel-APK gecontroleerd. Klaar om het Mixer-menu te injecteren.");
            }catch(Exception e){input.delete();message("Controle mislukt: "+e.getMessage());}
            finally{runOnUiThread(()->controls(true));}
        },"apk-check").start();
    }
    private void build() {
        controls(false);output.delete();status.setText("DEX patchen, APK bouwen en lokaal ondertekenen… Dit kan enkele minuten duren.");
        new Thread(() -> {
            File unsigned=new File(getCacheDir(),"unsigned.apk");
            try {
                if(getFilesDir().getUsableSpace()<700L*1024*1024)throw new IOException("Minstens 700 MB vrije ruimte nodig");
                output.getParentFile().mkdirs();
                PatchEngine.patch(input,PatchEngine.read(getAssets().open("mixer.dex")),unsigned);
                LocalSigner.sign(unsigned,output);
                message("APK gebouwd; v2-handtekening gecontroleerd. SHA-256: "+PatchEngine.sha256(output)+"\nOpen nu Android-installatie. Een officiële Spotify-installatie heeft een andere sleutel en kan niet worden bijgewerkt. Android kan verwijderen van Spotify vereisen; lokale downloads kunnen daarbij verloren gaan.");
            }catch(OutOfMemoryError e){output.delete();message("Onvoldoende werkgeheugen voor deze APK. Probeer een apparaat met meer geheugen.");}
            catch(LinkageError e){output.delete();message("Patchbibliotheek niet ondersteund op dit apparaat: "+e.getClass().getSimpleName());}
            catch(Exception e){output.delete();message("Bouwen mislukt: "+e.getClass().getSimpleName()+": "+e.getMessage());}
            finally{unsigned.delete();runOnUiThread(()->controls(true));}
        },"mixer-build").start();
    }
    private void install() {
        if(!output.isFile()||busy)return;
        if(android.os.Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())),PERMISSION);return;
        }
        try {
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",output);
            Intent i=new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);
            status.setText("Overgedragen aan Android-installatie. Dit bevestigt nog geen geslaagde installatie. Bevestig het systeemvenster en open Spotify om de Mixer-knop te testen.");
        }catch(Exception e){status.setText("Installatievenster openen mislukt: "+e.getMessage());}
    }
    private void message(String text){runOnUiThread(()->status.setText(text));}
}
