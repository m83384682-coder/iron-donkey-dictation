package com.irondonkey.dictation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Search and plain-text export for History. Pure Java, so the desktop test harness checks the same code. */
public final class HistoryText {
    private HistoryText() {}

    /** True when every word of the query is in the transcript, its model name or its source, ignoring case. */
    public static boolean matches(String query, String text, String model, String source) {
        if (query == null || query.trim().isEmpty()) return true;
        String hay = (text + "\n" + model + "\n" + source).toLowerCase(Locale.ROOT);
        for (String w : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!hay.contains(w)) return false;
        }
        return true;
    }

    /** One transcript as text for the exported file: a date line, where it came from, then the words. */
    public static String entry(long time, String model, String source, String text) {
        String when = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(time));
        StringBuilder b = new StringBuilder();
        b.append("=== ").append(when);
        String from = source == null ? "" : source;
        if (model != null && !model.isEmpty()) from = from.isEmpty() ? model : from + ", " + model;
        if (!from.isEmpty()) b.append(" (").append(from).append(')');
        b.append(" ===\n").append(text == null ? "" : text.trim()).append("\n\n");
        return b.toString();
    }
}
