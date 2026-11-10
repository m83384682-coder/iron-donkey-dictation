package com.irondonkey.dictation;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The last few errors, kept in a small text file on the phone so they can go into a problem report.
 * Only error messages are kept, never transcripts. Nothing leaves the phone unless the person reports it.
 */
public final class ErrorLog {
    private ErrorLog() {}

    private static final int MAX = 20;
    private static final String SEP = "\n\u001e\n";

    private static File file(Context c) { return new File(c.getApplicationContext().getFilesDir(), "errors.txt"); }
    private static File crashFlag(Context c) { return new File(c.getApplicationContext().getFilesDir(), "crashed.flag"); }

    public static void record(Context c, String where, Throwable t) {
        record(c, where, t == null ? "" : t.getClass().getName() + ": " + t.getMessage());
    }

    public static synchronized void record(Context c, String where, String what) {
        try {
            List<String> list = load(c);
            String when = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            list.add(when + " " + where + ": " + (what == null ? "" : what));
            while (list.size() > MAX) list.remove(0);
            try (OutputStream o = new FileOutputStream(file(c))) {
                o.write(String.join(SEP, list).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    /** Oldest first. */
    public static synchronized List<String> load(Context c) {
        List<String> out = new ArrayList<>();
        File f = file(c);
        if (!f.exists()) return out;
        try (InputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) Math.min(f.length(), 200_000)];
            int off = 0, r;
            while (off < b.length && (r = in.read(b, off, b.length - off)) > 0) off += r;
            for (String s : new String(b, 0, off, StandardCharsets.UTF_8).split(SEP)) if (!s.trim().isEmpty()) out.add(s);
        } catch (Exception ignored) {
        }
        return out;
    }

    public static synchronized void clear(Context c) {
        //noinspection ResultOfMethodCallIgnored
        file(c).delete();
        //noinspection ResultOfMethodCallIgnored
        crashFlag(c).delete();
    }

    /** Saves a crash with the first lines of its stack, then lets Android close the app as usual. */
    public static void installCrashHandler(Context c) {
        Context app = c.getApplicationContext();
        Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((th, e) -> {
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String[] lines = sw.toString().split("\n");
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < Math.min(lines.length, 14); i++) b.append(i == 0 ? "" : "\n  ").append(lines[i].trim());
                record(app, "Crash in " + th.getName(), b.toString());
                //noinspection ResultOfMethodCallIgnored
                crashFlag(app).createNewFile();
            } catch (Throwable ignored) {
            }
            if (old != null) old.uncaughtException(th, e);
        });
    }

    /** True once after the app crashed; the main screen then offers to report it. */
    public static boolean takeCrashFlag(Context c) {
        File f = crashFlag(c);
        return f.exists() && f.delete();
    }
}
