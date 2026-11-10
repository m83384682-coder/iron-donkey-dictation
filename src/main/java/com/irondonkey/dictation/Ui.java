package com.irondonkey.dictation;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small helpers for building the screens in code. */
final class Ui {
    private Ui() {}

    static final int BG = 0xFF121417;
    static final int CARD = 0xFF1E2228;
    static final int ACCENT = 0xFFE0A030;
    static final int TEXT = 0xFFF2F2F2;
    static final int DIM = 0xFFB8BDC4;

    static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    /** A scrolling page with a vertical column; returns the column. */
    static LinearLayout page(Activity a, String title) {
        a.setTitle(title);
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        LinearLayout col = column(a);
        int p = dp(a, 16);
        col.setPadding(p, p, p, p);
        sv.addView(col);
        a.setContentView(sv);
        applyInsets(sv);
        return col;
    }

    static void applyInsets(View root) {
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets i = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.ime() | android.view.WindowInsets.Type.displayCutout());
                v.setPadding(i.left, i.top, i.right, i.bottom);
                return insets;
            });
        }
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static TextView heading(Context c, String s) {
        TextView t = text(c, s, 22, TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        if (Build.VERSION.SDK_INT >= 28) t.setAccessibilityHeading(true);
        t.setPadding(0, dp(c, 8), 0, dp(c, 8));
        return t;
    }

    static TextView subheading(Context c, String s) {
        TextView t = text(c, s, 18, ACCENT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        if (Build.VERSION.SDK_INT >= 28) t.setAccessibilityHeading(true);
        t.setPadding(0, dp(c, 16), 0, dp(c, 4));
        return t;
    }

    static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    static TextView body(Context c, String s) {
        TextView t = text(c, s, 16, DIM);
        t.setPadding(0, dp(c, 2), 0, dp(c, 2));
        return t;
    }

    static Button button(Context c, String label, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setMinHeight(dp(c, 48));
        b.setOnClickListener(l);
        return b;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = column(c);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(c, 12));
        l.setBackground(g);
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        return l;
    }

    static LinearLayout.LayoutParams matchWrap(Context c, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topDp);
        return lp;
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    static void add(LinearLayout parent, View v, int topDp) {
        parent.addView(v, matchWrap(parent.getContext(), topDp));
    }

    static int transparent() { return Color.TRANSPARENT; }
}
