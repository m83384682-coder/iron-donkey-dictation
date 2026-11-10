package com.irondonkey.dictation;

import android.content.Context;
import android.content.SharedPreferences;

/** All user settings in one place. */
public final class Prefs {
    public static final int SIGNAL_NONE = 0, SIGNAL_VIBRATE = 1, SIGNAL_SOUND = 2, SIGNAL_TALKBACK = 3;
    public static final int MIC_PHONE = 0, MIC_WIRED = 1, MIC_BLUETOOTH = 2;

    private final SharedPreferences p;

    public Prefs(Context c) {
        p = c.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public String selectedModel() { return p.getString("model", null); }
    public void setSelectedModel(String id) { p.edit().putString("model", id).apply(); }

    public String language(Models.ModelInfo m) { return p.getString("lang_" + m.id, m.defaultLanguage()); }
    public void setLanguage(Models.ModelInfo m, String code) { p.edit().putString("lang_" + m.id, code).apply(); }

    public int signal() { return p.getInt("signal", SIGNAL_VIBRATE); }
    public void setSignal(int v) { p.edit().putInt("signal", v).apply(); }

    /** Seconds of each recording to skip at the start, 0 to 1.5. */
    public float skipStart() { return p.getFloat("skip", 0.5f); }
    public void setSkipStart(float v) { p.edit().putFloat("skip", v).apply(); }

    public boolean replaceText() { return p.getBoolean("replace", false); }
    public void setReplaceText(boolean v) { p.edit().putBoolean("replace", v).apply(); }

    public boolean autoCopy() { return p.getBoolean("autocopy", true); }
    public void setAutoCopy(boolean v) { p.edit().putBoolean("autocopy", v).apply(); }

    public boolean paragraphs() { return p.getBoolean("paragraphs", true); }
    public void setParagraphs(boolean v) { p.edit().putBoolean("paragraphs", v).apply(); }

    /** "www dot google dot com" becomes "www.google.com". */
    public boolean cleanLinks() { return p.getBoolean("links", true); }
    public void setCleanLinks(boolean v) { p.edit().putBoolean("links", v).apply(); }

    /** The text builder for a recording, with the user's settings. */
    public TextPipeline.Assembler assembler() {
        return new TextPipeline.Assembler(paragraphs(), true).withLinks(cleanLinks())
                .withCommands(spokenCommands()).withReplacements(TextExtras.parseReplacements(replacements()));
    }

    /** The text builder for a file: no "Stop." to strip at the start. */
    public TextPipeline.Assembler fileAssembler() {
        return new TextPipeline.Assembler(paragraphs(), false).withLinks(cleanLinks())
                .withCommands(spokenCommands()).withReplacements(TextExtras.parseReplacements(replacements()));
    }

    // ---- Translation, saved for each model on its own ----

    public boolean translateOn(Models.ModelInfo m) { return Translation.multilingual(m) && p.getBoolean("tr_on_" + m.id, false); }
    public void setTranslateOn(Models.ModelInfo m, boolean v) { p.edit().putBoolean("tr_on_" + m.id, v).apply(); }

    /** The language spoken. For most models this is the model's own language setting. */
    public String translateFrom(Models.ModelInfo m) {
        if (Translation.sharesModelLanguage(m)) return language(m);
        return p.getString("tr_from_" + m.id, "");
    }
    public void setTranslateFrom(Models.ModelInfo m, String code) {
        if (Translation.sharesModelLanguage(m)) setLanguage(m, code);
        else p.edit().putString("tr_from_" + m.id, code).apply();
    }

    public String translateTo(Models.ModelInfo m) { return p.getString("tr_to_" + m.id, "en"); }
    public void setTranslateTo(Models.ModelInfo m, String code) { p.edit().putString("tr_to_" + m.id, code).apply(); }

    public String translateEngine(Models.ModelInfo m) { return p.getString("tr_engine_" + m.id, Translation.ENGINE_AUTO); }
    public void setTranslateEngine(Models.ModelInfo m, String e) { p.edit().putString("tr_engine_" + m.id, e).apply(); }

    /** Put the original words under the translation. */
    public boolean keepOriginal() { return p.getBoolean("tr_keep", false); }
    public void setKeepOriginal(boolean v) { p.edit().putBoolean("tr_keep", v).apply(); }

    /** The user's own Google Cloud Translation API key, for the online translator. */
    public String googleKey() { return p.getString("tr_key", ""); }
    public void setGoogleKey(String k) { p.edit().putString("tr_key", k == null ? "" : k.trim()).apply(); }

    /** Download offline translation languages on mobile data too. */
    public boolean translateMobileData() { return p.getBoolean("tr_cell", true); }
    public void setTranslateMobileData(boolean v) { p.edit().putBoolean("tr_cell", v).apply(); }

    // ---- Text extras ----

    /** "new line", "new paragraph", "comma", "period" and friends become the real thing. Off by default. */
    public boolean spokenCommands() { return p.getBoolean("commands", false); }
    public void setSpokenCommands(boolean v) { p.edit().putBoolean("commands", v).apply(); }

    /** One rule per line: heard words = written words. */
    public String replacements() { return p.getString("replacements", ""); }
    public void setReplacements(String v) { p.edit().putString("replacements", v == null ? "" : v).apply(); }

    /** Stop recording by itself after this many seconds of silence; 0 never. */
    public int autoStopSec() { return p.getInt("autostop", 0); }
    public void setAutoStopSec(int v) { p.edit().putInt("autostop", v).apply(); }

    /** Text size of the text box, in sp. */
    public int textSize() { return p.getInt("textsize", 18); }
    public void setTextSize(int v) { p.edit().putInt("textsize", v).apply(); }

    // ---- Keyboard ----

    /** Hold the voice typing button to talk, let go to type. Off by default: tap to start, tap Stop. */
    public boolean pushToTalk() { return p.getBoolean("kb_ptt", false); }
    public void setPushToTalk(boolean v) { p.edit().putBoolean("kb_ptt", v).apply(); }

    /** Two spaces in a row type a period and a space. */
    public boolean doubleSpacePeriod() { return p.getBoolean("kb_dsp", true); }
    public void setDoubleSpacePeriod(boolean v) { p.edit().putBoolean("kb_dsp", v).apply(); }

    /** A tiny vibration on each key press. */
    public boolean keyVibration() { return p.getBoolean("kb_vib", true); }
    public void setKeyVibration(boolean v) { p.edit().putBoolean("kb_vib", v).apply(); }

    /** The row with the cursor arrows, Paste and Translate. */
    public boolean toolRow() { return p.getBoolean("kb_tools", true); }
    public void setToolRow(boolean v) { p.edit().putBoolean("kb_tools", v).apply(); }

    public boolean history() { return p.getBoolean("history", true); }
    public void setHistory(boolean v) { p.edit().putBoolean("history", v).apply(); }

    /** 0 means automatic. */
    public int cores() { return p.getInt("cores", 0); }
    public void setCores(int v) { p.edit().putInt("cores", v).apply(); }

    public int threads() {
        int c = cores();
        if (c > 0) return c;
        int n = Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(4, n / 2));
    }

    public boolean keepLoaded() { return p.getBoolean("keep", true); }
    public void setKeepLoaded(boolean v) { p.edit().putBoolean("keep", v).apply(); }

    public int mic() { return p.getInt("mic", MIC_PHONE); }
    public void setMic(int v) { p.edit().putInt("mic", v).apply(); }

    public boolean noiseReduction() { return p.getBoolean("ns", false); }
    public void setNoiseReduction(boolean v) { p.edit().putBoolean("ns", v).apply(); }

    public String boxText() { return p.getString("box", ""); }
    public void setBoxText(String t) { p.edit().putString("box", t).apply(); }

    public String undoText() { return p.getString("undo", null); }
    public void setUndoText(String t) { p.edit().putString("undo", t).apply(); }
}
