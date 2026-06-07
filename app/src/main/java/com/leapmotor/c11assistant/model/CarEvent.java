package com.leapmotor.c11assistant.model;

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public enum CarEvent {

    // ====== 挡位状态事件 ======
    GEAR_R("GEAR_R", "R挡", "C11CarSomeIp", "eventId: 1110 value: 1"),
    GEAR_N("GEAR_N", "N挡", "C11CarSomeIp", "eventId: 1110 value: 2"),
    GEAR_D("GEAR_D", "D挡", "C11CarSomeIp", "eventId: 1110 value: 3"),

    // ====== 车门/舱盖状态事件 ======
    DOOR_LEFT_FRONT_OPEN("DOOR_LEFT_FRONT_OPEN", "左前门打开", "C11CarSomeIp", "eventId: 9123 value: 1"),
    DOOR_LEFT_FRONT_CLOSE("DOOR_LEFT_FRONT_CLOSE", "左前门关闭", "C11CarSomeIp", "eventId: 9123 value: 0"),
    DOOR_RIGHT_FRONT_OPEN("DOOR_RIGHT_FRONT_OPEN", "右前门打开", "C11CarSomeIp", "eventId: 9124 value: 1"),
    DOOR_RIGHT_FRONT_CLOSE("DOOR_RIGHT_FRONT_CLOSE", "右前门关闭", "C11CarSomeIp", "eventId: 9124 value: 0"),
    DOOR_LEFT_REAR_OPEN("DOOR_LEFT_REAR_OPEN", "左后门打开", "C11CarSomeIp", "eventId: 9125 value: 1"),
    DOOR_LEFT_REAR_CLOSE("DOOR_LEFT_REAR_CLOSE", "左后门关闭", "C11CarSomeIp", "eventId: 9125 value: 0"),
    DOOR_RIGHT_REAR_OPEN("DOOR_RIGHT_REAR_OPEN", "右后门打开", "C11CarSomeIp", "eventId: 9126 value: 1"),
    DOOR_RIGHT_REAR_CLOSE("DOOR_RIGHT_REAR_CLOSE", "右后门关闭", "C11CarSomeIp", "eventId: 9126 value: 0"),
    TAILGATE_OPEN("TAILGATE_OPEN", "后备箱打开", "C11CarSomeIp", "eventId: 9127 value: 1"),
    TAILGATE_CLOSE("TAILGATE_CLOSE", "后备箱关闭", "C11CarSomeIp", "eventId: 9127 value: 0"),
    HOOD_OPEN("HOOD_OPEN", "前机盖打开", "C11CarSomeIp", "eventId: 9128 value: 1"),
    HOOD_CLOSE("HOOD_CLOSE", "前机盖关闭", "C11CarSomeIp", "eventId: 9128 value: 0"),

    // ====== 锁车状态事件 ======
    VEHICLE_UNLOCK("VEHICLE_UNLOCK", "车辆解锁", "C11CarSomeIp", "eventid: 1200 msg: 0"),
    VEHICLE_LOCK("VEHICLE_LOCK", "车辆上锁", "C11CarSomeIp", "eventid: 1200 msg: 1"),

    // ====== 灯光状态事件 ======
    TURN_LEFT_ON("TURN_LEFT_ON", "左转灯开启", "C11CarSomeIp", "turnLeft value: 1"),
    TURN_LEFT_OFF("TURN_LEFT_OFF", "左转灯关闭", "C11CarSomeIp", "turnLeft value: 0"),
    TURN_RIGHT_ON("TURN_RIGHT_ON", "右转灯开启", "C11CarSomeIp", "turnRight value: 1"),
    TURN_RIGHT_OFF("TURN_RIGHT_OFF", "右转灯关闭", "C11CarSomeIp", "turnRight value: 0"),
    LOW_BEAM_ON("LOW_BEAM_ON", "近光灯开启", "C11CarXml", "node_name : Close setTextContent: 0"),
    LOW_BEAM_OFF("LOW_BEAM_OFF", "近光灯关闭", "C11CarXml", "node_name : Close setTextContent: 1"),

    // ====== 天窗/遮阳帘事件 ======
    SUNROOF_FULL_OPEN("SUNROOF_FULL_OPEN", "天窗完全打开", "C11CarSomeIp", "eventId: 21201 value: 3"),
    SUNROOF_FULL_CLOSE("SUNROOF_FULL_CLOSE", "天窗完全关闭", "C11CarSomeIp", "eventId: 21201 value: 4"),
    SUNSHADE_FULL_OPEN("SUNSHADE_FULL_OPEN", "遮阳帘完全打开", "BleControlService", "eventId: 21207 value: 3"),
    SUNSHADE_FULL_CLOSE("SUNSHADE_FULL_CLOSE", "遮阳帘完全关闭", "BleControlService", "eventId: 21207 value: 4"),

    // ====== 多媒体控制事件 ======
    MEDIA_PLAY_PAUSE("MEDIA_PLAY_PAUSE", "音乐播放/暂停", "MediaTlog-CtrlService", "Recive wheelService mute: 1"),
    MEDIA_NEXT("MEDIA_NEXT", "下一首", "BtMusicManager", "Recive wheelService music: 2"),
    MEDIA_PREVIOUS("MEDIA_PREVIOUS", "上一首", "BtMusicManager", "Recive wheelService music: 1"),
    DRIVE_RECORD_START("DRIVE_RECORD_START", "行车记录仪开始录制", "TripService", "startMp4Record"),

    // ====== 360环视事件 ======
    AROUND_HIDE("AROUND_HIDE", "360环视隐藏", "C11CarSomeIp", "hideAnimView"),
    WHEEL_360("WHEEL_360", "方控360按钮", "C11CarSomeIp", "WHEEL_360_ID value: 48"),

    // ====== 屏幕状态事件 ======
    SCREEN_ON("SCREEN_ON", "屏幕点亮", "MediaTlog-CtrlService", "Recive the screen on"),

    // ====== 蓝牙状态事件 ======
    BLUETOOTH_CONNECTED("BLUETOOTH_CONNECTED", "蓝牙连接", "BtMusicManager", "isA2dpConneted: true"),
    BLUETOOTH_DISCONNECTED("BLUETOOTH_DISCONNECTED", "蓝牙断开", "BtMusicManager", "isA2dpConneted: false"),

    // ====== 车速事件 ======
    SPEED_UNDER_10("SPEED_UNDER_10", "车速低于10km/h", "C11CarXml", "node_name : speed setTextContent: 10"),

    // ====== 系统应用切换事件 ======
    CURRENT_PACKAGE_CHANGED("CURRENT_PACKAGE_CHANGED", "当前应用切换", "LPSysUI.LeapMotorTopTaskHelper", ""),
    MEDIA_PACKAGE_CHANGED("MEDIA_PACKAGE_CHANGED", "媒体包名变化", "LPSysUI.AppStatisticsUtil", "");

    private static final String TAG = "CarEvent";
    private static final Map<String, CarEvent> EVENT_MAP = new HashMap<>();

    static {
        for (CarEvent event : CarEvent.values()) {
            EVENT_MAP.put(event.eventId, event);
        }
    }

    private final String eventId;
    private final String displayName;
    private final String logTag;
    private final String matchPattern;

    CarEvent(String eventId, String displayName, String logTag, String matchPattern) {
        this.eventId = eventId;
        this.displayName = displayName;
        this.logTag = logTag;
        this.matchPattern = matchPattern;
    }

    public String getEventId() {
        return eventId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLogTag() {
        return logTag;
    }

    public String getMatchPattern() {
        return matchPattern;
    }

    public boolean matches(String logLine) {
        if (TextUtils.isEmpty(matchPattern)) return false;
        return logLine.contains(matchPattern);
    }

    public static CarEvent fromEventId(String eventId) {
        if (TextUtils.isEmpty(eventId)) return null;
        return EVENT_MAP.get(eventId);
    }

    public static CarEvent matchLogLine(String logLine) {
        if (TextUtils.isEmpty(logLine)) return null;
        for (CarEvent event : CarEvent.values()) {
            if (event.matches(logLine)) {
                return event;
            }
        }
        return null;
    }

    public static String[] getAllLogTags() {
        return new String[] {
            "C11CarSomeIp",
            "C11AirConditioner",
            "C11CarXml",
            "AroundService",
            "BleControlService",
            "TripService",
            "LPSysUI.LeapMotorTopTaskHelper",
            "LPSysUI.AppStatisticsUtil",
            "MediaTlog-CtrlService",
            "BtMusicManager",
            "TripService",
            "AutomationManager"
        };
    }
}
