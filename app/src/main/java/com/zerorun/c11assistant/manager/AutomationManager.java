package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class AutomationManager {
    public static final String ACTION_EVENT_TRIGGERED = "com.leapmotor.c11assistant.EVENT_TRIGGERED";
    public static final String EXTRA_EVENT = "event";
    public static final String EXTRA_RAW = "raw";

    private static AutomationManager sInstance;
    private final Context app;
    private String lastPackage = "";

    private AutomationManager(Context context) { this.app = context.getApplicationContext(); }

    public static synchronized AutomationManager get(Context context) {
        if (sInstance == null) sInstance = new AutomationManager(context);
        return sInstance;
    }

    public void onEvent(String event, String raw) {
        if (TextUtils.isEmpty(event)) return;
        if ("TURN_LEFT_ON".equals(event) || "TURN_RIGHT_ON".equals(event)) {
            if (SharedPreferencesUtils.getBoolean(app, "rule_turn_on_around", true)) {
                lastPackage = "";
                MultiScreenManager.get(app).launchPackage("com.leapmotor.aroundview");
            }
            return;
        }
        if ("TURN_LEFT_OFF".equals(event) || "TURN_RIGHT_OFF".equals(event)) {
            if (SharedPreferencesUtils.getBoolean(app, "rule_turn_off_back", true) && !TextUtils.isEmpty(lastPackage)) {
                MultiScreenManager.get(app).launchPackage(lastPackage);
            }
            return;
        }
        if ("LOCK".equals(event) && SharedPreferencesUtils.getBoolean(app, "rule_lock_child", true)) {
            ActionExecutor.execute(app, "CHILD_LOCK_ON", raw); return;
        }
        if ("UNLOCK".equals(event) && SharedPreferencesUtils.getBoolean(app, "rule_unlock_child", true)) {
            ActionExecutor.execute(app, "CHILD_LOCK_OFF", raw); return;
        }
        if ("WHEEL_360".equals(event) && SharedPreferencesUtils.getBoolean(app, "rule_wheel_360_toggle", true)) {
            ActionExecutor.execute(app, "AROUND_TOGGLE_VIEW", raw);
        }
    }

    public void publishEvent(String event, String raw) {
        Intent i = new Intent(ACTION_EVENT_TRIGGERED);
        i.putExtra(EXTRA_EVENT, event);
        i.putExtra(EXTRA_RAW, raw);
        app.sendBroadcast(i);
    }
}
