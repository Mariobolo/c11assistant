package com.leapmotor.c11assistant.model;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 动作数据模型
 * 支持多种动作类型：系统属性设置、发送广播、TTS语音、启动应用等
 */
public class Action {
    private static final String TAG = "Action";

    // 动作类型常量 - 严格基于 ActionExecutor 中的类型
    public static final String TYPE_SPEECH_SPEAK = "SPEECH_SPEAK";      // 语音播报
    public static final String TYPE_GLOBAL_BACK = "GLOBAL_BACK";       // 返回键
    public static final String TYPE_GLOBAL_HOME = "GLOBAL_HOME";       // 主页键
    public static final String TYPE_LAUNCH_PACKAGE = "LAUNCH_PACKAGE"; // 启动应用
    public static final String TYPE_AROUND_TOGGLE_VIEW = "AROUND_TOGGLE_VIEW"; // 360环视
    public static final String TYPE_SET_GLOBAL_INT = "SET_GLOBAL_INT"; // 设置全局属性
    public static final String TYPE_OFFICIAL_BROADCAST = "OFFICIAL_BROADCAST"; // 官方广播
    public static final String TYPE_SET_HVAC_DRIVER_TEMP = "SET_HVAC_DRIVER_TEMP"; // 主驾温度
    public static final String TYPE_SET_HVAC_PASSENGER_TEMP = "SET_HVAC_PASSENGER_TEMP"; // 副驾温度
    public static final String TYPE_LOW_BEAM_ON = "LOW_BEAM_ON";       // 近光灯开
    public static final String TYPE_LOW_BEAM_OFF = "LOW_BEAM_OFF";     // 近光灯关
    public static final String TYPE_DELAY = "DELAY";                   // 延迟执行
    public static final String TYPE_CLICK_AT_POSITION = "CLICK_AT_POSITION"; // 点击位置
    public static final String TYPE_SWIPE_GESTURE = "SWIPE_GESTURE";   // 滑动手势

    // JSON字段名常量
    private static final String KEY_ID = "id";
    private static final String KEY_TYPE = "type";
    private static final String KEY_PARAMS = "params";
    private static final String KEY_DELAY_MS = "delay_ms";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ORDER = "order";

    // 参数名常量
    public static final String PARAM_TEXT = "text";
    public static final String PARAM_PACKAGE_NAME = "packageName";
    public static final String PARAM_GLOBAL_KEY = "key";
    public static final String PARAM_GLOBAL_VALUE = "value";
    public static final String PARAM_BROADCAST_ACTION = "action";
    public static final String PARAM_TEMPERATURE = "temp";
    public static final String PARAM_X = "x";
    public static final String PARAM_Y = "y";
    public static final String PARAM_START_X = "startX";
    public static final String PARAM_START_Y = "startY";
    public static final String PARAM_END_X = "endX";
    public static final String PARAM_END_Y = "endY";
    public static final String PARAM_DURATION = "durationMs";

    private String id;                      // 动作ID
    private String type;                    // 动作类型
    private Map<String, Object> params;     // 动作参数
    private long delayMs;                   // 执行前延迟（毫秒）
    private String description;             // 动作描述
    private int order;                      // 动作序列中的顺序

    /**
     * 默认构造函数
     */
    public Action() {
        this.id = generateId();
        this.type = TYPE_SPEECH_SPEAK;
        this.params = new HashMap<>();
        this.delayMs = 0;
        this.order = 0;
    }

    /**
     * 通过JSON构造
     */
    public Action(JSONObject json) {
        this();
        fromJson(json);
    }

    /**
     * 生成唯一ID
     */
    private String generateId() {
        return "action_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // ==================== 从JSON解析 ====================

    public void fromJson(JSONObject json) {
        if (json == null) return;
        this.id = json.optString(KEY_ID, generateId());
        this.type = json.optString(KEY_TYPE, TYPE_SPEECH_SPEAK);
        this.delayMs = json.optLong(KEY_DELAY_MS, 0);
        this.description = json.optString(KEY_DESCRIPTION, "");
        this.order = json.optInt(KEY_ORDER, 0);

        // 解析参数
        JSONObject paramsJson = json.optJSONObject(KEY_PARAMS);
        if (paramsJson != null) {
            this.params = jsonToMap(paramsJson);
        }
    }

    /**
     * 将JSONObject转换为Map<String, Object>
     */
    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null) return map;

