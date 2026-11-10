package com.irondonkey.dictation;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Translation for the model in use. Hidden for models that hear only one language.
 * Every model keeps its own translation settings.
 */
public class TranslationActivity extends Activity {
    private Prefs prefs;
    private Models.ModelInfo model;
    private LinearLayout col, details;
    private TextView summary;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        col = Ui.page(this, "Translation");
        build();
    }

    private void build() {
        col.removeAllViews();
        model = Models.get(prefs.selectedModel());
        col.addView(Ui.heading(this, "Translation"));
        if (model == null) {
            col.addView(Ui.body(this, "Pick a model on the Models screen first."));
            return;
        }
        if (!Translation.multilingual(model)) {
            col.addView(Ui.body(this, model.name + " hears only English, so translation isn't available for it. "
                    + "Switch to a model that knows more languages (Parakeet Ultra, Canary, Qwen3, Whisper Small or Whisper Turbo) to use it."));
            return;
        }
        col.addView(Ui.body(this, "For " + model.name + ". Each model keeps its own translation settings. "
                + "It works for recording, files and keyboard voice typing."));

        Switch on = toggle("Translate what I say", prefs.translateOn(model), v -> {
            prefs.setTranslateOn(model, v);
            EngineManager.get(this).languageChanged();
            showDetails();
        });
        on.setContentDescription("Translate what I say, for " + model.name);

        summary = Ui.text(this, "", 15, Ui.ACCENT);
        Ui.add(col, summary, 8);
        details = Ui.column(this);
        col.addView(details, Ui.matchWrap(this, 4));
        showDetails();
    }

    private void showDetails() {
        details.removeAllViews();
        boolean on = prefs.translateOn(model);
        Translate.Plan plan = Translate.plan(this, model);
        summary.setText(!on ? "Translation is off." : plan == null ? "The spoken and target languages are the same, so nothing is translated."
                : Translate.describe(plan));
        details.setVisibility(on ? View.VISIBLE : View.GONE);
        if (!on) return;

        // Spoken language
        String[][] spoken = Translation.spokenChoices(model);
        String[] names = new String[spoken.length];
        int cur = 0;
        String from = prefs.translateFrom(model);
        for (int i = 0; i < spoken.length; i++) {
            names[i] = spoken[i][1];
            if (spoken[i][0].equals(from)) cur = i;
        }
        choice("I speak", names, cur, i -> {
            prefs.setTranslateFrom(model, spoken[i][0]);
            EngineManager.get(this).languageChanged();
            showDetails();
        });
        if (Translation.sharesModelLanguage(model)) {
            details.addView(Ui.text(this, "This is the same as the model's Language setting on the Models screen.", 13, Ui.DIM));
        }

        // Target language
        String[] targets = new String[Translation.TARGETS.length];
        int tcur = 0;
        for (int i = 0; i < targets.length; i++) {
            targets[i] = Translation.TARGETS[i][1];
            if (Translation.TARGETS[i][0].equals(prefs.translateTo(model))) tcur = i;
        }
        choice("Translate into", targets, tcur, i -> {
            prefs.setTranslateTo(model, Translation.TARGETS[i][0]);
            EngineManager.get(this).languageChanged();
            showDetails();
        });

        // Translator
        List<String> engines = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        engines.add(Translation.ENGINE_AUTO);
        labels.add(Translation.modelHasTranslator(model)
                ? "Best available (" + model.name + " itself when it can, otherwise offline)" : "Offline translator (on your phone)");
        if (Translation.modelHasTranslator(model)) {
            engines.add(Translation.ENGINE_OFFLINE);
            labels.add("Offline translator (on your phone)");
        }
        engines.add(Translation.ENGINE_ONLINE);
        labels.add("Online: Google Cloud Translation (needs your API key and internet)");
        String e = prefs.translateEngine(model);
        int ecur = Math.max(0, engines.indexOf(e));
        choice("Translator", labels.toArray(new String[0]), ecur, i -> {
            prefs.setTranslateEngine(model, engines.get(i));
            EngineManager.get(this).languageChanged();
            showDetails();
        });
        details.addView(Ui.text(this, engineHelp(plan), 13, Ui.DIM));

        if (plan != null && Translation.ENGINE_ONLINE.equals(plan.engine)) {
            Button key = Ui.button(this, prefs.googleKey().isEmpty() ? "Add your Google Cloud API key" : "Google Cloud API key: saved. Change it", v -> askKey());
            Ui.add(details, key, 4);
        }
        if (plan != null && Translation.ENGINE_OFFLINE.equals(plan.engine)) {
            Button dl = Ui.button(this, "Download the translation files now", v -> downloadNow(plan));
            Ui.add(details, dl, 4);
            Ui.add(details, Ui.button(this, "Manage translation files", v -> manage()), 4);
        }

        toggle(details, "Also keep the original words under the translation", prefs.keepOriginal(), prefs::setKeepOriginal);
        toggle(details, "Download translation files on mobile data too", prefs.translateMobileData(), prefs::setTranslateMobileData);
    }

    private String engineHelp(Translate.Plan plan) {
        if (plan == null) return "";
        switch (plan.engine) {
            case Translation.ENGINE_MODEL:
                return model.name + " translates by itself while it listens, fully offline, with nothing extra to download."
                        + (model.type == Models.Type.WHISPER ? " Whisper can only translate into English." : " Canary translates between English and Spanish, German or French.");
            case Translation.ENGINE_ONLINE:
                return "Your text is sent to Google to translate. This is the only part of the app that sends your words over the internet. "
                        + "It needs a Google Cloud API key with the Cloud Translation API turned on. Web addresses and emails are never sent for translating.";
            default:
                return "Translates on your phone with Google's ML Kit. Each language is a one-time download of about 30 MB, then it works with no internet. "
                        + "Web addresses and emails stay as they are.";
        }
    }

    private void askKey() {
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        in.setHint("API key");
        in.setText(prefs.googleKey());
        in.setContentDescription("Google Cloud API key");
        new AlertDialog.Builder(this)
                .setTitle("Google Cloud API key")
                .setMessage("It's kept only on this phone and is used only to call Google Cloud Translation.")
                .setView(in)
                .setPositiveButton("Save", (d, w) -> { prefs.setGoogleKey(in.getText().toString()); showDetails(); Hub.announce(this, "Key saved"); })
                .setNeutralButton("Remove", (d, w) -> { prefs.setGoogleKey(""); showDetails(); })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private volatile boolean busy;

    private void downloadNow(Translate.Plan plan) {
        List<String> langs = new ArrayList<>();
        if (plan.from != null && !plan.from.isEmpty()) langs.add(plan.from);
        langs.add(plan.to);
        download(langs, null);
    }

    /** Downloads the languages one at a time, saying which one is going, and explains any failure. */
    private void download(List<String> langs, Runnable after) {
        if (busy) {
            say("A download is already going. Please wait for it to finish.");
            return;
        }
        busy = true;
        say("Downloading " + names(langs) + "...");
        new Thread(() -> {
            try {
                int i = 0;
                for (String l : langs) {
                    i++;
                    if (l.equals("en")) continue;
                    String step = langs.size() > 1 ? " (" + i + " of " + langs.size() + ")" : "";
                    String name = Translation.targetName(l);
                    main.post(() -> say("Downloading " + name + step + ", about 30 MB..."));
                    Translate.download(this, l);
                }
                main.post(() -> {
                    say("Ready. " + names(langs) + " translation works offline now.");
                    Hub.announce(this, "Download finished");
                    if (after != null) after.run();
                });
            } catch (Exception e) {
                String why = Translate.explain(this, "Translation download", e);
                main.post(() -> failed("Download failed", why));
            } finally {
                busy = false;
            }
        }, "translate-dl").start();
    }

    /** Shows what went wrong in words, with a way to report it. */
    private void failed(String title, String why) {
        say(title + ". " + why);
        if (isFinishing()) return;
        new AlertDialog.Builder(this).setTitle(title).setMessage(why)
                .setPositiveButton("OK", null)
                .setNeutralButton("Report a problem", (d, w) -> Report.show(this))
                .show();
    }

    private void manage() {
        say("Checking which translation files are on this phone...");
        new Thread(() -> {
            try {
                List<String> have = Translate.downloaded();
                have.remove("en");
                main.post(() -> { say(have.isEmpty() ? "No translation files yet, apart from English, which is built in."
                        : have.size() + (have.size() == 1 ? " language" : " languages") + " on this phone, plus English."); showManage(have); });
            } catch (Exception e) {
                String why = Translate.explain(this, "List translation files", e);
                main.post(() -> failed("Couldn't list the files", why));
            }
        }).start();
    }

    private void showManage(List<String> have) {
        if (isFinishing()) return;
        List<String> items = new ArrayList<>();
        items.add("Download another language");
        for (String c : have) items.add("Delete " + Translation.targetName(c));
        new AlertDialog.Builder(this)
                .setTitle(have.isEmpty() ? "Translation files: only English, which is built in" : "Translation files on this phone")
                .setItems(items.toArray(new String[0]), (d, i) -> {
                    if (i == 0) pickLanguage(have);
                    else deleteLanguage(have.get(i - 1));
                })
                .setNegativeButton("Close", null).show();
    }

    /** Every offline language that isn't on the phone yet, to download one ahead of time. */
    private void pickLanguage(List<String> have) {
        List<String> codes = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (String[] t : Translation.TARGETS) {
            if (t[0].equals("en") || have.contains(t[0])) continue;
            codes.add(t[0]);
            names.add(t[1]);
        }
        new AlertDialog.Builder(this).setTitle("Download a language (about 30 MB)")
                .setItems(names.toArray(new String[0]), (d, i) -> {
                    List<String> one = new ArrayList<>();
                    one.add(codes.get(i));
                    download(one, null);
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void deleteLanguage(String code) {
        String name = Translation.targetName(code);
        new AlertDialog.Builder(this).setTitle("Delete " + name + "?")
                .setMessage("You can download it again later.")
                .setPositiveButton("Delete", (d, w) -> new Thread(() -> {
                    try {
                        Translate.delete(code);
                        main.post(() -> say(name + " deleted"));
                    } catch (Exception e) {
                        String why = Translate.explain(this, "Delete translation file", e);
                        main.post(() -> failed("Couldn't delete " + name, why));
                    }
                }).start())
                .setNegativeButton("Keep", null).show();
    }

    private static String names(List<String> codes) {
        List<String> n = new ArrayList<>();
        for (String c : codes) n.add(Translation.targetName(c));
        return String.join(" and ", n);
    }

    private void say(String s) {
        summary.setText(s);
        Hub.announce(this, s);
    }

    private interface IntSetter { void set(int v); }
    private interface BoolSetter { void set(boolean v); }

    private void choice(String label, String[] options, int current, IntSetter setter) {
        Button b = Ui.button(this, label + ": " + options[current], null);
        b.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(label)
                .setSingleChoiceItems(options, current, (d, which) -> { d.dismiss(); setter.set(which); })
                .setNegativeButton("Cancel", null)
                .show());
        Ui.add(details, b, 4);
    }

    private Switch toggle(String label, boolean on, BoolSetter setter) {
        return toggle(col, label, on, setter);
    }

    private Switch toggle(LinearLayout parent, String label, boolean on, BoolSetter setter) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(Ui.TEXT);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        s.setMinHeight(Ui.dp(this, 48));
        s.setChecked(on);
        s.setOnCheckedChangeListener((v, c) -> setter.set(c));
        Ui.add(parent, s, 4);
        return s;
    }
}
