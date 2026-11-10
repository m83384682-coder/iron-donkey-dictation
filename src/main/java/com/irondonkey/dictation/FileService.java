package com.irondonkey.dictation;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Turns an audio or video file into text in the background, with progress and Cancel. */
public class FileService extends Service {
    static final String ACTION_CANCEL = "cancel";

    /** The file to read, opened by the screen that received it (picker or share). */
    static volatile ParcelFileDescriptor pendingFd;
    static volatile String pendingName;

    private static volatile Transcription active;
    private static volatile boolean cancelRequested;

    private Thread thread;
    private PowerManager.WakeLock wake;
    private final Handler main = new Handler(Looper.getMainLooper());

    public static boolean start(Context c, Uri uri, String name) {
        if (Hub.fileBusy) return false;
        try {
            ParcelFileDescriptor fd = c.getContentResolver().openFileDescriptor(uri, "r");
            if (fd == null) throw new IOException("empty");
            pendingFd = fd;
        } catch (Exception e) {
            Hub.message("Could not open that file.");
            return false;
        }
        pendingName = name == null ? "Audio file" : name;
        cancelRequested = false;
        Hub.fileBusy = true;
        Hub.fileProgress = 0;
        Hub.fileName = pendingName;
        Hub.status();
        Intent i = new Intent(c, FileService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
        return true;
    }

    public static void cancel(Context c) {
        cancelRequested = true;
        Transcription t = active;
        if (t != null) t.cancel();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notif.startForeground(this, Notif.ID_FILE, note(0), Notif.typeMediaProcessing());
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            cancel(this);
            return START_NOT_STICKY;
        }
        if (thread == null || !thread.isAlive()) {
            ParcelFileDescriptor fd = pendingFd;
            String name = pendingName;
            pendingFd = null;
            if (fd == null) {
                Hub.fileBusy = false;
                Hub.status();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
            thread = new Thread(() -> run(fd, name), "file");
            thread.start();
        }
        return START_NOT_STICKY;
    }

    private Notification note(int pct) {
        return Notif.builder(this)
                .setContentTitle("Transcribing " + (Hub.fileName == null ? "file" : Hub.fileName))
                .setContentText(pct + "% done")
                .setProgress(100, pct, false)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Cancel",
                        Notif.serviceAction(this, FileService.class, ACTION_CANCEL, 2)).build())
                .build();
    }

    private void setProgress(int pct) {
        pct = Math.max(0, Math.min(100, pct));
        if (pct == Hub.fileProgress) return;
        Hub.fileProgress = pct;
        Hub.status();
        Notif.notify(this, Notif.ID_FILE, note(pct));
    }

    private void run(ParcelFileDescriptor fd, String name) {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dictation:file");
        wake.acquire(6 * 60 * 60 * 1000L);
        Prefs prefs = new Prefs(this);
        EngineManager em = EngineManager.get(this);
        em.beginUse();
        em.ensureLoaded();
        MediaExtractor ex = new MediaExtractor();
        MediaCodec codec = null;
        try {
            Models.ModelInfo m = Models.get(prefs.selectedModel());
            String modelName = m == null ? "" : m.name;
            File vad = ModelStore.vadModel(this);
            float maxPiece = m != null && m.isUrdu(prefs.language(m)) ? Urdu.MAX_PIECE_SEC : 20f;
            Transcription t = new Transcription(em, vad, maxPiece, new TextPipeline.Assembler(prefs.paragraphs(), false).withLinks(prefs.cleanLinks()));
            active = t;
            t.setMaxQueue(4);

            ex.setDataSource(fd.getFileDescriptor());
            int track = -1;
            MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat f = ex.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) { track = i; fmt = f; break; }
            }
            if (track < 0) throw new IOException("That file has no sound to transcribe.");
            ex.selectTrack(track);
            final long durationUs = fmt.containsKey(MediaFormat.KEY_DURATION) ? fmt.getLong(MediaFormat.KEY_DURATION) : 0;
            final long totalSamples = durationUs * 16000 / 1_000_000;
            t.setProgress(end -> {
                if (totalSamples > 0) setProgress((int) (end * 100 / totalSamples));
            });
            codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME));
            codec.configure(fmt, null, null, 0);
            codec.start();

            int rate = fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channels = fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
            int encoding = AudioFormat.ENCODING_PCM_16BIT;
            Resampler rs = new Resampler(rate, 16000);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false, outputDone = false;
            float[] mono = new float[0];
            while (!outputDone && !cancelRequested) {
                if (!inputDone) {
                    int in = codec.dequeueInputBuffer(10_000);
                    if (in >= 0) {
                        ByteBuffer b = codec.getInputBuffer(in);
                        int n = b == null ? -1 : ex.readSampleData(b, 0);
                        if (n < 0) {
                            codec.queueInputBuffer(in, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            codec.queueInputBuffer(in, 0, n, ex.getSampleTime(), 0);
                            ex.advance();
                        }
                    }
                }
                int out = codec.dequeueOutputBuffer(info, 10_000);
                if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat of = codec.getOutputFormat();
                    int newRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    if (Build.VERSION.SDK_INT >= 24 && of.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        encoding = of.getInteger(MediaFormat.KEY_PCM_ENCODING);
                    }
                    if (newRate != rate) { rate = newRate; rs = new Resampler(rate, 16000); }
                } else if (out >= 0) {
                    ByteBuffer b = codec.getOutputBuffer(out);
                    if (b != null && info.size > 0) {
                        b.position(info.offset);
                        b.limit(info.offset + info.size);
                        b.order(ByteOrder.nativeOrder());
                        int frames;
                        if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                            frames = info.size / 4 / channels;
                            if (mono.length < frames) mono = new float[frames];
                            java.nio.FloatBuffer fb = b.asFloatBuffer();
                            for (int i = 0; i < frames; i++) {
                                float s = 0;
                                for (int c = 0; c < channels; c++) s += fb.get(i * channels + c);
                                mono[i] = s / channels;
                            }
                        } else {
                            frames = info.size / 2 / channels;
                            if (mono.length < frames) mono = new float[frames];
                            java.nio.ShortBuffer sb = b.asShortBuffer();
                            for (int i = 0; i < frames; i++) {
                                float s = 0;
                                for (int c = 0; c < channels; c++) s += sb.get(i * channels + c);
                                mono[i] = s / channels / 32768f;
                            }
                        }
                        float[] y = rs.process(mono, frames);
                        if (y.length > 0) t.feed(y, y.length);
                    }
                    codec.releaseOutputBuffer(out, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
                }
            }
            if (!cancelRequested) {
                float[] y = rs.flush();
                if (y.length > 0) t.feed(y, y.length);
            }
            String text = cancelRequested ? "" : t.finish();
            if (cancelRequested) {
                t.cancel();
                t.finish();
                Hub.message("Cancelled.");
            } else if (text.isEmpty()) {
                Hub.message(TextPipeline.NO_SPEECH);
                Hub.announce(this, TextPipeline.NO_SPEECH);
            } else {
                setProgress(100);
                Hub.deliver(this, text, modelName, name);
            }
        } catch (Exception e) {
            Hub.message(cancelRequested ? "Cancelled." : "Could not transcribe the file: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        } finally {
            active = null;
            if (codec != null) {
                try { codec.stop(); } catch (RuntimeException ignored) { }
                codec.release();
            }
            ex.release();
            try { fd.close(); } catch (IOException ignored) { }
            em.endUse();
            Hub.fileBusy = false;
            Hub.status();
            if (wake.isHeld()) wake.release();
            main.post(() -> {
                stopForeground(true);
                stopSelf();
                if (!MainActivity.visible) em.maybeUnload();
            });
        }
    }
}
