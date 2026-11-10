// Java port of the data classes in sherpa-onnx v2.0.1 kotlin-api (Apache 2.0).
// Field names and types must match what libsherpa-onnx-jni.so reads through JNI.
package com.k2fsa.sherpa.onnx;

public class OfflineFunAsrNanoModelConfig {
    public String encoderAdaptor = "";
    public String llm = "";
    public String embedding = "";
    public String tokenizer = "";
    public String systemPrompt = "You are a helpful assistant.";
    public String userPrompt = "\u8bed\u97f3\u8f6c\u5199\uff1a";
    public int maxNewTokens = 512;
    public float temperature = 1e-6f;
    public float topP = 0.8f;
    public int seed = 42;
    public String language = "";
    public boolean itn = true;
    public String hotwords = "";
}
