package com.irondonkey.dictation;

/** Settings used when a Whisper model is set to Urdu. */
final class Urdu {
    private Urdu() {}

    /** Shorter pieces keep Whisper from dropping sentence ends and looping on Urdu. */
    static final float MAX_PIECE_SEC = 10f;

    static final String WARNING = "Urdu works, but it's rough. In tests on 20 Urdu recordings, Whisper Small got about "
            + "4 in 10 letters wrong, and Whisper Turbo about half. Whisper Small is the better pick for Urdu. "
            + "Expect to fix the text by hand. English is far more accurate.";
}
