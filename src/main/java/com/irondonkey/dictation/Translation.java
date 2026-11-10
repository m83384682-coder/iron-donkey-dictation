package com.irondonkey.dictation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The parts of translation that do not need Android: which languages each model can hear,
 * which translator to use, and how to split text so web addresses and emails are never translated.
 * Pure Java, so the desktop test harness runs the same code.
 */
public final class Translation {
    private Translation() {}

    /** Which translator turns the text into the target language. */
    public static final String ENGINE_AUTO = "auto", ENGINE_MODEL = "model", ENGINE_OFFLINE = "offline", ENGINE_ONLINE = "online";

    /** Languages the offline translator (Google ML Kit) knows. The online one knows these too. */
    public static final String[][] TARGETS = {
            {"af", "Afrikaans"}, {"sq", "Albanian"}, {"ar", "Arabic"}, {"be", "Belarusian"}, {"bn", "Bengali"},
            {"bg", "Bulgarian"}, {"ca", "Catalan"}, {"zh", "Chinese"}, {"hr", "Croatian"}, {"cs", "Czech"},
            {"da", "Danish"}, {"nl", "Dutch"}, {"en", "English"}, {"eo", "Esperanto"}, {"et", "Estonian"},
            {"fi", "Finnish"}, {"fr", "French"}, {"gl", "Galician"}, {"ka", "Georgian"}, {"de", "German"},
            {"el", "Greek"}, {"gu", "Gujarati"}, {"ht", "Haitian Creole"}, {"he", "Hebrew"}, {"hi", "Hindi"},
            {"hu", "Hungarian"}, {"is", "Icelandic"}, {"id", "Indonesian"}, {"ga", "Irish"}, {"it", "Italian"},
            {"ja", "Japanese"}, {"kn", "Kannada"}, {"ko", "Korean"}, {"lv", "Latvian"}, {"lt", "Lithuanian"},
            {"mk", "Macedonian"}, {"ms", "Malay"}, {"mt", "Maltese"}, {"mr", "Marathi"}, {"no", "Norwegian"},
            {"fa", "Persian"}, {"pl", "Polish"}, {"pt", "Portuguese"}, {"ro", "Romanian"}, {"ru", "Russian"},
            {"sk", "Slovak"}, {"sl", "Slovenian"}, {"es", "Spanish"}, {"sw", "Swahili"}, {"sv", "Swedish"},
            {"tl", "Tagalog"}, {"ta", "Tamil"}, {"te", "Telugu"}, {"th", "Thai"}, {"tr", "Turkish"},
            {"uk", "Ukrainian"}, {"ur", "Urdu"}, {"vi", "Vietnamese"}, {"cy", "Welsh"},
    };

    /** The 25 European languages Parakeet Ultra hears. It finds the language itself, so there is no model setting. */
    private static final String[][] PARAKEET_ULTRA = {
            {"", "Auto (detect)"}, {"bg", "Bulgarian"}, {"hr", "Croatian"}, {"cs", "Czech"}, {"da", "Danish"},
            {"nl", "Dutch"}, {"en", "English"}, {"et", "Estonian"}, {"fi", "Finnish"}, {"fr", "French"},
            {"de", "German"}, {"el", "Greek"}, {"hu", "Hungarian"}, {"it", "Italian"}, {"lv", "Latvian"},
            {"lt", "Lithuanian"}, {"mt", "Maltese"}, {"pl", "Polish"}, {"pt", "Portuguese"}, {"ro", "Romanian"},
            {"ru", "Russian"}, {"sk", "Slovak"}, {"sl", "Slovenian"}, {"es", "Spanish"}, {"sv", "Swedish"},
            {"uk", "Ukrainian"},
    };

    private static final Map<String, String> QWEN = new HashMap<>();
    static {
        String[][] q = {{"Arabic", "ar"}, {"Cantonese", "zh"}, {"Chinese", "zh"}, {"Czech", "cs"}, {"Danish", "da"},
                {"Dutch", "nl"}, {"English", "en"}, {"Filipino", "tl"}, {"Finnish", "fi"}, {"French", "fr"},
                {"German", "de"}, {"Greek", "el"}, {"Hindi", "hi"}, {"Hungarian", "hu"}, {"Indonesian", "id"},
                {"Italian", "it"}, {"Japanese", "ja"}, {"Korean", "ko"}, {"Macedonian", "mk"}, {"Malay", "ms"},
                {"Persian", "fa"}, {"Polish", "pl"}, {"Portuguese", "pt"}, {"Romanian", "ro"}, {"Russian", "ru"},
                {"Spanish", "es"}, {"Swedish", "sv"}, {"Thai", "th"}, {"Turkish", "tr"}, {"Vietnamese", "vi"}};
        for (String[] p : q) QWEN.put(p[0], p[1]);
    }

    private static final List<String> CANARY = Arrays.asList("en", "es", "de", "fr");

    /** Models that hear more than one language. Translation is hidden for the rest. */
    public static boolean multilingual(Models.ModelInfo m) {
        return m != null && (m.languageChoices != null || m.id.equals("parakeet-ultra"));
    }

    /** True when the spoken language is the model's own language setting (shared with the Models screen). */
    public static boolean sharesModelLanguage(Models.ModelInfo m) {
        return m != null && m.languageChoices != null;
    }

