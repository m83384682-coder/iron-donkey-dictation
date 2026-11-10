package com.irondonkey.dictation;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/** First launch: pick one model and download just that one. */
public class SetupActivity extends Activity implements DownloadService.Listener {
    private Prefs prefs;
    private RadioGroup group;
    private Button download;
    private ProgressBar bar;
    private TextView progress;
    private String chosen;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        LinearLayout col = Ui.page(this, "Pick a speech model");
        col.addView(Ui.heading(this, "Welcome to Iron Donkey Dictation"));
        col.addView(Ui.body(this, "Built by Iron Donkey"));
        Ui.add(col, Ui.body(this, "Pick one speech model to start. It downloads once, then everything works offline. "
                + "You can download more models later on the Models screen and switch between them."), 8);

        group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        group.setContentDescription("Speech models");
        String sel = prefs.selectedModel() != null ? prefs.selectedModel() : Models.DEFAULT_ID;
        for (int i = 0; i < Models.ALL.length; i++) {
            Models.ModelInfo m = Models.ALL[i];
            RadioButton rb = new RadioButton(this);
            rb.setId(1000 + i);
            String title = m.name + (m.id.equals(Models.DEFAULT_ID) ? " (suggested)" : "");
            rb.setText(title + "\n" + m.languages + ". " + ModelStore.formatSize(m.totalBytes) + ".\n" + m.description);
            rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            rb.setTextColor(Ui.TEXT);
            rb.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), 0, Ui.dp(this, 10));
            group.addView(rb);
            if (m.id.equals(sel)) rb.setChecked(true);
        }
        group.setOnCheckedChangeListener((g, id) -> {
            Models.ModelInfo m = Models.ALL[id - 1000];
            download.setText("Download " + m.name + " (" + ModelStore.formatSize(m.totalBytes) + ")");
        });
        Ui.add(col, group, 8);

        Ui.add(col, Ui.body(this, "Wi-Fi is best for the download. It keeps going if you leave this screen."), 8);
        Models.ModelInfo first = Models.get(sel);
        download = Ui.button(this, "Download " + first.name + " (" + ModelStore.formatSize(first.totalBytes) + ")", v -> startDownload());
        Ui.add(col, download, 8);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setContentDescription("Download progress");
        bar.setFocusable(true);
        bar.setVisibility(View.GONE);
        Ui.add(col, bar, 12);
        progress = Ui.body(this, "");
        progress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Ui.add(col, progress, 4);
    }

    private Models.ModelInfo checked() {
        int id = group.getCheckedRadioButtonId();
        return id >= 1000 ? Models.ALL[id - 1000] : Models.get(Models.DEFAULT_ID);
    }

    private void startDownload() {
        Models.ModelInfo m = checked();
        chosen = m.id;
        prefs.setSelectedModel(m.id);
        DownloadService.start(this, m.id);
        progress.setText("Starting the download of " + m.name + "…");
        onDownloadChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        DownloadService.addListener(this);
        if (prefs.selectedModel() != null && DownloadService.isActive(prefs.selectedModel())) chosen = prefs.selectedModel();
        onDownloadChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DownloadService.removeListener(this);
    }

    private int lastAnnounced = -1;

    @Override
    public void onDownloadChanged() {
        if (chosen == null) return;
        Models.ModelInfo m = Models.get(chosen);
        if (ModelStore.isDownloaded(this, m)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        boolean active = DownloadService.isActive(chosen);
        group.setEnabled(!active);
        for (int i = 0; i < group.getChildCount(); i++) group.getChildAt(i).setEnabled(!active);
        download.setText(active ? "Cancel download" : "Download " + m.name + " (" + ModelStore.formatSize(m.totalBytes) + ")");
        download.setOnClickListener(active ? v -> DownloadService.cancel(chosen) : v -> startDownload());
        String err = DownloadService.error(chosen);
        if (active) {
            bar.setVisibility(View.VISIBLE);
            int pm = DownloadService.permille(chosen);
            if (pm < 0) {
                bar.setIndeterminate(true);
                progress.setText("Checking the files…");
            } else {
                bar.setIndeterminate(false);
                bar.setProgress(pm);
                int pct = pm / 10;
                String s = DownloadService.isRunning(chosen)
                        ? pct + " percent, " + ModelStore.formatSize(DownloadService.bytesDone()) + " of " + ModelStore.formatSize(DownloadService.bytesTotal())
                        : "Waiting to start";
                if (Build.VERSION.SDK_INT >= 30) bar.setStateDescription(pct + " percent");
                // Only announce every 10 percent so TalkBack is not flooded.
                if (pct / 10 != lastAnnounced || !DownloadService.isRunning(chosen)) {
                    lastAnnounced = pct / 10;
                    progress.setText(s);
                }
            }
        } else {
            bar.setVisibility(View.GONE);
            if (err != null) progress.setText("The download stopped: " + err);
            else if (ModelStore.bytesPresent(this, m) > 0) progress.setText("Paused. Tap Download to resume.");
        }
    }
}
