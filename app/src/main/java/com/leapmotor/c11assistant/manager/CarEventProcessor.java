package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.leapmotor.c11assistant.model.CarEvent;
import com.leapmotor.c11assistant.model.ActionItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarEventProcessor {

    private static final String TAG = "CarEventProcessor";
    private static CarEventProcessor instance;

    private final Context app;
    private final Map<String, Long> lastEventTimestamps = new HashMap<>();
    private static final long DEFAULT_DEBOUNCE_MS = 500;

    private final List<EventActionRule> rules = new ArrayList<>();

    public static synchronized CarEventProcessor get(Context context) {
        if (instance == null) {
            instance = new CarEventProcessor(context.getApplicationContext());
        }
        return instance;
    }

    private CarEventProcessor(Context context) {
        this.app = context;
        initBuiltInRules();
    }

    private void initBuiltInRules() {
        rules.clear();
        addRule(CarEvent.GEAR_R.getEventId(), "rule_tts_gear_r", true, 500,
                newEventAction("SPEECH_SPEAK", "倒车请注意"));
        addRule(CarEvent.GEAR_D.getEventId(), "rule_tts_gear_d", true, 500,
                newEventAction("SPEECH_SPEAK", "起飞"));
        addRule(CarEvent.TURN_LEFT_ON.getEventId(), "rule_around_turn_left", true, 300,
                newEventAction("AROUND_TOGGLE_VIEW", ""));
        addRule(CarEvent.TURN_RIGHT_ON.getEventId(), "rule_around_turn_right", true, 300,
                newEventAction("AROUND_TOGGLE_VIEW", ""));
        addRule(CarEvent.VEHICLE_LOCK.getEventId(), "rule_child_lock_on", true, 1000,
                newEventAction("CHILD_LOCK_ON", ""));
        addRule(CarEvent.VEHICLE_UNLOCK.getEventId(), "rule_child_lock_off", true, 1000,
                newEventAction("CHILD_LOCK_OFF", ""));
        addRule(CarEvent.WHEEL_360.getEventId(), "rule_360_wheel", true, 300,
                newEventAction("AROUND_TOGGLE_VIEW", ""));
    }

    private void addRule(String eventId, String prefKey, boolean defaultEnabled, long debounceMs, EventAction... actions) {
        EventActionRule rule = new EventActionRule();
        rule.eventId = eventId;
        rule.prefKey = prefKey;
        rule.defaultEnabled = defaultEnabled;
        rule.debounceMs = debounceMs;
        for (EventAction action : actions) {
            rule.actions.add(action);
        }
        rules.add(rule);
    }

    private EventAction newEventAction(String actionType, String payload) {
        EventAction action = new EventAction();
        action.actionType = actionType;
        action.payload = payload;
        return action;
    }

    public void processEvent(String eventId, String rawLog) {
        if (TextUtils.isEmpty(eventId)) return;

        if (!passDebounce(eventId)) {
            Log.d(TAG, "Event debounced: " + eventId);
            return;
        }

        CarEvent carEvent = CarEvent.fromEventId(eventId);
        if (carEvent != null) {
            Log.i(TAG, "Processing event: " + carEvent.getDisplayName() + " (" + eventId + ")");
        } else {
            Log.i(TAG, "Processing event: " + eventId);
        }

        for (EventActionRule rule : rules) {
            if (rule.eventId.equals(eventId)) {
                executeRule(rule, rawLog);
            }
        }

        // 执行自定义任务
        TaskManager.get(app).processLogEvent(eventId, rawLog);

        AutomationManager.get(app).onEvent(eventId, rawLog);
    }

    private void executeRule(EventActionRule rule, String rawLog) {
        boolean enabled = SharedPreferencesUtils.getBoolean(app, rule.prefKey, rule.defaultEnabled);
        if (!enabled) {
            Log.d(TAG, "Rule disabled: " + rule.prefKey);
            return;
        }

        Log.i(TAG, "Executing rule: " + rule.prefKey);

        for (EventAction action : rule.actions) {
            if ("SPEECH_SPEAK".equals(action.actionType)) {
                TtsManager.get(app).speak(action.payload);
            } else {
                ActionExecutor.execute(app, action.actionType, action.payload);
            }
        }
    }

    private boolean passDebounce(String eventId) {
        long now = System.currentTimeMillis();
        Long last = lastEventTimestamps.get(eventId);
        if (last != null && now - last < DEFAULT_DEBOUNCE_MS) {
            return false;
        }
        lastEventTimestamps.put(eventId, now);
        return true;
    }

    private static class EventActionRule {
        String eventId;
        String prefKey;
        boolean defaultEnabled;
        long debounceMs = DEFAULT_DEBOUNCE_MS;
        List<EventAction> actions = new ArrayList<>();
    }

    private static class EventAction {
        String actionType;
        String payload;
        long delayMs = 0;
    }
}
