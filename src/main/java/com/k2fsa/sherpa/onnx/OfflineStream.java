// Java port of sherpa-onnx v2.0.1 kotlin-api OfflineStream (Apache 2.0).
package com.k2fsa.sherpa.onnx;

public class OfflineStream {
    long ptr;

    OfflineStream(long ptr) { this.ptr = ptr; }

    public void acceptWaveform(float[] samples, int sampleRate) {
        acceptWaveform(ptr, samples, sampleRate);
    }

    public void setOption(String key, String value) {
        setOption(ptr, key, value);
    }

    public synchronized void release() {
        if (ptr != 0) { delete(ptr); ptr = 0; }
    }

    private native void acceptWaveform(long ptr, float[] samples, int sampleRate);
    private native void setOption(long ptr, String key, String value);
    private native void delete(long ptr);
}
