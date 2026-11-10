#!/bin/bash
# Recompress the native library harder and sign the split APKs with your own key.
# Usage:
#   KS=/path/to/your.keystore KS_ALIAS=youralias KS_PASS=... KEY_PASS=... ./finalize.sh
# Needs ANDROID_HOME (or ANDROID_SDK_ROOT) pointing at an SDK with build-tools 36.1.0.
set -e
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:?Set ANDROID_HOME to your Android SDK}}
BT=$SDK/build-tools/36.1.0
: "${KS:?Set KS to your keystore path}" "${KS_ALIAS:?Set KS_ALIAS}" "${KS_PASS:?Set KS_PASS}"
export KS_PASS
export KEY_PASS=${KEY_PASS:-$KS_PASS}
mkdir -p out
for abi in arm64-v8a armeabi-v7a; do
  in=build/outputs/apk/debug/IronDonkeyDictation-$abi-debug.apk
  "$BT/zipalign" -f -p -z 4 "$in" "/tmp/dict-$abi-z.apk"
  "$BT/apksigner" sign --ks "$KS" --ks-pass env:KS_PASS --key-pass env:KEY_PASS --ks-key-alias "$KS_ALIAS" \
    --out "out/iron-donkey-dictation-$abi.apk" "/tmp/dict-$abi-z.apk"
done
ls -la out/*.apk
