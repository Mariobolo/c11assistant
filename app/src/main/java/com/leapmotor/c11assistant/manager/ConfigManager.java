package com.leapmotor.c11assistant.manager;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class ConfigManager {
    private static final String FILE_NAME = "c11_config.json";
    private final Context context;

    public ConfigManager(Context context) { this.context = context.getApplicationContext(); }

    public JSONObject load() {
        try {
            File f = new File(context.getFilesDir(), FILE_NAME);
            if (!f.exists()) return createDefault();

            StringBuilder sb = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(f);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }

            String json = sb.toString();
            return new JSONObject(json);
        } catch (Exception e) {
            return createDefault();
        }
    }

    public void save(JSONObject obj) {
        try {
            File out = new File(context.getFilesDir(), FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(out, false);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter bw = new BufferedWriter(osw)) {
                bw.write(obj.toString(2));
            }
        } catch (Exception ignored) {}
    }

    public JSONObject createDefault() {
        JSONObject root = new JSONObject();
        try {
            root.put("bootDelaySec", 15);
            root.put("globalCloseButton", true);
            root.put("skipRunningExceptions", new JSONArray());

            JSONArray actions = new JSONArray();
            actions.put(new JSONObject()
                    .put("id", "返回")
                    .put("type", "GLOBAL_BACK")
                    .put("enabled", true));
            actions.put(new JSONObject()
                    .put("id", "打开导航")
                    .put("type", "OPEN_NAVIGATION")
                    .put("packageName", "com.autonavi.minimap")
                    .put("delayMs", 300));
            actions.put(new JSONObject()
                    .put("id", "最大制冷")
                    .put("type", "HVAC_AC_MAX_ON")
                    .put("delayMs", 100));
            actions.put(new JSONObject()
                    .put("id", "经济驾驶模式")
                    .put("type", "SET_DRIVER_MODE")
                    .put("mode", 4));
            actions.put(new JSONObject()
                    .put("id", "导航音量60")
                    .put("type", "SET_C11_NAVI_VOLUME")
                    .put("value", 60));

            JSONArray screens = new JSONArray();
            screens.put(new JSONObject()
                    .put("displayId", -1)
                    .put("label", "副屏")
                    .put("actions", actions));
            root.put("screens", screens);

            JSONArray automationRules = new JSONArray();
            automationRules.put(new JSONObject()
                    .put("id", "wheel_360_custom")
                    .put("enabled", true)
                    .put("event", "WHEEL_360")
                    .put("priority", 120)
                    .put("conflictGroup", "around")
                    .put("actions", new JSONArray().put(new JSONObject()
                            .put("id", "toggle_around")
                            .put("type", "AROUND_TOGGLE_VIEW")
                            .put("retryCount", 1))));
            root.put("automationRules", automationRules);
        } catch (Exception ignored) {}
        return root;
    }

    public Set<String> getSkipExceptions(JSONObject root) {
        Set<String> data = new HashSet<>();
        JSONArray arr = root.optJSONArray("skipRunningExceptions");
        if (arr == null) return data;
        for (int i = 0; i < arr.length(); i++) data.add(arr.optString(i));
        return data;
    }
}
