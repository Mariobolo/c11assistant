package com.zerorun.c11assistant.service;

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
import androidx.annotation.Nullable;
import com.zerorun.c11assistant.manager.ConfigManager;
import com.zerorun.c11assistant.ui.MainActivity;
import org.json.JSONObject;

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
        new Handler(Looper.getMainLooper()).postDelayed(() -> executeForDisplay(display), delay);
        return START_STICKY;
    }

    private void executeForDisplay(int targetDisplay) {
        DisplayManager dm = getSystemService(DisplayManager.class);
        for (Display d : dm.getDisplays()) {
            if (targetDisplay != -1 && d.getDisplayId() != targetDisplay) continue;
            DisplayMetrics m = new DisplayMetrics();
            d.getRealMetrics(m);
            // 预置副屏参考分辨率: 1920x720，可作为固定窗口默认坐标系
            if (m.widthPixels == 1920 && m.heightPixels == 720) { /* 副屏 */ }
            // TODO: 读取对应动作列表并执行: 系统控制/启动App/悬浮图
        }
    }

    private void ensureChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, getString(com.zerorun.c11assistant.R.string.service_channel), NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        nm.createNotificationChannel(ch);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
