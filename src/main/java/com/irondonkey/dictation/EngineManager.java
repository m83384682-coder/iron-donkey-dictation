package com.irondonkey.dictation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps exactly one model in memory: the selected one. Switching unloads the old model
 * before the new one loads, so two big models are never in memory together.
 */
public final class EngineManager implements Transcription.EngineSource {
    public enum State { NONE, LOADING, READY, ERROR }

    public interface Listener { void onEngineState(State state, Models.ModelInfo model, String error); }

    private static EngineManager instance;

    public static synchronized EngineManager get(Context c) {
        if (instance == null) instance = new EngineManager(c.getApplicationContext());
        return instance;
    }

    private final Context app;
    private final Prefs prefs;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();

    private Engine engine;
    private State state = State.NONE;
    private Models.ModelInfo stateModel;
    private String error;
    private int generation = 0;
    private int busy = 0;

    private EngineManager(Context app) {
        this.app = app;
        this.prefs = new Prefs(app);
    }

    public void addListener(Listener l) {
        listeners.add(l);
        State s; Models.ModelInfo m; String e;
        synchronized (lock) { s = state; m = stateModel; e = error; }
        l.onEngineState(s, m, e);
    }

    public void removeListener(Listener l) { listeners.remove(l); }

    public State state() { synchronized (lock) { return state; } }

    private void setState(State s, Models.ModelInfo m, String err) {
        synchronized (lock) {
            state = s; stateModel = m; error = err;
            lock.notifyAll();
        }
        main.post(() -> { for (Listener l : listeners) l.onEngineState(s, m, err); });
    }

    /** Loads the selected model if it is not already the one in memory. */
    public void ensureLoaded() {
        Models.ModelInfo m = Models.get(prefs.selectedModel());
        if (m == null || !ModelStore.isDownloaded(app, m)) {
            unload();
            return;
        }
        int threads = prefs.threads();
        String lang = prefs.language(m);
        synchronized (lock) {
            if (engine != null && engine.model == m && engine.threads == threads) {
                engine.setLanguage(lang);
                return;
            }
            if (state == State.LOADING && stateModel == m) return;
            generation++;
        }
        final int gen;
        synchronized (lock) { gen = generation; }
        setState(State.LOADING, m, null);
        loader.execute(() -> {
            synchronized (lock) { if (gen != generation) return; }
            releaseEngine();
            try {
                Engine e = new Engine(m, ModelStore.dir(app, m), lang, threads);
                boolean stale;
                synchronized (lock) {
                    stale = gen != generation;
                    if (!stale) engine = e;
                }
                if (stale) { e.release(); return; }
                setState(State.READY, m, null);
            } catch (Throwable t) {
                synchronized (lock) { if (gen != generation) return; }
                setState(State.ERROR, m, t.getMessage() == null ? t.toString() : t.getMessage());
            }
        });
    }

    public void languageChanged() {
        Models.ModelInfo m = Models.get(prefs.selectedModel());
        synchronized (lock) {
            if (engine != null && m != null && engine.model == m) engine.setLanguage(prefs.language(m));
        }
    }

    public void unload() {
        synchronized (lock) { generation++; }
        loader.execute(this::releaseEngine);
        setState(State.NONE, null, null);
    }

    /** Unloads unless the user wants the model kept in memory or something is using it. */
    public void maybeUnload() {
        synchronized (lock) { if (busy > 0) return; }
        if (!prefs.keepLoaded()) unload();
    }

    public void beginUse() { synchronized (lock) { busy++; } }

    public void endUse() { synchronized (lock) { busy = Math.max(0, busy - 1); } }

    public boolean inUse() { synchronized (lock) { return busy > 0; } }

    private void releaseEngine() {
        Engine old;
        synchronized (lock) { old = engine; engine = null; }
        if (old != null) old.release();
        System.gc();
    }

    @Override
    public Engine await() throws Exception {
        ensureLoaded();
        synchronized (lock) {
            while (true) {
                if (state == State.READY && engine != null) return engine;
                if (state == State.ERROR) throw new Exception("The model could not load: " + error);
                if (state == State.NONE) throw new Exception("No model is downloaded. Pick one on the Models screen.");
                lock.wait();
            }
        }
    }

    /** The model in memory right now, or the one loading. */
    public Models.ModelInfo currentModel() {
        synchronized (lock) { return stateModel; }
    }
}
