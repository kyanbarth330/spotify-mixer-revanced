package org.example.spotifymixer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MixerActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);
        root.setBackgroundColor(Color.rgb(18, 18, 18));
        TextView title = new TextView(this);
        title.setText("Playlist Mixer — UI Demo");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        root.addView(title);
        root.addView(placeholder("Selecteer Playlist A"));
        root.addView(placeholder("Selecteer Playlist B"));
        TextView ratio = new TextView(this);
        ratio.setTextColor(Color.WHITE);
        ratio.setText("Playlist A: 70% / Playlist B: 30%");
        root.addView(ratio);
        SeekBar slider = new SeekBar(this);
        slider.setMax(100);
        slider.setProgress(70);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                ratio.setText("Playlist A: " + value + "% / Playlist B: " + (100 - value) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        root.addView(slider);
        root.addView(placeholder("Start Mixer"));
        setContentView(root);
    }
    private Button placeholder(String name) {
        Button button = new Button(this);
        button.setText(name);
        button.setOnClickListener(v -> Toast.makeText(this, "Nog niet beschikbaar", Toast.LENGTH_SHORT).show());
        return button;
    }
}
