package com.irondonkey.dictation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Iron Donkey Keyboard: a plain QWERTY keyboard with a number row, a symbols page, Shift,
 * and a Voice typing button that records with the selected model and types the text into the
 * box in use. Every key is a normal button with a spoken label, so TalkBack can read and type it.
 */
public class DonkeyKeyboard extends InputMethodService implements EngineManager.Listener, Hub.Listener {
    private static final int SHIFT_OFF = 0, SHIFT_ONCE = 1, SHIFT_LOCK = 2;

    private static final String[] LETTER_ROWS = {"1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"};
    private static final String[][] SYMBOL_ROWS = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"@", "#", "$", "%", "&", "-", "+", "(", ")", "/"},
            {"*", "\"", "'", ":", ";", "!", "?", "_", "=", "~"},
            {"<", ">", "[", "]", "{", "}", "\\", "|", "^", "`"},
    };
    private static final Map<String, String> NAMES = new HashMap<>();
    static {
        String[][] n = {{"@", "At sign"}, {"#", "Hash"}, {"$", "Dollar"}, {"%", "Percent"}, {"&", "Ampersand"},
                {"-", "Dash"}, {"+", "Plus"}, {"(", "Left parenthesis"}, {")", "Right parenthesis"}, {"/", "Slash"},
                {"*", "Star"}, {"\"", "Quote"}, {"'", "Apostrophe"}, {":", "Colon"}, {";", "Semicolon"},
                {"!", "Exclamation mark"}, {"?", "Question mark"}, {"_", "Underscore"}, {"=", "Equals"}, {"~", "Tilde"},
                {"<", "Less than"}, {">", "Greater than"}, {"[", "Left bracket"}, {"]", "Right bracket"},
                {"{", "Left brace"}, {"}", "Right brace"}, {"\\", "Backslash"}, {"|", "Vertical bar"}, {"^", "Caret"},
                {"`", "Backtick"}, {",", "Comma"}, {".", "Period"}};
        for (String[] p : n) NAMES.put(p[0], p[1]);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private EngineManager em;

    private LinearLayout root, keys;
    private Button warning, voice, shiftKey, enterKey;
    private TextView status;
    private final List<Button> letterKeys = new ArrayList<>();
    private boolean symbols = false;
    private int shift = SHIFT_OFF;
    private String engineLine = "";

    /** One voice typing recording. */
    private static final class Session {
        volatile boolean stop;
    }
    private Session current;
    private int finishing = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        em = EngineManager.get(this);
    }

    @Override
    public void onDestroy() {
        if (current != null) current.stop = true;
        em.removeListener(this);
        Hub.removeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onEvaluateFullscreenMode() { return false; }

    // ---- Layout ----

    @Override
    public View onCreateInputView() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int p = dp(4);
        root.setPadding(p, p, p, p);

        warning = new Button(this);
        warning.setAllCaps(false);
        warning.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        warning.setTextColor(0xFF121417);
        warning.setBackground(keyBackground(Ui.ACCENT, 0xFFF0C060));
        warning.setText("Warning: battery optimization is on, so Android can stop voice typing. Tap to allow ignoring battery optimizations.");
        warning.setOnClickListener(v -> Battery.request(this));
        root.addView(warning, rowParams(0));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        voice = new Button(this);
        voice.setAllCaps(false);
        voice.setTypeface(Typeface.DEFAULT_BOLD);
        voice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        voice.setTextColor(0xFF121417);
        voice.setBackground(keyBackground(Ui.ACCENT, 0xFFF0C060));
        voice.setMinHeight(dp(56));
        voice.setOnClickListener(v -> toggleVoice());
        top.addView(voice, new LinearLayout.LayoutParams(0, dp(56), 1));
        root.addView(top, rowParams(4));

        status = new TextView(this);
        status.setTextColor(Ui.DIM);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        status.setPadding(dp(4), dp(2), dp(4), dp(2));
        root.addView(status, rowParams(2));

        keys = new LinearLayout(this);
        keys.setOrientation(LinearLayout.VERTICAL);
        root.addView(keys, rowParams(2));

        em.removeListener(this);
        em.addListener(this);
        Hub.removeListener(this);
        Hub.addListener(this);
        buildKeys();
        updateVoice();
        return root;
    }

    private LinearLayout.LayoutParams rowParams(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    private void buildKeys() {
        keys.removeAllViews();
        letterKeys.clear();
        shiftKey = null;
        if (!symbols) {
            for (int r = 0; r < LETTER_ROWS.length; r++) {
                LinearLayout row = row();
                if (r == 3) {
                    shiftKey = special("Shift", "Shift", 1.5f, v -> cycleShift());
                    row.addView(shiftKey, keyParams(1.5f));
                }
                if (r == 2) row.addView(new View(this), keyParams(0.5f));
                for (char ch : LETTER_ROWS[r].toCharArray()) {
                    String s = String.valueOf(ch);
                    Button b = key(s, s, 1f, v -> typeKey(s));
                    if (Character.isLetter(ch)) letterKeys.add(b);
                    row.addView(b, keyParams(1f));
                }
                if (r == 2) row.addView(new View(this), keyParams(0.5f));
                if (r == 3) addDelete(row);
                keys.addView(row);
            }
        } else {
            for (int r = 0; r < SYMBOL_ROWS.length; r++) {
                LinearLayout row = row();
                for (String s : SYMBOL_ROWS[r]) row.addView(key(s, NAMES.getOrDefault(s, s), 1f, v -> commit(s)), keyParams(1f));
                if (r == 3) addDelete(row);
                keys.addView(row);
            }
        }
        LinearLayout bottom = row();
        bottom.addView(special(symbols ? "ABC" : "?123", symbols ? "Letters" : "Symbols", 1.5f, v -> toggleSymbols()), keyParams(1.5f));
        Button sw = special("\uD83C\uDF10", "Switch keyboard", 1f, v -> switchKeyboard());
        sw.setOnLongClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
            return true;
        });
        bottom.addView(sw, keyParams(1f));
        bottom.addView(key(",", "Comma", 1f, v -> commit(",")), keyParams(1f));
        bottom.addView(special("space", "Space", 4f, v -> commit(" ")), keyParams(4f));
        bottom.addView(key(".", "Period", 1f, v -> commit(".")), keyParams(1f));
        enterKey = special("Enter", "Enter", 1.5f, v -> enter());
        bottom.addView(enterKey, keyParams(1.5f));
        keys.addView(bottom);
        updateEnter();
        updateShiftLabels();
    }

    private void addDelete(LinearLayout row) {
        Button del = special("\u232B", "Delete", 1.5f, v -> deleteOne());
        del.setOnLongClickListener(v -> { deleteWord(); return true; });
        del.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, "delete the word before the cursor"));
            }
        });
        row.addView(del, keyParams(1.5f));
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private LinearLayout.LayoutParams keyParams(float w) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), w);
        int m = dp(2);
        lp.setMargins(m, m, m, m);
        return lp;
    }

    private Button key(String label, String spoken, float w, View.OnClickListener l) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setContentDescription(spoken);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        b.setTextColor(Ui.TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackground(keyBackground(0xFF2A2F37, 0xFF4A515C));
        b.setOnClickListener(l);
        b.setSoundEffectsEnabled(true);
        return b;
    }

    private Button special(String label, String spoken, float w, View.OnClickListener l) {
        Button b = key(label, spoken, w, l);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setBackground(keyBackground(0xFF1E2228, 0xFF4A515C));
        return b;
    }

    private StateListDrawable keyBackground(int normal, int pressed) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, round(pressed));
        s.addState(new int[]{}, round(normal));
        return s;
    }

    private GradientDrawable round(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(6));
        return g;
    }

    private int dp(float v) { return Ui.dp(this, v); }

    // ---- Typing ----

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (symbols) { symbols = false; buildKeys(); }
        if (shift != SHIFT_LOCK) shift = SHIFT_OFF;
        autoShift();
        updateEnter();
        updateVoice();
        Models.ModelInfo m = Models.get(prefs.selectedModel());
        // Have the model ready before the voice button is pressed, if the user keeps it loaded.
        if (prefs.keepLoaded() && m != null && ModelStore.isDownloaded(this, m)) em.ensureLoaded();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        // The keyboard is going away: finish the recording and type what was said.
        if (current != null) {
            current.stop = true;
            current = null;
            updateVoice();
        }
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        autoShift();
    }

    /** Capital letter at the start of a sentence, when the text box asks for it. */
    private void autoShift() {
        if (shift == SHIFT_LOCK || symbols) return;
        InputConnection ic = getCurrentInputConnection();
        EditorInfo ei = getCurrentInputEditorInfo();
        int want = SHIFT_OFF;
        if (ic != null && ei != null && ei.inputType != InputType.TYPE_NULL) {
            if (ic.getCursorCapsMode(ei.inputType) != 0) want = SHIFT_ONCE;
        }
        if (want != shift) {
            shift = want;
            updateShiftLabels();
        }
    }

    private void typeKey(String s) {
        String t = shift != SHIFT_OFF ? s.toUpperCase() : s;
        commit(t);
        if (shift == SHIFT_ONCE) {
            shift = SHIFT_OFF;
            updateShiftLabels();
        }
    }

    private void commit(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(s, 1);
    }

    private void deleteOne() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) ic.commitText("", 1);
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
    }

    private void deleteWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) { ic.commitText("", 1); return; }
        CharSequence before = ic.getTextBeforeCursor(100, 0);
        if (before == null || before.length() == 0) return;
        int i = before.length();
        while (i > 0 && Character.isWhitespace(before.charAt(i - 1))) i--;
        while (i > 0 && !Character.isWhitespace(before.charAt(i - 1))) i--;
        int n = before.length() - i;
        if (n > 0) {
            ic.deleteSurroundingText(n, 0);
            speak("Word deleted");
        }
    }

    private void enter() {
        InputConnection ic = getCurrentInputConnection();
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ic == null) return;
        int action = ei == null ? EditorInfo.IME_ACTION_NONE : ei.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean noAction = ei == null || (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                || action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED;
        if (noAction) sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        else ic.performEditorAction(action);
    }

    private void updateEnter() {
        if (enterKey == null) return;
        EditorInfo ei = getCurrentInputEditorInfo();
        String label = "Enter";
        if (ei != null && (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            switch (ei.imeOptions & EditorInfo.IME_MASK_ACTION) {
                case EditorInfo.IME_ACTION_GO: label = "Go"; break;
                case EditorInfo.IME_ACTION_SEARCH: label = "Search"; break;
                case EditorInfo.IME_ACTION_SEND: label = "Send"; break;
                case EditorInfo.IME_ACTION_NEXT: label = "Next"; break;
                case EditorInfo.IME_ACTION_DONE: label = "Done"; break;
                case EditorInfo.IME_ACTION_PREVIOUS: label = "Previous"; break;
                default: break;
            }
        }
        enterKey.setText(label);
        enterKey.setContentDescription(label);
    }

    private void cycleShift() {
        shift = (shift + 1) % 3;
        updateShiftLabels();
        speak(shift == SHIFT_OFF ? "Shift off" : shift == SHIFT_ONCE ? "Shift on" : "Caps lock on");
    }

    private void updateShiftLabels() {
        for (Button b : letterKeys) {
            String s = b.getText().toString().toLowerCase();
            if (shift != SHIFT_OFF) {
                b.setText(s.toUpperCase());
                b.setContentDescription("Capital " + s.toUpperCase());
            } else {
                b.setText(s);
                b.setContentDescription(s);
            }
        }
        if (shiftKey != null) {
            String[] labels = {"Shift", "SHIFT", "CAPS"};
            String[] spoken = {"Shift, off", "Shift, on", "Caps lock, on"};
            shiftKey.setText(labels[shift]);
            shiftKey.setContentDescription(spoken[shift]);
            shiftKey.setBackground(shift == SHIFT_OFF ? keyBackground(0xFF1E2228, 0xFF4A515C) : keyBackground(0xFF5A6270, 0xFF6A7380));
        }
    }

    private void toggleSymbols() {
        symbols = !symbols;
        buildKeys();
        if (!symbols) autoShift();
        speak(symbols ? "Symbols" : "Letters");
    }

    @SuppressWarnings("deprecation")
    private void switchKeyboard() {
        if (Build.VERSION.SDK_INT >= 28) {
            if (!switchToNextInputMethod(false)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm == null) return;
            android.os.IBinder token = getWindow().getWindow().getAttributes().token;
            if (!imm.switchToNextInputMethod(token, false)) imm.showInputMethodPicker();
        }
    }

    // ---- Voice typing ----

    private void toggleVoice() {
        if (current != null) {
            current.stop = true;
            current = null;
            updateVoice();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            say("Voice typing needs the microphone. Opening Iron Donkey Dictation so you can allow it.");
            startActivity(new Intent(this, KeyboardSetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (Hub.recording) {
            say("The Iron Donkey Dictation app is already recording. Stop it there first.");
            return;
        }
        Models.ModelInfo m = Models.get(prefs.selectedModel());
        if (m == null || !ModelStore.isDownloaded(this, m)) {
            say("No model is downloaded yet. Pick one on the Models screen in Iron Donkey Dictation.");
            return;
        }
        Session s = new Session();
        current = s;
        updateVoice();
        new Thread(() -> record(s, m), "keyboard-record").start();
    }

    private void record(Session s, Models.ModelInfo m) {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dictation:keyboard");
        wake.acquire(60 * 60 * 1000L);
        em.beginUse();
        em.ensureLoaded();
        Transcription t = null;
        try {
            float maxPiece = m.isUrdu(prefs.language(m)) ? Urdu.MAX_PIECE_SEC : 20f;
            t = new Transcription(em, ModelStore.vadModel(this), maxPiece, prefs.assembler());
            Mic.capture(this, t, () -> s.stop);
        } catch (Exception e) {
            String msg = "Voice typing stopped: " + (e.getMessage() == null ? e.toString() : e.getMessage());
            main.post(() -> say(msg));
        }
        main.post(() -> {
            if (current == s) current = null;
            finishing++;
            updateVoice();
        });
        try {
            if (t != null) {
                String text = t.finish();
                main.post(() -> deliver(text, m.name));
            }
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            main.post(() -> say(msg));
        } finally {
            em.endUse();
            if (wake.isHeld()) wake.release();
            main.post(() -> {
                finishing--;
                updateVoice();
                if (!prefs.keepLoaded() && current == null && !Hub.recording && !Hub.fileBusy && !MainActivity.visible) em.maybeUnload();
            });
        }
    }

    private void deliver(String text, String modelName) {
        if (text == null || text.isEmpty()) {
            say(TextPipeline.NO_SPEECH);
            return;
        }
        if (prefs.history()) History.add(this, text, modelName, "Keyboard");
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            Hub.copy(this, text);
            say("No text box is open, so the text was copied instead.");
            return;
        }
        String out = text;
        CharSequence before = ic.getTextBeforeCursor(1, 0);
        if (before != null && before.length() > 0 && !Character.isWhitespace(before.charAt(0))
                && !(out.length() > 0 && ",.!?;:".indexOf(out.charAt(0)) >= 0)) {
            out = " " + out;
        }
        ic.commitText(out, 1);
        Hub.signal(this);
    }

    private void updateVoice() {
        if (voice == null) return;
        boolean rec = current != null;
        voice.setText(rec ? "Stop" : "Voice typing");
        voice.setContentDescription(rec ? "Stop" : "Voice typing");
        if (warning != null) warning.setVisibility(Battery.ignoring(this) ? View.GONE : View.VISIBLE);
        updateStatus();
    }

    private void updateStatus() {
        if (status == null) return;
        String s = engineLine;
        if (finishing > 0 && current == null) s = "Writing the text… " + s;
        status.setText(s);
    }

    private void say(String msg) {
        if (status != null) status.setText(msg);
        speak(msg);
    }

    @SuppressWarnings("deprecation")
    private void speak(String msg) {
        if (root != null && root.isAttachedToWindow()) root.announceForAccessibility(msg);
        else Hub.announce(this, msg);
    }

    @Override
    public void onEngineState(EngineManager.State state, Models.ModelInfo model, String error) {
        Models.ModelInfo sel = Models.get(prefs.selectedModel());
        String name = model != null ? model.name : sel != null ? sel.name : null;
        switch (state) {
            case LOADING: engineLine = name + " is loading. You can start talking now."; break;
            case READY: engineLine = "Model: " + name; break;
            case ERROR: engineLine = "The model could not load: " + error; break;
            default:
                engineLine = sel == null || !ModelStore.isDownloaded(this, sel)
                        ? "No model downloaded. Pick one in Iron Donkey Dictation." : "Model: " + sel.name + " (loads when you start)";
        }
        updateStatus();
    }

    // Hub: the keyboard only shows the app's messages, it never takes the app's text.
    @Override public void onStatus() { }
    @Override public boolean onResult(String text) { return false; }
    @Override public void onMessage(String message) { if (status != null) status.setText(message); }
}
