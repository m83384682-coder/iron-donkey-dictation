package com.irondonkey.dictation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Builds a problem report: the text people can copy, and a link that opens a new GitHub issue with it filled in.
 * Nothing is sent by the app; the person reads it and presses Submit on GitHub themselves.
 * Pure Java, so the desktop test harness checks the same code.
 */
public final class ReportText {
    private ReportText() {}

    public static final String REPO = "https://github.com/m83384682-coder/iron-donkey-dictation";
    public static final String ISSUES = REPO + "/issues";

    /** GitHub refuses very long links, so the body in the link is kept under this many characters. */
    static final int MAX_URL_BODY = 5000;

    public static String body(String appVersion, String device, String android, String model, String translation,
                              List<String> errors) {
        StringBuilder b = new StringBuilder();
        b.append("**What happened?**\n\n(Describe what you did and what went wrong.)\n\n");
        b.append("**Phone and app**\n");
        b.append("- App: Iron Donkey Dictation ").append(appVersion).append('\n');
        b.append("- Phone: ").append(device).append('\n');
        b.append("- Android: ").append(android).append('\n');
        b.append("- Speech model: ").append(model == null || model.isEmpty() ? "none chosen" : model).append('\n');
        if (translation != null && !translation.isEmpty()) b.append("- ").append(translation).append('\n');
        b.append("\n**Recent errors in the app**\n\n");
        if (errors == null || errors.isEmpty()) {
            b.append("None recorded.\n");
        } else {
            b.append("```\n");
            for (String e : errors) b.append(e.replace("```", "'''")).append('\n');
            b.append("```\n");
        }
        return b.toString();
    }

    /** A link to GitHub's new-issue page with the title and body filled in. */
    public static String issueUrl(String title, String body) {
        String bd = body;
        if (bd.length() > MAX_URL_BODY) {
            bd = bd.substring(0, MAX_URL_BODY);
            if (bd.contains("```") && count(bd, "```") % 2 == 1) bd += "\n```";
            bd += "\n(Report shortened. Use Copy report in the app for the full text.)";
        }
        return ISSUES + "/new?title=" + enc(title) + "&body=" + enc(bd);
    }

    private static int count(String s, String sub) {
        int n = 0, i = 0;
        while ((i = s.indexOf(sub, i)) >= 0) { n++; i += sub.length(); }
        return n;
    }

    static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
