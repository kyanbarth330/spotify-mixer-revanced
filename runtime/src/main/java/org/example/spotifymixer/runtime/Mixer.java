package org.example.spotifymixer.runtime;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Original in-process UI. No account, playback or premium hooks. */
public final class Mixer {
    private static final String TAG = "spotify-mixer-entry-v1";
    public static void attach(Activity activity) {
        // Posted after onResume so that Spotify has installed its content view.
        activity.getWindow().getDecorView().post(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            android.view.ViewGroup decor = (android.view.ViewGroup) activity.getWindow().getDecorView();
            if (decor.findViewWithTag(TAG) != null) return;
            Button button = new Button(activity);
            button.setTag(TAG);
            button.setText("Spotify Mixer");
            button.setOnClickListener(v -> show(activity));
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
            params.bottomMargin = (int)(96 * activity.getResources().getDisplayMetrics().density);
            decor.addView(button, params);
        });
    }
    private static void show(Activity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 16, 32, 16);
        EditText first = new EditText(a); first.setHint("Playlist A URL of URI");
        EditText second = new EditText(a); second.setHint("Playlist B URL of URI");
        android.content.SharedPreferences prefs = a.getSharedPreferences(TAG, 0);
        first.setText(prefs.getString("a", "")); second.setText(prefs.getString("b", ""));
        TextView ratio = new TextView(a);
        SeekBar bar = new SeekBar(a); bar.setMax(100); bar.setProgress(prefs.getInt("ratio", 70));
        ratio.setText("A: " + bar.getProgress() + "% · B: " + (100-bar.getProgress()) + "%");
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int n, boolean user) { ratio.setText("A: " + n + "% · B: " + (100-n) + "%"); }
            public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {}
        });
        TextView note = new TextView(a);
        note.setText("UI-menu: bewaart playlistvelden en verhouding. Afspelen, playlist ophalen en wachtrij mengen zijn nog niet geïmplementeerd.");
        box.addView(first); box.addView(second); box.addView(ratio); box.addView(bar); box.addView(note);
        new AlertDialog.Builder(a).setTitle("Spotify Mixer").setView(box)
            .setPositiveButton("Bewaren", (d,w) -> prefs.edit().putString("a", first.getText().toString()).putString("b", second.getText().toString()).putInt("ratio", bar.getProgress()).apply())
            .setNegativeButton("Sluiten", null).show();
    }
}
