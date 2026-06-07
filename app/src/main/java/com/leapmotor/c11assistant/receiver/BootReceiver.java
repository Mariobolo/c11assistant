package com.leapmotor.c11assistant.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.leapmotor.c11assistant.manager.SharedPreferencesUtils;
import com.leapmotor.c11assistant.service.C11ForegroundService;
import com.leapmotor.c11assistant.service.FloatBallService;
import com.leapmotor.c11assistant.service.LogcatMonitorService;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
            if (!SharedPreferencesUtils.getBoolean(context, "auto_start", true)) return;
            C11ForegroundService.start(context, true, -1);
            context.startService(new Intent(context, LogcatMonitorService.class));
            context.startService(new Intent(context, FloatBallService.class));
        }
    }
}
