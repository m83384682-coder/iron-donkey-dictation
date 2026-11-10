package com.irondonkey.dictation;

import android.content.Context;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Runs translation on the phone: the speech model's own translation, the offline translator
 * (Google ML Kit, one small download per language), or the online one (Google Cloud Translation, with the
 * user's own API key). Call finish() off the main thread.
 */
public final class Translate {
    private Translate() {}

    public interface Status { void say(String s); }

    /** What will happen for the current model, or null when translation is off. */
    public static final class Plan {
        public final Models.ModelInfo model;
        public final String from;       // ISO code, "" for automatic, null when no translator knows it
        public final String fromName;
        public final String to;
        public final String engine;     // Translation.ENGINE_MODEL, _OFFLINE or _ONLINE
        Plan(Models.ModelInfo m, String from, String fromName, String to, String engine) {
            this.model = m; this.from = from; this.fromName = fromName; this.to = to; this.engine = engine;
        }
        public String toName() { return Translation.targetName(to); }
    }

    public static Plan plan(Context c, Models.ModelInfo m) {
        Prefs p = new Prefs(c);
        if (m == null || !p.translateOn(m)) return null;
        String code = p.translateFrom(m);
        String from = Translation.isoCode(m, code);
        String to = p.translateTo(m);
        if (from != null && from.equals(to)) return null;
        String engine = Translation.resolve(p.translateEngine(m), m, from, to);
        return new Plan(m, from, Translation.spokenName(m, code), to, engine);
    }

    /** The language the speech model should translate into by itself, or null. */
    public static String modelTarget(Context c, Models.ModelInfo m) {
        Plan pl = plan(c, m);
        return pl != null && Translation.ENGINE_MODEL.equals(pl.engine) ? pl.to : null;
    }

    /** A line for the main screen and the keyboard: "Translating Urdu to English (offline)". */
    public static String describe(Plan pl) {
        if (pl == null) return "Translation: off";
        String how;
        switch (pl.engine) {
            case Translation.ENGINE_MODEL: how = "by " + pl.model.name; break;
            case Translation.ENGINE_ONLINE: how = "online"; break;
            default: how = "offline";
        }
        String from = pl.from == null || pl.from.isEmpty() ? pl.fromName : Translation.targetName(pl.from);
        return "Translation: " + from + " to " + pl.toName() + ", " + how;
    }

    /**
     * Translates finished text with the offline or online translator. The model's own translation already
     * happened while decoding, so then the text comes back as it is. On failure the original text comes back
     * and status says why.
     */
    public static String finish(Context c, Plan pl, String text, Status status) {
        if (pl == null || text == null || text.isEmpty() || Translation.ENGINE_MODEL.equals(pl.engine)) return text;
        Prefs prefs = new Prefs(c);
        try {
            String out = Translation.ENGINE_ONLINE.equals(pl.engine)
                    ? online(prefs.googleKey(), pl.from, pl.to, text)
                    : offline(c, prefs, pl.from, pl.to, text, status);
            if (out == null) return text;
            if (prefs.keepOriginal() && !out.equals(text)) out = out + "\n\nOriginal: " + text;
            return out;
        } catch (Exception e) {
            String why = e.getMessage() == null ? e.toString() : e.getMessage();
            status.say("Translation didn't work, so you got the original text. " + why);
            return text;
        }
    }

    // ---- Offline: Google ML Kit ----

    private static String offline(Context c, Prefs prefs, String from, String to, String text, Status status) throws Exception {
        String src = from;
        if (src == null) throw new Exception("The offline translator doesn't know the spoken language.");
        if (src.isEmpty()) {
            src = detect(text);
            if (src == null) throw new Exception("It couldn't tell which language was spoken. Pick the spoken language in Translation settings.");
        }
        String s = TranslateLanguage.fromLanguageTag(src);
        String t = TranslateLanguage.fromLanguageTag(to);
        if (s == null) throw new Exception("The offline translator doesn't know " + Translation.targetName(src) + ". Try the online translator.");
        if (t == null) throw new Exception("The offline translator doesn't know " + Translation.targetName(to) + ".");
        if (s.equals(t)) return text;
        if (!isDownloaded(s) || !isDownloaded(t)) status.say("Getting the " + Translation.targetName(src) + " and "
                + Translation.targetName(to) + " translation files, about 30 MB each, once.");
        Translator tr = com.google.mlkit.nl.translate.Translation.getClient(
                new TranslatorOptions.Builder().setSourceLanguage(s).setTargetLanguage(t).build());
        try {
            Tasks.await(tr.downloadModelIfNeeded(conditions(prefs)), 10, TimeUnit.MINUTES);
            return Translation.apply(text, part -> Tasks.await(tr.translate(part), 2, TimeUnit.MINUTES));
        } finally {
            tr.close();
        }
    }

    private static DownloadConditions conditions(Prefs prefs) {
        DownloadConditions.Builder b = new DownloadConditions.Builder();
        if (!prefs.translateMobileData()) b.requireWifi();
        return b.build();
    }

    /** The spoken language from the text itself, or null when it can't tell. */
    private static String detect(String text) throws Exception {
        LanguageIdentifier id = LanguageIdentification.getClient();
        try {
            String code = Tasks.await(id.identifyLanguage(text), 1, TimeUnit.MINUTES);
            if (code == null || code.equals("und")) return null;
            int dash = code.indexOf('-');
            return dash > 0 ? code.substring(0, dash) : code;
        } finally {
            id.close();
        }
    }

    public static boolean isDownloaded(String mlkitCode) {
        if (mlkitCode == null) return false;
        if (mlkitCode.equals(TranslateLanguage.ENGLISH)) return true; // English always ships with ML Kit
        try {
            return Tasks.await(RemoteModelManager.getInstance().isModelDownloaded(
                    new TranslateRemoteModel.Builder(mlkitCode).build()), 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    /** Downloads one offline language. Blocking. */
    public static void download(Context c, String iso) throws Exception {
        String code = TranslateLanguage.fromLanguageTag(iso);
        if (code == null) throw new Exception("The offline translator doesn't know " + Translation.targetName(iso) + ".");
        Tasks.await(RemoteModelManager.getInstance().download(new TranslateRemoteModel.Builder(code).build(),
                conditions(new Prefs(c))), 10, TimeUnit.MINUTES);
    }

    /** The offline languages on the phone, as ISO codes. Blocking. */
    public static List<String> downloaded() throws Exception {
        Set<TranslateRemoteModel> s = Tasks.await(RemoteModelManager.getInstance()
                .getDownloadedModels(TranslateRemoteModel.class), 30, TimeUnit.SECONDS);
        List<String> out = new ArrayList<>();
        for (TranslateRemoteModel m : s) out.add(m.getLanguage());
        return out;
    }

    public static void delete(String iso) throws Exception {
        Tasks.await(RemoteModelManager.getInstance().deleteDownloadedModel(
                new TranslateRemoteModel.Builder(iso).build()), 30, TimeUnit.SECONDS);
    }

    // ---- Online: Google Cloud Translation (v2, with the user's API key) ----

    static String online(String key, String from, String to, String text) throws Exception {
        if (key == null || key.isEmpty()) throw new Exception("The online translator needs your Google Cloud API key. Add it in Translation settings.");
        return Translation.apply(text, part -> onlineOne(key, from, to, part));
    }

    private static String onlineOne(String key, String from, String to, String part) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("q=").append(URLEncoder.encode(part, "UTF-8"));
        body.append("&target=").append(URLEncoder.encode(to, "UTF-8"));
        if (from != null && !from.isEmpty()) body.append("&source=").append(URLEncoder.encode(from, "UTF-8"));
        body.append("&format=text");
        HttpURLConnection h = (HttpURLConnection) new URL("https://translation.googleapis.com/language/translate/v2?key="
                + URLEncoder.encode(key, "UTF-8")).openConnection();
        try {
            h.setConnectTimeout(15000);
            h.setReadTimeout(30000);
            h.setRequestMethod("POST");
            h.setDoOutput(true);
            h.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            try (OutputStream o = h.getOutputStream()) { o.write(body.toString().getBytes("UTF-8")); }
            int code = h.getResponseCode();
            String resp = read(code >= 400 ? h.getErrorStream() : h.getInputStream());
            if (code >= 400) {
                String msg = "Google said: error " + code;
                try { msg = "Google said: " + new JSONObject(resp).getJSONObject("error").getString("message"); } catch (Exception ignored) {}
                throw new Exception(msg);
            }
            JSONArray arr = new JSONObject(resp).getJSONObject("data").getJSONArray("translations");
            return arr.getJSONObject(0).getString("translatedText");
        } finally {
            h.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) return "";
        try (InputStream i = in) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = i.read(buf)) > 0) b.write(buf, 0, n);
            return b.toString("UTF-8");
        }
    }
}
