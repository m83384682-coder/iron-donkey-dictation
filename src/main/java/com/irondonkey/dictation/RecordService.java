package com.irondonkey.dictation;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.io.File;

/** Records from the microphone and turns finished pieces into text while the user keeps talking. */
public class RecordService extends Service {
    static final String ACTION_START = "start";
    static final String ACTION_STOP = "stop";

    private volatile boolean stopRequested;
    private Thread thread;
    private PowerManager.WakeLock wake;
    private final Handler main = new Handler(Looper.getMainLooper());

    public static void start(Context c) {
        Intent i = new Intent(c, RecordService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    public static void stop(Context c) {
        if (!Hub.recording) return;
        c.startService(new Intent(c, RecordService.class).setAction(ACTION_STOP));
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopRequested = true;
            Hub.recording = false;
            Hub.status();
            return START_NOT_STICKY;
        }
        Notification n = Notif.builder(this)
                .setContentTitle("Recording")
                .setContentText("Iron Donkey Dictation is listening")
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Stop",
                        Notif.serviceAction(this, RecordService.class, ACTION_STOP, 1)).build())
                .build();
        try {
            Notif.startForeground(this, Notif.ID_RECORD, n, Notif.typeMicrophone());
        } catch (RuntimeException e) {
            Hub.message("Recording could not start: " + e.getMessage());
            stopSelf();
            return START_NOT_STICKY;
        }
        if (thread != null && thread.isAlive()) return START_NOT_STICKY;
        stopRequested = false;
        Hub.recording = true;
        Hub.status();
        thread = new Thread(this::run, "record");
        thread.start();
        return START_NOT_STICKY;
    }

    private void run() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dictation:record");
        wake.acquire(4 * 60 * 60 * 1000L);
        Prefs prefs = new Prefs(this);
        EngineManager em = EngineManager.get(this);
        em.beginUse();
        em.ensureLoaded();
        Transcription t = null;
        String modelName = "";
        try {
            Models.ModelInfo m = Models.get(prefs.selectedModel());
            modelName = m == null ? "" : m.name;
            File vad = ModelStore.vadModel(this);
            float maxPiece = m != null && m.isUrdu(prefs.language(m)) ? Urdu.MAX_PIECE_SEC : 20f;
            t = new Transcription(em, vad, maxPiece, prefs.assembler());
            Mic.capture(this, t, () -> stopRequested);
        } catch (Exception e) {
            Hub.message("Recording stopped: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
            stopRequested = true;
        } finally {
            Hub.recording = false;
            Hub.finishing++;
            Hub.status();
            main.post(() -> {
                // The microphone is off; the notification can go while the last piece is decoded.
                stopForeground(true);
            });
        }
        try {
            if (t != null) {
                String text = t.finish();
                if (text.isEmpty()) {
                    Hub.message(TextPipeline.NO_SPEECH);
                    Hub.announce(this, TextPipeline.NO_SPEECH);
                } else {
                    Hub.deliver(this, text, modelName, "Recording");
                }
            }
        } catch (Exception e) {
            Hub.message(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            Hub.finishing--;
            Hub.status();
            em.endUse();
            if (wake.isHeld()) wake.release();
            main.post(() -> {
                if (!Hub.recording) stopSelf();
                if (!MainActivity.visible) em.maybeUnload();
            });
        }
    }
}
