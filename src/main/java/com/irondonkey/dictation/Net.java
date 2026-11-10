package com.irondonkey.dictation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Whether the phone is online right now, and whether the connection is Wi-Fi-like (not metered). */
final class Net {
    private Net() {}

    static final String NONE = "none", METERED = "metered", UNMETERED = "unmetered", UNKNOWN = "unknown";

    static String state(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return UNKNOWN;
            Network n = cm.getActiveNetwork();
            if (n == null) return NONE;
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            if (nc == null || !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return NONE;
            return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ? UNMETERED : METERED;
        } catch (RuntimeException e) {
            return UNKNOWN; // never block a download because of this check
        }
    }
}
