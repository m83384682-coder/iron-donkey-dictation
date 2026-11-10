package com.irondonkey.dictation;

import android.app.Application;

/** Starts the crash log and makes sure Google ML Kit (the offline translator) is set up before any screen uses it. */
public class DonkeyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ErrorLog.installCrashHandler(this);
        Translate.init(this);
    }
}
