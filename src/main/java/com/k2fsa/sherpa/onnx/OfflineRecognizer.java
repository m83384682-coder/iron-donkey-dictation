// Java port of sherpa-onnx v2.0.1 kotlin-api OfflineRecognizer (Apache 2.0).
package com.k2fsa.sherpa.onnx;

import android.content.res.AssetManager;

public class OfflineRecognizer {
    static { System.loadLibrary("sherpa-onnx-jni"); }

    private long ptr;

    public OfflineRecognizer(OfflineRecognizerConfig config) {
        ptr = newFromFile(config);
        if (ptr == 0) throw new IllegalStateException("Could not load the model");
    }

    public synchronized void release() {
        if (ptr != 0) { delete(ptr); ptr = 0; }
    }

    public void setConfig(OfflineRecognizerConfig config) {
        setConfig(ptr, config);
    }

    public OfflineStream createStream() {
        return new OfflineStream(createStream(ptr));
    }

    public void decode(OfflineStream stream) {
        decode(ptr, stream.ptr);
    }

    public OfflineRecognizerResult getResult(OfflineStream stream) {
        return getResult(stream.ptr);
    }

    private native void delete(long ptr);
    private native long newFromAsset(AssetManager assetManager, OfflineRecognizerConfig config);
    private native long newFromFile(OfflineRecognizerConfig config);
    private native void setConfig(long ptr, OfflineRecognizerConfig config);
    private native long createStream(long ptr);
    private native void decode(long ptr, long streamPtr);
    private native OfflineRecognizerResult getResult(long streamPtr);
}
