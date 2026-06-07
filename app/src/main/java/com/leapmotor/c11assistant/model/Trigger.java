package com.leapmotor.c11assistant.model;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 触发条件数据模型
 * 支持多种触发类型：日志事件、系统属性变化、时间触发、应用状态变化
 */
public class Trigger {
    private static final String TAG = "Trigger";

    // 触发类型常量
    public static final String TYPE_LOG_EVENT = "log_event";         // 日志事件触发
    public static final String TYPE_PROPERTY_CHANGE = "property_change"; // 系统属性变化触发
    public static final String TYPE_TIME = "time";                   // 时间触发
    public static final String TYPE_APP_STATE = "app_state";         // 应用状态变化触发

    // JSON字段名常量
    private static final String KEY_TYPE = "type";
    private static final String KEY_EVENT_ID = "event_id";
    private static final String KEY_EVENT_VALUE = "event_value";
    private static final String KEY_PROPERTY_NAME = "property_name";
    private static final String KEY_PROPERTY_VALUE = "property_value";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_PACKAGE_NAME = "package_name";
    private static final String KEY_APP_STATE = "app_state";
    private static final String KEY_DESCRIPTION = "description";

    private String id;              // 触发条件ID
    private String type;            // 触发类型
    private String eventId;         // 日志事件ID (仅TYPE_LOG_EVENT)
    private String eventValue;      // 事件匹配值 (仅TYPE_LOG_EVENT)
    private String propertyName;    // 系统属性名 (仅TYPE_PROPERTY_CHANGE)
    private String propertyValue;   // 系统属性值 (仅TYPE_PROPERTY_CHANGE)
    private int hour;               // 小时 (仅TYPE_TIME)
    private int minute;             // 分钟 (仅TYPE_TIME)
    private String packageName;     // 应用包名 (仅TYPE_APP_STATE)
    private String appState;        // 应用状态 (如"start", "close") (仅TYPE_APP_STATE)
    private String description;     // 触发条件描述

    /**
     * 默认构造函数
     */
    public Trigger() {
        this.id = generateId();
        this.type = TYPE_LOG_EVENT;
        this.hour = -1;
        this.minute = -1;
    }

    /**
     * 通过JSON构造
     */
    public Trigger(JSONObject json) {
        fromJson(json);
    }

    /**
     * 生成唯一ID
     */
    private String generateId() {
        return "trigger_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // ==================== 从JSON解析 ====================

    public void fromJson(JSONObject json) {
        if (json == null) return;
        this.id = json.optString(KEY_TYPE + "_id", generateId());
        this.type = json.optString(KEY_TYPE, TYPE_LOG_EVENT);
        this.eventId = json.optString(KEY_EVENT_ID, "");
        this.eventValue = json.optString(KEY_EVENT_VALUE, "");
        this.propertyName = json.optString(KEY_PROPERTY_NAME, "");
        this.propertyValue = json.optString(KEY_PROPERTY_VALUE, "");
        this.hour = json.optInt(KEY_HOUR, -1);
        this.minute = json.optInt(KEY_MINUTE, -1);
        this.packageName = json.optString(KEY_PACKAGE_NAME, "");
        this.appState = json.optString(KEY_APP_STATE, "");
        this.description = json.optString(KEY_DESCRIPTION, "");
    }

    // ==================== 转换为JSON ====================

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_TYPE + "_id", id);
            json.put(KEY_TYPE, type);
            if (!TextUtils.isEmpty(eventId)) json.put(KEY_EVENT_ID, eventId);
            if (!TextUtils.isEmpty(eventValue)) json.put(KEY_EVENT_VALUE, eventValue);
            if (!TextUtils.isEmpty(propertyName)) json.put(KEY_PROPERTY_NAME, propertyName);
            if (!TextUtils.isEmpty(propertyValue)) json.put(KEY_PROPERTY_VALUE, propertyValue);
            if (hour >= 0) json.put(KEY_HOUR, hour);
            if (minute >= 0) json.put(KEY_MINUTE, minute);
            if (!TextUtils.isEmpty(packageName)) json.put(KEY_PACKAGE_NAME, packageName);
            if (!TextUtils.isEmpty(appState)) json.put(KEY_APP_STATE, appState);
            if (!TextUtils.isEmpty(description)) json.put(KEY_DESCRIPTION, description);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    // ==================== 便捷构造函数 ====================

    /**
     * 创建日志事件触发条件
     */
    public static Trigger createLogEventTrigger(String eventId, String eventValue, String description) {
        Trigger trigger = new Trigger();
        trigger.type = TYPE_LOG_EVENT;
        trigger.eventId = eventId;
        trigger.eventValue = eventValue;
        trigger.description = description;
        return trigger;
    }

    /**
     * 创建系统属性变化触发条件
     */
    public static Trigger createPropertyTrigger(String propertyName, String propertyValue, String description) {
        Trigger trigger = new Trigger();
        trigger.type = TYPE_PROPERTY_CHANGE;
        trigger.propertyName = propertyName;
        trigger.propertyValue = propertyValue;
        trigger.description = description;
        return trigger;
    }

    /**
     * 创建时间触发条件
     */
    public static Trigger createTimeTrigger(int hour, int minute, String description) {
        Trigger trigger = new Trigger();
        trigger.type = TYPE_TIME;
        trigger.hour = hour;
        trigger.minute = minute;
        trigger.description = description;
        return trigger;
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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventValue() {
        return eventValue;
    }

    public void setEventValue(String eventValue) {
        this.eventValue = eventValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppState() {
        return appState;
    }

    public void setAppState(String appState) {
        this.appState = appState;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取用户友好的显示名称
     */
    public String getDisplayName() {
        if (!TextUtils.isEmpty(description)) {
            return description;
        }
        switch (type) {
            case TYPE_LOG_EVENT:
                CarEvent event = CarEvent.fromEventId(eventId);
                return event != null ? event.getDisplayName() : eventId;
            case TYPE_PROPERTY_CHANGE:
                return propertyName + " = " + propertyValue;
            case TYPE_TIME:
                return String.format("%02d:%02d", hour, minute);
            case TYPE_APP_STATE:
                return packageName + " " + appState;
            default:
                return "未知触发条件";
        }
    }
}
