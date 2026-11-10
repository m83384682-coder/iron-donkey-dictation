package com.irondonkey.dictation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;

/** Notification helpers for the three background services. */
final class Notif {
    private Notif() {}

    static final String CHANNEL = "work";
    static final int ID_RECORD = 1, ID_FILE = 2, ID_DOWNLOAD = 3, ID_DONE = 4;

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Recording and downloads", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
    }

    static Notification.Builder builder(Context c) {
        ensureChannel(c);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        b.setSmallIcon(R.drawable.ic_notif)
                .setContentIntent(PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .setOnlyAlertOnce(true);
        return b;
    }

    static PendingIntent serviceAction(Context c, Class<?> cls, String action, int req) {
        Intent i = new Intent(c, cls).setAction(action);
        return PendingIntent.getService(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void startForeground(Service s, int id, Notification n, int type) {
        if (Build.VERSION.SDK_INT >= 29) s.startForeground(id, n, type);
        else s.startForeground(id, n);
    }

    static int typeMicrophone() {
        return Build.VERSION.SDK_INT >= 30 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE : 0;
    }

    static int typeDataSync() {
        return Build.VERSION.SDK_INT >= 29 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0;
    }

    static int typeMediaProcessing() {
        if (Build.VERSION.SDK_INT >= 35) return ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING;
        return typeDataSync();
    }

    static void notify(Context c, int id, Notification n) {
        try {
            c.getSystemService(NotificationManager.class).notify(id, n);
        } catch (SecurityException ignored) {
        }
    }
}
