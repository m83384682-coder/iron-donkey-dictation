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
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
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

    /** Hold a key for these. The extra symbols on the number row follow the usual phone layout. */
    private static final Map<String, String> MORE = new HashMap<>();
    static {
        String[][] m = {
                {"a", "\u00e0\u00e1\u00e2\u00e4\u00e3\u00e5\u0101\u00e6"}, {"c", "\u00e7\u0107\u010d"}, {"d", "\u010f\u00f0"},
                {"e", "\u00e8\u00e9\u00ea\u00eb\u0113\u0117\u0119"}, {"g", "\u011f"}, {"i", "\u00ec\u00ed\u00ee\u00ef\u012b\u0131"},
                {"l", "\u0142"}, {"n", "\u00f1\u0144\u0148"}, {"o", "\u00f2\u00f3\u00f4\u00f6\u00f5\u00f8\u014d\u0153"},
                {"r", "\u0159"}, {"s", "\u00df\u015b\u0161\u015f"}, {"t", "\u0165\u00fe"}, {"u", "\u00f9\u00fa\u00fb\u00fc\u016b\u016f"},
                {"y", "\u00fd\u00ff"}, {"z", "\u017a\u017c\u017e"},
                {"1", "!\u00b9\u00bd\u2153\u00bc"}, {"2", "@\u00b2\u2154"}, {"3", "#\u00b3\u00be"}, {"4", "$\u20ac\u00a3\u00a5\u20b9"},
                {"5", "%\u2030"}, {"6", "^"}, {"7", "&"}, {"8", "*\u2022"}, {"9", "("}, {"0", ")\u00b0\u2205"},
                {".", ",?!:;\u2026'\"-"}, {",", ";:'\""}, {"-", "\u2013\u2014_"}, {"?", "\u00bf"}, {"!", "\u00a1"},
                {"$", "\u20ac\u00a3\u00a5\u20b9\u00a2"}, {"\"", "\u201c\u201d\u00ab\u00bb"}, {"'", "\u2018\u2019"},
                {"*", "\u2022\u00d7\u2605"}, {"=", "\u2260\u2248\u00b1"}, {"/", "\u00f7"}, {"%", "\u2030"},
        };
        for (String[] p : m) MORE.put(p[0], p[1]);
        String[][] n = {{"\u20ac", "Euro"}, {"\u00a3", "Pound"}, {"\u00a5", "Yen"}, {"\u20b9", "Rupee"}, {"\u00a2", "Cent"},
                {"\u2026", "Ellipsis"}, {"\u2013", "En dash"}, {"\u2014", "Em dash"}, {"\u00b0", "Degree"},
                {"\u2022", "Bullet"}, {"\u00d7", "Times"}, {"\u00f7", "Divided by"}, {"\u2260", "Not equal"},
                {"\u2248", "Approximately"}, {"\u00b1", "Plus or minus"}, {"\u2605", "Star"}, {"\u2030", "Per mille"},
                {"\u00bf", "Inverted question mark"}, {"\u00a1", "Inverted exclamation mark"}, {"\u00bd", "One half"},
                {"\u00bc", "One quarter"}, {"\u00be", "Three quarters"}, {"\u2153", "One third"}, {"\u2154", "Two thirds"},
                {"\u00b9", "Superscript one"}, {"\u00b2", "Squared"}, {"\u00b3", "Cubed"}, {"\u201c", "Left quote"},
                {"\u201d", "Right quote"}, {"\u2018", "Left single quote"}, {"\u2019", "Right single quote"},
                {"\u00ab", "Left angle quote"}, {"\u00bb", "Right angle quote"}, {"\u2205", "Empty set"}, {"\u00df", "Sharp s"},
                {"\u00e6", "a e"}, {"\u0153", "o e"}, {"\u00f0", "Eth"}, {"\u00fe", "Thorn"}, {"\u0131", "Dotless i"}};
        for (String[] p : n) NAMES.put(p[0], p[1]);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private EngineManager em;

    private LinearLayout root, keys, tools, more;
    private Button warning, voice, shiftKey, enterKey, translateKey;
    private TextView status;
    private long lastSpace = 0;       // for double space -> period
    private long pttDown = 0;         // push to talk: when the voice button went down
    private final List<Button> letterKeys = new ArrayList<>();
    private boolean symbols = false;
    private int shift = SHIFT_OFF;
    private String engineLine = "";

    /** One voice typing recording. */
    private static final class Session {
        volatile boolean stop;
        volatile boolean discard;   // push to talk let go too soon: type nothing
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
        voice.setOnTouchListener((v, e) -> pushToTalk(e));
        top.addView(voice, new LinearLayout.LayoutParams(0, dp(56), 1));
        root.addView(top, rowParams(4));

        status = new TextView(this);
        status.setTextColor(Ui.DIM);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        status.setPadding(dp(4), dp(2), dp(4), dp(2));
        root.addView(status, rowParams(2));

        tools = row();
        root.addView(tools, rowParams(2));
        more = row();
        more.setVisibility(View.GONE);
        root.addView(more, rowParams(2));

        keys = new LinearLayout(this);
        keys.setOrientation(LinearLayout.VERTICAL);
        root.addView(keys, rowParams(2));

        em.removeListener(this);
        em.addListener(this);
        Hub.removeListener(this);
        Hub.addListener(this);
        buildTools();
        buildKeys();
        updateVoice();
        return root;
    }

    // ---- Tool row: cursor, select, paste, translate ----

    private void buildTools() {
        tools.removeAllViews();
        if (!prefs.toolRow()) { tools.setVisibility(View.GONE); return; }
        tools.setVisibility(View.VISIBLE);
        Button left = tool("\u2190", "Move cursor left", v -> moveCursor(KeyEvent.KEYCODE_DPAD_LEFT));
        left.setOnLongClickListener(v -> { home(); return true; });
        longLabel(left, "move to the start");
        tools.addView(left, toolParams(1f));
        Button right = tool("\u2192", "Move cursor right", v -> moveCursor(KeyEvent.KEYCODE_DPAD_RIGHT));
        right.setOnLongClickListener(v -> { end(); return true; });
        longLabel(right, "move to the end");
        tools.addView(right, toolParams(1f));
        tools.addView(tool("Select all", "Select all", v -> contextAction(android.R.id.selectAll, "All text selected")), toolParams(1.6f));
        tools.addView(tool("Copy", "Copy", v -> contextAction(android.R.id.copy, "Copied")), toolParams(1.2f));
        tools.addView(tool("Paste", "Paste", v -> contextAction(android.R.id.paste, "Pasted")), toolParams(1.2f));
        translateKey = tool("Translate", "Translate", v -> toggleTranslate());
        tools.addView(translateKey, toolParams(1.8f));
        updateTranslateKey();
    }

    private Button tool(String label, String spoken, View.OnClickListener l) {
        Button b = special(label, spoken, 1f, l);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return b;
    }

    private LinearLayout.LayoutParams toolParams(float w) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), w);
        int m = dp(2);
        lp.setMargins(m, m, m, m);
        return lp;
    }

    /** Tells TalkBack what a long press (double tap and hold) does. */
    private void longLabel(View v, String what) {
        v.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, what));
            }
        });
    }

    private void moveCursor(int code) {
        tick(voice);
        sendDownUpKeyEvents(code);
        // Say the character the cursor moved past, like other keyboards do.
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence c = code == KeyEvent.KEYCODE_DPAD_LEFT ? ic.getTextAfterCursor(1, 0) : ic.getTextBeforeCursor(1, 0);
        if (c != null && c.length() > 0) speak(charName(c.toString()));
    }

    private void home() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.setSelection(0, 0);
        speak("Start of text");
    }

    private void end() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence after = ic.getTextAfterCursor(100000, 0);
        CharSequence before = ic.getTextBeforeCursor(100000, 0);
        int n = (before == null ? 0 : before.length()) + (after == null ? 0 : after.length());
        ic.setSelection(n, n);
        speak("End of text");
    }

    private void contextAction(int id, String said) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (ic.performContextMenuAction(id)) speak(said);
    }

    private String charName(String c) {
        if (c.equals(" ")) return "Space";
        if (c.equals("\n")) return "New line";
        String n = NAMES.get(c);
        if (n != null) return n;
        if (c.length() == 1 && Character.isUpperCase(c.charAt(0))) return "Capital " + c;
        return c;
    }

    /** Translation on or off for the current model, right from the keyboard. Only for models that hear several languages. */
    private void toggleTranslate() {
        Models.ModelInfo m = Models.get(prefs.selectedModel());
        if (!Translation.multilingual(m)) return;
        prefs.setTranslateOn(m, !prefs.translateOn(m));
        em.languageChanged();
        updateTranslateKey();
        Translate.Plan pl = Translate.plan(this, m);
        say(pl != null ? Translate.describe(pl) : "Translation off");
    }

    private void updateTranslateKey() {
        if (translateKey == null) return;
        Models.ModelInfo m = Models.get(prefs.selectedModel());
        if (!Translation.multilingual(m)) { translateKey.setVisibility(View.GONE); return; }
        translateKey.setVisibility(View.VISIBLE);
        boolean on = prefs.translateOn(m);
        translateKey.setText(on ? "\u2192 " + Translation.targetName(prefs.translateTo(m)) : "Translate: off");
        translateKey.setContentDescription(on ? "Translate into " + Translation.targetName(prefs.translateTo(m)) + ", on" : "Translate, off");
        translateKey.setBackground(on ? keyBackground(0xFF2F5D50, 0xFF4A515C) : keyBackground(0xFF1E2228, 0xFF4A515C));
    }

    // ---- Accents and extra symbols: hold a key ----

    private boolean showMore(String base) {
        String extra = MORE.get(base.toLowerCase());
        if (extra == null) return false;
        tick(voice);
        more.removeAllViews();
        boolean upper = shift != SHIFT_OFF && Character.isLetter(base.charAt(0));
        List<String> spoken = new ArrayList<>();
        extra.codePoints().forEach(cp -> {
            String ch = new String(Character.toChars(cp));
            String out = upper ? ch.toUpperCase() : ch;
            if (upper && out.length() > 1) out = ch; // "\u00df" has no single capital
            final String typed = out;
            String name = charName(typed);
            spoken.add(name);
            Button b = key(typed, name, 1f, v -> { commitPlain(typed); hideMore(); afterLetter(); });
            more.addView(b, keyParams(1f));
        });
        Button close = special("\u2715", "Close accents", 1f, v -> { hideMore(); speak("Closed"); });
        more.addView(close, keyParams(1f));
        more.setVisibility(View.VISIBLE);
        speak("More for " + charName(upper ? base.toUpperCase() : base) + ": " + String.join(", ", spoken) + ". They're above the keyboard.");
        return true;
    }

    private void hideMore() {
        if (more != null) { more.setVisibility(View.GONE); more.removeAllViews(); }
    }

    /** A small vibration on key presses, when the user wants it. */
    private void tick(View v) {
        if (v != null && prefs.keyVibration()) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
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
                    if (MORE.containsKey(s)) {
                        b.setOnLongClickListener(v -> showMore(s));
                        longLabel(b, Character.isLetter(ch) ? "show accents" : "show more symbols");
                    }
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
                for (String s : SYMBOL_ROWS[r]) {
                    Button b = key(s, NAMES.getOrDefault(s, s), 1f, v -> commit(s));
                    if (MORE.containsKey(s)) {
                        b.setOnLongClickListener(v -> showMore(s));
                        longLabel(b, "show more symbols");
                    }
                    row.addView(b, keyParams(1f));
                }
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
        Button comma = key(",", "Comma", 1f, v -> commit(","));
        comma.setOnLongClickListener(v -> showMore(","));
        longLabel(comma, "show more symbols");
        bottom.addView(comma, keyParams(1f));
        bottom.addView(special("space", "Space", 4f, v -> space()), keyParams(4f));
        Button period = key(".", "Period", 1f, v -> commit("."));
        period.setOnLongClickListener(v -> showMore("."));
        longLabel(period, "show more symbols");
        bottom.addView(period, keyParams(1f));
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
        b.setOnClickListener(v -> { tick(v); l.onClick(v); });
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
        hideMore();
        if (tools != null) buildTools();
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
        commitPlain(t);
        afterLetter();
    }

    private void afterLetter() {
        if (shift == SHIFT_ONCE) {
            shift = SHIFT_OFF;
            updateShiftLabels();
        }
    }

    private void commit(String s) {
        commitPlain(s);
    }

    private void commitPlain(String s) {
        lastSpace = 0;
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(s, 1);
    }

    /** Space; a second space soon after a word types ". " instead, when turned on. */
    private void space() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long now = System.currentTimeMillis();
        if (prefs.doubleSpacePeriod() && lastSpace > 0 && now - lastSpace < 800) {
            CharSequence b = ic.getTextBeforeCursor(2, 0);
            if (b != null && b.length() == 2 && b.charAt(1) == ' ' && Character.isLetterOrDigit(b.charAt(0))) {
                ic.deleteSurroundingText(1, 0);
                ic.commitText(". ", 1);
                lastSpace = 0;
                speak("Period");
                return;
            }
        }
        ic.commitText(" ", 1);
        lastSpace = now;
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

    /**
     * Push to talk: hold the voice button to record, let go to type. With TalkBack, double tap and hold does
     * the same, and a plain double tap still starts and stops, so it always works.
     */
    private boolean pushToTalk(MotionEvent e) {
        if (!prefs.pushToTalk()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                voice.setPressed(true);
                pttDown = System.currentTimeMillis();
                if (current == null) { tick(voice); toggleVoice(); }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                voice.setPressed(false);
                long held = System.currentTimeMillis() - pttDown;
                if (current != null) {
                    if (held < 350) {
                        // Too short to say anything: drop it and explain.
                        current.discard = true;
                        current.stop = true;
                        current = null;
                        updateVoice();
                        say("Hold the button while you talk, then let go.");
                    } else {
                        tick(voice);
                        toggleVoice();
                    }
                }
                return true;
        }
        return true;
    }

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
            final Transcription tt = t;
            final int auto = prefs.pushToTalk() ? 0 : prefs.autoStopSec();
            Mic.capture(this, t, () -> {
                if (s.stop) return true;
                if (auto > 0 && tt.silenceSec() >= auto) {
                    main.post(() -> { if (current == s) { current = null; updateVoice(); } speak("Stopped after " + auto + " seconds of silence."); });
                    return true;
                }
                return false;
            });
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
            if (t != null && !s.discard) {
                String raw = t.finish();
                Translate.Plan plan = Translate.plan(this, m);
                String text = raw;
                if (!raw.isEmpty() && plan != null && !Translation.ENGINE_MODEL.equals(plan.engine)) {
                    main.post(() -> say("Translating to " + plan.toName() + "..."));
                    text = Translate.finish(this, plan, raw, msg -> main.post(() -> say(msg)));
                }
                final String out = text;
                main.post(() -> deliver(out, m.name));
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
        boolean ptt = prefs.pushToTalk();
        voice.setText(rec ? (ptt ? "Listening\u2026 let go to type" : "Stop") : (ptt ? "Hold to talk" : "Voice typing"));
        voice.setContentDescription(rec ? (ptt ? "Listening, let go to type" : "Stop")
                : (ptt ? "Voice typing. Double tap and hold to talk, let go to type." : "Voice typing"));
        updateTranslateKey();
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
