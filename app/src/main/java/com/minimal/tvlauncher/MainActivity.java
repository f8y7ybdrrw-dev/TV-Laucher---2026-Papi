package com.minimal.tvlauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private TextView clock;
    private TextView date;
    private LinearLayout row;
    private final Handler handler = new Handler();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
            date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
            handler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(64), dp(48), dp(64), dp(48));

        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(72);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        root.addView(clock);

        date = new TextView(this);
        date.setTextColor(0xFF8A8A8A);
        date.setTextSize(22);
        root.addView(date);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);

        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), dp(16), dp(8), dp(16));
        scroll.addView(row);

        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public void onBackPressed() {
        // Stay on the launcher.
    }

    private void loadApps() {
        row.removeAllViews();
        PackageManager pm = getPackageManager();

        List<ResolveInfo> all = new ArrayList<ResolveInfo>();
        Set<String> seen = new HashSet<String>();
        String[] cats = {
                Intent.CATEGORY_LEANBACK_LAUNCHER,
                Intent.CATEGORY_LAUNCHER
        };
        for (String cat : cats) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (seen.add(pkg)) all.add(ri);
            }
        }
        Collections.sort(all, new ResolveInfo.DisplayNameComparator(pm));

        View first = null;
        for (ResolveInfo ri : all) {
            View tile = makeTile(pm, ri);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(16);
            row.addView(tile, lp);
            if (first == null) first = tile;
        }
        if (first != null) first.requestFocus();
    }

    private View makeTile(PackageManager pm, final ResolveInfo ri) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(16), dp(16), dp(16), dp(16));
        tile.setFocusable(true);
        tile.setClickable(true);
        setBg(tile, false);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(ri.loadIcon(pm));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(80), dp(80)));

        TextView label = new TextView(this);
        label.setText(ri.loadLabel(pm));
        label.setTextColor(Color.WHITE);
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                dp(110), ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(10);
        tile.addView(label, llp);

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                setBg(v, hasFocus);
                float s = hasFocus ? 1.1f : 1f;
                v.animate().scaleX(s).scaleY(s).setDuration(120).start();
            }
        });

        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.setComponent(new ComponentName(
                        ri.activityInfo.packageName, ri.activityInfo.name));
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            }
        });

        return tile;
    }

    private void setBg(View v, boolean focused) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(focused ? 0x33FFFFFF : Color.TRANSPARENT);
        g.setStroke(dp(2), focused ? Color.WHITE : Color.TRANSPARENT);
        v.setBackground(g);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
