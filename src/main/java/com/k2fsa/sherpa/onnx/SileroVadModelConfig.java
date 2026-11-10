// Java port of the data classes in sherpa-onnx v2.0.1 kotlin-api (Apache 2.0).
// Field names and types must match what libsherpa-onnx-jni.so reads through JNI.
package com.k2fsa.sherpa.onnx;

public class SileroVadModelConfig {
    public String model = "";
    public float threshold = 0.5f;
    public float minSilenceDuration = 0.25f;
    public float minSpeechDuration = 0.25f;
    public int windowSize = 512;
    public float maxSpeechDuration = 5.0f;
}
