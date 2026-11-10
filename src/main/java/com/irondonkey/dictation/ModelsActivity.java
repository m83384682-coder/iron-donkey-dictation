package com.irondonkey.dictation;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Every model: what it is good for, its status, and Download, Delete, Use and Language. */
public class ModelsActivity extends Activity implements DownloadService.Listener, EngineManager.Listener {
    private Prefs prefs;
    private EngineManager em;
    private final List<Holder> holders = new ArrayList<>();

    private final class Holder {
        final Models.ModelInfo m;
        TextView title, status;
        Button use, download, delete, language;
        String lastStatus = "";
        int lastDecile = -1;

        Holder(Models.ModelInfo m, LinearLayout parent) {
            this.m = m;
            LinearLayout card = Ui.card(ModelsActivity.this);
            title = Ui.subheading(ModelsActivity.this, m.name);
            title.setPadding(0, 0, 0, Ui.dp(ModelsActivity.this, 4));
            card.addView(title);
            card.addView(Ui.body(ModelsActivity.this, m.description));
            card.addView(Ui.body(ModelsActivity.this, "Languages: " + m.languages));
            card.addView(Ui.body(ModelsActivity.this, "Size: " + ModelStore.formatSize(m.totalBytes)));
            card.addView(Ui.body(ModelsActivity.this, "Speed: " + m.speed));
            status = Ui.body(ModelsActivity.this, "");
            status.setTextColor(Ui.TEXT);
            Ui.add(card, status, 6);
            use = Ui.button(ModelsActivity.this, "Use this model", v -> use());
            Ui.add(card, use, 6);
            language = Ui.button(ModelsActivity.this, "", v -> pickLanguage());
            Ui.add(card, language, 0);
            LinearLayout r = Ui.row(ModelsActivity.this);
            download = Ui.button(ModelsActivity.this, "Download", v -> toggleDownload());
            delete = Ui.button(ModelsActivity.this, "Delete", v -> confirmDelete());
            r.addView(download, Ui.weight(1));
            r.addView(delete, Ui.weight(1));
            Ui.add(card, r, 0);
            Ui.add(parent, card, 12);
        }

        void update() {
            boolean have = ModelStore.isDownloaded(ModelsActivity.this, m);
            boolean active = DownloadService.isActive(m.id);
            boolean selected = m.id.equals(prefs.selectedModel());
            String s;
            if (active) {
                int pm = DownloadService.permille(m.id);
                if (!DownloadService.isRunning(m.id)) s = "Waiting to download";
                else if (pm < 0) s = "Checking the files";
                else {
                    int dec = pm / 100;
                    if (dec == lastDecile && lastStatus.startsWith("Status: Downloading")) return; // keep TalkBack quiet between steps
                    lastDecile = dec;
                    s = "Downloading, " + (pm / 10) + " percent";
                }
            } else if (have) {
                Models.ModelInfo cur = em.currentModel();
                if (selected && cur == m && em.state() == EngineManager.State.READY) s = "Loaded, in use";
                else if (selected && cur == m && em.state() == EngineManager.State.LOADING) s = "Loading, in use";
                else if (selected) s = "Downloaded, in use";
                else s = "Downloaded";
            } else {
                String err = DownloadService.error(m.id);
                if (err != null) s = "Download stopped: " + err;
                else if (ModelStore.bytesPresent(ModelsActivity.this, m) > 0) s = "Partly downloaded. Download resumes it.";
                else s = "Not downloaded";
            }
            s = "Status: " + s;
            if (!s.equals(lastStatus)) {
                lastStatus = s;
                status.setText(s);
            }
            title.setText(m.name + (selected ? " (in use)" : ""));
            use.setVisibility(have && !selected ? View.VISIBLE : View.GONE);
            use.setContentDescription("Use " + m.name);
            download.setVisibility(have ? View.GONE : View.VISIBLE);
            download.setText(active ? "Cancel download" : "Download");
            download.setContentDescription((active ? "Cancel download of " : "Download ") + m.name);
            boolean partial = !have && ModelStore.bytesPresent(ModelsActivity.this, m) > 0;
            delete.setVisibility((have || partial) && !active ? View.VISIBLE : View.GONE);
            delete.setContentDescription("Delete " + m.name);
            if (m.languageChoices != null) {
                language.setVisibility(View.VISIBLE);
                language.setText("Language: " + m.languageLabel(prefs.language(m)));
                language.setContentDescription("Language for " + m.name + ": " + m.languageLabel(prefs.language(m)));
            } else {
                language.setVisibility(View.GONE);
            }
        }

