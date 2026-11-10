# The sherpa-onnx JNI library reads these classes and fields by name.
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# Google ML Kit (the offline translator) finds its parts by reflection: it reads the registrar class names from
# the manifest and creates each one with its no-argument constructor. The rule that ships with
# firebase-components 16.1.0 keeps only the class names, so R8 full mode removed those constructors in 1.2.
# ML Kit then started with no parts registered, RemoteModelManager.getInstance() returned null and downloads
# crashed. Keep the registrars whole, and keep ML Kit's own code as it is.
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.firebase.components.** { *; }
