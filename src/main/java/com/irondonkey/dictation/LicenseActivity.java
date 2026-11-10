package com.irondonkey.dictation;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Shows the full license texts bundled in the app. */
public class LicenseActivity extends Activity {
    private static final String[][] FILES = {
            {"Notes on what is inside", "third-party-notes.txt"},
            {"Creative Commons Attribution 4.0 (Parakeet v2, Parakeet Ultra, Canary 180M Flash)", "CC-BY-4.0.txt"},
            {"Apache License 2.0 (sherpa-onnx, Qwen3-ASR, kaldi-native-fbank, kaldi-decoder, OpenFst, simple-sentencepiece)", "Apache-2.0.txt"},
            {"MIT License (OpenAI Whisper)", "MIT-whisper.txt"},
            {"Moonshine license (MIT for the English models)", "MIT-moonshine.txt"},
            {"MIT License (Silero VAD)", "MIT-silero-vad.txt"},
            {"MIT License (ONNX Runtime)", "MIT-onnxruntime.txt"},
            {"BSD License (hclust-cpp)", "BSD-hclust-cpp.txt"},
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout col = Ui.page(this, "Licenses");
        col.addView(Ui.heading(this, "License texts"));
        for (String[] f : FILES) {
            LinearLayout card = Ui.card(this);
            TextView h = Ui.subheading(this, f[0]);
            h.setPadding(0, 0, 0, Ui.dp(this, 6));
            card.addView(h);
            TextView t = Ui.text(this, read(f[1]), 12, Ui.DIM);
            t.setTypeface(Typeface.MONOSPACE);
            card.addView(t);
            Ui.add(col, card, 12);
        }
    }

    private String read(String name) {
        try (InputStream in = getAssets().open("licenses/" + name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "Could not open " + name;
        }
    }
}
