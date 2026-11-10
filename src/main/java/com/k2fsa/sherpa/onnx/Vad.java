// Java port of sherpa-onnx v2.0.1 kotlin-api Vad (Apache 2.0).
package com.k2fsa.sherpa.onnx;

import android.content.res.AssetManager;

public class Vad {
    static { System.loadLibrary("sherpa-onnx-jni"); }

    private long ptr;

    public Vad(VadModelConfig config) {
        ptr = newFromFile(config);
        if (ptr == 0) throw new IllegalStateException("Could not load the voice detector");
    }

    public synchronized void release() {
        if (ptr != 0) { delete(ptr); ptr = 0; }
    }

    public void acceptWaveform(float[] samples) { acceptWaveform(ptr, samples); }
    public boolean empty() { return empty(ptr); }
    public void pop() { pop(ptr); }
    public SpeechSegment front() { return front(ptr); }
    public void clear() { clear(ptr); }
    public boolean isSpeechDetected() { return isSpeechDetected(ptr); }
    public void reset() { reset(ptr); }
    public void flush() { flush(ptr); }

    private native void delete(long ptr);
    private native long newFromAsset(AssetManager assetManager, VadModelConfig config);
    private native long newFromFile(VadModelConfig config);
    private native void acceptWaveform(long ptr, float[] samples);
    private native boolean empty(long ptr);
    private native void pop(long ptr);
    private native void clear(long ptr);
    private native SpeechSegment front(long ptr);
    private native boolean isSpeechDetected(long ptr);
    private native void reset(long ptr);
    private native void flush(long ptr);
}
