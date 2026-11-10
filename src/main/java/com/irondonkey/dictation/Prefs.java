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
        return new TextPipeline.Assembler(paragraphs(), true).withLinks(cleanLinks());
    }

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
