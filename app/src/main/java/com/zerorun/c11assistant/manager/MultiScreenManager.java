package com.leapmotor.c11assistant.manager;

import android.app.ActivityOptions;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.view.Display;

public class MultiScreenManager {
    private static final String TAG = "MultiScreenManager";
    private static MultiScreenManager sInstance;
    private final Context app;

    private MultiScreenManager(Context context) { this.app = context.getApplicationContext(); }

    public static synchronized MultiScreenManager get(Context context) {
        if (sInstance == null) sInstance = new MultiScreenManager(context);
        return sInstance;
    }

    public boolean launchOnSecondary(String packageName) {
        int displayId = findSecondaryDisplayId();
        return launchOnDisplay(packageName, displayId);
    }

    public boolean launchOnDisplay(String packageName, int displayId) {
        try {
            Intent launch = app.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && displayId >= 0) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchDisplayId(displayId);
                app.startActivity(launch, options.toBundle());
            } else {
                app.startActivity(launch);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "launchOnDisplay failed: " + packageName + " display=" + displayId, e);
            return false;
        }
    }

    public boolean launchFreeform(String packageName, Rect bounds, int displayId) {
        try {
            Intent launch = app.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
            ActivityOptions options = ActivityOptions.makeBasic();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && displayId >= 0) options.setLaunchDisplayId(displayId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && bounds != null) options.setLaunchBounds(bounds);
            app.startActivity(launch, options.toBundle());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "launchFreeform failed", e);
            return false;
        }
    }

    public PictureInPictureParams buildPictureInPictureParams(int width, int height) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null;
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        return new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(safeWidth, safeHeight))
                .build();
    }

    public Display[] getDisplays() {
        DisplayManager dm = (DisplayManager) app.getSystemService(Context.DISPLAY_SERVICE);
        return dm == null ? new Display[0] : dm.getDisplays();
    }

    public int findSecondaryDisplayId() {
        DisplayManager dm = (DisplayManager) app.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) return -1;
        for (Display d : dm.getDisplays()) {
            if (d.getDisplayId() == Display.DEFAULT_DISPLAY) continue;
            DisplayMetrics m = new DisplayMetrics();
            d.getRealMetrics(m);
            if (m.widthPixels >= 1000 || m.heightPixels >= 600) return d.getDisplayId();
        }
        return -1;
    }

    public boolean launchPackage(String packageName) {
        try {
            PackageManager pm = app.getPackageManager();
            Intent i = pm.getLaunchIntentForPackage(packageName);
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            app.startActivity(i);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "launchPackage failed: " + packageName, e);
            return false;
        }
    }
}
