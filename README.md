# Iron Donkey Dictation

**Built by Iron Donkey.** Offline speech-to-text for Android, plus a keyboard with a big voice typing button.

You talk, your phone writes it down. After you download a speech model, everything happens on your phone: no account, no cloud, no audio leaving the device. The internet is only used to download models and translation files, and for the online translator if you choose to turn it on.

[**Download the latest APK**](https://github.com/m83384682-coder/iron-donkey-dictation/releases/latest) · Android 7.0 or newer

> **Tested on a real device.** The author has tested the app on a real Android phone. Even so, some errors may sometimes happen. If something goes wrong, please report it in [GitHub Issues](https://github.com/m83384682-coder/iron-donkey-dictation/issues), with what you did, what happened, your phone model and your Android version. The app's **About → Report a problem** button fills most of that in for you.

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
- **Translation (new in 1.2).** Speak one language, get text in another. Off by default, set separately for each model, and only shown for models that know more than one language.
- **Push to talk in the keyboard (new in 1.2).** Hold the voice typing button to talk, let go to type.
- **A better keyboard (new in 1.2).** Accents and extra symbols on long press, cursor arrows, Select all, Copy and Paste, double space for a period, and more TalkBack feedback.
- **Handy extras (new in 1.2).** Spoken commands ("new line", "comma"), your own word replacements, stop by itself after silence, a word counter and text size.
- **Fixes and reporting (new in 1.3).** Offline translator downloads work again, errors are explained in plain words, a Report a problem button fills in a GitHub issue for you, and History can be searched, shared and saved as a text file.
- **Fully offline after setup**, unless you pick the online translator.

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

Since 1.2 it also understands the punctuation and capitals Whisper adds inside spoken links (see below).

Stutters are not cleaned up: what you say is what you get.

### 9. Translation (new in 1.2)

Speak in one language and get the text in another. It works everywhere the app writes text: recording, file transcription, and voice typing in the keyboard.

- **Off by default.** Turn it on with the **Translation** button on the main screen, in Settings, or with the **Translate** key in the keyboard's tool row.
- **Only for models that know more than one language:** Parakeet Ultra, Canary, Qwen3, Whisper Small and Whisper Turbo. For the English-only models (Parakeet v2, Whisper Base, Moonshine Tiny) the button and the key are hidden.
- **Every model keeps its own settings.** Turn it on for Whisper Small with Urdu to English, switch to Canary with English to German, and switch back: each one remembers what you set.
- **You choose** the language you speak (Auto, or a specific one; for most models this is the same as the model's Language setting) and the language to translate into (59 languages).

There are three translators, and the app picks the best one by itself unless you choose:

| Translator | When it's used | Internet |
|---|---|---|
| **The speech model itself** | Whisper Small into English, and Canary between English and Spanish, German or French. It translates while it listens, with nothing extra to download. | No |
| **Offline translator** (Google ML Kit) | Any other pair. Each language is a one-time download of about 30 MB, then it works with no internet. There's a button to download the files ahead of time and a list to delete them. | Only for the one-time download |
| **Online translator** (Google Cloud Translation) | Only if you choose it. It needs your own Google Cloud API key with the Cloud Translation API turned on, kept only on your phone. | Yes: your text is sent to Google |

Whisper Turbo was not trained to translate, so it uses the offline translator. Web addresses, emails and line breaks are never sent to a translator; they stay exactly as they were. You can also keep the original words under the translation. If translating fails, you get the original text and a message saying why.

### 10. Keyboard improvements (new in 1.2)

- **Push to talk:** turn it on in Settings, under Keyboard. Hold the voice typing button to talk and let go to type. With TalkBack, double tap and hold. A press that's too short to say anything types nothing and tells you to hold it. Tap to start and tap Stop is still the default.
- **Accents and extra symbols:** hold a letter for its accents (hold e for è é ê ë...), hold a number for its symbol and fractions, hold the period for more punctuation, and hold $ for other currencies. They show in a row above the keys, and TalkBack reads them all out. TalkBack also tells you that a key has more on double tap and hold.
- **Tool row:** cursor left and right (hold for the start or end of the text), Select all, Copy, Paste, and Translate. TalkBack says the character the cursor moved past. You can hide the row in Settings.
- **Two spaces type a period**, like other keyboards (can be turned off).
- **A small vibration on key presses** (can be turned off).

### 11. Other new extras in 1.2

- **Spoken commands** (off by default): say "new line", "new paragraph", "comma", "period" or "full stop", "question mark", "exclamation mark", "colon" or "semicolon" and get the real thing. The next sentence starts with a capital.
- **Word replacements:** your own list, one per line, like `iron donkey = Iron Donkey` or `btw = by the way`. Whole words only, capitals don't matter.
- **Stop by itself after silence:** never (default), or after 3, 5, 10 or 20 seconds of silence.
- **Word and character counter** under the text box.
- **Text size** for the text box: small, normal, large or extra large.
- **Link cleanup understands Whisper's punctuation.** Whisper likes to write "https colon slash slash www. My website dot com", with a period and a capital after "www". That now becomes `https://www.mywebsite.com`, and so do "https: www...", "https/www..." and addresses said letter by letter ("H T T P S colon slash slash W W W dot ..."). A period the model writes after ".com" stays as the end of the sentence.

### 12. New in 1.3

- **Offline translator downloads fixed.** In 1.2, downloading a translation language could fail with `Download failed: Attempt to invoke virtual method '...RemoteModelManager.download(...)' on a null object reference`. The cause: the code shrinker (R8) removed the constructors of ML Kit's component registrars, so ML Kit started with nothing registered and `RemoteModelManager.getInstance()` returned null. The registrars are now kept whole, the app also starts ML Kit itself when it opens, and if a download still fails you get a plain message (no internet, Wi-Fi only, not enough space, timed out...) instead of a Java exception.
- **Checks before downloading:** with no internet, or on mobile data with "Download translation files on mobile data too" turned off, the app says so right away instead of waiting.
- **Download any language ahead of time:** Translation files now has "Download another language", and multi-language downloads say which one is going ("2 of 2").
- **Report a problem** (in About, and offered after an error or a crash): opens a new GitHub issue with the app version, phone model, Android version, chosen model and the last few errors filled in. Transcripts are never included, and nothing is sent until you press Submit on GitHub yourself. You can also copy the report instead.
- **Error log:** the app keeps the last 20 error messages (never transcripts) on the phone for reports. You can clear it in About.
- **Crash notice:** if the app closed unexpectedly, the next time you open it, it offers to report it.
- **History search:** type words to find old transcripts (it matches the text, the model and the source).
- **History export:** save all your transcripts as one text file.
- **History share:** each transcript has a Share button next to Copy and Delete.

### 13. Urdu (experimental)

Urdu works with the Whisper models, but it's rough. In tests on 20 Urdu recordings, Whisper Small got about 38% of letters wrong and Whisper Turbo about 49%, so **Whisper Small is the better pick for Urdu**. When Urdu is selected, the app uses shorter 10-second pieces plus the loop filter, and shows a warning to expect fixing the text by hand.

### Other settings

- History of your transcriptions, with the date and model, search, share and export (can be turned off)
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

### Tests for 1.2

Run the same way, with the app's own code and sherpa-onnx 2.0.1, on new spoken test clips:

- **Links, emails and spoken commands:** on clips saying a link, an email and a short letter with spoken commands, all 8 models got the email right except Moonshine Tiny, which wrote "John dotsmith@yahoo.com". Six models wrote both links exactly right. Qwen3 wrote both as links too, but heard "http" instead of "https" in one. Whisper Small stopped transcribing at the letter-by-letter "H T T P S", so its text had no link to fix. "New line", "new paragraph", "period" and "exclamation mark" worked on all 8; "comma" was misheard by five models (as "come on", "common", "Kama" and so on).
- **Model translation:** Canary turned two English sentences into correct German, Spanish and French. Whisper Small translated 5 Urdu recordings into English at about 3 seconds per recording on the test computer; the meaning came through only roughly, and sometimes not at all, so treat Urdu translation as experimental too.
- **Offline and online translators:** these use Google ML Kit and Google Cloud, which only run on a phone, so they weren't run on the computer. The part of the app that keeps links, emails and line breaks away from the translator was tested.
- 73 small checks of the text pipeline passed, including the Whisper link fix, normal sentences that must stay as they were ("the dot com era", "It was late. My website was down."), stutters left alone, spoken commands, replacements, and which translator each model uses.

### Tests for 1.3

- 95 small checks of the app's own text code passed on the computer: the 73 from 1.2 plus 22 new ones for the translator error messages (including the exact 1.2 crash message), problem reports and history search and export.
- The final APK was inspected: ML Kit's start-up provider and all four component registrars are in the manifest, and the registrar classes now keep their constructors (in 1.2's APK they had none).
- The translator download itself needs a phone, so it couldn't be run on the computer.

---

## Install

1. Go to the [latest release](https://github.com/m83384682-coder/iron-donkey-dictation/releases/latest).
2. Download the right APK for your phone:
   - **`iron-donkey-dictation-1.3-arm64-v8a.apk`** (64-bit): **pick this one.** It's right for almost every phone from the last several years.
   - **`iron-donkey-dictation-1.3-armeabi-v7a.apk`** (32-bit): only for older or budget phones with a 32-bit processor.
   - **`iron-donkey-dictation-1.3-universal.apk`**: works on every phone, tablet and emulator (64-bit, 32-bit and x86), but it's a much bigger download. Use it if you're not sure which one you need.
3. Open the APK on your phone. Android will probably ask you to allow installs from unknown sources. Play Protect may warn you because the app isn't from the Play Store and is signed with a debug key.

   Or with ADB: `adb install iron-donkey-dictation-1.3-arm64-v8a.apk`

   Already have 1.1 or 1.2? 1.3 installs over it as an update and keeps your downloaded models and settings.
4. Open the app, pick a model, and wait for the download (Wi-Fi recommended: models are 44 MB to about 1 GB).

Requires **Android 7.0 (API 24) or newer**.

The app has been tested on a real device by the author, but some errors may sometimes happen. Please report them in [GitHub Issues](https://github.com/m83384682-coder/iron-donkey-dictation/issues), or with **About → Report a problem** in the app.

---

## Building from source

The repo includes the prebuilt sherpa-onnx 2.0.1 Android native libraries (`src/main/jniLibs`), so you only need the Android SDK (platform 36, build-tools 36.1.0) and JDK 17 or newer.

```sh
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug
```

This produces one APK per chip type plus a universal APK in `build/outputs/apk/debug/`. The optional `finalize.sh` re-aligns and signs them with your own keystore (it reads the keystore path, alias and passwords from environment variables; see the top of the script). No keys are stored in this repo.

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
- **Google ML Kit Translation and Language ID** (`com.google.mlkit:translate`, `com.google.mlkit:language-id`), for offline translation. Free to use under the [ML Kit Terms of Service](https://developers.google.com/ml-kit/terms); the translation models are downloaded by ML Kit on the phone.
- **Google Cloud Translation API**, only when you choose the online translator, with your own API key.
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
