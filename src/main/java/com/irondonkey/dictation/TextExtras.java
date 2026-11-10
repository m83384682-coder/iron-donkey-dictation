package com.irondonkey.dictation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional text clean-ups: spoken commands ("new line", "comma") and the user's own word replacements.
 * Pure Java, so the desktop test harness runs the same code.
 */
public final class TextExtras {
    private TextExtras() {}

    private static final char MARK = '\u0001';

    /** Spoken word(s) and what they turn into. Longer phrases first. */
    private static final String[][] COMMANDS = {
            {"new paragraphs", "\n\n"}, {"new paragraph", "\n\n"}, {"new lines", "\n"}, {"new line", "\n"}, {"newline", "\n"}, {"next line", "\n"},
            {"question mark", "?"}, {"exclamation mark", "!"}, {"exclamation point", "!"},
            {"full stop", "."}, {"period", "."}, {"comma", ","}, {"semicolon", ";"}, {"semi colon", ";"},
            {"colon", ":"},
    };
    private static final Pattern[] COMMAND_PATTERNS = new Pattern[COMMANDS.length];
    static {
        for (int i = 0; i < COMMANDS.length; i++) {
            String words = COMMANDS[i][0].replace(" ", "[ -]");
            // The model often wraps a spoken command in its own punctuation: "Hello, comma, world."
            // Line breaks keep the sentence's own period before them.
            String before = COMMANDS[i][1].startsWith("\n") ? "[ ]*" : "[ ]*[,.;:!?]?[ ]*";
            COMMAND_PATTERNS[i] = Pattern.compile("(?i)" + before + "\\b" + words + "\\b[,.;:!?]*");
        }
    }

    /**
     * Turns spoken commands into the real thing: "new line", "new paragraph", "comma", "period", "full stop",
     * "question mark", "exclamation mark", "colon", "semicolon". Capitalizes the word after a new sentence.
     */
    public static String applyCommands(String text) {
        if (text == null || text.isEmpty()) return text;
        String s = text;
        for (int i = 0; i < COMMANDS.length; i++) {
            s = COMMAND_PATTERNS[i].matcher(s).replaceAll(Matcher.quoteReplacement(MARK + COMMANDS[i][1] + MARK));
        }
        if (s.indexOf(MARK) < 0) return text;
        StringBuilder b = new StringBuilder();
        int i = 0;
        boolean capNext = false;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == MARK) {
                int j = s.indexOf(MARK, i + 1);
                String sym = s.substring(i + 1, j);
                i = j + 1;
                // Join with the text before: no space before punctuation or a line break.
                while (b.length() > 0 && b.charAt(b.length() - 1) == ' ') b.setLength(b.length() - 1);
                if (sym.startsWith("\n")) {
                    // Several line breaks in a row: keep the longest, not the sum.
                    int nl = 0;
                    while (nl < b.length() && b.charAt(b.length() - 1 - nl) == '\n') nl++;
                    if (sym.length() > nl) b.append(sym.substring(nl));
                    capNext = true;
                } else {
                    // "Hello. Comma" -> "Hello," : a spoken mark replaces the model's own one.
                    if (b.length() > 0 && ",.;:!?".indexOf(b.charAt(b.length() - 1)) >= 0) b.setLength(b.length() - 1);
                    b.append(sym);
                    capNext = ".!?".contains(sym);
                }
                // Skip spaces after; add one back before the next word.
                while (i < s.length() && s.charAt(i) == ' ') i++;
                if (i < s.length() && s.charAt(i) != MARK && s.charAt(i) != '\n' && !sym.startsWith("\n")) b.append(' ');
                continue;
            }
            if (capNext && Character.isLetter(c)) {
                b.append(Character.toUpperCase(c));
                capNext = false;
            } else {
                if (!Character.isWhitespace(c)) capNext = false;
                b.append(c);
            }
            i++;
        }
        // No leading or trailing spaces at line ends.
        return b.toString().replaceAll(" +\n", "\n").replaceAll("\n +", "\n");
    }

    /** One rule: heard words and what to write instead. */
    public static final class Rule {
        public final String heard, written;
        final Pattern pattern;
        Rule(String heard, String written) {
            this.heard = heard;
            this.written = written;
            String q = Pattern.quote(heard);
            String pre = Character.isLetterOrDigit(heard.charAt(0)) ? "(?<![\\p{L}\\p{N}])" : "";
            String post = Character.isLetterOrDigit(heard.charAt(heard.length() - 1)) ? "(?![\\p{L}\\p{N}])" : "";
            pattern = Pattern.compile(pre + q + post, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    /**
     * Reads the replacement list: one rule per line, "heard = written" ("->" works too).
     * Blank lines and lines starting with # are skipped. Longer heard phrases are applied first.
     */
    public static List<Rule> parseReplacements(String list) {
        List<Rule> out = new ArrayList<>();
        if (list == null) return out;
        for (String line : list.split("\n")) {
            String l = line.trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            int at = l.indexOf("->");
            int len = 2;
            if (at < 0) { at = l.indexOf('\u2192'); len = 1; }
            if (at < 0) { at = l.indexOf('='); len = 1; }
            if (at <= 0) continue;
            String heard = l.substring(0, at).trim();
            String written = l.substring(at + len).trim();
            if (heard.isEmpty()) continue;
            out.add(new Rule(heard, written));
        }
        out.sort((a, b) -> b.heard.length() - a.heard.length());
        return out;
    }

    /** Applies the user's replacements, whole words only, ignoring case. */
    public static String applyReplacements(String text, List<Rule> rules) {
        if (text == null || text.isEmpty() || rules == null || rules.isEmpty()) return text;
        // Replace each match with a placeholder first, so one rule never rewrites another rule's output.
        List<String> outs = new ArrayList<>();
        String s = text;
        for (Rule r : rules) {
            Matcher m = r.pattern.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                outs.add(r.written);
                m.appendReplacement(sb, Matcher.quoteReplacement("\u0002" + (outs.size() - 1) + "\u0003"));
            }
            m.appendTail(sb);
            s = sb.toString();
        }
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\u0002') {
                int j = s.indexOf('\u0003', i);
                b.append(outs.get(Integer.parseInt(s.substring(i + 1, j))));
                i = j + 1;
            } else {
                b.append(c);
                i++;
            }
        }
        // An empty replacement can leave two spaces.
        return b.toString().replaceAll("  +", " ");
    }

    /** Words and characters, for the counter under the text box. */
    public static int words(String text) {
        if (text == null) return 0;
        String t = text.trim();
        if (t.isEmpty()) return 0;
        return t.split("\\s+").length;
    }

    public static String lower(String s) { return s.toLowerCase(Locale.ROOT); }
}
