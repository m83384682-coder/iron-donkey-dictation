# Iron Donkey Dictation

**Built by Iron Donkey.** Offline speech-to-text for Android, plus a keyboard with a big voice typing button.

You talk, your phone writes it down. After you download a speech model, everything happens on your phone: no account, no cloud, no audio leaving the device. The internet is only used to download models.

[**Download the latest APK**](https://github.com/m83384682-coder/iron-donkey-dictation/releases/latest) · Android 7.0 or newer

---

## Why is the name so bad?

Great question. Here is the full, unedited history of how this app got its name.

The author sat down to name it like a professional. They opened a fresh document titled "Name Ideas" and typed:

1. **VoxScribe** – taken.
2. **SpeakEasy** – taken, and also a bar.
3. **Dictate.ly** – taken, and also a crime against vowels.
4. **WhisperNote** – taken, 40 times over.
5. **Transcribr** – the vowel situation was getting worse.
6. **SayType** – sounded like a sneeze.
7. **Parakeet Pal** – the lawyers of an actual parakeet got involved. (They did not. But they could have.)

At this point the author had run out of naming conventions entirely. CamelCase: used up. Dropping vowels: used up. Adding "-ify": the author refused on moral grounds. Greek gods: all of them were already apps. Even the backup plan, "Untitled Project (2) FINAL final", was rejected for being too descriptive.

So the author looked inward, past the branding guides and the domain name search, to the only title they truly had left: their own. They are the Supreme Iron Donkey. The donkey had already built a Hello World app and a text-to-speech engine. The donkey was clearly the brand.

That is how a privacy-focused, multi-model, on-device speech recognition app came to be named after a metallic farm animal. It is not a metaphor. Nobody knows where the title came from, and the author will not explain it. Please stop asking.

On the bright side, "Iron Donkey Dictation" was not taken.

---

## What it does

- **Record and get text.** One big button: it says **Record**, and while recording it says **Stop**. Nothing else to find. It was designed with TalkBack in mind.
- **Text appears while you talk.** Each part you finish saying is turned into text while you keep going.
- **Transcribe audio files.** Pick a file, or share one from another app.
- **Iron Donkey Keyboard.** A full keyboard with a large voice typing button at the top, so you can dictate into any app.
- **8 speech models to choose from**, from a tiny 44 MB English model to big multilingual ones. Download only the ones you want.
- **Spoken links and emails get written properly.** "https colon slash slash www dot google dot com" becomes `https://www.google.com`.
- **Fully offline after setup.**

---

## How it works

### 1. First launch: pick one model

On first launch you pick one speech model, with **Parakeet v2** suggested, and only that one downloads. You can add more any time later.

### 2. Models screen: download, delete, switch

Every model has its own **Download**, **Delete** and **Use this model** buttons. Models that know more than one language also get a **Language** button, where you pick **Auto** or a specific language. There's a **Download all** button too.

- Downloads keep going in the background and pick up where they stopped if your connection drops.
- Every file is checked against a known checksum before the model can be used.
- Only the model you're using is loaded into memory. When you switch, you see a loading message.

### 3. Recording

1. Tap **Record** and talk.
2. The app listens for pauses (using the Silero voice activity detector). Each time you finish a phrase, that piece is sent to the speech model and turned into text, while you keep talking.
3. Tap **Stop**. The last piece is finished off and the text is ready.

Some small touches:

- The first half second is skipped, and a lone "Stop." at the very start is removed, so the sound of you tapping the button doesn't end up in your text.
- A pause of more than 2 seconds starts a new paragraph (you can turn this off).
- A loop filter cuts phrases that a model repeats over and over.
- Each piece of speech keeps 0.3 seconds of real audio on either side, so the model never loses the start or end of a sentence.
- If nothing was said, you get "No speech heard."
- A recording notification with a Stop button keeps it going in the background.

### 4. When the text is ready

By default you get a short vibration. In Settings you can switch to no signal, a sound, or TalkBack saying "Done".

The text box adds new text to the end by default, or replaces it if you prefer. The text is copied to the clipboard automatically (you can turn that off), and there are **Copy**, **Share**, **Save**, **Clear** and **Undo** buttons.

### 5. Transcribing files

Pick an audio file or share one from another app. The app splits the audio at pauses into pieces of at most 20 seconds and transcribes them one by one. There's a progress bar TalkBack can read, a **Cancel** button, and it keeps working with the screen off.

### 6. Iron Donkey Keyboard

The app includes a full keyboard. Tap **Use as a keyboard** on the main screen for the setup steps: turn the keyboard on, choose it, and allow the microphone.

- **Voice typing button:** large and at the top. Tap to start, it changes to Stop, and tapping Stop turns your speech into text and types it into the text box you're in. It uses the model you picked, with your settings and history. If no text box is open, it copies the text instead.
- **Layout:** a number row with QWERTY letters below it.
- **Shift:** tap once for one capital letter, twice for caps lock, and a third time to turn it off. It also turns on by itself at the start of a sentence.
- **Symbols:** the ?123 key opens a page with about 40 symbols. ABC brings the letters back.
- **Bottom row:** a key to switch to your other keyboards (hold it to see the list), comma, Space, period, and Enter. Enter shows Go, Search, Send or Done when the text box asks for that.
- **Delete:** tap to delete one character, hold to delete a whole word.
- **TalkBack:** every key has a spoken label, like "Capital A", "Comma", "Shift, on" or "Caps lock, on". TalkBack also announces "Symbols", "Letters" and "Word deleted".
- No emojis, on purpose. The donkey keeps it simple.

### 7. Battery optimization warning

Android can pause apps in the background to save battery, which can interrupt long recordings and downloads. While battery optimization is still on for the app, a warning sits at the top of the keyboard and on the keyboard setup screen. Tapping it opens Android's "allow ignoring battery optimizations" setting.

### 8. Link and email cleanup

Spoken web addresses and emails are written out as real ones:

| You say | You get |
|---|---|
| "HTTPS colon slash slash www dot google dot com" | `https://www.google.com` |
| "john dot smith at gmail dot com" | `john.smith@gmail.com` |

Paths and dashes work too. Normal sentences like "the dot com era" or "to be honest, at home" are left alone. It's on by default and can be turned off in Settings. It can still get it wrong when you say "dot" right before an ending like "com" in a normal sentence.

Stutters are not cleaned up: what you say is what you get.

### 9. Urdu (experimental)

Urdu works with the Whisper models, but it's rough. In tests on 20 Urdu recordings, Whisper Small got about 38% of letters wrong and Whisper Turbo about 49%, so **Whisper Small is the better pick for Urdu**. When Urdu is selected, the app uses shorter 10-second pieces plus the loop filter, and shows a warning to expect fixing the text by hand.

### Other settings

- History of your transcriptions, with the date and model (can be turned off)
- Number of processor cores to use
- Keep the model loaded between recordings
- Which microphone to use: phone, wired or Bluetooth
- Android noise reduction

---

## The 8 models

All models run through [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) and are downloaded from Hugging Face, from repositories converted for sherpa-onnx by csukuangfj (Fangjun Kuang). Download sizes are approximate.

| Model | Languages | Download | Good for |
|---|---|---|---|
| **Parakeet v2** (default) | English | 661 MB | Best for English. Most accurate in testing and handles background noise well. |
| **Parakeet Ultra** | 25 European languages, detected automatically | 671 MB | European languages. About as accurate and fast as Parakeet v2. |
| **Canary 180M Flash** | English, Spanish, German, French | 207 MB | Light but accurate. Small download, loads quickly. |
| **Whisper Turbo** | 99 languages | 1,037 MB | Very accurate and knows the most languages, but slow. |
| **Whisper Small** | 99 languages | 375 MB | Many languages in a smaller download. Best pick for Urdu. |
| **Qwen3-ASR 0.6B** | 30 languages, detected automatically | 987 MB | Detects the language by itself. Includes Hindi, Arabic and Persian, but not Urdu. |
| **Whisper Base** | English | 161 MB | Fast and small. Less accurate with background noise. |
| **Moonshine Tiny** | English | 44 MB | Instant, for quick notes. Rough with background noise. |

### Test results

All 8 models were run on a computer with the app's own text pipeline and the same sherpa-onnx 2.0.1 library, on clean English recordings, the same recordings with noise, and a 5-minute file. The error rate is the share of words wrong (lower is better). Speed was measured on a fast computer, so expect your phone to be slower.

| Model | Clean | With noise | Notes |
|---|---|---|---|
| Parakeet Ultra | 5.9% | 8.3% | |
| Parakeet v2 | 6.2% | 6.8% | About 3 seconds per minute of speech. Best overall, the default. |
| Whisper Turbo | 6.6% | 8.8% | Slow: about 23 seconds per minute of speech |
| Canary 180M Flash | 7.9% | 9.4% | |
| Qwen3-ASR 0.6B | 8.0% | 9.3% | |
| Whisper Small | 8.4% | 12.3% | |
| Whisper Base | 8.5% | 16.0% | |
| Moonshine Tiny | 14.0% | 26.1% | |

Every model handled the 5-minute file, at 5.3% to 14.1% errors.

---

## Install

1. Go to the [latest release](https://github.com/m83384682-coder/iron-donkey-dictation/releases/latest).
2. Download the right APK for your phone:
   - **`iron-donkey-dictation-1.1-arm64-v8a.apk`** (64-bit): **pick this one.** It's right for almost every phone from the last several years.
   - **`iron-donkey-dictation-1.1-armeabi-v7a.apk`** (32-bit): only for older or budget phones with a 32-bit processor. If the 64-bit one won't install, try this one.
3. Open the APK on your phone. Android will probably ask you to allow installs from unknown sources. Play Protect may warn you because the app isn't from the Play Store and is signed with a debug key.

   Or with ADB: `adb install iron-donkey-dictation-1.1-arm64-v8a.apk`
4. Open the app, pick a model, and wait for the download (Wi-Fi recommended: models are 44 MB to about 1 GB).

Requires **Android 7.0 (API 24) or newer**.

Version 1.1 hasn't been tested on a real phone yet, so you're the QA department. If voice typing in the keyboard shows a microphone error on your phone, please open an issue.

---

## Building from source

The repo includes the prebuilt sherpa-onnx 2.0.1 Android native libraries (`src/main/jniLibs`), so you only need the Android SDK (platform 36, build-tools 36.1.0) and JDK 17 or newer.

```sh
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug
```

This produces one APK per chip type in `build/outputs/apk/debug/`. The optional `finalize.sh` re-aligns and signs them with your own keystore (it reads the keystore path, alias and passwords from environment variables; see the top of the script). No keys are stored in this repo.

---

## Credits and licenses

### Iron Donkey Dictation

The app's own code (`com.irondonkey.dictation`) is released under the **[Apache License 2.0](LICENSE)**. Apache 2.0 was chosen because it matches sherpa-onnx, the library the app is built on, and it lets anyone use, change and share the code, as long as they keep the credits and license.

### Libraries

- **sherpa-onnx 2.0.1** by the k2-fsa project (Xiaomi and contributors), https://github.com/k2-fsa/sherpa-onnx. Apache License 2.0. The app uses the prebuilt Android library `libsherpa-onnx-jni.so`. The Java classes in `com.k2fsa.sherpa.onnx` are a Java port of the sherpa-onnx Kotlin API (Apache License 2.0).
- Inside sherpa-onnx:
  - **ONNX Runtime**, Copyright (c) Microsoft Corporation. MIT License.
  - **kaldi-native-fbank**, Copyright Xiaomi Corporation. Apache License 2.0.
  - **kaldi-decoder**, Copyright Xiaomi Corporation. Apache License 2.0.
  - **OpenFst** (as used by kaldi-decoder). Apache License 2.0.
  - **simple-sentencepiece**. Apache License 2.0.
  - **utf8proc**. MIT License.
  - **nlohmann/json**, Copyright (c) Niels Lohmann. MIT License.
  - **Eigen**. Mozilla Public License 2.0 (source: https://gitlab.com/libeigen/eigen).
  - **hclust-cpp**. BSD-style license.
- **Silero VAD** (`silero_vad.onnx`) by the Silero Team, used to find pauses in speech. MIT License. It ships inside the app.

### Speech models

The speech models are not inside the app or this repo. You download the ones you pick, from Hugging Face repositories by csukuangfj (Fangjun Kuang), who converted them for sherpa-onnx.

- **Parakeet v2:** NVIDIA parakeet-tdt-0.6b-v2. CC BY 4.0.
- **Parakeet Ultra:** Moondream's improved version of NVIDIA parakeet-tdt-0.6b-v3. CC BY 4.0.
- **Canary 180M Flash:** NVIDIA canary-180m-flash. CC BY 4.0.
- **Whisper Turbo, Whisper Small, Whisper Base:** OpenAI Whisper (large-v3-turbo, small, base.en). MIT License.
- **Qwen3-ASR 0.6B:** Alibaba Qwen team. Apache License 2.0.
- **Moonshine Tiny:** Moonshine AI (Useful Sensors). MIT License.

The CC BY 4.0 models were converted to ONNX and quantized to 8-bit by the sherpa-onnx project; this app does not change them further.

The full license texts are in [`src/main/assets/licenses`](src/main/assets/licenses) and on the app's About screen.

---

*Iron Donkey Dictation. Built by Iron Donkey. The name is final. The donkey has spoken.*
