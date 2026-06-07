package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.leapmotor.c11assistant.model.ActionItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutomationManager {
    public static final String ACTION_EVENT_TRIGGERED = "com.leapmotor.c11assistant.EVENT_TRIGGERED";
    public static final String EXTRA_EVENT = "event";
    public static final String EXTRA_RAW = "raw";

    private static final String TAG = "AutomationManager";
    private static final long DEFAULT_CONFLICT_WINDOW_MS = 500L;

    private static AutomationManager sInstance;
    private final Context app;
    private final List<AutomationRule> rules = new ArrayList<>();
    private final Map<String, Long> lastExecutionMap = new HashMap<>();
    private String lastPackage = "";
    private boolean rulesLoaded;

    private AutomationManager(Context context) { this.app = context.getApplicationContext(); }

    public static synchronized AutomationManager get(Context context) {
        if (sInstance == null) sInstance = new AutomationManager(context);
        return sInstance;
    }

    public synchronized void reloadRules() {
        rules.clear();
        loadBuiltInRules();
        loadCustomRules();
        Collections.sort(rules, (a, b) -> b.priority - a.priority);
        rulesLoaded = true;
        Log.i(TAG, "rules loaded: " + rules.size());
    }

    public void onEvent(String event, String raw) {
        if (TextUtils.isEmpty(event)) return;
        ensureRules();
        List<AutomationRule> matched = new ArrayList<>();
        synchronized (this) {
            for (AutomationRule rule : rules) {
                if (rule.matches(event, null, null)) matched.add(rule);
            }
        }
        if (!matched.isEmpty()) {
            executeMatchedRules(event, matched);
            return;
        }
        executeLegacyEvent(event, raw);
    }

    public void onAccessibilityWindowChanged(String packageName, String className, String scene) {
        ensureRules();
        if (!TextUtils.isEmpty(packageName)) lastPackage = packageName;
        List<AutomationRule> matched = new ArrayList<>();
        synchronized (this) {
            for (AutomationRule rule : rules) {
                if (rule.matches("WINDOW_CHANGED", packageName, scene)) matched.add(rule);
            }
        }
        executeMatchedRules("WINDOW_CHANGED", matched);
    }

    public void onSceneChanged(String scene, String packageName) {
        ensureRules();
        List<AutomationRule> matched = new ArrayList<>();
        synchronized (this) {
            for (AutomationRule rule : rules) {
                if (rule.matches("SCENE_CHANGED", packageName, scene)) matched.add(rule);
            }
        }
        executeMatchedRules("SCENE_CHANGED", matched);
    }

    public synchronized List<AutomationRule> getRules() {
        ensureRules();
        return new ArrayList<>(rules);
    }

    public void publishEvent(String event, String raw) {
        Intent i = new Intent(ACTION_EVENT_TRIGGERED);
        i.putExtra(EXTRA_EVENT, event);
        i.putExtra(EXTRA_RAW, raw);
        app.sendBroadcast(i);
    }

    private void executeMatchedRules(String trigger, List<AutomationRule> matched) {
        if (matched == null || matched.isEmpty()) return;
        Set<String> usedConflictGroups = new HashSet<>();
        for (AutomationRule rule : matched) {
            if (!rule.enabled) continue;
            if (!TextUtils.isEmpty(rule.conflictGroup) && usedConflictGroups.contains(rule.conflictGroup)) continue;
            if (!canExecute(rule)) continue;
            usedConflictGroups.add(rule.conflictGroup);
            markExecuted(rule);
            Log.i(TAG, "execute rule=" + rule.id + " trigger=" + trigger);
            ActionSequenceExecutor.executeSequence(app, rule.actions);
        }
    }

    private synchronized boolean canExecute(AutomationRule rule) {
        long now = System.currentTimeMillis();
        Long last = lastExecutionMap.get(rule.id);
        long interval = rule.debounceMs > 0L ? rule.debounceMs : DEFAULT_CONFLICT_WINDOW_MS;
        return last == null || now - last > interval;
    }

    private synchronized void markExecuted(AutomationRule rule) {
        lastExecutionMap.put(rule.id, System.currentTimeMillis());
    }

    private synchronized void ensureRules() {
        if (!rulesLoaded) reloadRules();
    }

    private void loadBuiltInRules() {
        addSimpleRule("turn_on_around", "TURN_LEFT_ON", "rule_turn_on_around", 100, "around", "AROUND_TOGGLE_VIEW", "");
        addSimpleRule("turn_right_on_around", "TURN_RIGHT_ON", "rule_turn_on_around", 100, "around", "AROUND_TOGGLE_VIEW", "");
        addSimpleRule("lock_child", "LOCK", "rule_lock_child", 80, "child_lock", "CHILD_LOCK_ON", "");
        addSimpleRule("unlock_child", "UNLOCK", "rule_unlock_child", 80, "child_lock", "CHILD_LOCK_OFF", "");
        addSimpleRule("wheel_360", "WHEEL_360", "rule_wheel_360_toggle", 90, "around", "AROUND_TOGGLE_VIEW", "");
    }

    private void addSimpleRule(String id, String event, String prefKey, int priority, String conflictGroup, String actionType, String packageName) {
        AutomationRule rule = new AutomationRule();
        rule.id = id;
        rule.triggerEvent = event;
        rule.enabled = SharedPreferencesUtils.getBoolean(app, prefKey, true);
        rule.priority = priority;
        rule.conflictGroup = conflictGroup;
        rule.debounceMs = 800L;
        ActionItem item = new ActionItem();
        item.id = id + "_action";
        item.type = actionType;
        item.packageName = packageName;
        rule.actions.add(item);
        rules.add(rule);
    }

    private void loadCustomRules() {
        JSONObject root = new ConfigManager(app).load();
        JSONArray arr = root.optJSONArray("automationRules");
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject json = arr.optJSONObject(i);
            if (json == null) continue;
            AutomationRule rule = AutomationRule.fromJson(json);
            if (!TextUtils.isEmpty(rule.id)) rules.add(rule);
        }
    }

    private void executeLegacyEvent(String event, String raw) {
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

    public static class AutomationRule {
        public String id;
        public boolean enabled = true;
        public String triggerEvent;
        public String packageName;
        public String scene;
        public int priority;
        public String conflictGroup;
        public long debounceMs;
        public final List<ActionItem> actions = new ArrayList<>();

        boolean matches(String event, String pkg, String currentScene) {
            if (!enabled) return false;
            if (!TextUtils.isEmpty(triggerEvent) && !triggerEvent.equals(event)) return false;
            if (!TextUtils.isEmpty(packageName) && !packageName.equals(pkg)) return false;
            return TextUtils.isEmpty(scene) || scene.equalsIgnoreCase(currentScene);
        }

        static AutomationRule fromJson(JSONObject json) {
            AutomationRule rule = new AutomationRule();
            rule.id = json.optString("id", "");
            rule.enabled = json.optBoolean("enabled", true);
            rule.triggerEvent = json.optString("triggerEvent", json.optString("event", ""));
            rule.packageName = json.optString("packageName", "");
            rule.scene = json.optString("scene", "");
            rule.priority = json.optInt("priority", 0);
            rule.conflictGroup = json.optString("conflictGroup", "");
            rule.debounceMs = json.optLong("debounceMs", 500L);
            JSONArray actions = json.optJSONArray("actions");
            if (actions != null) {
                for (int i = 0; i < actions.length(); i++) {
                    JSONObject item = actions.optJSONObject(i);
                    if (item != null) rule.actions.add(ActionItem.fromJson(item));
                }
            }
            return rule;
        }
    }
}
