package com.zerorun.c11assistant.manager;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
            String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (Exception e) {
            return createDefault();
        }
    }

    public void save(JSONObject obj) {
        try {
            Files.writeString(new File(context.getFilesDir(), FILE_NAME).toPath(), obj.toString(2), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    public JSONObject createDefault() {
        JSONObject root = new JSONObject();
        try {
            root.put("bootDelaySec", 15);
            root.put("globalCloseButton", true);
            root.put("skipRunningExceptions", new JSONArray());
            root.put("screens", new JSONArray());
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