        try {
            for (String key : json.keySet()) {
                Object value = json.opt(key);
                if (value != null) {
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    // ==================== 转换为JSON ====================

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_ID, id);
            json.put(KEY_TYPE, type);
            json.put(KEY_DELAY_MS, delayMs);
            if (!TextUtils.isEmpty(description)) json.put(KEY_DESCRIPTION, description);
            json.put(KEY_ORDER, order);

            // 添加参数
            if (params != null && !params.isEmpty()) {
                JSONObject paramsJson = new JSONObject();
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    paramsJson.put(entry.getKey(), entry.getValue());
                }
                json.put(KEY_PARAMS, paramsJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    // ==================== 便捷构造函数 ====================

    /**
     * 创建语音播报动作
     */
    public static Action createSpeechAction(String text, String description) {
        Action action = new Action();
        action.type = TYPE_SPEECH_SPEAK;
        action.params.put(PARAM_TEXT, text);
        action.description = description;
        return action;
    }

    /**
     * 创建启动应用动作
     */
    public static Action createLaunchAction(String packageName, String description) {
        Action action = new Action();
        action.type = TYPE_LAUNCH_PACKAGE;
        action.params.put(PARAM_PACKAGE_NAME, packageName);
        action.description = description;
        return action;
    }

    /**
     * 创建设置全局属性动作
     */
    public static Action createSetGlobalAction(String key, int value, String description) {
        Action action = new Action();
        action.type = TYPE_SET_GLOBAL_INT;
        action.params.put(PARAM_GLOBAL_KEY, key);
        action.params.put(PARAM_GLOBAL_VALUE, value);
        action.description = description;
        return action;
    }

    /**
     * 创建延迟动作
     */
    public static Action createDelayAction(long delayMs, String description) {
        Action action = new Action();
        action.type = TYPE_DELAY;
        action.delayMs = delayMs;
        action.description = description;
        return action;
    }

    /**
     * 创建点击动作
     */
    public static Action createClickAction(int x, int y, String description) {
        Action action = new Action();
        action.type = TYPE_CLICK_AT_POSITION;
        action.params.put(PARAM_X, x);
        action.params.put(PARAM_Y, y);
        action.description = description;
        return action;
    }

    // ==================== 参数操作 ====================

    /**
     * 设置参数
     */
    public void setParam(String key, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(key, value);
    }

    /**
     * 获取字符串参数
     */
    public String getParamString(String key, String defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 获取整数参数
     */
    public int getParamInt(String key, int defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 获取长整数参数
     */
    public long getParamLong(String key, long defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 构建 ActionExecutor 可用的 payload
     */
    public String buildPayload() {
        if (params == null || params.isEmpty()) return "";
        return toJson().optString(KEY_PARAMS, "");
    }

    // ==================== Getter和Setter ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * 获取用户友好的显示名称
     */
    public String getDisplayName() {
        if (!TextUtils.isEmpty(description)) {
            return description;
        }
        switch (type) {
            case TYPE_SPEECH_SPEAK:
                return "语音播报: " + getParamString(PARAM_TEXT, "");
            case TYPE_LAUNCH_PACKAGE:
                return "启动应用: " + getParamString(PARAM_PACKAGE_NAME, "");
            case TYPE_SET_GLOBAL_INT:
                return "设置属性: " + getParamString(PARAM_GLOBAL_KEY, "") + " = " + getParamInt(PARAM_GLOBAL_VALUE, 0);
            case TYPE_DELAY:
                return "延迟 " + delayMs + "ms";
            case TYPE_CLICK_AT_POSITION:
                return "点击位置 (" + getParamInt(PARAM_X, 0) + ", " + getParamInt(PARAM_Y, 0) + ")";
            case TYPE_AROUND_TOGGLE_VIEW:
                return "切换360环视";
            case TYPE_GLOBAL_BACK:
                return "返回";
            case TYPE_GLOBAL_HOME:
                return "主页";
            default:
                return "动作: " + type;
        }
    }
}
