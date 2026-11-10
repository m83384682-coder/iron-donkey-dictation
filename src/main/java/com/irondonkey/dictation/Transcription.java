package com.irondonkey.dictation;

import java.io.File;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * One recording or one file being turned into text. Audio goes in as it arrives; finished
 * pieces are decoded on a worker thread right away, so after the audio ends only the last
 * piece is left to decode. Pure Java plus sherpa-onnx, shared by the app and the desktop test.
 */
public final class Transcription {
    public interface EngineSource {
        /** Blocks until the selected model is loaded. */
        Engine await() throws Exception;
    }

    public interface Progress {
        /** endSample: where the finished piece ends, in 16 kHz samples from the start. */
        void onPieceDone(long endSample);
    }

    private static final Object END = new Object();
    /** Desktop test only: print each piece's raw model text. */
    public static volatile boolean debugRaw = false;

    private final EngineSource source;
    private final Segmenter segmenter;
    private final TextPipeline.Assembler assembler;
    private final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private final Thread worker;
    private volatile boolean cancelled = false;
    private volatile Exception error;
    private volatile int queued = 0, done = 0;
    private volatile Progress progress;
    private long fed = 0;

    public Transcription(EngineSource source, File vadModel, float maxSpeechSec, TextPipeline.Assembler assembler) {
        this.source = source;
        this.segmenter = new Segmenter(vadModel, maxSpeechSec);
        this.assembler = assembler;
        worker = new Thread(this::work, "decode");
        worker.start();
    }

    public void setProgress(Progress p) { progress = p; }

    private volatile int maxQueue = 0;

    /** For files: make feed() wait while this many pieces are waiting, so memory stays small. */
    public void setMaxQueue(int n) { maxQueue = n; }

    /** 16 kHz mono samples, in order. */
    public void feed(float[] samples, int n) {
        if (cancelled) return;
        fed += n;
        enqueue(segmenter.accept(samples, n));
        while (maxQueue > 0 && queue.size() > maxQueue && !cancelled && worker.isAlive()) {
            try { Thread.sleep(20); } catch (InterruptedException e) { return; }
        }
    }

    public long samplesFed() { return fed; }

    private void enqueue(List<Segmenter.Piece> segs) {
        for (Segmenter.Piece s : segs) {
            queued++;
            queue.add(s);
        }
    }

    /** Ends the audio and waits for every piece. Returns the text, or "" when nothing was said. */
    public String finish() throws Exception {
        try {
            if (!cancelled) enqueue(segmenter.finish());
            queue.add(END);
            worker.join();
        } finally {
            segmenter.release();
        }
        if (error != null) throw error;
        return cancelled ? "" : assembler.text();
    }

    public void cancel() {
        cancelled = true;
        queue.clear();
        queue.add(END);
    }

    public boolean isCancelled() { return cancelled; }

    private void work() {
        try {
            while (true) {
                Object o = queue.take();
                if (o == END || cancelled) break;
                Segmenter.Piece s = (Segmenter.Piece) o;
                Engine e = source.await();
                if (cancelled) break;
                String raw = e.decode(s.samples);
                if (debugRaw) System.err.println("RAW [" + s.start + "-" + s.end + " decoded " + s.samples.length + "] " + raw);
                assembler.add(TextPipeline.cleanSegment(raw), s.start, s.end);
                done++;
                Progress p = progress;
                if (p != null) p.onPieceDone(s.end);
            }
        } catch (Exception ex) {
            error = ex;
        }
    }
}
