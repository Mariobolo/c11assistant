package com.leapmotor.c11assistant.manager;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;

public class MultiScreenManager {
    private static MultiScreenManager sInstance;
    private final Context app;

    private MultiScreenManager(Context context) { this.app = context.getApplicationContext(); }

    public static synchronized MultiScreenManager get(Context context) {
        if (sInstance == null) sInstance = new MultiScreenManager(context);
        return sInstance;
    }

    public boolean launchOnSecondary(String packageName) {
        try {
            Intent launch = app.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            int displayId = findSecondaryDisplayId();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && displayId >= 0) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchDisplayId(displayId);
                app.startActivity(launch, options.toBundle());
            } else {
                app.startActivity(launch);
            }
            return true;
        } catch (Exception e) { return false; }
    }

    public int findSecondaryDisplayId() {
        DisplayManager dm = app.getSystemService(DisplayManager.class);
        if (dm == null) return -1;
        for (Display d : dm.getDisplays()) {
            if (d.getDisplayId() == Display.DEFAULT_DISPLAY) continue;
            DisplayMetrics m = new DisplayMetrics();
            d.getRealMetrics(m);
            if (m.widthPixels >= 1000) return d.getDisplayId();
        }
        return -1;
    }

    public boolean launchPackage(String packageName) {
        try {
            PackageManager pm = app.getPackageManager();
            Intent i = pm.getLaunchIntentForPackage(packageName);
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(i);
            return true;
        } catch (Exception e) { return false; }
    }
}
