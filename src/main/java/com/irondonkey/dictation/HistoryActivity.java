package com.irondonkey.dictation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {
    private static final int REQ_EXPORT = 1;
    private static final int SHOW_MAX = 100;

    private LinearLayout list;
    private TextView note;
    private EditText search;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout col = Ui.page(this, "History");
        col.addView(Ui.heading(this, "History"));
        note = Ui.body(this, "");
        note.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        col.addView(note);

        search = new EditText(this);
        search.setHint("Search history");
        search.setContentDescription("Search history");
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.DIM);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int bf, int c) {}
            @Override public void afterTextChanged(Editable s) { fill(); }
        });
        Ui.add(col, search, 4);

        LinearLayout r = Ui.row(this);
        r.addView(Ui.button(this, "Save all as a text file", v -> exportAll()), Ui.weight(1));
        r.addView(Ui.button(this, "Delete all history", v -> new AlertDialog.Builder(this)
                .setTitle("Delete all history?")
                .setPositiveButton("Delete all", (d, w) -> { History.clear(this); fill(); note.setText("History deleted."); })
                .setNegativeButton("Keep", null)
                .show()), Ui.weight(1));
        Ui.add(col, r, 4);
        list = Ui.column(this);
        Ui.add(col, list, 8);
        fill();
    }

    private void fill() {
        list.removeAllViews();
        List<History.Entry> all = History.load(this);
        if (!new Prefs(this).history()) note.setText("History is turned off in Settings.");
        if (all.isEmpty()) {
            list.addView(Ui.body(this, "No transcripts yet."));
            return;
        }
        String q = search == null ? "" : search.getText().toString();
        List<History.Entry> entries = new ArrayList<>();
        for (History.Entry e : all) if (HistoryText.matches(q, e.text, e.model, e.source)) entries.add(e);
        if (!q.trim().isEmpty()) {
            note.setText(entries.isEmpty() ? "Nothing matches \"" + q.trim() + "\"."
                    : entries.size() + (entries.size() == 1 ? " transcript matches." : " transcripts match."));
        }
        java.text.DateFormat df = DateFormat.getMediumDateFormat(this);
        java.text.DateFormat tf = DateFormat.getTimeFormat(this);
        int shown = 0;
        for (History.Entry e : entries) {
            if (shown++ >= SHOW_MAX) {
                list.addView(Ui.body(this, "Showing the newest " + SHOW_MAX + ". Search to find older ones, or save all as a text file."));
                break;
            }
            LinearLayout card = Ui.card(this);
            Date d = new Date(e.time);
            String when = df.format(d) + ", " + tf.format(d);
            TextView head = Ui.subheading(this, when);
            head.setPadding(0, 0, 0, Ui.dp(this, 2));
            card.addView(head);
            card.addView(Ui.body(this, e.source + (e.model.isEmpty() ? "" : ", " + e.model)));
            TextView t = Ui.text(this, e.text, 16, Ui.TEXT);
            t.setTextIsSelectable(true);
            Ui.add(card, t, 6);
            LinearLayout r = Ui.row(this);
            android.widget.Button copy = Ui.button(this, "Copy", v -> { Hub.copy(this, e.text); note.setText("Copied."); });
            copy.setContentDescription("Copy transcript from " + when);
            android.widget.Button share = Ui.button(this, "Share", v -> share(e.text));
            share.setContentDescription("Share transcript from " + when);
            android.widget.Button del = Ui.button(this, "Delete", v -> { History.delete(this, e.time); fill(); note.setText("Deleted."); });
            del.setContentDescription("Delete transcript from " + when);
            r.addView(copy, Ui.weight(1));
            r.addView(share, Ui.weight(1));
            r.addView(del, Ui.weight(1));
            Ui.add(card, r, 4);
            Ui.add(list, card, 10);
        }
    }

    private void share(String text) {
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(i, "Share transcript"));
        } catch (Exception ex) {
            note.setText("No app found to share with.");
        }
    }

    private void exportAll() {
        if (History.load(this).isEmpty()) {
            note.setText("There's no history to save.");
            return;
        }
        String name = "Dictation history " + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()) + ".txt";
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/plain").putExtra(Intent.EXTRA_TITLE, name);
        try {
            startActivityForResult(i, REQ_EXPORT);
        } catch (Exception e) {
            note.setText("No file saver found on this phone.");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_EXPORT || res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        List<History.Entry> all = History.load(this);
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new java.io.IOException("no stream");
            StringBuilder b = new StringBuilder();
            for (History.Entry e : all) b.append(HistoryText.entry(e.time, e.model, e.source, e.text));
            out.write(b.toString().getBytes(StandardCharsets.UTF_8));
            note.setText("Saved " + all.size() + (all.size() == 1 ? " transcript." : " transcripts."));
        } catch (Exception e) {
            ErrorLog.record(this, "Save history", e);
            note.setText("Could not save the file.");
        }
    }
}
