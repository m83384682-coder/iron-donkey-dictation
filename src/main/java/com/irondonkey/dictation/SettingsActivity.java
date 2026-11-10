package com.irondonkey.dictation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
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
        toggle(col, "Spoken commands: say new line, new paragraph, comma, period, question mark, colon or semicolon to type them",
                prefs.spokenCommands(), prefs::setSpokenCommands);
        Ui.add(col, Ui.button(this, "Word replacements" + countRules(), v -> editReplacements((Button) v)), 4);
        choice(col, "Text size", new String[]{"Small", "Normal", "Large", "Extra large"},
                sizeIndex(prefs.textSize()), v -> prefs.setTextSize(new int[]{15, 18, 22, 26}[v]));

        col.addView(Ui.subheading(this, "Translation"));
        Ui.add(col, Ui.button(this, "Translation settings for the current model", v -> startActivity(new Intent(this, TranslationActivity.class))), 4);

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
        int[] autos = {0, 3, 5, 10, 20};
        int ai = 0;
        for (int i = 0; i < autos.length; i++) if (autos[i] == prefs.autoStopSec()) ai = i;
        choice(col, "Stop by itself after silence", new String[]{"Never", "3 seconds", "5 seconds", "10 seconds", "20 seconds"},
                ai, v -> prefs.setAutoStopSec(autos[v]));

        col.addView(Ui.subheading(this, "Keyboard"));
        toggle(col, "Push to talk: hold the voice typing button to talk, let go to type. With TalkBack, double tap and hold",
                prefs.pushToTalk(), prefs::setPushToTalk);
        toggle(col, "Two spaces type a period", prefs.doubleSpacePeriod(), prefs::setDoubleSpacePeriod);
        toggle(col, "Vibrate on key presses", prefs.keyVibration(), prefs::setKeyVibration);
        toggle(col, "Show the tool row (cursor arrows, Select all, Copy, Paste, Translate)", prefs.toolRow(), prefs::setToolRow);

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

    private String countRules() {
        int n = TextExtras.parseReplacements(prefs.replacements()).size();
        return n == 0 ? ": none" : ": " + n + (n == 1 ? " rule" : " rules");
    }

    private static int sizeIndex(int sp) {
        int[] s = {15, 18, 22, 26};
        for (int i = 0; i < s.length; i++) if (s[i] == sp) return i;
        return 1;
    }

    /** One rule per line, like "iron donkey = Iron Donkey". */
    private void editReplacements(Button b) {
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        in.setMinLines(5);
        in.setText(prefs.replacements());
        in.setHint("iron donkey = Iron Donkey");
        in.setContentDescription("Word replacements, one per line: heard words, equals sign, written words");
        new AlertDialog.Builder(this)
                .setTitle("Word replacements")
                .setMessage("One per line: what the model writes, an equals sign, then what you want instead. "
                        + "For example: iron donkey = Iron Donkey. Whole words only, capitals don't matter.")
                .setView(in)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.setReplacements(in.getText().toString());
                    b.setText("Word replacements" + countRules());
                })
                .setNegativeButton("Cancel", null)
                .show();
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
