package com.irondonkey.dictation;

import com.k2fsa.sherpa.onnx.SpeechSegment;
import com.k2fsa.sherpa.onnx.Vad;
import com.k2fsa.sherpa.onnx.VadModelConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits 16 kHz audio at pauses (Silero VAD), into pieces no longer than maxSpeechSec.
 * The voice detector trims the edges of speech tightly, which can make a model drop a whole
 * sentence, so each piece gets up to 0.3 s of the real audio around it back, as long as that
 * does not reach into the piece before it.
 */
public final class Segmenter {
    public static final class Piece {
        /** Where the speech starts and ends, in samples from the start of the audio. */
        public final long start, end;
        /** The audio to decode, with its padding. */
        public final float[] samples;
        Piece(long start, long end, float[] samples) { this.start = start; this.end = end; this.samples = samples; }
    }

    private static final int WINDOW = 512;
    private static final int PAD = (int) (0.3f * TextPipeline.SAMPLE_RATE);

    private final Vad vad;
    private final float[] window = new float[WINDOW];
    private int filled = 0;
    private final int maxSpeechSamples;

    // Recent audio, so pieces can be padded from the real recording.
    private final float[] ring;
    private long total = 0;           // samples written to the ring so far
    private long lastEnd = 0;         // end of the previous piece's speech

    public Segmenter(File vadModel, float maxSpeechSec) {
        VadModelConfig c = new VadModelConfig();
        c.sileroVadModelConfig.model = vadModel.getAbsolutePath();
        c.sileroVadModelConfig.threshold = 0.5f;
        c.sileroVadModelConfig.minSilenceDuration = 0.5f;
        c.sileroVadModelConfig.minSpeechDuration = 0.25f;
        c.sileroVadModelConfig.maxSpeechDuration = maxSpeechSec;
        c.sileroVadModelConfig.windowSize = WINDOW;
        c.sampleRate = TextPipeline.SAMPLE_RATE;
        c.numThreads = 1;
        vad = new Vad(c);
        maxSpeechSamples = (int) (maxSpeechSec * TextPipeline.SAMPLE_RATE);
        ring = new float[maxSpeechSamples + 10 * TextPipeline.SAMPLE_RATE];
    }

    /** Feeds samples; returns any pieces that are now complete. */
    public List<Piece> accept(float[] samples, int n) {
        List<Piece> out = new ArrayList<>();
        int i = 0;
        while (i < n) {
            int take = Math.min(WINDOW - filled, n - i);
            System.arraycopy(samples, i, window, filled, take);
            filled += take;
            i += take;
            if (filled == WINDOW) {
                push(window, WINDOW);
                vad.acceptWaveform(window.clone());
                filled = 0;
                drain(out);
            }
        }
        return out;
    }

    /** Ends the audio: returns the last pieces. */
    public List<Piece> finish() {
        List<Piece> out = new ArrayList<>();
        if (filled > 0) {
            float[] last = new float[WINDOW];
            System.arraycopy(window, 0, last, 0, filled);
            push(last, WINDOW);
            vad.acceptWaveform(last);
            filled = 0;
        }
        vad.flush();
        drain(out);
        return out;
    }

    private void push(float[] x, int n) {
        for (int k = 0; k < n; k++) ring[(int) ((total + k) % ring.length)] = x[k];
        total += n;
    }

    private void drain(List<Piece> out) {
        while (!vad.empty()) {
            SpeechSegment s = vad.front();
            vad.pop();
            long start = s.start, end = s.start + s.samples.length;
            boolean forcedEnd = s.samples.length >= maxSpeechSamples - 2 * WINDOW;
            long oldest = Math.max(0, total - ring.length);
            long from = Math.min(start, Math.max(Math.max(start - PAD, lastEnd), oldest));
            long to = Math.max(end, forcedEnd ? end : Math.min(end + PAD, total));
            float[] x;
            if (from == start && to == end) {
                x = s.samples;
            } else {
                x = new float[(int) (to - from)];
                for (long k = from; k < to; k++) {
                    if (k >= start && k < end) x[(int) (k - from)] = s.samples[(int) (k - start)];
                    else if (k < total) x[(int) (k - from)] = ring[(int) (k % ring.length)];
                }
            }
            lastEnd = end;
            out.add(new Piece(start, end, x));
        }
    }

    public void release() { vad.release(); }
}
