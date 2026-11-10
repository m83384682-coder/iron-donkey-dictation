package com.k2fsa.sherpa.onnx;

// Constructor signature must match what the JNI library calls.
public class OfflineRecognizerResult {
    public final String text;
    public final String[] tokens;
    public final float[] timestamps;
    public final String lang;

    public OfflineRecognizerResult(String text, String[] tokens, float[] timestamps, String lang,
                                   String emotion, String event, float[] durations, int[] words) {
        this.text = text == null ? "" : text;
        this.tokens = tokens;
        this.timestamps = timestamps;
        this.lang = lang;
    }
}
