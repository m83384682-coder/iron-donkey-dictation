// Java port of the data classes in sherpa-onnx v2.0.1 kotlin-api (Apache 2.0).
// Field names and types must match what libsherpa-onnx-jni.so reads through JNI.
package com.k2fsa.sherpa.onnx;

public class VadModelConfig {
    public SileroVadModelConfig sileroVadModelConfig = new SileroVadModelConfig();
    public TenVadModelConfig tenVadModelConfig = new TenVadModelConfig();
    public int sampleRate = 16000;
    public int numThreads = 1;
    public String provider = "cpu";
    public boolean debug = false;
}
