// Java port of the data classes in sherpa-onnx v2.0.1 kotlin-api (Apache 2.0).
// Field names and types must match what libsherpa-onnx-jni.so reads through JNI.
package com.k2fsa.sherpa.onnx;

public class OfflineCanaryModelConfig {
    public String encoder = "";
    public String decoder = "";
    public String srcLang = "en";
    public String tgtLang = "en";
    public boolean usePnc = true;
}
