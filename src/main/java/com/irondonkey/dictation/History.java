package com.irondonkey.dictation;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Past transcripts, newest first, kept in one small JSON file on the phone. */
public final class History {
    private History() {}

    private static final int MAX = 500;

    public static final class Entry {
        public final long time;
        public final String model;
        public final String source;
        public final String text;
        Entry(long time, String model, String source, String text) {
            this.time = time; this.model = model; this.source = source; this.text = text;
        }
    }

    private static File file(Context c) { return new File(c.getFilesDir(), "history.json"); }

    public static synchronized List<Entry> load(Context c) {
        List<Entry> out = new ArrayList<>();
        File f = file(c);
        if (!f.exists()) return out;
        try (InputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int off = 0, r;
            while (off < b.length && (r = in.read(b, off, b.length - off)) > 0) off += r;
            JSONArray a = new JSONArray(new String(b, 0, off, StandardCharsets.UTF_8));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new Entry(o.getLong("t"), o.optString("m"), o.optString("s"), o.optString("x")));
            }
        } catch (IOException | JSONException ignored) {
        }
        return out;
    }

    private static void save(Context c, List<Entry> list) {
        JSONArray a = new JSONArray();
        try {
            for (Entry e : list) {
                JSONObject o = new JSONObject();
                o.put("t", e.time); o.put("m", e.model); o.put("s", e.source); o.put("x", e.text);
                a.put(o);
            }
        } catch (JSONException ignored) {
        }
        File tmp = new File(c.getFilesDir(), "history.json.tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            out.write(a.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        tmp.renameTo(file(c));
    }

    public static synchronized void add(Context c, String text, String model, String source) {
        List<Entry> list = load(c);
        list.add(0, new Entry(System.currentTimeMillis(), model, source, text));
        while (list.size() > MAX) list.remove(list.size() - 1);
        save(c, list);
    }

    public static synchronized void delete(Context c, long time) {
        List<Entry> list = load(c);
        for (int i = 0; i < list.size(); i++) if (list.get(i).time == time) { list.remove(i); break; }
        save(c, list);
    }

    public static synchronized void clear(Context c) {
        //noinspection ResultOfMethodCallIgnored
        file(c).delete();
    }
}
