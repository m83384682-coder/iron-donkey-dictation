// Java port of the data classes in sherpa-onnx v2.0.1 kotlin-api (Apache 2.0).
// Field names and types must match what libsherpa-onnx-jni.so reads through JNI.
package com.k2fsa.sherpa.onnx;

public class OfflineQwen3AsrModelConfig {
    public String convFrontend = "";
    public String encoder = "";
    public String decoder = "";
    public String tokenizer = "";
    public int maxTotalLen = 512;
    public int maxNewTokens = 128;
    public float temperature = 1e-6f;
    public float topP = 0.8f;
    public int seed = 42;
    public String hotwords = "";
}
