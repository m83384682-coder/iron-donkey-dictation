package com.irondonkey.dictation;

import java.util.Locale;

/**
 * Turns what went wrong in the offline translator into a sentence a person can act on, instead of a raw Java
 * exception. Pure Java, so the desktop test harness checks the same code.
 */
public final class TranslateErrors {
    private TranslateErrors() {}

    /** What kind of problem it was, worked out on the phone from the ML Kit error code. */
    public static final String NETWORK = "network", NO_SPACE = "space", TIMEOUT = "timeout", NOT_STARTED = "not_started",
            WAITING_WIFI = "wifi", OFFLINE = "offline", CANCELLED = "cancelled", OTHER = "other";

    public static final String REPORT_HINT = " If it keeps happening, please report it from About, Report a problem.";

    /**
     * The message for the user. kind is one of the constants above (or null when unknown), className is the
     * exception's class name and message its message, both possibly null.
     */
    public static String friendly(String kind, String className, String message) {
        if (kind == null || kind.equals(OTHER)) kind = guess(className, message);
        switch (kind) {
            case OFFLINE:
                return "There's no internet connection. Connect to the internet and try again. Translation files only need the internet once.";
            case WAITING_WIFI:
                return "You're not on Wi-Fi, and downloads over mobile data are turned off in Translation settings. "
                        + "Connect to Wi-Fi, or turn on \"Download translation files on mobile data too\", and try again.";
            case NETWORK:
                return "The download couldn't reach Google's servers. Check your internet connection and try again.";
            case NO_SPACE:
                return "There isn't enough free space on your phone. Each language needs about 30 MB. Free up some space and try again.";
            case TIMEOUT:
                return "The download took too long and was stopped. Check your internet connection and try again.";
            case CANCELLED:
                return "The download was cancelled.";
            case NOT_STARTED:
                return "The offline translator couldn't start on this phone, so nothing was downloaded. "
                        + "Try closing the app fully and opening it again." + REPORT_HINT;
            default:
                String detail = message == null || message.trim().isEmpty() ? null : message.trim();
                if (detail != null && detail.length() > 160) detail = detail.substring(0, 157) + "...";
                return "Something went wrong with the offline translator" + (detail == null ? "." : ": " + detail)
                        + " Please try again." + REPORT_HINT;
        }
    }

    /** Works out the kind from the exception alone, for errors that didn't come with an ML Kit code. */
    static String guess(String className, String message) {
        String c = className == null ? "" : className;
        String m = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (c.endsWith("TimeoutException")) return TIMEOUT;
        if (c.endsWith("CancellationException")) return CANCELLED;
        if (c.endsWith("NullPointerException") || m.contains("null object reference")
                || m.contains("mlkitcontext") || m.contains("has not been initialized")) return NOT_STARTED;
        if (c.endsWith("UnknownHostException") || c.endsWith("ConnectException") || c.endsWith("SocketTimeoutException")
                || m.contains("unable to resolve host") || m.contains("network")) return NETWORK;
        if (m.contains("no space") || m.contains("enospc") || m.contains("not enough space")) return NO_SPACE;
        return OTHER;
    }
}
