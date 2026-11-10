package com.irondonkey.dictation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout col = Ui.page(this, "About");
        col.addView(Ui.heading(this, "Iron Donkey Dictation"));
        col.addView(Ui.text(this, "Built by Iron Donkey", 18, Ui.ACCENT));
        col.addView(Ui.body(this, "Version " + BuildConfig.VERSION_NAME));
        Ui.add(col, Ui.body(this, "Offline speech to text. The internet is only used to download models and translation files, "
                + "and for the online translator if you turn it on; everything else happens on your phone."), 8);

        col.addView(Ui.subheading(this, "Speech models"));
        for (Models.ModelInfo m : Models.ALL) col.addView(Ui.body(this, m.name + ": " + m.license + "."));
        col.addView(Ui.body(this, "Models converted for sherpa-onnx by csukuangfj (Fangjun Kuang) and downloaded from Hugging Face."));

        col.addView(Ui.subheading(this, "Libraries"));
        col.addView(Ui.body(this, "sherpa-onnx 2.0.1 by the k2-fsa project (Xiaomi and contributors). Apache License 2.0."));
        col.addView(Ui.body(this, "ONNX Runtime by Microsoft. MIT License."));
        col.addView(Ui.body(this, "Silero VAD by the Silero Team, used to find pauses. MIT License."));
        col.addView(Ui.body(this, "kaldi-native-fbank, kaldi-decoder, OpenFst, simple-sentencepiece, utf8proc, nlohmann/json, Eigen and hclust-cpp, inside sherpa-onnx."));
        col.addView(Ui.body(this, "Google ML Kit Translation and Language ID, for offline translation. Free to use under the ML Kit Terms of Service; its open-source parts are listed in Android's open-source notices."));
        col.addView(Ui.body(this, "Online translation, when you choose it, uses Google Cloud Translation with your own API key."));

        Ui.add(col, Ui.button(this, "Read the license texts", v -> startActivity(new Intent(this, LicenseActivity.class))), 16);

        col.addView(Ui.subheading(this, "Problems and ideas"));
        col.addView(Ui.body(this, "The app has been tested on a real phone, but errors may still happen sometimes. "
                + "If something goes wrong, please report it on GitHub so it can be fixed."));
        Ui.add(col, Ui.button(this, "Report a problem", v -> Report.show(this)), 4);
        Ui.add(col, Ui.button(this, "Open the project page on GitHub", v -> Report.open(this, ReportText.REPO)), 4);
        Ui.add(col, Ui.button(this, "Clear the error log", v -> {
            ErrorLog.clear(this);
            Hub.announce(this, "Error log cleared");
            v.setContentDescription("Error log cleared");
            ((android.widget.Button) v).setText("Error log cleared");
        }), 4);
    }
}
