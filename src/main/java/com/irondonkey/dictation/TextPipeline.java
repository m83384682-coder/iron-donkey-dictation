package com.irondonkey.dictation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns raw model output into the text the user sees. Pure Java, so the same code
 * runs in the app and in the desktop test harness.
 */
public final class TextPipeline {
    private TextPipeline() {}

    public static final String NO_SPEECH = "No speech heard";
    public static final int SAMPLE_RATE = 16000;

    private static final Pattern SPECIAL_TOKENS = Pattern.compile("<\\|[^|>]*\\|>|</?asr_text>");
    private static final Pattern NON_SPEECH_TAGS = Pattern.compile(
            "\\[[^\\]]{0,40}\\]|\\((?i:music|applause|laughter|laughs|silence|inaudible|blank audio|blank_audio|noise|background noise|static)\\)|[\u266a\u266b\u266c]+");
    private static final Pattern SPACES = Pattern.compile("[ \\t\\x0B\\f\\r\\n]+");
    private static final Pattern SPACE_BEFORE_PUNCT = Pattern.compile(" +([,.!?;:\u060c\u061f\u06d4])");
    private static final Pattern CHAR_LOOP = Pattern.compile("(\\S{1,20}?)\\1{5,}");
    private static final Pattern LEADING_STOP = Pattern.compile("^(?i:stop)\\s*[.!\u3002]+\\s*");
    private static final Pattern ONLY_STOP = Pattern.compile("^(?i:stop)[\\s.!\u3002]*$");

    // Phrases Whisper is known to invent on silence or noise. Only dropped when they are the whole segment.
    private static final Set<String> HALLUCINATIONS = new HashSet<>(Arrays.asList(
            "thanks for watching", "thank you for watching", "thanks for watching and see you next time",
            "please subscribe", "please subscribe to my channel", "like and subscribe",
            "subtitles by the amaraorg community", "subtitles by amaraorg", "transcription by castingwords",
            "see you in the next video", "you"));

    /** Cleans one segment's text: special tokens, non-speech tags, loops, stray spaces. */
    public static String cleanSegment(String raw) {
        if (raw == null) return "";
        String s = SPECIAL_TOKENS.matcher(raw).replaceAll(" ");
        s = NON_SPEECH_TAGS.matcher(s).replaceAll(" ");
        s = SPACES.matcher(s).replaceAll(" ").trim();
        if (s.isEmpty()) return "";
        if (HALLUCINATIONS.contains(normalize(s))) return "";
        s = collapseLoops(s);
        s = SPACE_BEFORE_PUNCT.matcher(s).replaceAll("$1");
        // Only punctuation left (".", "...") means nothing was said.
        if (normalize(s).isEmpty()) return "";
        return s.trim();
    }

