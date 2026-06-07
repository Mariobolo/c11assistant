package com.leapmotor.c11assistant.service;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.support.annotation.Nullable;
import com.leapmotor.c11assistant.manager.ActionSequenceExecutor;
import com.leapmotor.c11assistant.manager.ConfigManager;
import com.leapmotor.c11assistant.model.ActionItem;
import com.leapmotor.c11assistant.model.ScreenConfig;
import com.leapmotor.c11assistant.ui.MainActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class C11ForegroundService extends Service {
    private static final String CHANNEL = "c11_bg";
    private static final String EXTRA_BOOT = "boot";
    private static final String EXTRA_DISPLAY = "display";

    public static void start(Context c, boolean bootFlow, int displayId) {
        Intent i = new Intent(c, C11ForegroundService.class).putExtra(EXTRA_BOOT, bootFlow).putExtra(EXTRA_DISPLAY, displayId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i); else c.startService(i);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        ensureChannel();
        PendingIntent pi = PendingIntent.getActivity(this, 100, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle("C11助手运行中")
                .setSmallIcon(android.R.drawable.presence_invisible)
                .setContentIntent(pi)
                .setOngoing(true).build();
        startForeground(99, n);

        JSONObject cfg = new ConfigManager(this).load();
        boolean boot = intent != null && intent.getBooleanExtra(EXTRA_BOOT, false);
        int display = intent != null ? intent.getIntExtra(EXTRA_DISPLAY, -1) : -1;
        long delay = boot ? cfg.optLong("bootDelaySec", 15) * 1000L : 0L;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            executeForDisplay(display);
            startService(new Intent(this, LogcatMonitorService.class));
        }, delay);
        return START_STICKY;
    }

    private void executeForDisplay(int targetDisplay) {
        JSONObject cfg = new ConfigManager(this).load();
        List<ActionItem> actions = new ArrayList<>();
        JSONArray screens = cfg.optJSONArray("screens");
        if (screens != null) {
            for (int i = 0; i < screens.length(); i++) {
                JSONObject screenJson = screens.optJSONObject(i);
                if (screenJson == null) continue;
                ScreenConfig screen = ScreenConfig.fromJson(screenJson);
                if (targetDisplay != -1 && screen.displayId != -1 && screen.displayId != targetDisplay) continue;
                actions.addAll(screen.actions);
            }
        }
        if (actions.isEmpty()) actions.addAll(ActionSequenceExecutor.actionsFromConfig(cfg));
        if (!actions.isEmpty()) ActionSequenceExecutor.executeSequence(this, actions);

        DisplayManager dm = getSystemService(DisplayManager.class);
        if (dm == null) return;
        for (Display d : dm.getDisplays()) {
            if (targetDisplay != -1 && d.getDisplayId() != targetDisplay) continue;
            DisplayMetrics m = new DisplayMetrics();
            d.getRealMetrics(m);
            if (m.widthPixels == 1920 && m.heightPixels == 720) {
                android.util.Log.i("C11ForegroundService", "detected C11 secondary display: " + d.getDisplayId());
            }
        }
    }

    private void ensureChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, getString(com.leapmotor.c11assistant.R.string.service_channel), NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        nm.createNotificationChannel(ch);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
