package com.leapmotor.c11assistant.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ScreenConfig {
    public int displayId;
    public String label;
    public int width;
    public int height;
    public List<ActionItem> actions = new ArrayList<>();

    public static ScreenConfig fromJson(JSONObject json) {
        ScreenConfig config = new ScreenConfig();
        if (json == null) return config;

        config.displayId = json.optInt("displayId", -1);
        config.label = json.optString("label", "");
        config.width = json.optInt("width", 0);
        config.height = json.optInt("height", 0);

        JSONArray actionArray = json.optJSONArray("actions");
        if (actionArray == null) return config;

        for (int i = 0; i < actionArray.length(); i++) {
            JSONObject itemJson = actionArray.optJSONObject(i);
            if (itemJson == null) continue;

            ActionItem item = new ActionItem();
            item.id = itemJson.optString("id", "");
            item.type = itemJson.optString("type", "");
            item.enabled = itemJson.optBoolean("enabled", true);
            item.packageName = itemJson.optString("packageName", "");
            item.launchMode = itemJson.optString("launchMode", "A");
            item.x = itemJson.optInt("x", 0);
            item.y = itemJson.optInt("y", 0);
            item.width = itemJson.optInt("width", 960);
            item.height = itemJson.optInt("height", 360);
            item.alpha = (float) itemJson.optDouble("alpha", 1.0d);
            item.addCloseButton = itemJson.optBoolean("addCloseButton", false);
            item.delayMs = itemJson.optLong("delayMs", 0L);
            item.skipIfRunning = itemJson.optBoolean("skipIfRunning", false);
            config.actions.add(item);
        }
        return config;
    }
}
