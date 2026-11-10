package com.irondonkey.dictation;

import com.k2fsa.sherpa.onnx.OfflineRecognizer;
import com.k2fsa.sherpa.onnx.OfflineRecognizerResult;
import com.k2fsa.sherpa.onnx.OfflineStream;

import java.io.File;

/** One loaded speech model. Only one Engine is alive at a time. */
public final class Engine {
    public final Models.ModelInfo model;
    public final File dir;
    public final int threads;
    private volatile String language;
    private volatile String translateTo;   // the model translates by itself into this language, or null
    private String appliedLanguage;
    private OfflineRecognizer rec;

    public Engine(Models.ModelInfo model, File dir, String language, int threads) {
        this.model = model;
        this.dir = dir;
        this.threads = threads;
        this.language = language == null ? model.defaultLanguage() : language;
        this.rec = new OfflineRecognizer(model.config(dir, this.language, threads));
        this.appliedLanguage = this.language + "|transcribe";
    }

    public void setLanguage(String lang) {
        this.language = lang == null ? model.defaultLanguage() : lang;
    }

    public String language() { return language; }

    /** Let the model translate by itself while it listens (Whisper into English, Canary between en/es/de/fr). Null turns it off. */
    public void setTranslateTo(String target) { this.translateTo = target == null || target.isEmpty() ? null : target; }

    public String translateTo() { return translateTo; }

    /** Decodes 16 kHz mono samples and returns the raw model text. */
    public synchronized String decode(float[] samples) {
        if (rec == null) throw new IllegalStateException("Model was unloaded");
        String lang = language;
        String target = translateTo;
        if (model.type == Models.Type.WHISPER) {
            String task = target != null ? "translate" : "transcribe";
            String key = lang + "|" + task;
            if (!key.equals(appliedLanguage)) {
                rec.setConfig(model.config(dir, lang, threads, task));
                appliedLanguage = key;
            }
        }
        OfflineStream s = rec.createStream();
        try {
            if (model.type == Models.Type.CANARY) {
                String l = lang.isEmpty() ? "en" : lang;
                s.setOption("src_lang", l);
                s.setOption("tgt_lang", target != null ? target : l);
            } else if (model.type == Models.Type.QWEN3 && !lang.isEmpty()) {
                s.setOption("language", lang);
            }
            s.acceptWaveform(samples, TextPipeline.SAMPLE_RATE);
            rec.decode(s);
            OfflineRecognizerResult r = rec.getResult(s);
            return r == null ? "" : r.text;
        } finally {
            s.release();
        }
    }

    public synchronized void release() {
        if (rec != null) { rec.release(); rec = null; }
    }
}
