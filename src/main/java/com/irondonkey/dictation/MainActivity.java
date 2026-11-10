package com.irondonkey.dictation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements Hub.Listener, EngineManager.Listener, DownloadService.Listener {
    static volatile boolean visible = false;

    private static final int REQ_PERMS = 1, REQ_OPEN = 2, REQ_SAVE = 3;

    private Prefs prefs;
    private EngineManager em;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<String> undo = new ArrayDeque<>();

    private TextView modelLine, status, translateLine, counter;
    private Button translateButton;
    private Button record;
    private EditText box;
    private LinearLayout fileArea;
    private TextView fileLabel;
    private ProgressBar fileBar;
    private Button fileButton;
    private Uri pendingShare;
    private String pendingShareName;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        em = EngineManager.get(this);
        if (ModelStore.downloadedCount(this) == 0 && !DownloadService.anyActive()) {
            Intent s = new Intent(this, SetupActivity.class);
            if (isShare(getIntent())) Hub.message("Download a model first, then share the file again.");
            startActivity(s);
            finish();
            return;
        }
        build();
        handleIntent(getIntent());
    }

    private void build() {
        LinearLayout col = Ui.page(this, "Iron Donkey Dictation");
        TextView title = Ui.heading(this, "Iron Donkey Dictation");
        col.addView(title);
        col.addView(Ui.body(this, "Built by Iron Donkey"));

        modelLine = Ui.body(this, "");
        modelLine.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Ui.add(col, modelLine, 8);
        translateLine = Ui.text(this, "", 14, Ui.ACCENT);
        Ui.add(col, translateLine, 2);

        record = new Button(this);
        record.setAllCaps(false);
        record.setText("Record");
        record.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        record.setTypeface(Typeface.DEFAULT_BOLD);
        record.setMinHeight(Ui.dp(this, 140));
        record.setOnClickListener(v -> toggleRecord());
        Ui.add(col, record, 12);

        status = Ui.body(this, "");
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        Ui.add(col, status, 6);

        fileArea = Ui.card(this);
        fileLabel = Ui.body(this, "");
        fileArea.addView(fileLabel);
        fileBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        fileBar.setMax(100);
        fileBar.setContentDescription("Transcription progress");
        fileBar.setFocusable(true);
        Ui.add(fileArea, fileBar, 8);
        Ui.add(fileArea, Ui.button(this, "Cancel", v -> FileService.cancel(this)), 8);
        fileArea.setVisibility(View.GONE);
        Ui.add(col, fileArea, 8);

        TextView boxLabel = Ui.subheading(this, "Text");
        col.addView(boxLabel);
        box = new EditText(this);
        box.setId(View.generateViewId());
        boxLabel.setLabelFor(box.getId());
        box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.setMinLines(6);
        box.setGravity(Gravity.TOP | Gravity.START);
        box.setTextColor(Ui.TEXT);
        box.setHintTextColor(Ui.DIM);
        box.setHint("Your text appears here");
        box.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.textSize());
        col.addView(box, Ui.matchWrap(this, 0));
        counter = Ui.text(this, "", 13, Ui.DIM);
        Ui.add(col, counter, 2);
        box.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable e) { updateCounter(); }
        });
        updateCounter();

        LinearLayout r1 = Ui.row(this);
        r1.addView(Ui.button(this, "Copy", v -> {
            Hub.copy(this, box.getText().toString());
            say("Copied");
        }), Ui.weight(1));
        r1.addView(Ui.button(this, "Share", v -> share()), Ui.weight(1));
        r1.addView(Ui.button(this, "Save", v -> saveAsFile()), Ui.weight(1));
        Ui.add(col, r1, 8);
        LinearLayout r2 = Ui.row(this);
        r2.addView(Ui.button(this, "Clear", v -> clear()), Ui.weight(1));
        r2.addView(Ui.button(this, "Undo", v -> undo()), Ui.weight(1));
        Ui.add(col, r2, 0);

        fileButton = Ui.button(this, "Transcribe a file", v -> pickFile());
        Ui.add(col, fileButton, 16);

        translateButton = Ui.button(this, "Translation", v -> startActivity(new Intent(this, TranslationActivity.class)));
        Ui.add(col, translateButton, 8);

        LinearLayout r3 = Ui.row(this);
        r3.addView(Ui.button(this, "Models", v -> startActivity(new Intent(this, ModelsActivity.class))), Ui.weight(1));
        r3.addView(Ui.button(this, "History", v -> startActivity(new Intent(this, HistoryActivity.class))), Ui.weight(1));
        Ui.add(col, r3, 16);
        LinearLayout r4 = Ui.row(this);
        r4.addView(Ui.button(this, "Settings", v -> startActivity(new Intent(this, SettingsActivity.class))), Ui.weight(1));
        r4.addView(Ui.button(this, "About", v -> startActivity(new Intent(this, AboutActivity.class))), Ui.weight(1));
        Ui.add(col, r4, 0);
        Ui.add(col, Ui.button(this, "Use as a keyboard", v -> startActivity(new Intent(this, KeyboardSetupActivity.class))), 0);
    }

    /** Word and character count under the text box. */
    private void updateCounter() {
        if (counter == null || box == null) return;
        String t = box.getText().toString();
        int w = TextExtras.words(t);
        counter.setText(w + (w == 1 ? " word, " : " words, ") + t.length() + (t.length() == 1 ? " character" : " characters"));
    }

    /** The translation line and button: hidden for models that hear only one language. */
    private void updateTranslation() {
        if (translateLine == null) return;
        Models.ModelInfo m = Models.get(prefs.selectedModel());
        boolean multi = Translation.multilingual(m);
        translateButton.setVisibility(multi ? View.VISIBLE : View.GONE);
        Translate.Plan pl = multi ? Translate.plan(this, m) : null;
        translateLine.setVisibility(pl != null ? View.VISIBLE : View.GONE);
        if (pl != null) translateLine.setText(Translate.describe(pl));
        translateButton.setText(pl != null ? "Translation: on" : "Translation: off");
        if (box != null) box.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.textSize());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private static boolean isShare(Intent i) {
        return i != null && (Intent.ACTION_SEND.equals(i.getAction()) || Intent.ACTION_VIEW.equals(i.getAction()));
    }

    @SuppressWarnings("deprecation")
    private void handleIntent(Intent i) {
        if (!isShare(i)) return;
        Uri uri = Intent.ACTION_SEND.equals(i.getAction()) ? (Uri) i.getParcelableExtra(Intent.EXTRA_STREAM) : i.getData();
        if (uri == null) {
            say("That share had no audio file in it.");
            return;
        }
        startFile(uri);
        i.setAction(Intent.ACTION_MAIN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (box == null) return;
        visible = true;
        updateTranslation();
        Hub.addListener(this);
        DownloadService.addListener(this);
        em.addListener(this);
        em.ensureLoaded();
        String saved = prefs.boxText();
        if (!saved.equals(box.getText().toString())) {
            box.setText(saved);
            box.setSelection(box.length());
        }
        onStatus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (box == null) return;
        visible = false;
        prefs.setBoxText(box.getText().toString());
        Hub.removeListener(this);
        DownloadService.removeListener(this);
        em.removeListener(this);
        if (!Hub.recording && !Hub.fileBusy && Hub.finishing == 0) em.maybeUnload();
    }

    // ---- Recording ----

    private void toggleRecord() {
        if (Hub.recording) {
            RecordService.stop(this);
            record.setText("Record");
            return;
        }
        if (ModelStore.downloadedCount(this) == 0) {
            say("No model is downloaded yet. Open Models to download one.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}, REQ_PERMS);
            } else {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_PERMS);
            }
            return;
        }
        RecordService.start(this);
        record.setText("Stop");
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        if (req != REQ_PERMS) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            RecordService.start(this);
            record.setText("Stop");
        } else {
            say("The app needs microphone permission to record. You can allow it in Android settings.");
        }
    }

    // ---- Files ----

    private void pickFile() {
        if (Hub.fileBusy) {
            say("A file is already being transcribed.");
            return;
        }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*", "application/ogg"});
        try {
            startActivityForResult(i, REQ_OPEN);
        } catch (Exception e) {
            say("No file picker found on this phone.");
        }
    }

    private void startFile(Uri uri) {
        if (ModelStore.downloadedCount(this) == 0) {
            say("No model is downloaded yet. Open Models to download one.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && pendingShare == null) {
            // Ask once so the progress notification can show; the work runs either way.
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        }
        if (FileService.start(this, uri, displayName(uri))) {
            say("Transcribing " + Hub.fileName);
        } else if (Hub.fileBusy) {
            say("A file is already being transcribed.");
        }
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String n = c.getString(0);
                if (n != null) return n;
            }
        } catch (Exception ignored) {
        }
        String last = uri.getLastPathSegment();
        return last == null ? "Audio file" : last;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        if (req == REQ_OPEN) startFile(data.getData());
        else if (req == REQ_SAVE) writeText(data.getData());
    }

    // ---- Text box ----

    private void pushUndo() {
        undo.push(box.getText().toString());
        while (undo.size() > 30) undo.removeLast();
    }

    private void clear() {
        if (box.length() == 0) {
            say("The text is already empty.");
            return;
        }
        pushUndo();
        box.setText("");
        prefs.setBoxText("");
        say("Cleared. Undo brings it back.");
    }

    private void undo() {
        String prev = undo.isEmpty() ? null : undo.pop();
        if (prev == null) {
            String saved = prefs.undoText();
            if (saved != null) {
                prev = saved;
                prefs.setUndoText(null);
            }
        }
        if (prev == null) {
            say("Nothing to undo.");
            return;
        }
        box.setText(prev);
        box.setSelection(box.length());
        prefs.setBoxText(prev);
        say("Undone.");
    }

    private void share() {
        String t = box.getText().toString();
        if (t.trim().isEmpty()) {
            say("There is no text to share.");
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t);
        startActivity(Intent.createChooser(i, "Share text"));
    }

    private void saveAsFile() {
        if (box.getText().toString().trim().isEmpty()) {
            say("There is no text to save.");
            return;
        }
        String name = "Dictation " + new SimpleDateFormat("yyyy-MM-dd HHmm", Locale.US).format(new Date()) + ".txt";
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/plain").putExtra(Intent.EXTRA_TITLE, name);
        try {
            startActivityForResult(i, REQ_SAVE);
        } catch (Exception e) {
            say("No file saver found on this phone.");
        }
    }

    private void writeText(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new java.io.IOException();
            out.write(box.getText().toString().getBytes(StandardCharsets.UTF_8));
            say("Saved.");
        } catch (Exception e) {
            say("Could not save the file.");
        }
    }

    private final Runnable clearStatus = () -> { if (status != null) status.setText(""); };

    private void say(String s) {
        status.setText(s);
        handler.removeCallbacks(clearStatus);
        handler.postDelayed(clearStatus, 8000);
    }

    // ---- Listeners ----

    @Override
    public void onStatus() {
        record.setText(Hub.recording ? "Stop" : "Record");
        if (Hub.fileBusy) {
            fileArea.setVisibility(View.VISIBLE);
            fileLabel.setText("Transcribing " + (Hub.fileName == null ? "file" : Hub.fileName));
            fileBar.setProgress(Hub.fileProgress);
            String pct = Hub.fileProgress + " percent";
            if (Build.VERSION.SDK_INT >= 30) fileBar.setStateDescription(pct);
            else fileBar.setContentDescription("Transcription progress, " + pct);
        } else {
            fileArea.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onResult(String text) {
        pushUndo();
        String combined = TextPipeline.combine(box.getText().toString(), text, prefs.replaceText());
        box.setText(combined);
        box.setSelection(box.length());
        prefs.setBoxText(combined);
        if (prefs.autoCopy()) Hub.copy(this, combined);
        return true;
    }

    @Override
    public void onMessage(String message) {
        say(message);
    }

    @Override
    public void onEngineState(EngineManager.State state, Models.ModelInfo m, String error) {
        Models.ModelInfo sel = Models.get(prefs.selectedModel());
        String lang = "";
        if (sel != null && sel.languageChoices != null) lang = ", " + sel.languageLabel(prefs.language(sel));
        switch (state) {
            case LOADING:
                modelLine.setText("Model: " + m.name + lang + ". Loading… You can record while it loads.");
                break;
            case READY:
                modelLine.setText("Model: " + m.name + lang + ". Ready.");
                break;
            case ERROR:
                modelLine.setText("Model: " + m.name + " could not load: " + error);
                break;
            default:
                if (sel != null && DownloadService.isActive(sel.id)) modelLine.setText("Model: " + sel.name + ". Downloading…");
                else if (sel != null && !ModelStore.isDownloaded(this, sel)) modelLine.setText("Model: " + sel.name + " is not downloaded. Open Models.");
                else modelLine.setText("No model loaded.");
        }
    }

    @Override
    public void onDownloadChanged() {
        if (em.state() == EngineManager.State.NONE) onEngineState(EngineManager.State.NONE, null, null);
    }
}
