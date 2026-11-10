package com.irondonkey.dictation;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

public class HistoryActivity extends Activity {
    private LinearLayout list;
    private TextView note;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout col = Ui.page(this, "History");
        col.addView(Ui.heading(this, "History"));
        note = Ui.body(this, "");
        note.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        col.addView(note);
        Ui.add(col, Ui.button(this, "Delete all history", v -> new AlertDialog.Builder(this)
                .setTitle("Delete all history?")
                .setPositiveButton("Delete all", (d, w) -> { History.clear(this); fill(); note.setText("History deleted."); })
                .setNegativeButton("Keep", null)
                .show()), 4);
        list = Ui.column(this);
        Ui.add(col, list, 8);
        fill();
    }

    private void fill() {
        list.removeAllViews();
        List<History.Entry> entries = History.load(this);
        if (!new Prefs(this).history()) note.setText("History is turned off in Settings.");
        if (entries.isEmpty()) {
            list.addView(Ui.body(this, "No transcripts yet."));
            return;
        }
        java.text.DateFormat df = DateFormat.getMediumDateFormat(this);
        java.text.DateFormat tf = DateFormat.getTimeFormat(this);
        for (History.Entry e : entries) {
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
            android.widget.Button del = Ui.button(this, "Delete", v -> { History.delete(this, e.time); fill(); note.setText("Deleted."); });
            del.setContentDescription("Delete transcript from " + when);
            r.addView(copy, Ui.weight(1));
            r.addView(del, Ui.weight(1));
            Ui.add(card, r, 4);
            Ui.add(list, card, 10);
        }
    }
}
