package com.irondonkey.dictation;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** How to turn on the keyboard, with the battery optimization warning first. */
public class KeyboardSetupActivity extends Activity {
    private TextView batteryStatus, keyboardStatus, micStatus, modelLine;
    private Button batteryButton, micButton;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout col = Ui.page(this, "Keyboard");
        col.addView(Ui.heading(this, "Use as a keyboard"));
        col.addView(Ui.body(this, "A simple keyboard with letters, numbers, a symbols page, Shift and a Voice typing button. "
                + "Voice typing works like the Record button: tap to start, tap Stop to finish. The text goes straight "
                + "into the box you're typing in, using the model you picked on the Models screen."));

        LinearLayout warn = Ui.card(this);
        warn.addView(Ui.text(this, Battery.WARNING, 17, Ui.ACCENT));
        batteryStatus = Ui.body(this, "");
        Ui.add(warn, batteryStatus, 6);
        batteryButton = Ui.button(this, "Allow ignoring battery optimizations", v -> Battery.request(this));
        Ui.add(warn, batteryButton, 6);
        Ui.add(col, warn, 12);

        col.addView(Ui.subheading(this, "Step 1: turn on the keyboard"));
        keyboardStatus = Ui.body(this, "");
        col.addView(keyboardStatus);
        Ui.add(col, Ui.button(this, "Open Android keyboard settings", v -> {
            try {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        }), 4);

        col.addView(Ui.subheading(this, "Step 2: switch to it"));
        col.addView(Ui.body(this, "Choose Iron Donkey Keyboard from the list. In any text box you can also use the "
                + "Switch keyboard key to go back to your other keyboard."));
        Ui.add(col, Ui.button(this, "Choose keyboard", v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        }), 4);

        col.addView(Ui.subheading(this, "Step 3: microphone"));
        micStatus = Ui.body(this, "");
        col.addView(micStatus);
        micButton = Ui.button(this, "Allow the microphone", v -> requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1));
        Ui.add(col, micButton, 4);

        col.addView(Ui.subheading(this, "Voice typing model"));
        modelLine = Ui.body(this, "");
        col.addView(modelLine);
        Ui.add(col, Ui.button(this, "Models", v -> startActivity(new Intent(this, ModelsActivity.class))), 4);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        refresh();
    }

    private void refresh() {
        boolean bat = Battery.ignoring(this);
        batteryStatus.setText(bat ? "Done: Iron Donkey Dictation is allowed to ignore battery optimizations."
                : "Not done yet: battery optimization is still on for this app.");
        batteryButton.setText(bat ? "Battery optimization settings" : "Allow ignoring battery optimizations");

        keyboardStatus.setText(isEnabled() ? "Iron Donkey Keyboard is turned on."
                : "Iron Donkey Keyboard is off. Turn it on in the list of keyboards, then come back.");

        boolean mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        micStatus.setText(mic ? "The microphone is allowed." : "The microphone isn't allowed yet. Voice typing needs it.");
        micButton.setEnabled(!mic);

        Models.ModelInfo m = Models.get(new Prefs(this).selectedModel());
        if (m == null || !ModelStore.isDownloaded(this, m)) modelLine.setText("No model downloaded yet. Pick one on the Models screen.");
        else modelLine.setText("Voice typing uses " + m.name + ".");
    }

    private boolean isEnabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
            if (i.getPackageName().equals(getPackageName())) return true;
        }
        return false;
    }
}
