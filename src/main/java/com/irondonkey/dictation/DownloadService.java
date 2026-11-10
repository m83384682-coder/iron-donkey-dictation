package com.irondonkey.dictation;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StatFs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads models in the background, one at a time. Each file resumes from where it stopped
 * and is checked against its SHA-256 before it is used.
 */
public class DownloadService extends Service {
    public interface Listener { void onDownloadChanged(); }

    static final String ACTION_CANCEL = "cancel";
    static final String EXTRA_MODEL = "model";

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final Set<String> queue = new LinkedHashSet<>();
    private static final java.util.Map<String, String> errors = new java.util.HashMap<>();

    private static volatile String current;
    private static volatile long done, total;
    private static volatile boolean checking;
    private static final AtomicBoolean cancel = new AtomicBoolean(false);

    public static void addListener(Listener l) { listeners.add(l); }
    public static void removeListener(Listener l) { listeners.remove(l); }

    private static void changed() {
        main.post(() -> { for (Listener l : listeners) l.onDownloadChanged(); });
    }

    public static void start(Context c, String modelId) {
        synchronized (queue) {
            errors.remove(modelId);
            if (modelId.equals(current) || queue.contains(modelId)) return;
            queue.add(modelId);
        }
        Intent i = new Intent(c, DownloadService.class).putExtra(EXTRA_MODEL, modelId);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
        changed();
    }

    /** Cancels one model's download (queued or running). Partial files stay so it can resume later. */
    public static void cancel(String modelId) {
        synchronized (queue) {
            queue.remove(modelId);
            if (modelId.equals(current)) cancel.set(true);
        }
        changed();
    }

    public static boolean isActive(String id) {
        synchronized (queue) { return id.equals(current) || queue.contains(id); }
    }

    public static boolean isRunning(String id) { return id.equals(current); }

    public static boolean anyActive() {
        synchronized (queue) { return current != null || !queue.isEmpty(); }
    }

    public static String error(String id) {
        synchronized (queue) { return errors.get(id); }
    }

    /** Progress of the running download, 0..1000, or -1 while checking files. */
    public static int permille(String id) {
        if (!id.equals(current)) return 0;
        if (checking) return -1;
        long t = total;
        return t <= 0 ? 0 : (int) Math.min(1000, done * 1000 / t);
    }

    public static long bytesDone() { return done; }
    public static long bytesTotal() { return total; }

