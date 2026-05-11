package com.leapmotor.c11assistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import com.leapmotor.c11assistant.R;
import com.leapmotor.c11assistant.manager.ActionExecutor;
import com.leapmotor.c11assistant.manager.SharedPreferencesUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogcatMonitorService extends Service {
    private static final String TAG = "LogcatMonitorService";
    private static final String CH = "c11_logcat";
    private final Map<String, Long> lastHit = new HashMap<>();
    private volatile boolean running;

    @Override public void onCreate() {
        super.onCreate();
        ensureChannel();
        Notification n = new Notification.Builder(this, CH)
                .setContentTitle("C11日志监控中")
                .setSmallIcon(android.R.drawable.presence_invisible)
                .setOngoing(true).build();
        startForeground(100, n);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (running) return START_STICKY;
        running = true;
        new Thread(this::readLoop, "c11-logcat").start();
        new Handler(Looper.getMainLooper()).postDelayed(this::scheduleReset, 300_000L);
        return START_STICKY;
    }

    private void scheduleReset() {
        if (!running) return;
        stopSelf();
        startService(new Intent(this, LogcatMonitorService.class));
    }

    private void readLoop() {
        final String cmd = "logcat -v brief -s C11CarSomeIp:D C11AirConditioner:D C11CarXml:D AroundService:I BleControlService:D TripService:I";
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (running && (line = br.readLine()) != null) handleLine(line);
        } catch (Exception e) {
            Log.e(TAG, "logcat loop failed", e);
        }
    }

    private void handleLine(String rawLine) {
        if (!SharedPreferencesUtils.getBoolean(this, "rule_enabled_global", true)) return;
        matchEvent(rawLine, "gear_r", "eventId:1110 value:1", "GEAR_R");
        matchEvent(rawLine, "gear_n", "eventId:1110 value:2", "GEAR_N");
        matchEvent(rawLine, "gear_d", "eventId:1110 value:3", "GEAR_D");
        matchEvent(rawLine, "wheel_360", "WHEEL_360_ID value:48", "WHEEL_360");
        matchEvent(rawLine, "unlock", "eventid:1200 msg:0", "UNLOCK");
        matchEvent(rawLine, "lock", "eventid:1200 msg:1", "LOCK");
        matchEvent(rawLine, "speed10", "node_name:speed setTextContent:10", "SPEED10");
        matchEvent(rawLine, "speed80", "node_name:speed setTextContent:80", "SPEED80");
        matchEvent(rawLine, "sunroof", "eventId:21201", "SUNROOF");
        matchEvent(rawLine, "curtain", "eventId:21207", "CURTAIN");
        matchEvent(rawLine, "seat_main_vent", "sendCarBuffer:1505", "SEAT_MAIN_VENT");
        matchEvent(rawLine, "seat_sub_vent", "sendCarBuffer:1506", "SEAT_SUB_VENT");
        matchEvent(rawLine, "seat_heat", "sendCarBuffer:1518", "SEAT_HEAT");
        for (int i = 9123; i <= 9128; i++) matchEvent(rawLine, "door_" + i, "eventId:" + i + " value:", "DOOR_" + i);

        Matcher m = Pattern.compile("eventId:1108 value:(\\d+)").matcher(rawLine);
        if (m.find()) {
            int rpm = Integer.parseInt(m.group(1));
            int min = SharedPreferencesUtils.getInt(this, "rpm_threshold", 1000);
            if (rpm > min && passDebounce("rpm_high")) ActionExecutor.execute(this, "MOTOR_RPM_HIGH", String.valueOf(rpm));
        }
    }

    private void matchEvent(String rawLine, String key, String needle, String action) {
        if (!SharedPreferencesUtils.getBoolean(this, "rule_" + key, true)) return;
        if (rawLine.contains(needle) && passDebounce(key)) ActionExecutor.execute(this, action, rawLine);
    }

    private boolean passDebounce(String key) {
        long now = System.currentTimeMillis();
        Long last = lastHit.get(key);
        if (last != null && now - last < 500L) return false;
        lastHit.put(key, now);
        return true;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(CH, getString(R.string.service_channel), NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { running = false; super.onDestroy(); }
}
