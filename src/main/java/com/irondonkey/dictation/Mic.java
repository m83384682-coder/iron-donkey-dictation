package com.irondonkey.dictation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;

import java.util.List;

/**
 * Reads the microphone at 16 kHz and feeds a Transcription until told to stop.
 * Shared by the app's Record button and the keyboard's voice typing button.
 */
final class Mic {
    private Mic() {}

    interface StopFlag { boolean stop(); }

    private static final int RATE = 16000;

    /** Blocks until stop() is true. Throws if the microphone cannot be used. */
    @SuppressLint("MissingPermission")
    static void capture(Context c, Transcription t, StopFlag stop) throws Exception {
        Prefs prefs = new Prefs(c);
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        boolean bluetooth = false;
        AudioRecord rec = null;
        NoiseSuppressor ns = null;
        try {
            int mic = prefs.mic();
            AudioDeviceInfo device = null;
            if (mic == Prefs.MIC_BLUETOOTH) {
                bluetooth = startBluetooth(am);
                if (!bluetooth) Hub.message("No Bluetooth headset found. Using the phone microphone.");
            } else {
                device = findInput(am, mic);
                if (mic == Prefs.MIC_WIRED && device == null) Hub.message("No wired headset found. Using the phone microphone.");
            }
            int source = bluetooth ? MediaRecorder.AudioSource.VOICE_COMMUNICATION : MediaRecorder.AudioSource.VOICE_RECOGNITION;
            int minBuf = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            rec = new AudioRecord(source, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minBuf, RATE * 2));
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("The microphone is busy or not allowed");
            if (device != null) rec.setPreferredDevice(device);
            if (prefs.noiseReduction() && NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(rec.getAudioSessionId());
                if (ns != null) ns.setEnabled(true);
            }
            rec.startRecording();

            long skip = (long) (prefs.skipStart() * RATE);
            short[] buf = new short[RATE / 10];
            float[] f = new float[buf.length];
            while (!stop.stop()) {
                int n = rec.read(buf, 0, buf.length);
                if (n < 0) throw new IllegalStateException("The microphone stopped (error " + n + ")");
                int off = 0;
                if (skip > 0) {
                    off = (int) Math.min(skip, n);
                    skip -= off;
                }
                int len = n - off;
                if (len <= 0) continue;
                for (int i = 0; i < len; i++) f[i] = buf[off + i] / 32768f;
                t.feed(f, len);
            }
        } finally {
            if (rec != null) {
                try { rec.stop(); } catch (RuntimeException ignored) { }
                rec.release();
            }
            if (ns != null) ns.release();
            if (bluetooth) stopBluetooth(am);
        }
    }

    private static AudioDeviceInfo findInput(AudioManager am, int mic) {
        AudioDeviceInfo[] devs = am.getDevices(AudioManager.GET_DEVICES_INPUTS);
        for (AudioDeviceInfo d : devs) {
            int ty = d.getType();
            if (mic == Prefs.MIC_PHONE && ty == AudioDeviceInfo.TYPE_BUILTIN_MIC) return d;
            if (mic == Prefs.MIC_WIRED && (ty == AudioDeviceInfo.TYPE_WIRED_HEADSET || ty == AudioDeviceInfo.TYPE_USB_HEADSET
                    || ty == AudioDeviceInfo.TYPE_USB_DEVICE)) return d;
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static boolean startBluetooth(AudioManager am) {
        if (Build.VERSION.SDK_INT >= 31) {
            List<AudioDeviceInfo> devs = am.getAvailableCommunicationDevices();
            for (AudioDeviceInfo d : devs) {
                int ty = d.getType();
                if (ty == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || ty == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                    am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    if (am.setCommunicationDevice(d)) return true;
                    am.setMode(AudioManager.MODE_NORMAL);
                }
            }
            return false;
        }
        if (!am.isBluetoothScoAvailableOffCall()) return false;
        boolean found = false;
        for (AudioDeviceInfo d : am.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            if (d.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) found = true;
        }
        if (!found) return false;
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.startBluetoothSco();
        am.setBluetoothScoOn(true);
        // Give the headset a moment to switch to its microphone.
        for (int i = 0; i < 20 && !am.isBluetoothScoOn(); i++) {
            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private static void stopBluetooth(AudioManager am) {
        if (Build.VERSION.SDK_INT >= 31) am.clearCommunicationDevice();
        else {
            am.setBluetoothScoOn(false);
            am.stopBluetoothSco();
        }
        am.setMode(AudioManager.MODE_NORMAL);
    }
}
