package com.irondonkey.dictation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns spoken web addresses and emails into real ones:
 * "HTTPS colon slash slash www dot google dot com" becomes "https://www.google.com",
 * "john dot smith at gmail dot com" becomes "john.smith@gmail.com".
 * Only rewrites a run of words that really forms an address; everything else is left alone.
 * Pure Java, so the desktop test harness runs the same code.
 */
public final class LinkCleaner {
    private LinkCleaner() {}

    private static final Set<String> TLDS = new HashSet<>(Arrays.asList(
            "com", "net", "org", "edu", "gov", "mil", "int", "io", "co", "ai", "app", "dev", "me", "info",
            "biz", "tv", "us", "uk", "ca", "au", "in", "pk", "de", "fr", "es", "it", "nl", "ru", "ch", "se",
            "no", "jp", "cn", "br", "mx", "ae", "sa", "xyz", "online", "site", "store", "tech", "blog", "news",
            "ly", "gg", "to", "fm", "page", "link", "live", "cloud", "shop", "wiki", "email", "pro", "eu"));

    /** Words that are almost never the first part of a spoken address. */
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "the", "a", "an", "this", "that", "these", "those", "my", "our", "your", "his", "her", "their", "its",
            "of", "in", "on", "to", "is", "are", "was", "were", "be", "and", "or", "but", "for", "with", "from",
            "me", "us", "him", "them", "it", "you", "i", "we", "they", "he", "she", "look", "looking", "looked",
            "meet", "see", "stay", "arrive", "arrived", "here", "there", "where", "am", "email", "mail", "write",
            "reach", "contact", "call", "site", "website", "page", "go", "went", "visit", "open", "at", "dot"));

    private static final int WORD = 0, SYM = 1;

    /** Words followed by a comma or other pause, for the line being cleaned. */
    private static final ThreadLocal<boolean[]> breaks = new ThreadLocal<>();

    private static final class Item {
        final int type;
        final String text;   // a word in its original case, or one symbol character
        final int token;
        /** A period or colon the model wrote as punctuation ("www. My website"), not one that was spoken. */
        final boolean soft;
        Item(int type, String text, int token) { this(type, text, token, false); }
        Item(int type, String text, int token, boolean soft) { this.type = type; this.text = text; this.token = token; this.soft = soft; }
        String low() { return text.toLowerCase(Locale.ROOT); }
    }

    private static final class Tok {
        final String lead, core, trail;
        Tok(String lead, String core, String trail) { this.lead = lead; this.core = core; this.trail = trail; }
    }

    /** Cleans each paragraph of a transcript on its own. */
    public static String clean(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] paras = text.split("\n", -1);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < paras.length; i++) {
            if (i > 0) b.append('\n');
            b.append(cleanLine(paras[i]));
        }
        return b.toString();
    }

    /** "H T T P S colon" and "W W W dot", said letter by letter. Only right before the rest of an address. */
    private static final java.util.regex.Pattern SPELLED_SCHEME = java.util.regex.Pattern.compile(
            "(?i)\\b(h)[ .-]?(t)[ .-]?(t)[ .-]?(p)(?:[ .-]?(s))?\\b(?=[ ,]*(:|colon\\b|/|slash\\b|forward\\b))");
    private static final java.util.regex.Pattern SPELLED_WWW = java.util.regex.Pattern.compile(
            "(?i)\\b(?<![\\w])w[ .-]w[ .-]w\\b(?=[ ,]*(\\.|dot\\b))");
    private static final java.util.regex.Pattern GLUED_COLON = java.util.regex.Pattern.compile("(?i)\\b(https?)colon\\b");

    static String cleanLine(String line) {
        if (line.length() > 4) {
            line = SPELLED_SCHEME.matcher(line).replaceAll(m -> m.group(5) == null ? "http" : "https");
            line = SPELLED_WWW.matcher(line).replaceAll("www");
            line = GLUED_COLON.matcher(line).replaceAll("$1 colon");
        }
        String low = line.toLowerCase(Locale.ROOT);
        // Fast exit: nothing that could be an address.
        if (!(low.contains("dot") || low.contains(".") || low.contains("slash") || low.contains("@"))) return line;
        String[] words = line.trim().isEmpty() ? new String[0] : line.trim().split(" +");
        if (words.length == 0) return line;
        List<Tok> toks = new ArrayList<>();
        for (String w : words) toks.add(split(w));

        // Items: words and symbols, with spoken connectors turned into symbols.
        List<Item> items = new ArrayList<>();
        int[] firstItem = new int[toks.size()];
        int[] lastItem = new int[toks.size()];
        for (int t = 0; t < toks.size(); t++) {
            firstItem[t] = items.size();
            expand(toks.get(t).core, t, items);
            lastItem[t] = items.size() - 1;
            // Whisper writes "www. My website" and "https: //": keep that punctuation as a soft symbol,
            // which only counts inside an address that clearly started (https:// or www).
            String trail = toks.get(t).trail;
            if (t + 1 < toks.size() && !toks.get(t).core.isEmpty()) {
                if (trail.equals(".")) items.add(new Item(SYM, ".", t, true));
                else if (trail.equals(":")) items.add(new Item(SYM, ":", t, true));
            }
        }

        boolean[] pause = new boolean[toks.size()];
        for (int k = 0; k < toks.size(); k++) pause[k] = toks.get(k).trail.matches(".*[,;!?].*");
        breaks.set(pause);

        List<String> out = new ArrayList<>();
        int t = 0;
        while (t < toks.size()) {
            int[] end = new int[1];
            String link = toks.get(t).core.isEmpty() ? null : match(items, firstItem[t], end);
            if (link != null) {
                int lastTok = items.get(end[0]).token;
                // The match has to cover whole words.
                if (end[0] == lastItem[lastTok]) {
                    out.add(toks.get(t).lead + link + toks.get(lastTok).trail);
                    t = lastTok + 1;
                    continue;
                }
            }
            out.add(words[t]);
            t++;
        }
        String joined = String.join(" ", out);
        // Keep the original leading and trailing spaces.
        int a = 0;
        while (a < line.length() && line.charAt(a) == ' ') a++;
        int z = line.length();
        while (z > a && line.charAt(z - 1) == ' ') z--;
        return line.substring(0, a) + joined + line.substring(z);
    }

    private static Tok split(String w) {
        int a = 0, z = w.length();
        while (a < z && !isWordChar(w.charAt(a)) && w.charAt(a) != '@' && w.charAt(a) != '/') a++;
        while (z > a && !isWordChar(w.charAt(z - 1)) && w.charAt(z - 1) != '/') z--;
        return new Tok(w.substring(0, a), w.substring(a, z), w.substring(z));
    }

    private static boolean isWordChar(char c) { return Character.isLetterOrDigit(c); }

    private static void expand(String core, int token, List<Item> items) {
        if (core.isEmpty()) return;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < core.length(); i++) {
            char c = core.charAt(i);
            if (isWordChar(c)) {
                cur.append(c);
            } else {
                if (cur.length() > 0) { addWord(cur.toString(), token, items); cur.setLength(0); }
                if ("./:@-_".indexOf(c) >= 0) items.add(new Item(SYM, String.valueOf(c), token));
                else items.add(new Item(SYM, "?", token)); // anything else breaks an address
            }
        }
        if (cur.length() > 0) addWord(cur.toString(), token, items);
    }

    private static void addWord(String w, int token, List<Item> items) {
        switch (w.toLowerCase(Locale.ROOT)) {
            case "dot": items.add(new Item(SYM, ".", token)); break;
            case "slash": items.add(new Item(SYM, "/", token)); break;
            case "colon": items.add(new Item(SYM, ":", token)); break;
            case "dash": case "hyphen": items.add(new Item(SYM, "-", token)); break;
            case "underscore": items.add(new Item(SYM, "_", token)); break;
            default: items.add(new Item(WORD, w, token));
        }
    }

    private static boolean isSym(List<Item> it, int p, char c) {
        return p < it.size() && it.get(p).type == SYM && !it.get(p).soft && it.get(p).text.charAt(0) == c;
    }

    /** A spoken symbol, or one the model wrote as punctuation. */
    /**
     * Models capitalize words at random ("slash About"). A word in Title case becomes lower case in a path;
     * anything else (README, iPhone) stays as it was said.
     */
    private static String pathWord(String w) {
        if (w.length() > 1 && Character.isUpperCase(w.charAt(0)) && w.substring(1).equals(w.substring(1).toLowerCase(java.util.Locale.ROOT)))
            return w.toLowerCase(java.util.Locale.ROOT);
        return w;
    }

    /** Words that end a spoken address: never glued into a web address label. */
    private static final java.util.Set<String> JOIN_STOP = new java.util.HashSet<>(java.util.Arrays.asList(
            "and", "or", "but", "then", "to", "at", "for", "from", "in", "on", "is", "was", "the", "a", "an", "it", "if", "so"));

    private static boolean isAnySym(List<Item> it, int p, char c) {
        return p < it.size() && it.get(p).type == SYM && it.get(p).text.charAt(0) == c;
    }

    private static boolean isWord(List<Item> it, int p) {
        return p < it.size() && it.get(p).type == WORD;
    }

    /** "slash", or "forward slash": returns the length, or 0. */
    private static int slash(List<Item> it, int p) {
        if (isSym(it, p, '/')) return 1;
        if (isWord(it, p) && it.get(p).low().equals("forward") && isSym(it, p + 1, '/')) return 2;
        return 0;
    }

    /** Tries an address starting at item p. Returns it and puts its last item in end[0], or null. */
    static String match(List<Item> it, int p, int[] end) {
        String s = matchUrl(it, p, end);
        if (s != null) return s;
        return matchEmail(it, p, end);
    }

    private static String matchUrl(List<Item> it, int p, int[] end) {
        int q = p;
        String scheme = "";
        if (isWord(it, q) && (it.get(q).low().equals("http") || it.get(q).low().equals("https"))) {
            int r = q + 1;
            if (isAnySym(it, r, ':')) r++;
            int s1 = slash(it, r);
            int s2 = s1 > 0 ? slash(it, r + s1) : 0;
            if (s1 > 0 && s2 > 0) {
                scheme = it.get(q).low() + "://";
                q = r + s1 + s2;
                // "slash/slash": a spoken slash and a written one for the same thing.
                int s3 = slash(it, q);
                if (s3 > 0 && isWord(it, q + s3) && it.get(q + s3).low().equals("www")) q += s3;
            } else if ((r > q + 1 || s1 > 0) && isWord(it, r + s1) && it.get(r + s1).low().equals("www")) {
                // "https: www.site.com", "https: slash www..." or "https/www...": the model dropped a slash or two.
                scheme = it.get(q).low() + "://";
                q = r + s1;
            }
        }
        List<String> labels = new ArrayList<>();
        boolean anchored = !scheme.isEmpty() || (isWord(it, q) && it.get(q).low().equals("www"));
        int h = host(it, q, labels, anchored);
        if (h < 0) return null;
        boolean ok;
        boolean soft = false;
        for (int j = q; j <= h; j++) if (it.get(j).soft) soft = true;
        // A dot the model wrote by itself only counts when the address ends like one (.com, .org...).
        if (soft) ok = TLDS.contains(labels.get(labels.size() - 1));
        else if (!scheme.isEmpty() || labels.get(0).equals("www")) ok = true;
        else ok = TLDS.contains(labels.get(labels.size() - 1)) && !STOP.contains(labels.get(0));
        if (!ok) return null;
        StringBuilder b = new StringBuilder(scheme).append(String.join(".", labels));
        int last = h;
        // Path: "slash maps slash place", parts may hold dots, dashes and underscores.
        int r = h + 1;
        while (true) {
            int sl = slash(it, r);
            if (sl == 0) break;
            b.append('/');
            last = r + sl - 1;
            r += sl;
            if (isWord(it, r)) {
                b.append(pathWord(it.get(r).text));
                last = r;
                r++;
                while ((isSym(it, r, '.') || isSym(it, r, '-') || isSym(it, r, '_')) && isWord(it, r + 1)) {
                    b.append(it.get(r).text).append(pathWord(it.get(r + 1).text));
                    last = r + 1;
                    r += 2;
                }
            }
        }
        end[0] = last;
        return b.toString();
    }

    /**
     * label (dot label)+ ; each label may hold dashes. Returns the last item used, or -1.
     * In an address that clearly started (https:// or www), a label said as several words is joined
     * ("www dot my website dot com" is www.mywebsite.com), and the model's own period after "www" counts as a dot.
     */
    private static int host(List<Item> it, int q, List<String> labels, boolean anchored) {
        int r = q;
        int last = -1;
        while (true) {
            if (!isWord(it, r)) break;
            StringBuilder lab = new StringBuilder(it.get(r).low());
            int e = r;
            while (isSym(it, e + 1, '-') && isWord(it, e + 2)) {
                lab.append('-').append(it.get(e + 2).low());
                e += 2;
            }
            if (anchored && !dotOk(it, e + 1, lab.toString(), true)
                    && !(labels.size() >= 1 && TLDS.contains(lab.toString()))) {
                // Up to three more words, when a dot and another label follow them.
                int k = e;
                for (int n = 0; n < 3 && isWord(it, k + 1) && !JOIN_STOP.contains(it.get(k + 1).low()); n++) {
                    k++;
                    if (dotOk(it, k + 1, "", true) && isWord(it, k + 2)) {
                        for (int j = e + 1; j <= k; j++) lab.append(it.get(j).low());
                        e = k;
                        break;
                    }
                }
            }
            labels.add(lab.toString());
            last = e;
            // "www.mywebsite.com. It has": once the address ends in .com, the model's own period ends the sentence.
            boolean complete = labels.size() >= 2 && TLDS.contains(lab.toString());
            boolean dot = isSym(it, e + 1, '.') || (!complete && dotOk(it, e + 1, lab.toString(), anchored));
            if (dot && isWord(it, e + 2)) r = e + 2;
            else break;
        }
        if (labels.size() < 2) return -1;
        return last;
    }

    /** A dot after a label: a spoken one, or the model's period right after "www" or before a domain ending. */
    private static boolean dotOk(List<Item> it, int p, String label, boolean anchored) {
        if (isSym(it, p, '.')) return true;
        if (!anchored || !isAnySym(it, p, '.') || !isWord(it, p + 1)) return false;
        return label.equals("www") || TLDS.contains(it.get(p + 1).low());
    }

    private static String matchEmail(List<Item> it, int p, int[] end) {
        if (!isWord(it, p) || STOP.contains(it.get(p).low())) return null;
        StringBuilder local = new StringBuilder(it.get(p).low());
        int r = p + 1;
        while ((isSym(it, r, '.') || isSym(it, r, '_') || isSym(it, r, '-')) && isWord(it, r + 1)
                && !it.get(r + 1).low().equals("at")) {
            local.append(it.get(r).text).append(it.get(r + 1).low());
            r += 2;
        }
        boolean at = isSym(it, r, '@') || (isWord(it, r) && it.get(r).low().equals("at"));
        if (!at) return null;
        // "To be honest, at home dot me" is a sentence, not an email.
        boolean[] pause = breaks.get();
        if (isWord(it, r) && pause != null && pause[it.get(r - 1).token]) return null;
        List<String> labels = new ArrayList<>();
        int h = host(it, r + 1, labels, false);
        if (h < 0) return null;
        String tld = labels.get(labels.size() - 1);
        if (!TLDS.contains(tld) && !(tld.length() >= 2 && tld.length() <= 6 && tld.chars().allMatch(Character::isLetter))) return null;
        end[0] = h;
        return local + "@" + String.join(".", labels);
    }
}
