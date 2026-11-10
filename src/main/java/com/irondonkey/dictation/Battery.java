package com.irondonkey.dictation;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

/** Battery optimization: the keyboard needs Android to leave it alone. */
final class Battery {
    private Battery() {}

    static final String WARNING = "Warning: if you use Iron Donkey Dictation as a keyboard, allow it to ignore battery "
            + "optimizations. Otherwise Android can stop voice typing or throw the model out of memory in the background.";

    static boolean ignoring(Context c) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
    }

    /** Opens Android's "allow ignoring battery optimizations" question, or the settings list if that is not offered. */
    static void request(Context c) {
        Intent[] tries = {
                new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + c.getPackageName())),
                new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + c.getPackageName())),
        };
        for (Intent i : tries) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
            }
        }
    }
}
