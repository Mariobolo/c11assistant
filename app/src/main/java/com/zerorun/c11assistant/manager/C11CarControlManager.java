package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.util.Iterator;

public class C11CarControlManager {
    private static final String TAG = "C11CarControlManager";

    public static final String ACTION_SETTING = "com.leapmotor.speech.setting";
    public static final String ACTION_TO_AIR_CONDITIONER = "com.leapmotor.speech.toairconditioner";
    public static final String ACTION_TO_AUTONAVI = "com.leapmotor.speech.toautonavi";
    public static final String ACTION_BACK_TO_HOME = "com.leapmotor.speech.backtohome";
    public static final String ACTION_TO_CAR_CONTROL = "com.leapmotor.speech.tocarcontrol";
    public static final String ACTION_TO_DRIVE_RECORD = "com.leapmotor.speech.todriverecord";
    public static final String ACTION_TO_IQIYI = "com.leapmotor.speech.toiqiyi";
    public static final String ACTION_TO_JOURNEY = "com.leapmotor.speech.tojourney";
    public static final String ACTION_TO_MEDIA = "com.leapmotor.speech.tomedia";
    public static final String ACTION_TO_PHONE = "com.leapmotor.speech.tophone";
    public static final String ACTION_TO_SETTINGS = "com.leapmotor.speech.tosettings";
    public static final String ACTION_TO_SPEECH = "com.iflytek.autofly.sendToSpeech.message";
    public static final String ACTION_VOICE_WARM_TIP = "com.iflytek.aufofly.warmtip";

    public static final String GLOBAL_SPEECH_SPEAK = "SPEECH_SPEAK";
    public static final String GLOBAL_SECONDARY_DISPLAY_STATE = "display_1_state";
    public static final String GLOBAL_VOLUME_CALL = "C11_CALL";
    public static final String GLOBAL_VOLUME_NAVI = "C11_NAVI";
    public static final String GLOBAL_VOLUME_MUSIC = "C11_MUSIC";
    public static final String GLOBAL_XIAOLING_FLOAT = "HOME_XIAOLING_FLOAT";
    public static final String GLOBAL_HVAC_DRIVER_TEMP = "strCar1409";
    public static final String GLOBAL_HVAC_PASSENGER_TEMP = "strCar1410";
    public static final String GLOBAL_HVAC_PANEL = "strCar100006";
    public static final String GLOBAL_AMBIENT_LIGHT = "strCar1800";
    public static final String GLOBAL_AMBIENT_COLOR = "strCar8867";

    private final Context app;

    public C11CarControlManager(Context context) {
        this.app = context.getApplicationContext();
    }

    public ActionExecutor.ActionResult putGlobalInt(String key, int value) {
        if (TextUtils.isEmpty(key)) return ActionExecutor.ActionResult.failed("SET_GLOBAL_INT", "global key empty");
        try {
            Settings.Global.putInt(app.getContentResolver(), key, value);
            return ActionExecutor.ActionResult.ok("SET_GLOBAL_INT", key + "=" + value);
        } catch (SecurityException e) {
            Log.w(TAG, "WRITE_SETTINGS/WRITE_SECURE_SETTINGS permission required for " + key, e);
            return ActionExecutor.ActionResult.failed("SET_GLOBAL_INT", "permission denied for " + key + ": " + e.getMessage());
        } catch (Exception e) {
            return ActionExecutor.ActionResult.failed("SET_GLOBAL_INT", e.getMessage());
        }
    }

    public ActionExecutor.ActionResult getGlobalInt(String key, int defaultValue) {
        if (TextUtils.isEmpty(key)) return ActionExecutor.ActionResult.failed("GET_GLOBAL_INT", "global key empty");
        try {
            int value = Settings.Global.getInt(app.getContentResolver(), key, defaultValue);
            ActionExecutor.ActionResult result = ActionExecutor.ActionResult.ok("GET_GLOBAL_INT", key + "=" + value);
            result.extras.put("key", key);
            result.extras.put("value", String.valueOf(value));
            return result;
        } catch (Exception e) {
            return ActionExecutor.ActionResult.failed("GET_GLOBAL_INT", e.getMessage());
        }
    }

