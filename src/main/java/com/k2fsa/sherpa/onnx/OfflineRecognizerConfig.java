// Java port of the data classes in sherpa-onnx v2.0.1 kotlin-api (Apache 2.0).
// Field names and types must match what libsherpa-onnx-jni.so reads through JNI.
package com.k2fsa.sherpa.onnx;

public class OfflineRecognizerConfig {
    public FeatureConfig featConfig = new FeatureConfig();
    public OfflineModelConfig modelConfig = new OfflineModelConfig();
    public HomophoneReplacerConfig hr = new HomophoneReplacerConfig();
    public String decodingMethod = "greedy_search";
    public int maxActivePaths = 4;
    public String hotwordsFile = "";
    public float hotwordsScore = 1.5f;
    public String ruleFsts = "";
    public String ruleFars = "";
    public float blankPenalty = 0f;
    public OfflineCtcFstDecoderConfig ctcFstDecoderConfig = new OfflineCtcFstDecoderConfig();
}