    private Thread worker;
    private PowerManager.WakeLock wake;
    private WifiManager.WifiLock wifi;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notif.startForeground(this, Notif.ID_DOWNLOAD, note("Preparing download", 0, true), Notif.typeDataSync());
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            String c = current;
            synchronized (queue) { queue.clear(); }
            if (c != null) cancel(c);
        }
        if (worker == null || !worker.isAlive()) {
            worker = new Thread(this::run, "download");
            worker.start();
        }
        return START_NOT_STICKY;
    }

    private Notification note(String text, int permille, boolean indeterminate) {
        return Notif.builder(this)
                .setContentTitle("Downloading speech model")
                .setContentText(text)
                .setProgress(1000, permille, indeterminate)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Cancel",
                        Notif.serviceAction(this, DownloadService.class, ACTION_CANCEL, 3)).build())
                .build();
    }

    private void run() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dictation:download");
        wake.acquire(6 * 60 * 60 * 1000L);
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm != null) {
            wifi = wm.createWifiLock(Build.VERSION.SDK_INT >= 29 ? WifiManager.WIFI_MODE_FULL_HIGH_PERF : WifiManager.WIFI_MODE_FULL, "dictation:download");
            wifi.acquire();
        }
        try {
            while (true) {
                String id;
                synchronized (queue) {
                    if (queue.isEmpty()) { current = null; break; }
                    id = queue.iterator().next();
                    queue.remove(id);
                    current = id;
                    cancel.set(false);
                }
                Models.ModelInfo m = Models.get(id);
                changed();
                try {
                    download(m);
                    Hub.message(m.name + " is downloaded.");
                    // Load it right away when it is the model the user picked.
                    if (id.equals(new Prefs(this).selectedModel())) EngineManager.get(this).ensureLoaded();
                } catch (Exception e) {
                    if (!cancel.get()) {
                        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                        synchronized (queue) { errors.put(id, msg); }
                        Hub.message(m.name + " did not download: " + msg);
                    }
                }
                synchronized (queue) { current = null; checking = false; }
                changed();
            }
        } finally {
            if (wake.isHeld()) wake.release();
            if (wifi != null && wifi.isHeld()) wifi.release();
            changed();
            main.post(() -> {
                stopForeground(true);
                stopSelf();
            });
        }
    }

    private void download(Models.ModelInfo m) throws IOException {
        File dir = ModelStore.dir(this, m);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Could not create the model folder");
        long need = m.totalBytes - ModelStore.bytesPresent(this, m);
        long free = new StatFs(dir.getAbsolutePath()).getAvailableBytes();
        if (need > free - 200_000_000L) {
            throw new IOException("Not enough storage. It needs " + ModelStore.formatSize(need) + ", and " + ModelStore.formatSize(free) + " is free.");
        }
        //noinspection ResultOfMethodCallIgnored
        ModelStore.marker(this, m).delete();
        total = m.totalBytes;
        done = ModelStore.bytesPresent(this, m);
        checking = false;
        for (Models.FileInfo f : m.files) {
            File out = new File(dir, f.path);
            File parent = out.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create a folder");
            if (out.exists() && out.length() == f.size) continue;
            if (out.exists()) //noinspection ResultOfMethodCallIgnored
                out.delete();
            File part = new File(dir, f.path + ".part");
            fetchWithRetries(m, f, part);
            if (!part.renameTo(out)) throw new IOException("Could not save " + f.path);
        }
        // Check every file before the model may be used.
        checking = true;
        changed();
        Notif.notify(this, Notif.ID_DOWNLOAD, note("Checking " + m.name, 0, true));
        for (Models.FileInfo f : m.files) {
            File out = new File(dir, f.path);
            String sha = ModelStore.sha256(out, cancel);
            if (!sha.equals(f.sha256)) {
                //noinspection ResultOfMethodCallIgnored
                out.delete();
                throw new IOException(f.path + " failed its check and was deleted. Tap Download to try again.");
            }
        }
        if (!ModelStore.marker(this, m).createNewFile() && !ModelStore.marker(this, m).exists()) {
            throw new IOException("Could not mark the model as checked");
        }
    }

    private void fetchWithRetries(Models.ModelInfo m, Models.FileInfo f, File part) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            if (cancel.get()) throw new IOException("Cancelled");
            try {
                fetch(m, f, part);
                return;
            } catch (IOException e) {
                if (cancel.get()) throw e;
                last = e;
                Notif.notify(this, Notif.ID_DOWNLOAD, note("Connection lost. Retrying…", 0, true));
                try { Thread.sleep(Math.min(30_000, 2000L << attempt)); } catch (InterruptedException ie) { throw e; }
            }
        }
        throw new IOException("The download kept failing (" + (last == null ? "unknown" : last.getMessage()) + "). Tap Download to resume.");
    }

    private void fetch(Models.ModelInfo m, Models.FileInfo f, File part) throws IOException {
        long have = part.exists() ? part.length() : 0;
        if (have > f.size) { //noinspection ResultOfMethodCallIgnored
            part.delete(); have = 0; }
        if (have == f.size) return;
        HttpURLConnection c = (HttpURLConnection) new URL(f.url).openConnection();
        c.setConnectTimeout(20_000);
        c.setReadTimeout(30_000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "IronDonkeyDictation/1.0");
        if (have > 0) c.setRequestProperty("Range", "bytes=" + have + "-");
        int code = c.getResponseCode();
        boolean append;
        if (code == 206) append = true;
        else if (code == 200) {
            append = false;
            done -= have;
            have = 0;
        } else {
            c.disconnect();
            throw new IOException("Server answered " + code);
        }
        long lastNote = 0;
        try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(part, append)) {
            byte[] b = new byte[1 << 16];
            int r;
            while ((r = in.read(b)) > 0) {
                if (cancel.get()) throw new IOException("Cancelled");
                out.write(b, 0, r);
                have += r;
                done += r;
                long now = System.currentTimeMillis();
                if (now - lastNote > 1000) {
                    lastNote = now;
                    int pm = (int) (done * 1000 / Math.max(1, total));
                    Notif.notify(this, Notif.ID_DOWNLOAD, note(m.name + ": " + ModelStore.formatSize(done) + " of " + ModelStore.formatSize(total), pm, false));
                    changed();
                }
            }
        } finally {
            c.disconnect();
        }
        if (have != f.size) throw new IOException("Connection ended early");
    }
}