    /** Lowercase, letters and digits only, single spaces. */
    static String normalize(String s) {
        StringBuilder b = new StringBuilder(s.length());
        boolean space = false;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp) || Character.getType(cp) == Character.NON_SPACING_MARK
                    || Character.getType(cp) == Character.COMBINING_SPACING_MARK) {
                if (space && b.length() > 0) b.append(' ');
                space = false;
                b.appendCodePoint(Character.toLowerCase(cp));
            } else if (Character.isWhitespace(cp)) {
                space = true;
            }
        }
        return b.toString();
    }

    /**
     * Cuts repetition loops: a single word said 4+ times in a row, or a phrase of 2 to 12 words
     * repeated 3+ times in a row, is kept once. Runs of the same characters inside a word
     * (for scripts without spaces) are cut the same way.
     */
    public static String collapseLoops(String s) {
        String[] words = s.split(" ");
        List<String> w = new ArrayList<>(Arrays.asList(words));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int n = 1; n <= 12 && !changed; n++) {
                int need = n == 1 ? 4 : 3;
                for (int i = 0; i + n * need <= w.size() && !changed; i++) {
                    int reps = 1;
                    while (i + (reps + 1) * n <= w.size() && sameRun(w, i, i + reps * n, n)) reps++;
                    if (reps >= need) {
                        // Keep the last copy so its trailing punctuation survives.
                        List<String> keep = new ArrayList<>(w.subList(i + (reps - 1) * n, i + reps * n));
                        w.subList(i, i + reps * n).clear();
                        w.addAll(i, keep);
                        changed = true;
                    }
                }
            }
        }
        StringBuilder b = new StringBuilder();
        for (String x : w) {
            if (x.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(CHAR_LOOP.matcher(x).replaceAll("$1"));
        }
        return b.toString();
    }

    private static boolean sameRun(List<String> w, int a, int b, int n) {
        for (int k = 0; k < n; k++) {
            String x = normalize(w.get(a + k)), y = normalize(w.get(b + k));
            if (x.isEmpty() && y.isEmpty()) {
                if (!w.get(a + k).equals(w.get(b + k))) return false;
            } else if (!x.equals(y)) return false;
        }
        return true;
    }

    /** Builds one transcript from segments in time order. */
    public static final class Assembler {
        private final boolean paragraphs;
        private final boolean stripLeadingStop;
        private final float paragraphGapSec;
        private final StringBuilder out = new StringBuilder();
        private long lastEnd = -1;
        private boolean first = true;
        private boolean links = false;

        /** Turn spoken web addresses and emails into real ones in text(). */
        public Assembler withLinks(boolean on) { links = on; return this; }

        private boolean commands = false;
        private java.util.List<TextExtras.Rule> rules = null;

        /** Turn "new line", "comma" and other spoken commands into the real thing in text(). */
        public Assembler withCommands(boolean on) { commands = on; return this; }

        /** Apply the user's word replacements in text(). */
        public Assembler withReplacements(java.util.List<TextExtras.Rule> r) { rules = r; return this; }

        public Assembler(boolean paragraphs, boolean stripLeadingStop) {
            this(paragraphs, stripLeadingStop, 2.0f);
        }

        public Assembler(boolean paragraphs, boolean stripLeadingStop, float paragraphGapSec) {
            this.paragraphs = paragraphs;
            this.stripLeadingStop = stripLeadingStop;
            this.paragraphGapSec = paragraphGapSec;
        }

        /** startSample and endSample are positions at 16 kHz from the start of the audio. */
        public synchronized void add(String cleaned, long startSample, long endSample) {
            String t = cleaned == null ? "" : cleaned.trim();
            if (first && stripLeadingStop && !t.isEmpty()) {
                if (ONLY_STOP.matcher(t).matches()) t = "";
                else t = LEADING_STOP.matcher(t).replaceFirst("");
                if (!t.isEmpty()) t = capitalizeFirst(t);
            }
            if (t.isEmpty()) return;
            first = false;
            if (out.length() > 0) {
                boolean para = paragraphs && lastEnd >= 0
                        && (startSample - lastEnd) > (long) (paragraphGapSec * SAMPLE_RATE);
                out.append(para ? "\n\n" : " ");
            }
            out.append(t);
            lastEnd = endSample;
        }

        public synchronized boolean isEmpty() { return out.length() == 0; }

        public synchronized String text() {
            String t = out.toString();
            if (links) t = LinkCleaner.clean(t);
            if (commands) t = TextExtras.applyCommands(t);
            if (rules != null && !rules.isEmpty()) t = TextExtras.applyReplacements(t, rules);
            return t;
        }
    }

    static String capitalizeFirst(String t) {
        int cp = t.codePointAt(0);
        if (Character.isLowerCase(cp)) {
            return new String(Character.toChars(Character.toUpperCase(cp))) + t.substring(Character.charCount(cp));
        }
        return t;
    }

    /** Adds a new transcript to the existing box text, either appending or replacing. */
    public static String combine(String existing, String added, boolean replace) {
        if (replace || existing == null || existing.trim().isEmpty()) return added;
        String e = existing;
        while (e.endsWith(" ")) e = e.substring(0, e.length() - 1);
        if (e.endsWith("\n")) return e + added;
        return e + " " + added;
    }

    public static String lower(String s) { return s.toLowerCase(Locale.ROOT); }
}
