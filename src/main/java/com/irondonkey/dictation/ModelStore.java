package com.irondonkey.dictation;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Where models live on the phone, and whether each one is fully downloaded and checked. */
public final class ModelStore {
    private ModelStore() {}

    public static File root(Context c) {
        return new File(c.getFilesDir(), "models");
    }

    public static File dir(Context c, Models.ModelInfo m) {
        return new File(root(c), m.id);
    }

    /** Written only after every file's SHA-256 matched. */
    static File marker(Context c, Models.ModelInfo m) {
        return new File(dir(c, m), ".verified");
    }

    public static boolean isDownloaded(Context c, Models.ModelInfo m) {
        if (!marker(c, m).exists()) return false;
        for (Models.FileInfo f : m.files) {
            File x = new File(dir(c, m), f.path);
            if (!x.exists() || x.length() != f.size) return false;
        }
        return true;
    }

    public static int downloadedCount(Context c) {
        int n = 0;
        for (Models.ModelInfo m : Models.ALL) if (isDownloaded(c, m)) n++;
        return n;
    }

    public static void delete(Context c, Models.ModelInfo m) {
        deleteTree(dir(c, m));
    }

    static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    /** Bytes already on disk for this model (finished files plus partial downloads). */
    public static long bytesPresent(Context c, Models.ModelInfo m) {
        long n = 0;
        for (Models.FileInfo f : m.files) {
            File x = new File(dir(c, m), f.path);
            File part = new File(dir(c, m), f.path + ".part");
            if (x.exists()) n += Math.min(x.length(), f.size);
            else if (part.exists()) n += Math.min(part.length(), f.size);
        }
        return n;
    }

    /** The voice detector ships inside the APK; copy it out once so the native library can open it. */
    public static synchronized File vadModel(Context c) throws IOException {
        File f = new File(c.getFilesDir(), "silero_vad.onnx");
        if (f.exists() && f.length() > 0) return f;
        File tmp = new File(c.getFilesDir(), "silero_vad.onnx.tmp");
        try (InputStream in = c.getAssets().open("silero_vad.onnx"); OutputStream out = new FileOutputStream(tmp)) {
            byte[] b = new byte[65536];
            int r;
            while ((r = in.read(b)) > 0) out.write(b, 0, r);
        }
        if (!tmp.renameTo(f)) throw new IOException("Could not save the voice detector");
        return f;
    }

    public static String formatSize(long bytes) {
        if (bytes >= 1_000_000_000L) return String.format(java.util.Locale.US, "%.2f GB", bytes / 1e9);
        return String.format(java.util.Locale.US, "%d MB", Math.round(bytes / 1e6));
    }

    static String sha256(File f, java.util.concurrent.atomic.AtomicBoolean cancel) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            try (InputStream in = new FileInputStream(f)) {
                byte[] b = new byte[1 << 16];
                int r;
                while ((r = in.read(b)) > 0) {
                    if (cancel != null && cancel.get()) throw new IOException("Cancelled");
                    md.update(b, 0, r);
                }
            }
            StringBuilder s = new StringBuilder();
            for (byte x : md.digest()) s.append(String.format("%02x", x));
            return s.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
