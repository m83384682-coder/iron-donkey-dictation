package com.irondonkey.dictation;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;

import java.util.Locale;

public class SettingsActivity extends Activity {
    private Prefs prefs;

    private interface IntSetter { void set(int v); }
    private interface BoolSetter { void set(boolean v); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        LinearLayout col = Ui.page(this, "Settings");
        col.addView(Ui.heading(this, "Settings"));

        col.addView(Ui.subheading(this, "When the text is ready"));
        choice(col, "Signal", new String[]{"None", "Short vibration", "Short sound", "TalkBack says Done"},
                prefs.signal(), prefs::setSignal);
        toggle(col, "Copy the text automatically", prefs.autoCopy(), prefs::setAutoCopy);
        choice(col, "New text", new String[]{"Adds to the end", "Replaces the text"},
                prefs.replaceText() ? 1 : 0, v -> prefs.setReplaceText(v == 1));
        toggle(col, "New paragraph after a pause of more than 2 seconds", prefs.paragraphs(), prefs::setParagraphs);
        toggle(col, "Turn spoken web addresses and emails into real ones (www dot google dot com becomes www.google.com)",
                prefs.cleanLinks(), prefs::setCleanLinks);
        toggle(col, "Keep history", prefs.history(), prefs::setHistory);

        col.addView(Ui.subheading(this, "Recording"));
        String[] skips = new String[7];
        int skipIdx = 0;
        for (int i = 0; i < skips.length; i++) {
            float s = i * 0.25f;
            skips[i] = i == 0 ? "None" : String.format(Locale.US, "%s seconds", trim(s));
            if (Math.abs(prefs.skipStart() - s) < 0.01f) skipIdx = i;
        }
        choice(col, "Skip at the start (so TalkBack saying Stop is not picked up)", skips, skipIdx, v -> prefs.setSkipStart(v * 0.25f));
        choice(col, "Microphone", new String[]{"Phone", "Wired headset", "Bluetooth headset"}, prefs.mic(), prefs::setMic);
        toggle(col, "Android noise reduction", prefs.noiseReduction(), prefs::setNoiseReduction);

        col.addView(Ui.subheading(this, "Performance"));
        int n = Runtime.getRuntime().availableProcessors();
        String[] cores = new String[Math.min(8, n) + 1];
        cores[0] = "Auto (" + autoThreads() + ")";
        for (int i = 1; i < cores.length; i++) cores[i] = String.valueOf(i);
        choice(col, "Processor cores", cores, Math.min(prefs.cores(), cores.length - 1), v -> {
            prefs.setCores(v);
            EngineManager.get(this).ensureLoaded();
        });
        toggle(col, "Keep the model loaded (faster start, uses memory)", prefs.keepLoaded(), prefs::setKeepLoaded);
    }

    private static String trim(float f) {
        String s = String.format(Locale.US, "%.2f", f);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static int autoThreads() {
        return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    }

    private void choice(LinearLayout col, String label, String[] options, int current, IntSetter setter) {
        Button b = Ui.button(this, "", null);
        final int[] cur = {current};
        Runnable label2 = () -> b.setText(label + ": " + options[cur[0]]);
        label2.run();
        b.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(label)
                .setSingleChoiceItems(options, cur[0], (d, which) -> {
                    cur[0] = which;
                    setter.set(which);
                    label2.run();
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show());
        Ui.add(col, b, 4);
    }

    private void toggle(LinearLayout col, String label, boolean on, BoolSetter setter) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(Ui.TEXT);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        s.setMinHeight(Ui.dp(this, 48));
        s.setChecked(on);
        s.setOnCheckedChangeListener((v, c) -> setter.set(c));
        Ui.add(col, s, 4);
    }
}
