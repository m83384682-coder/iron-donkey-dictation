package com.irondonkey.dictation;

/**
 * Streaming windowed-sinc resampler (mono float). Used to bring decoded audio files to 16 kHz
 * piece by piece, so a long file never has to sit in memory at once.
 */
public final class Resampler {
    private static final int TABLE_RES = 512;   // kernel table entries per input sample
    private static final int ZERO_CROSSINGS = 16;

    private final int inRate, outRate;
    private final double step;       // input samples per output sample
    private final double cutoff;     // relative to input Nyquist
    private final double halfWidth;  // kernel half width in input samples
    private final float[] table;
    private float[] buf = new float[8192];
    private int bufLen = 0;
    private long bufStart = 0;       // absolute input index of buf[0]
    private double t = 0;            // absolute input position of the next output sample

    public Resampler(int inRate, int outRate) {
        this.inRate = inRate;
        this.outRate = outRate;
        this.step = (double) inRate / outRate;
        this.cutoff = Math.min(1.0, (double) outRate / inRate) * 0.94;
        this.halfWidth = ZERO_CROSSINGS / cutoff;
        int n = (int) Math.ceil(halfWidth * TABLE_RES) + 2;
        table = new float[n];
        for (int i = 0; i < n; i++) {
            double x = (double) i / TABLE_RES;          // distance in input samples
            double u = x / halfWidth;
            if (u >= 1) { table[i] = 0; continue; }
            double arg = Math.PI * cutoff * x;
            double sinc = x == 0 ? 1.0 : Math.sin(arg) / arg;
            double win = 0.42 + 0.5 * Math.cos(Math.PI * u) + 0.08 * Math.cos(2 * Math.PI * u); // Blackman
            table[i] = (float) (cutoff * sinc * win);
        }
    }

    public boolean passthrough() { return inRate == outRate; }

    public float[] process(float[] in, int n) {
        if (passthrough()) {
            float[] o = new float[n];
            System.arraycopy(in, 0, o, 0, n);
            return o;
        }
        append(in, n);
        return produce(false);
    }

    /** Call once at the end: returns the remaining output. */
    public float[] flush() {
        if (passthrough()) return new float[0];
        int pad = (int) Math.ceil(halfWidth) + 2;
        flushing = true;
        append(new float[pad], pad);
        return produce(true);
    }

    private long realEnd = 0; // absolute index one past the last real input sample
    private boolean flushing = false;

    private void append(float[] in, int n) {
        if (!flushing) realEnd = bufStart + bufLen + n;
        if (bufLen + n > buf.length) {
            float[] nb = new float[Math.max(buf.length * 2, bufLen + n)];
            System.arraycopy(buf, 0, nb, 0, bufLen);
            buf = nb;
        }
        System.arraycopy(in, 0, buf, bufLen, n);
        bufLen += n;
    }

    private float[] produce(boolean last) {
        long avail = bufStart + bufLen;
        int maxOut = (int) Math.max(0, (avail - t) / step) + 2;
        float[] out = new float[maxOut];
        int count = 0;
        while (true) {
            if (last) {
                if (t >= realEnd) break;
            } else if (t + halfWidth >= avail) break;
            long k0 = (long) Math.ceil(t - halfWidth);
            long k1 = (long) Math.floor(t + halfWidth);
            double acc = 0;
            for (long k = k0; k <= k1; k++) {
                if (k < 0) continue;
                int idx = (int) (k - bufStart);
                if (idx < 0 || idx >= bufLen) continue;
                double d = Math.abs(t - k) * TABLE_RES;
                int i0 = (int) d;
                if (i0 + 1 >= table.length) continue;
                double frac = d - i0;
                double w = table[i0] + (table[i0 + 1] - table[i0]) * frac;
                acc += buf[idx] * w;
            }
            if (count == out.length) out = java.util.Arrays.copyOf(out, out.length * 2);
            out[count++] = (float) acc;
            t += step;
        }
        // Drop input that no future output needs.
        long keepFrom = (long) Math.floor(t - halfWidth) - 1;
        int drop = (int) Math.max(0, Math.min(bufLen, keepFrom - bufStart));
        if (drop > 0) {
            System.arraycopy(buf, drop, buf, 0, bufLen - drop);
            bufLen -= drop;
            bufStart += drop;
        }
        return count == out.length ? out : java.util.Arrays.copyOf(out, count);
    }
}