    /** The languages the user can say they speak, as {code, name}; the first is the default. */
    public static String[][] spokenChoices(Models.ModelInfo m) {
        if (m == null) return new String[0][];
        if (m.languageChoices != null) return m.languageChoices;
        if (m.id.equals("parakeet-ultra")) return PARAKEET_ULTRA;
        return new String[0][];
    }

    public static String spokenName(Models.ModelInfo m, String code) {
        for (String[] c : spokenChoices(m)) if (c[0].equals(code)) return c[1].replace(" (experimental)", "");
        return code.isEmpty() ? "Auto (detect)" : code;
    }

    /** The ISO 639-1 code for a model's language setting, "" for automatic, null when no translator knows it. */
    public static String isoCode(Models.ModelInfo m, String lang) {
        if (lang == null || lang.isEmpty()) return "";
        if (m.type == Models.Type.QWEN3) return QWEN.get(lang);
        if (lang.equals("yue")) return "zh";
        if (lang.equals("nn")) return "no";
        return lang;
    }

    public static String targetName(String code) {
        for (String[] t : TARGETS) if (t[0].equals(code)) return t[1];
        return code;
    }

    public static boolean isTarget(String code) {
        for (String[] t : TARGETS) if (t[0].equals(code)) return true;
        return false;
    }

    /**
     * Can the speech model translate by itself while it listens? Whisper Small can translate any language it
     * hears into English. Canary translates between English and Spanish, German or French.
     * Whisper Turbo was not trained to translate, so it cannot.
     */
    public static boolean modelCanTranslate(Models.ModelInfo m, String from, String to) {
        if (m == null || to == null || to.isEmpty()) return false;
        if (m.id.equals("whisper-small")) return to.equals("en") && !"en".equals(from);
        if (m.type == Models.Type.CANARY) {
            String f = from == null || from.isEmpty() ? "en" : from;
            return CANARY.contains(f) && CANARY.contains(to) && !f.equals(to) && (f.equals("en") || to.equals("en"));
        }
        return false;
    }

    /** Whether the model has a built-in translator at all, for showing that choice. */
    public static boolean modelHasTranslator(Models.ModelInfo m) {
        return m != null && (m.id.equals("whisper-small") || m.type == Models.Type.CANARY);
    }

    /** Which translator runs, given the user's choice. ENGINE_AUTO picks the model's own when it can. */
    public static String resolve(String choice, Models.ModelInfo m, String from, String to) {
        boolean model = modelCanTranslate(m, from, to);
        if (ENGINE_ONLINE.equals(choice)) return ENGINE_ONLINE;
        if (ENGINE_OFFLINE.equals(choice)) return ENGINE_OFFLINE;
        return model ? ENGINE_MODEL : ENGINE_OFFLINE;
    }

    /** A part of the text: translated, or kept exactly as it is (links, emails, line breaks). */
    public static final class Part {
        public final String text;
        public final boolean translate;
        Part(String text, boolean translate) { this.text = text; this.translate = translate; }
    }

    private static final Pattern KEEP = Pattern.compile(
            "(?i)(https?://\\S+|www\\.\\S+|[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+|\\b[\\w-]+(\\.[\\w-]+)*\\.(com|net|org|edu|gov|io|co|ai|app|dev|info|pk|uk|de|fr|es|in)(/\\S*)?\\b|\\n+)");

    /**
     * Splits text into parts, so links, emails and line breaks pass through untouched and only the words
     * around them go to the translator. A trailing period after a link stays with the link's sentence.
     */
    public static List<Part> split(String text) {
        List<Part> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        Matcher mt = KEEP.matcher(text);
        int pos = 0;
        while (mt.find()) {
            String keep = mt.group();
            int end = mt.end();
            // "www.google.com." -> keep the link, translate nothing for the period.
            while (keep.length() > 1 && !keep.startsWith("\n") && ".,;:!?)".indexOf(keep.charAt(keep.length() - 1)) >= 0) {
                keep = keep.substring(0, keep.length() - 1);
                end--;
            }
            if (mt.start() > pos) addText(out, text.substring(pos, mt.start()));
            out.add(new Part(keep, false));
            pos = end;
        }
        if (pos < text.length()) addText(out, text.substring(pos));
        return out;
    }

    /** Words go to the translator; the spaces around them and lone punctuation are kept. */
    private static void addText(List<Part> out, String s) {
        int a = 0, z = s.length();
        while (a < z && Character.isWhitespace(s.charAt(a))) a++;
        while (z > a && Character.isWhitespace(s.charAt(z - 1))) z--;
        if (a > 0) out.add(new Part(s.substring(0, a), false));
        if (z > a) {
            String mid = s.substring(a, z);
            boolean words = false;
            for (int i = 0; i < mid.length(); i++) if (Character.isLetter(mid.charAt(i))) { words = true; break; }
            out.add(new Part(mid, words));
        }
        if (z < s.length() && z >= a) out.add(new Part(s.substring(Math.max(z, a)), false));
    }

    public interface Translator {
        String translate(String text) throws Exception;
    }

    /** Translates the text part by part and joins it back, links and line breaks untouched. */
    public static String apply(String text, Translator t) throws Exception {
        StringBuilder b = new StringBuilder();
        for (Part p : split(text)) {
            if (!p.translate) { b.append(p.text); continue; }
            String r = t.translate(p.text);
            b.append(r == null ? p.text : r.trim());
        }
        return b.toString();
    }

    public static String lower(String s) { return s.toLowerCase(Locale.ROOT); }
}
