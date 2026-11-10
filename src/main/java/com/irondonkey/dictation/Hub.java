package com.irondonkey.dictation;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.concurrent.CopyOnWriteArrayList;

/** Shared state between the screens and the background services, plus the "text is ready" signal. */
public final class Hub {
    private Hub() {}

    public interface Listener {
        /** Recording or file work started or stopped, or progress changed. */
        void onStatus();
        /** A finished transcript. Return true if the screen took it into its text box. */
        boolean onResult(String text);
        void onMessage(String message);
    }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());

    public static volatile boolean recording = false;
    public static volatile int finishing = 0;          // recordings stopped but still being turned into text
    public static volatile boolean fileBusy = false;
    public static volatile int fileProgress = 0;       // 0..100
    public static volatile String fileName = null;

    public static void addListener(Listener l) { listeners.add(l); }
    public static void removeListener(Listener l) { listeners.remove(l); }

    public static void status() {
        main.post(() -> { for (Listener l : listeners) l.onStatus(); });
    }

    public static void message(String m) {
        main.post(() -> { for (Listener l : listeners) l.onMessage(m); });
    }

    /** Delivers a transcript: into the open screen if there is one, else straight into the saved text. */
    public static void deliver(Context c, String text, String modelName, String source) {
        Context app = c.getApplicationContext();
        Prefs prefs = new Prefs(app);
        if (prefs.history()) History.add(app, text, modelName, source);
        main.post(() -> {
            boolean taken = false;
            for (Listener l : listeners) taken |= l.onResult(text);
            if (!taken) {
                String old = prefs.boxText();
                prefs.setUndoText(old);
                prefs.setBoxText(TextPipeline.combine(old, text, prefs.replaceText()));
                if (prefs.autoCopy()) copy(app, prefs.boxText());
            }
            signal(app);
        });
    }

    public static void copy(Context c, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Dictation", text));
        } catch (RuntimeException ignored) {
            // Some phones refuse clipboard writes from the background.
        }
    }

    public static void signal(Context c) {
        int s = new Prefs(c).signal();
        try {
            if (s == Prefs.SIGNAL_VIBRATE) {
                Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
                    else v.vibrate(70);
                }
            } else if (s == Prefs.SIGNAL_SOUND) {
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70);
                tg.startTone(ToneGenerator.TONE_PROP_ACK, 200);
                main.postDelayed(tg::release, 400);
            } else if (s == Prefs.SIGNAL_TALKBACK) {
                announce(c, "Done");
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static void announce(Context c, String text) {
        AccessibilityManager am = (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null || !am.isEnabled()) return;
        AccessibilityEvent e;
        if (Build.VERSION.SDK_INT >= 30) e = new AccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        else {
            e = AccessibilityEvent.obtain();
            e.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        }
        e.setClassName(Hub.class.getName());
        e.setPackageName(c.getPackageName());
        e.getText().add(text);
        am.sendAccessibilityEvent(e);
    }
}
