# The sherpa-onnx JNI library reads these classes and fields by name.
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclasseswithmembernames class * { native <methods>; }
