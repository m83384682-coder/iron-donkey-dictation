// Java port of the data classes in sherpa-onnx v2.0.1 kotlin-api (Apache 2.0).
// Field names and types must match what libsherpa-onnx-jni.so reads through JNI.
package com.k2fsa.sherpa.onnx;

public class OfflineWhisperModelConfig {
    public String encoder = "";
    public String decoder = "";
    public String language = "en";
    public String task = "transcribe";
    public int tailPaddings = 1000;
    public boolean enableTokenTimestamps = false;
    public boolean enableSegmentTimestamps = false;
}
