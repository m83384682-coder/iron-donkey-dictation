package com.irondonkey.dictation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.List;

/** "Report a problem": a GitHub issue filled in with the phone, the app version and the recent errors. */
public final class Report {
    private Report() {}

    public static String body(Activity a) {
        Prefs p = new Prefs(a);
        Models.ModelInfo m = Models.get(p.selectedModel());
        String tr = null;
        if (m != null && Translation.multilingual(m)) {
            Translate.Plan pl = Translate.plan(a, m);
            tr = Translate.describe(pl);
        }
        String device = Build.MANUFACTURER + " " + Build.MODEL;
        String android = Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), " + String.join("/", Build.SUPPORTED_ABIS);
        List<String> errors = ErrorLog.load(a);
        return ReportText.body(BuildConfig.VERSION_NAME, device, android, m == null ? null : m.name, tr, errors);
    }

    /** Explains what will be shared, then opens GitHub or copies the report. Nothing is sent by the app itself. */
    public static void show(Activity a) {
        String body = body(a);
        int n = ErrorLog.load(a).size();
        new AlertDialog.Builder(a)
                .setTitle("Report a problem")
                .setMessage("This opens a new GitHub issue with your phone model, Android version, app version, chosen model and "
                        + (n == 0 ? "a note that no errors were recorded" : "the last " + n + (n == 1 ? " error" : " errors") + " the app recorded")
                        + " filled in. No transcripts are included. You can read and change it before posting, and nothing is sent until "
                        + "you press Submit on GitHub. You need a free GitHub account. If you don't have one, copy the report and send it another way.")
                .setPositiveButton("Open GitHub", (d, w) -> open(a, ReportText.issueUrl("Problem: ", body)))
                .setNeutralButton("Copy report", (d, w) -> { Hub.copy(a, body); Hub.announce(a, "Report copied"); })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void open(Activity a, String url) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Hub.copy(a, url);
            new AlertDialog.Builder(a).setMessage("No web browser found. The link was copied instead.")
                    .setPositiveButton("OK", null).show();
        }
    }
}
