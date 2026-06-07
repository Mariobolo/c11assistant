package com.leapmotor.c11assistant.model;

import org.json.JSONObject;

public class ActionItem {
    public String id;
    public String type;
    public boolean enabled = true;
    public String packageName;
    public String launchMode = "A";
    public int displayId = -1;
    public int x, y, width = 960, height = 360;
    public int startX, startY, endX, endY;
    public long durationMs = 350L;
    public float alpha = 1.0f;
    public boolean addCloseButton;
    public long delayMs;
    public boolean skipIfRunning;
    public int retryCount;
    public long retryDelayMs = 300L;
    public boolean stopOnFailure;
    public String text;
    public String command;
    public String globalKey;
    public int value;
    public int mode;
    public String extraName;
    public String broadcastAction;
    public String targetPackage;
    public String conditionScene;
    public String conditionPackage;
    public String payloadJson;

    public static ActionItem fromJson(JSONObject itemJson) {
        ActionItem item = new ActionItem();
        if (itemJson == null) return item;
        item.id = itemJson.optString("id", "");
        item.type = itemJson.optString("type", "");
        item.enabled = itemJson.optBoolean("enabled", true);
        item.packageName = itemJson.optString("packageName", itemJson.optString("package", ""));
        item.launchMode = itemJson.optString("launchMode", "A");
        item.displayId = itemJson.optInt("displayId", -1);
        item.x = itemJson.optInt("x", 0);
        item.y = itemJson.optInt("y", 0);
        item.width = itemJson.optInt("width", 960);
        item.height = itemJson.optInt("height", 360);
        item.startX = itemJson.optInt("startX", itemJson.optInt("x1", 0));
        item.startY = itemJson.optInt("startY", itemJson.optInt("y1", 0));
        item.endX = itemJson.optInt("endX", itemJson.optInt("x2", 0));
        item.endY = itemJson.optInt("endY", itemJson.optInt("y2", 0));
        item.durationMs = itemJson.optLong("durationMs", 350L);
        item.alpha = (float) itemJson.optDouble("alpha", 1.0d);
        item.addCloseButton = itemJson.optBoolean("addCloseButton", false);
        item.delayMs = itemJson.optLong("delayMs", 0L);
        item.skipIfRunning = itemJson.optBoolean("skipIfRunning", false);
        item.retryCount = itemJson.optInt("retryCount", 0);
        item.retryDelayMs = itemJson.optLong("retryDelayMs", 300L);
        item.stopOnFailure = itemJson.optBoolean("stopOnFailure", false);
        item.text = itemJson.optString("text", "");
        item.command = itemJson.optString("command", "");
        item.globalKey = itemJson.optString("globalKey", itemJson.optString("key", ""));
        item.value = itemJson.optInt("value", 0);
        item.mode = itemJson.optInt("mode", item.value);
        item.extraName = itemJson.optString("extraName", "");
        item.broadcastAction = itemJson.optString("broadcastAction", itemJson.optString("action", ""));
        item.targetPackage = itemJson.optString("targetPackage", "");
        item.conditionScene = itemJson.optString("conditionScene", "");
        item.conditionPackage = itemJson.optString("conditionPackage", "");
        JSONObject payload = itemJson.optJSONObject("payload");
        item.payloadJson = payload == null ? itemJson.optString("payloadJson", "") : payload.toString();
        return item;
    }
}
