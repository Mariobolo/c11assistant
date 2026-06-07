package com.leapmotor.c11assistant.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.leapmotor.c11assistant.manager.AutomationManager;

public class AutomationEventReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!AutomationManager.ACTION_EVENT_TRIGGERED.equals(intent.getAction())) return;
        String event = intent.getStringExtra(AutomationManager.EXTRA_EVENT);
        String raw = intent.getStringExtra(AutomationManager.EXTRA_RAW);
        AutomationManager.get(context).onEvent(event, raw);
    }
}