        void use() {
            if (em.inUse()) {
                toast("Wait until the current recording or file is finished.");
                return;
            }
            prefs.setSelectedModel(m.id);
            em.ensureLoaded();
            toast("Switching to " + m.name + ". It is loading.");
            refresh();
        }

        void toggleDownload() {
            if (DownloadService.isActive(m.id)) DownloadService.cancel(m.id);
            else DownloadService.start(ModelsActivity.this, m.id);
            refresh();
        }

        void confirmDelete() {
            new AlertDialog.Builder(ModelsActivity.this)
                    .setTitle("Delete " + m.name + "?")
                    .setMessage("This frees " + ModelStore.formatSize(m.totalBytes) + ". You can download it again later.")
                    .setPositiveButton("Delete", (d, w) -> delete())
                    .setNegativeButton("Keep", null)
                    .show();
        }

        void delete() {
            boolean selected = m.id.equals(prefs.selectedModel());
            if (selected && em.inUse()) {
                toast("Wait until the current recording or file is finished.");
                return;
            }
            if (selected) em.unload();
            new Thread(() -> {
                // Let the unload finish before the files go.
                try { Thread.sleep(300); } catch (InterruptedException ignored) { }
                ModelStore.delete(ModelsActivity.this, m);
                runOnUiThread(() -> {
                    if (selected) {
                        for (Models.ModelInfo o : Models.ALL) {
                            if (ModelStore.isDownloaded(ModelsActivity.this, o)) {
                                prefs.setSelectedModel(o.id);
                                em.ensureLoaded();
                                toast(m.name + " deleted. Now using " + o.name + ".");
                                refresh();
                                return;
                            }
                        }
                    }
                    toast(m.name + " deleted.");
                    refresh();
                });
            }).start();
        }

        void pickLanguage() {
            String[] labels = new String[m.languageChoices.length];
            int checked = 0;
            String cur = prefs.language(m);
            for (int i = 0; i < labels.length; i++) {
                labels[i] = m.languageChoices[i][1];
                if (m.languageChoices[i][0].equals(cur)) checked = i;
            }
            new AlertDialog.Builder(ModelsActivity.this)
                    .setTitle("Language for " + m.name)
                    .setSingleChoiceItems(labels, checked, (d, which) -> {
                        prefs.setLanguage(m, m.languageChoices[which][0]);
                        em.languageChanged();
                        d.dismiss();
                        refresh();
                        if ("ur".equals(m.languageChoices[which][0])) {
                            new AlertDialog.Builder(ModelsActivity.this)
                                    .setTitle("Urdu is experimental")
                                    .setMessage(Urdu.WARNING)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private TextView note;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        em = EngineManager.get(this);
        LinearLayout col = Ui.page(this, "Models");
        col.addView(Ui.heading(this, "Models"));
        col.addView(Ui.body(this, "Only the model in use is kept in memory. Speeds were measured on a fast computer; "
                + "a phone will be slower."));
        note = Ui.body(this, "");
        note.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Ui.add(col, note, 4);
        Ui.add(col, Ui.button(this, "Download all", v -> downloadAll()), 8);
        for (Models.ModelInfo m : Models.ALL) holders.add(new Holder(m, col));
    }

    private void downloadAll() {
        long need = 0;
        List<Models.ModelInfo> todo = new ArrayList<>();
        for (Models.ModelInfo m : Models.ALL) {
            if (!ModelStore.isDownloaded(this, m) && !DownloadService.isActive(m.id)) {
                todo.add(m);
                need += m.totalBytes - ModelStore.bytesPresent(this, m);
            }
        }
        if (todo.isEmpty()) {
            toast("Every model is already downloaded or downloading.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Download all models?")
                .setMessage(todo.size() + " models, about " + ModelStore.formatSize(need) + ". Wi-Fi is best.")
                .setPositiveButton("Download", (d, w) -> {
                    for (Models.ModelInfo m : todo) DownloadService.start(this, m.id);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toast(String s) {
        note.setText(s);
    }

    private void refresh() {
        for (Holder h : holders) h.update();
    }

    @Override
    protected void onStart() {
        super.onStart();
        DownloadService.addListener(this);
        em.addListener(this);
        refresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DownloadService.removeListener(this);
        em.removeListener(this);
    }

    @Override
    public void onDownloadChanged() { refresh(); }

    @Override
    public void onEngineState(EngineManager.State state, Models.ModelInfo model, String error) {
        if (state == EngineManager.State.ERROR && model != null) toast(model.name + " could not load: " + error);
        refresh();
    }
}
