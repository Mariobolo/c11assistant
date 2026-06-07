package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.leapmotor.c11assistant.model.ActionItem;
import com.leapmotor.c11assistant.service.C11AccessibilityService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionSequenceExecutor {
    private static final String TAG = "ActionSequenceExecutor";
    private static final int DEFAULT_RETRY_COUNT = 0;
    private static final long DEFAULT_RETRY_DELAY_MS = 300L;
    private static final int MAX_LOG_SIZE = 80;

    private static final List<ExecutionLog> LOGS = Collections.synchronizedList(new ArrayList<ExecutionLog>());

    public interface Callback {
        void onActionFinished(ActionItem item, ActionExecutor.ActionResult result);
        void onSequenceFinished(List<ActionExecutor.ActionResult> results);
    }

    public static void executeSequence(Context context, List<ActionItem> actions) {
        executeSequence(context, actions, null);
    }

    public static void executeSequence(final Context context, final List<ActionItem> actions, final Callback callback) {
        if (context == null || actions == null || actions.isEmpty()) return;
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ActionExecutor.ActionResult> results = new ArrayList<>();
                for (ActionItem item : actions) {
                    if (item == null || !item.enabled) continue;
                    if (!checkCondition(item)) {
                        ActionExecutor.ActionResult skipped = ActionExecutor.ActionResult.failed(item.type, "condition not matched");
                        results.add(skipped);
                        appendLog(item, skipped);
                        continue;
                    }
                    sleep(item.delayMs);
                    ActionExecutor.ActionResult result = executeWithRetry(app, item);
                    results.add(result);
                    appendLog(item, result);
                    if (callback != null) callback.onActionFinished(item, result);
                    if (!result.success && item.stopOnFailure) break;
                }
                if (callback != null) callback.onSequenceFinished(results);
            }
        }, "c11-action-sequence").start();
    }

    public static ActionExecutor.ActionResult executeNow(Context context, ActionItem item) {
        if (context == null || item == null) return ActionExecutor.ActionResult.failed("UNKNOWN", "context/item empty");
        if (!item.enabled) return ActionExecutor.ActionResult.failed(item.type, "disabled");
        if (!checkCondition(item)) return ActionExecutor.ActionResult.failed(item.type, "condition not matched");
        sleep(item.delayMs);
        ActionExecutor.ActionResult result = executeWithRetry(context.getApplicationContext(), item);
        appendLog(item, result);
        return result;
    }

    public static List<ActionItem> actionsFromConfig(JSONObject root) {
        List<ActionItem> list = new ArrayList<>();
        if (root == null) return list;
        JSONArray screens = root.optJSONArray("screens");
        if (screens != null) {
            for (int i = 0; i < screens.length(); i++) {
                JSONObject screen = screens.optJSONObject(i);
                if (screen == null) continue;
                JSONArray actions = screen.optJSONArray("actions");
                readActionArray(actions, list);
            }
        }
        readActionArray(root.optJSONArray("actions"), list);
        return list;
    }

    public static void executeConfig(Context context, JSONObject root) {
        executeSequence(context, actionsFromConfig(root));
    }

    public static List<ExecutionLog> getLogs() {
        synchronized (LOGS) {
            return new ArrayList<>(LOGS);
        }
    }

    public static void clearLogs() {
        LOGS.clear();
    }

    public static String buildPayload(ActionItem item) {
        JSONObject json = new JSONObject();
        try {
            json.put("packageName", item.packageName == null ? "" : item.packageName);
            json.put("displayId", item.displayId);
            json.put("x", item.x);
            json.put("y", item.y);
            json.put("width", item.width);
            json.put("height", item.height);
            json.put("startX", item.startX);
            json.put("startY", item.startY);
            json.put("endX", item.endX);
            json.put("endY", item.endY);
            json.put("durationMs", item.durationMs);
            json.put("text", item.text == null ? "" : item.text);
            json.put("command", item.command == null ? "" : item.command);
            json.put("globalKey", item.globalKey == null ? "" : item.globalKey);
            json.put("key", item.globalKey == null ? "" : item.globalKey);
            json.put("value", item.value);
            json.put("mode", item.mode);
            json.put("extraName", item.extraName == null ? "" : item.extraName);
            json.put("broadcastAction", item.broadcastAction == null ? "" : item.broadcastAction);
            json.put("action", item.broadcastAction == null ? "" : item.broadcastAction);
            json.put("targetPackage", item.targetPackage == null ? "" : item.targetPackage);
            if (item.payloadJson != null) {
                JSONObject extra = new JSONObject(item.payloadJson);
                JSONArray names = extra.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String key = names.optString(i);
                        json.put(key, extra.opt(key));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "buildPayload failed", e);
        }
        return json.toString();
    }

    private static ActionExecutor.ActionResult executeWithRetry(Context app, ActionItem item) {
        int retryCount = Math.max(DEFAULT_RETRY_COUNT, item.retryCount);
        long retryDelayMs = item.retryDelayMs > 0L ? item.retryDelayMs : DEFAULT_RETRY_DELAY_MS;
        ActionExecutor.ActionResult result = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            result = ActionExecutor.executeWithResult(app, item.type, buildPayload(item));
            if (result.success) return result;
            if (attempt < retryCount) sleep(retryDelayMs);
        }
        return result == null ? ActionExecutor.ActionResult.failed(item.type, "not executed") : result;
    }

    private static boolean checkCondition(ActionItem item) {
        if (TextUtils.isEmpty(item.conditionScene) && TextUtils.isEmpty(item.conditionPackage)) return true;
        C11AccessibilityService service = ActionExecutor.getAccessibilityService();
        if (service == null) return false;
        if (!TextUtils.isEmpty(item.conditionScene) && !item.conditionScene.equalsIgnoreCase(service.getCurrentScene())) return false;
        return TextUtils.isEmpty(item.conditionPackage) || item.conditionPackage.equals(service.getCurrentPackageName());
    }

    private static void readActionArray(JSONArray actions, List<ActionItem> out) {
        if (actions == null) return;
        for (int i = 0; i < actions.length(); i++) {
            JSONObject itemJson = actions.optJSONObject(i);
            if (itemJson != null) out.add(ActionItem.fromJson(itemJson));
        }
    }

    private static void appendLog(ActionItem item, ActionExecutor.ActionResult result) {
        ExecutionLog log = new ExecutionLog(item == null ? "" : item.id, item == null ? "" : item.type, result);
        synchronized (LOGS) {
            LOGS.add(0, log);
            while (LOGS.size() > MAX_LOG_SIZE) LOGS.remove(LOGS.size() - 1);
        }
        Log.i(TAG, log.toString());
    }

    private static void sleep(long ms) {
        if (ms <= 0L) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class ExecutionLog {
        public final String actionId;
        public final String actionType;
        public final ActionExecutor.ActionResult result;
        public final long timestamp;

        ExecutionLog(String actionId, String actionType, ActionExecutor.ActionResult result) {
            this.actionId = actionId;
            this.actionType = actionType;
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "ExecutionLog{" +
                    "actionId='" + actionId + '\'' +
                    ", actionType='" + actionType + '\'' +
                    ", result=" + result +
                    '}';
        }
    }
}