    public ActionExecutor.ActionResult setSpeechSpeak(boolean enabled) {
        return putGlobalInt(GLOBAL_SPEECH_SPEAK, enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setSecondaryDisplay(boolean enabled) {
        return putGlobalInt(GLOBAL_SECONDARY_DISPLAY_STATE, enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setXiaolingFloat(boolean enabled) {
        return putGlobalInt(GLOBAL_XIAOLING_FLOAT, enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setMusicVolume(int volume) {
        return putGlobalInt(GLOBAL_VOLUME_MUSIC, clamp(volume, 0, 100));
    }

    public ActionExecutor.ActionResult setNaviVolume(int volume) {
        return putGlobalInt(GLOBAL_VOLUME_NAVI, clamp(volume, 0, 100));
    }

    public ActionExecutor.ActionResult setCallVolume(int volume) {
        return putGlobalInt(GLOBAL_VOLUME_CALL, clamp(volume, 0, 100));
    }

    public ActionExecutor.ActionResult setHvacDriverTemp(int temp) {
        return putGlobalInt(GLOBAL_HVAC_DRIVER_TEMP, clamp(temp, 16, 30));
    }

    public ActionExecutor.ActionResult setHvacPassengerTemp(int temp) {
        return putGlobalInt(GLOBAL_HVAC_PASSENGER_TEMP, clamp(temp, 16, 30));
    }

    public ActionExecutor.ActionResult setHvacPanel(boolean opened) {
        return putGlobalInt(GLOBAL_HVAC_PANEL, opened ? 1 : 0);
    }

    public ActionExecutor.ActionResult setAmbientLight(boolean enabled) {
        return putGlobalInt(GLOBAL_AMBIENT_LIGHT, enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setAmbientColor(int color) {
        return putGlobalInt(GLOBAL_AMBIENT_COLOR, clamp(color, 0, 16));
    }

    public ActionExecutor.ActionResult setHvacAcMax(boolean enabled) {
        return sendOfficialBroadcast("HVAC_AC_MAX", ACTION_TO_AIR_CONDITIONER, "HVACACMAXREQ", enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setDayNightMode(boolean dayMode) {
        return sendOfficialBroadcast(dayMode ? "DAY_MODE" : "NIGHT_MODE", ACTION_TO_SETTINGS, "mode", dayMode ? 1 : 0);
    }

    public ActionExecutor.ActionResult setSettingsPage(boolean opened) {
        return sendOfficialBroadcast("SETTINGS_PAGE", ACTION_TO_SETTINGS, "setting", opened ? 1 : 0);
    }

    public ActionExecutor.ActionResult setWifi(boolean enabled) {
        return sendOfficialBroadcast("WIFI", ACTION_TO_SETTINGS, "wifi", enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setBluetooth(boolean enabled) {
        return sendOfficialBroadcast("BLUETOOTH", ACTION_TO_SETTINGS, "bluetooth", enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setLowBeam(boolean enabled) {
        return sendOfficialBroadcast("LOW_BEAM", ACTION_TO_CAR_CONTROL, "CARLIGHT_JINGUANG", enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setRearFog(boolean enabled) {
        return sendOfficialBroadcast("REAR_FOG", ACTION_TO_CAR_CONTROL, "CARLIGHT_REARFOGCTL", enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setPositionLight(boolean enabled) {
        return sendOfficialBroadcast("POSITION_LIGHT", ACTION_TO_CAR_CONTROL, "CARLIGHT_SHEKUODENG", enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setPedestriansAlert(boolean enabled) {
        return sendOfficialBroadcast("PEDESTRIANS_ALERT", ACTION_TO_CAR_CONTROL, "PEDESTRIANS_ALERT", enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult setDriverMode(int mode) {
        return sendOfficialBroadcast("DRIVER_MODE", ACTION_TO_CAR_CONTROL, "MMI_DRIVER_MODE_SET", clamp(mode, 0, 5));
    }

    public ActionExecutor.ActionResult setSceneMode(String extraName, boolean enabled) {
        if (TextUtils.isEmpty(extraName)) return ActionExecutor.ActionResult.failed("SCENE_MODE", "scene mode extra empty");
        return sendOfficialBroadcast("SCENE_MODE", ACTION_TO_CAR_CONTROL, extraName, enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult launchCarControl(boolean enabled) {
        return sendOfficialBroadcast("CAR_CONTROL_LAUNCH", ACTION_TO_CAR_CONTROL, "LAUNCH", enabled ? 1 : 0);
    }

    public ActionExecutor.ActionResult showJourneyEnergy() {
        return sendOfficialBroadcast("JOURNEY_ENERGY", ACTION_TO_JOURNEY, "journey", 1);
    }

    public ActionExecutor.ActionResult showVehicleHealth() {
        return sendOfficialBroadcast("VEHICLE_HEALTH", ACTION_TO_JOURNEY, "healthy", 1);
    }

    public ActionExecutor.ActionResult openAutonavi(JSONObject extras) {
        return sendOfficialBroadcast("OPEN_NAVIGATION", ACTION_TO_AUTONAVI, extras);
    }

    public ActionExecutor.ActionResult openMedia(JSONObject extras) {
        return sendOfficialBroadcast("OPEN_MUSIC", ACTION_TO_MEDIA, extras);
    }

    public ActionExecutor.ActionResult backToHome() {
        return sendOfficialBroadcast("BACK_TO_HOME", ACTION_BACK_TO_HOME, (JSONObject) null);
    }

    public ActionExecutor.ActionResult sendOfficialBroadcast(String name, String action, String extraName, int value) {
        try {
            Intent intent = new Intent(action);
            intent.putExtra(extraName, value);
            app.sendBroadcast(intent);
            return ActionExecutor.ActionResult.ok(name, "broadcast " + action + " " + extraName + "=" + value);
        } catch (SecurityException e) {
            Log.w(TAG, "official broadcast permission denied: " + action, e);
            return ActionExecutor.ActionResult.failed(name, "permission denied for " + action + ": " + e.getMessage());
        } catch (Exception e) {
            return ActionExecutor.ActionResult.failed(name, e.getMessage());
        }
    }

    public ActionExecutor.ActionResult sendOfficialBroadcast(String name, String action, JSONObject extras) {
        if (TextUtils.isEmpty(action)) return ActionExecutor.ActionResult.failed(name, "official action empty");
        try {
            Intent intent = new Intent(action);
            copyJsonExtras(intent, extras);
            app.sendBroadcast(intent);
            return ActionExecutor.ActionResult.ok(name, "broadcast " + action);
        } catch (SecurityException e) {
            Log.w(TAG, "official broadcast permission denied: " + action, e);
            return ActionExecutor.ActionResult.failed(name, "permission denied for " + action + ": " + e.getMessage());
        } catch (Exception e) {
            return ActionExecutor.ActionResult.failed(name, e.getMessage());
        }
    }

    public static void copyJsonExtras(Intent intent, JSONObject extras) {
        if (intent == null || extras == null) return;
        Iterator<String> keys = extras.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = extras.opt(key);
            if (value instanceof Boolean) intent.putExtra(key, (Boolean) value);
            else if (value instanceof Integer) intent.putExtra(key, (Integer) value);
            else if (value instanceof Long) intent.putExtra(key, (Long) value);
            else if (value instanceof Double) intent.putExtra(key, (Double) value);
            else if (value != null) intent.putExtra(key, String.valueOf(value));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
