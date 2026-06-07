package com.leapmotor.c11assistant.manager;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.leapmotor.c11assistant.service.C11AccessibilityService;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ActionExecutor {
    private static final String TAG = "ActionExecutor";

    public static final String RESULT_OK = "OK";
    public static final String RESULT_FAILED = "FAILED";
    public static final String RESULT_UNSUPPORTED = "UNSUPPORTED";

    private static C11AccessibilityService accessibilityService;

    public static void setAccessibilityService(C11AccessibilityService service) {
        accessibilityService = service;
    }

    public static C11AccessibilityService getAccessibilityService() {
        return accessibilityService != null ? accessibilityService : C11AccessibilityService.get();
    }

    public static boolean execute(Context context, String action, String payload) {
        ActionResult result = executeWithResult(context, action, payload);
        return result.success;
    }

    public static ActionResult executeWithResult(Context context, String action, String payload) {
        Context app = context == null ? null : context.getApplicationContext();
        if (app == null || TextUtils.isEmpty(action)) {
            return ActionResult.failed(action, "context/action empty");
        }

        String normalized = normalizeAction(action);
        JSONObject args = parsePayload(payload);
        Log.i(TAG, "execute action=" + normalized + " payload=" + payload);

        try {
            switch (normalized) {
                case "GLOBAL_BACK":
                    return global(normalized, AccessibilityService.GLOBAL_ACTION_BACK, KeyEvent.KEYCODE_BACK);
                case "GLOBAL_HOME":
                    return global(normalized, AccessibilityService.GLOBAL_ACTION_HOME, KeyEvent.KEYCODE_HOME);
                case "GLOBAL_RECENTS":
                    return global(normalized, AccessibilityService.GLOBAL_ACTION_RECENTS, KeyEvent.KEYCODE_APP_SWITCH);
                case "GLOBAL_POWER":
                    return global(normalized, AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, KeyEvent.KEYCODE_POWER);
                case "VOLUME_UP":
                    return adjustVolume(app, normalized, AudioManager.ADJUST_RAISE);
                case "VOLUME_DOWN":
                    return adjustVolume(app, normalized, AudioManager.ADJUST_LOWER);
                case "VOLUME_MUTE":
                case "MUTE":
                    return adjustVolume(app, normalized, AudioManager.ADJUST_TOGGLE_MUTE);
                case "MEDIA_NEXT":
                    return mediaKey(app, KeyEvent.KEYCODE_MEDIA_NEXT, normalized);
                case "MEDIA_PREVIOUS":
                    return mediaKey(app, KeyEvent.KEYCODE_MEDIA_PREVIOUS, normalized);
                case "MEDIA_PLAY_PAUSE":
                    return mediaKey(app, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, normalized);
                case "BRIGHTNESS_UP":
                    return adjustBrightness(app, normalized, true);
                case "BRIGHTNESS_DOWN":
                    return adjustBrightness(app, normalized, false);
                case "LAUNCH_PACKAGE":
                case "LAUNCH":
                    return launchPackage(app, firstNonEmpty(args.optString("packageName"), args.optString("package"), payload));
                case "LAUNCH_ON_DISPLAY":
                    return launchOnDisplay(app, args);
                case "LAUNCH_FREEFORM":
                    return launchFreeform(app, args);
                case "BRING_TO_FRONT":
                    return bringToFront(app, firstNonEmpty(args.optString("packageName"), args.optString("package"), payload));
                case "OPEN_SETTINGS":
                    return openSettings(app, normalized);
                case "OPEN_MUSIC":
                    return openMusic(app, normalized, args);
                case "OPEN_NAVIGATION":
                    return openNavigation(app, normalized, args);
                case "AROUND_TOGGLE_VIEW":
                    return aroundToggle(app, args);
                case "CHILD_LOCK_ON":
                    return C11CarControlManager.getInstance(app).setChildLock(true);
                case "CHILD_LOCK_OFF":
                    return C11CarControlManager.getInstance(app).setChildLock(false);
                case "SPEECH_SPEAK":
                case "SPEAK":
                    return speechSpeak(app, firstNonEmpty(args.optString("text"), payload));
                case "SET_GLOBAL_INT":
                case "CAR_GLOBAL_SET":
                    return new C11CarControlManager(app).putGlobalInt(args.optString("key", args.optString("globalKey", "")), args.optInt("value", 0));
                case "GET_GLOBAL_INT":
                case "CAR_GLOBAL_GET":
                    return new C11CarControlManager(app).getGlobalInt(args.optString("key", args.optString("globalKey", "")), args.optInt("defaultValue", -1));
                case "OFFICIAL_BROADCAST":
                case "CAR_BROADCAST":
                    return new C11CarControlManager(app).sendOfficialBroadcast(normalized, args.optString("action", args.optString("broadcastAction", "")), args.optJSONObject("extras"));
                case "SET_C11_MUSIC_VOLUME":
                    return new C11CarControlManager(app).setMusicVolume(args.optInt("value", args.optInt("volume", 50)));
                case "SET_C11_NAVI_VOLUME":
                    return new C11CarControlManager(app).setNaviVolume(args.optInt("value", args.optInt("volume", 50)));
                case "SET_C11_CALL_VOLUME":
                    return new C11CarControlManager(app).setCallVolume(args.optInt("value", args.optInt("volume", 50)));
                case "SPEECH_SPEAK_ON":
                    return new C11CarControlManager(app).setSpeechSpeak(true);
                case "SPEECH_SPEAK_OFF":
                    return new C11CarControlManager(app).setSpeechSpeak(false);
                case "SECONDARY_DISPLAY_ON":
                    return new C11CarControlManager(app).setSecondaryDisplay(true);
                case "SECONDARY_DISPLAY_OFF":
                    return new C11CarControlManager(app).setSecondaryDisplay(false);
                case "XIAOLING_FLOAT_ON":
                    return new C11CarControlManager(app).setXiaolingFloat(true);
                case "XIAOLING_FLOAT_OFF":
                    return new C11CarControlManager(app).setXiaolingFloat(false);
                case "HVAC_PANEL_ON":
                    return new C11CarControlManager(app).setHvacPanel(true);
                case "HVAC_PANEL_OFF":
                    return new C11CarControlManager(app).setHvacPanel(false);
                case "SET_HVAC_DRIVER_TEMP":
                    return new C11CarControlManager(app).setHvacDriverTemp(args.optInt("value", args.optInt("temp", 24)));
                case "SET_HVAC_PASSENGER_TEMP":
                    return new C11CarControlManager(app).setHvacPassengerTemp(args.optInt("value", args.optInt("temp", 24)));
                case "HVAC_AC_MAX_ON":
                    return new C11CarControlManager(app).setHvacAcMax(true);
                case "HVAC_AC_MAX_OFF":
                    return new C11CarControlManager(app).setHvacAcMax(false);
                case "AMBIENT_LIGHT_ON":
                    return new C11CarControlManager(app).setAmbientLight(true);
                case "AMBIENT_LIGHT_OFF":
                    return new C11CarControlManager(app).setAmbientLight(false);
                case "SET_AMBIENT_COLOR":
                    return new C11CarControlManager(app).setAmbientColor(args.optInt("value", args.optInt("color", 14)));
                case "DAY_MODE":
                    return new C11CarControlManager(app).setDayNightMode(true);
                case "NIGHT_MODE":
                    return new C11CarControlManager(app).setDayNightMode(false);
                case "WIFI_ON":
                    return new C11CarControlManager(app).setWifi(true);
                case "WIFI_OFF":
                    return new C11CarControlManager(app).setWifi(false);
                case "BLUETOOTH_ON":
                    return new C11CarControlManager(app).setBluetooth(true);
                case "BLUETOOTH_OFF":
                    return new C11CarControlManager(app).setBluetooth(false);
                case "LOW_BEAM_ON":
                    return new C11CarControlManager(app).setLowBeam(true);
                case "LOW_BEAM_OFF":
                    return new C11CarControlManager(app).setLowBeam(false);
                case "REAR_FOG_ON":
                    return new C11CarControlManager(app).setRearFog(true);
                case "REAR_FOG_OFF":
                    return new C11CarControlManager(app).setRearFog(false);
                case "POSITION_LIGHT_ON":
                    return new C11CarControlManager(app).setPositionLight(true);
                case "POSITION_LIGHT_OFF":
                    return new C11CarControlManager(app).setPositionLight(false);
                case "PEDESTRIANS_ALERT_ON":
                    return new C11CarControlManager(app).setPedestriansAlert(true);
                case "PEDESTRIANS_ALERT_OFF":
                    return new C11CarControlManager(app).setPedestriansAlert(false);
                case "SET_DRIVER_MODE":
                    return new C11CarControlManager(app).setDriverMode(args.optInt("value", args.optInt("mode", 0)));
                case "GUARD_MODE_ON":
                    return new C11CarControlManager(app).setSceneMode("GUARD_MODE", true);
                case "GUARD_MODE_OFF":
                    return new C11CarControlManager(app).setSceneMode("GUARD_MODE", false);
                case "REST_MODE_ON":
                    return new C11CarControlManager(app).setSceneMode("REST_MODE", true);
                case "REST_MODE_OFF":
                    return new C11CarControlManager(app).setSceneMode("REST_MODE", false);
                case "EXPERIENCE_MODE_ON":
                    return new C11CarControlManager(app).setSceneMode("EXPERIENCE_MODE", true);
                case "EXPERIENCE_MODE_OFF":
                    return new C11CarControlManager(app).setSceneMode("EXPERIENCE_MODE", false);
                case "CAMPING_MODE_ON":
                    return new C11CarControlManager(app).setSceneMode("CAMPING_MODE", true);
                case "CAMPING_MODE_OFF":
                    return new C11CarControlManager(app).setSceneMode("CAMPING_MODE", false);
                case "POWER_SAVE_MODE_ON":
                    return new C11CarControlManager(app).setSceneMode("POWER_SAVE_MODE", true);
                case "POWER_SAVE_MODE_OFF":
                    return new C11CarControlManager(app).setSceneMode("POWER_SAVE_MODE", false);
                case "SENTINEL_MODE_ON":
                    return new C11CarControlManager(app).setSceneMode("SENTINEL_MODE", true);
                case "SENTINEL_MODE_OFF":
                    return new C11CarControlManager(app).setSceneMode("SENTINEL_MODE", false);
                case "CAR_CONTROL_ON":
                    return new C11CarControlManager(app).launchCarControl(true);
                case "CAR_CONTROL_OFF":
                    return new C11CarControlManager(app).launchCarControl(false);
                case "JOURNEY_ENERGY":
                    return new C11CarControlManager(app).showJourneyEnergy();
                case "VEHICLE_HEALTH":
                    return new C11CarControlManager(app).showVehicleHealth();
                case "CLICK_AT_POSITION":
                    return click(args.optInt("x", 0), args.optInt("y", 0));
                case "SWIPE_GESTURE":
                    return swipe(args.optInt("startX", args.optInt("x1", 0)), args.optInt("startY", args.optInt("y1", 0)),
                            args.optInt("endX", args.optInt("x2", 0)), args.optInt("endY", args.optInt("y2", 0)),
                            args.optLong("durationMs", 350));
                case "LONG_PRESS":
                    return longPress(args.optInt("x", 0), args.optInt("y", 0), args.optLong("durationMs", 800));
                case "INPUT_TEXT":
                    return inputText(app, firstNonEmpty(args.optString("text"), payload));
                case "SHELL":
                    return shell(args.optString("command", payload), false, normalized);
                case "ROOT_SHELL":
                    return shell(args.optString("command", payload), true, normalized);
                case "BROADCAST":
                    return broadcast(app, args, normalized);
                default:
                    return ActionResult.unsupported(normalized, "unknown action");
            }
        } catch (Exception e) {
            Log.e(TAG, "execute failed: " + normalized, e);
            return ActionResult.failed(normalized, e.getMessage());
        }
    }

    public static JSONObject buildPayload(String packageName, int x, int y, int width, int height) {
        JSONObject json = new JSONObject();
        try {
            json.put("packageName", packageName == null ? "" : packageName);
            json.put("x", x);
            json.put("y", y);
            json.put("width", width);
            json.put("height", height);
        } catch (Exception ignored) { }
        return json;
    }

    private static String normalizeAction(String action) {
        String trimmed = action == null ? "" : action.trim();
        if ("launch".equalsIgnoreCase(trimmed)) return "LAUNCH_PACKAGE";
        if ("launch_secondary".equalsIgnoreCase(trimmed)) return "LAUNCH_ON_DISPLAY";
        return trimmed.toUpperCase(Locale.US);
    }

    private static JSONObject parsePayload(String payload) {
        if (TextUtils.isEmpty(payload)) return new JSONObject();
        String trimmed = payload.trim();
        if (!trimmed.startsWith("{")) return new JSONObject();
        try {
            return new JSONObject(trimmed);
        } catch (Exception e) {
            Log.w(TAG, "invalid payload json: " + payload);
            return new JSONObject();
        }
    }

    private static ActionResult global(String name, int globalAction, int fallbackKeyCode) {
        C11AccessibilityService service = getAccessibilityService();
        if (service != null && service.performGlobalAction(globalAction)) {
            return ActionResult.ok(name, "accessibility global action");
        }
        if (sendKeyByShell(fallbackKeyCode)) {
            return ActionResult.ok(name, "shell keyevent fallback");
        }
        return ActionResult.failed(name, "accessibility unavailable and shell fallback failed");
    }

    private static ActionResult adjustVolume(Context app, String name, int direction) {
        AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return ActionResult.failed(name, "AudioManager unavailable");
        am.adjustSuggestedStreamVolume(direction, AudioManager.STREAM_MUSIC, AudioManager.FLAG_SHOW_UI);
        return ActionResult.ok(name, "volume adjusted");
    }

    private static ActionResult mediaKey(Context app, int keyCode, String name) {
        AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return ActionResult.failed(name, "AudioManager unavailable");
        long now = SystemClock.uptimeMillis();
        am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
        am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
        return ActionResult.ok(name, "media key dispatched");
    }

    private static ActionResult adjustBrightness(Context app, String name, boolean up) {
        try {
            int current = Settings.System.getInt(app.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 125);
            int next = Math.max(10, Math.min(255, current + (up ? 25 : -25)));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(app)) {
                Settings.System.putInt(app.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, next);
                return ActionResult.ok(name, "brightness=" + next);
            }
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + app.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(intent);
            return ActionResult.failed(name, "WRITE_SETTINGS permission required; opened settings");
        } catch (Exception e) {
            return ActionResult.failed(name, e.getMessage());
        }
    }

    private static ActionResult launchPackage(Context app, String packageName) {
        if (TextUtils.isEmpty(packageName)) return ActionResult.failed("LAUNCH_PACKAGE", "packageName empty");
        return MultiScreenManager.get(app).launchPackage(packageName)
                ? ActionResult.ok("LAUNCH_PACKAGE", packageName)
                : ActionResult.failed("LAUNCH_PACKAGE", "launch failed: " + packageName);
    }

    private static ActionResult launchOnDisplay(Context app, JSONObject args) {
        String packageName = firstNonEmpty(args.optString("packageName"), args.optString("package"));
        int displayId = args.optInt("displayId", -1);
        boolean ok = displayId >= 0
                ? MultiScreenManager.get(app).launchOnDisplay(packageName, displayId)
                : MultiScreenManager.get(app).launchOnSecondary(packageName);
        return ok ? ActionResult.ok("LAUNCH_ON_DISPLAY", packageName) : ActionResult.failed("LAUNCH_ON_DISPLAY", "launch failed");
    }

    private static ActionResult launchFreeform(Context app, JSONObject args) {
        String packageName = firstNonEmpty(args.optString("packageName"), args.optString("package"));
        android.graphics.Rect bounds = new android.graphics.Rect(
                args.optInt("x", 0),
                args.optInt("y", 0),
                args.optInt("x", 0) + args.optInt("width", 960),
                args.optInt("y", 0) + args.optInt("height", 540));
        boolean ok = MultiScreenManager.get(app).launchFreeform(packageName, bounds, args.optInt("displayId", -1));
        return ok ? ActionResult.ok("LAUNCH_FREEFORM", packageName + " " + bounds) : ActionResult.failed("LAUNCH_FREEFORM", "launch failed");
    }

    private static ActionResult bringToFront(Context app, String packageName) {
        if (TextUtils.isEmpty(packageName)) return ActionResult.failed("BRING_TO_FRONT", "packageName empty");
        C11AccessibilityService service = getAccessibilityService();
        if (service != null && packageName.equals(service.getCurrentPackageName())) {
            return ActionResult.ok("BRING_TO_FRONT", "already foreground");
        }
        return launchPackage(app, packageName);
    }

    private static ActionResult openActivity(Context app, String intentAction, String name) {
        return openActivityResult(app, intentAction, name);
    }

    private static ActionResult openSettings(Context app, String actionName) {
        ActionResult officialResult = new C11CarControlManager(app).setSettingsPage(true);
        return officialResult.success ? officialResult : openActivityResult(app, Settings.ACTION_SETTINGS, actionName);
    }

    private static ActionResult openMusic(Context app, String actionName, JSONObject args) {
        ActionResult officialResult = new C11CarControlManager(app).openMedia(args.optJSONObject("extras"));
        return officialResult.success ? officialResult : launchKnownPackage(app, actionName, args, "com.leapmotor.music", "com.android.music");
    }

    private static ActionResult openNavigation(Context app, String actionName, JSONObject args) {
        ActionResult officialResult = new C11CarControlManager(app).openAutonavi(args.optJSONObject("extras"));
        return officialResult.success ? officialResult : launchKnownPackage(app, actionName, args, "com.autonavi.minimap", "com.leapmotor.navigation");
    }

    private static ActionResult openActivityResult(Context app, String intentAction, String name) {
        try {
            Intent i = new Intent(intentAction);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(i);
            return ActionResult.ok(name, intentAction);
        } catch (Exception e) {
            return ActionResult.failed(name, e.getMessage());
        }
    }


    private static ActionResult launchKnownPackage(Context app, String action, JSONObject args, String... packages) {
        String configured = args.optString("packageName", "");
        if (!TextUtils.isEmpty(configured) && MultiScreenManager.get(app).launchPackage(configured)) {
            return ActionResult.ok(action, configured);
        }
        for (String packageName : packages) {
            if (MultiScreenManager.get(app).launchPackage(packageName)) return ActionResult.ok(action, packageName);
        }
        return ActionResult.failed(action, "no known package launched");
    }

    private static ActionResult aroundToggle(Context app, JSONObject args) {
        if (!TextUtils.isEmpty(args.optString("broadcastAction", "")) || !TextUtils.isEmpty(args.optString("action", ""))) {
            ActionResult broadcast = broadcast(app, args, "AROUND_TOGGLE_VIEW");
            if (broadcast.success) return broadcast;
        }
        return launchKnownPackage(app, "AROUND_TOGGLE_VIEW", args, "com.leapmotor.aroundview", "com.leapmotor.avm");
    }

    private static ActionResult carCommand(Context app, String action, JSONObject args, boolean enabled) {
        String globalKey = args.optString("globalKey", args.optString("key", ""));
        if (!TextUtils.isEmpty(globalKey)) {
            return new C11CarControlManager(app).putGlobalInt(globalKey, enabled ? 1 : 0);
        }

        String broadcastAction = args.optString("broadcastAction", args.optString("action", ""));
        if (!TextUtils.isEmpty(broadcastAction)) {
            JSONObject extras = args.optJSONObject("extras");
            if (extras == null) {
                extras = new JSONObject();
                try {
                    extras.put(args.optString("extraName", "enabled"), enabled ? 1 : 0);
                } catch (Exception ignored) { }
            }
            return new C11CarControlManager(app).sendOfficialBroadcast(action, broadcastAction, extras);
        }

        String shellCommand = args.optString("command", "");
        if (!TextUtils.isEmpty(shellCommand)) return shell(shellCommand, args.optBoolean("root", false), action);

        return ActionResult.unsupported(action, "No official C11 child-lock interface is documented; provide globalKey, broadcastAction/extras, or command in config.");
    }

    private static ActionResult click(int x, int y) {
        C11AccessibilityService service = getAccessibilityService();
        if (service != null && service.performClick(x, y)) return ActionResult.ok("CLICK_AT_POSITION", x + "," + y);
        return shell("input tap " + x + " " + y, false, "CLICK_AT_POSITION");
    }

    private static ActionResult swipe(int startX, int startY, int endX, int endY, long durationMs) {
        C11AccessibilityService service = getAccessibilityService();
        if (service != null && service.performSwipe(startX, startY, endX, endY, durationMs)) {
            return ActionResult.ok("SWIPE_GESTURE", "accessibility gesture");
        }
        return shell("input swipe " + startX + " " + startY + " " + endX + " " + endY + " " + durationMs, false, "SWIPE_GESTURE");
    }

    private static ActionResult longPress(int x, int y, long durationMs) {
        C11AccessibilityService service = getAccessibilityService();
        if (service != null && service.performLongPress(x, y, durationMs)) return ActionResult.ok("LONG_PRESS", "accessibility gesture");
        return shell("input swipe " + x + " " + y + " " + x + " " + y + " " + durationMs, false, "LONG_PRESS");
    }

    private static ActionResult inputText(Context app, String text) {
        C11AccessibilityService service = getAccessibilityService();
        if (service != null && service.inputText(text)) return ActionResult.ok("INPUT_TEXT", "accessibility input");
        ClipboardManager clipboard = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("c11_text", text));
        }
        return shell("input text " + shellEscapeInputText(text), false, "INPUT_TEXT");
    }

    private static ActionResult broadcast(Context app, JSONObject args, String name) {
        String action = args.optString("broadcastAction", args.optString("action", ""));
        if (TextUtils.isEmpty(action)) return ActionResult.failed(name, "broadcastAction empty");
        Intent intent = new Intent(action);
        String targetPackage = args.optString("targetPackage", "");
        if (!TextUtils.isEmpty(targetPackage)) intent.setPackage(targetPackage);
        C11CarControlManager.copyJsonExtras(intent, args.optJSONObject("extras"));
        app.sendBroadcast(intent);
        return ActionResult.ok(name, "broadcast sent: " + action);
    }

    private static boolean sendKeyByShell(int keyCode) {
        return keyCode > 0 && shell("input keyevent " + keyCode, false, "KEYEVENT").success;
    }

    private static ActionResult shell(String command, boolean root, String actionName) {
        if (TextUtils.isEmpty(command)) return ActionResult.failed(actionName, "command empty");
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec(root ? "su" : "sh");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int exit = process.waitFor();
            return exit == 0 ? ActionResult.ok(actionName, "shell ok") : ActionResult.failed(actionName, "shell exit=" + exit);
        } catch (Exception e) {
            return ActionResult.failed(actionName, e.getMessage());
        } finally {
            try { if (os != null) os.close(); } catch (Exception ignored) { }
            if (process != null) process.destroy();
        }
    }

    private static String shellEscapeInputText(String text) {
        if (text == null) return "";
        return text.replace("'", "\\'");
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) return value;
        }
        return "";
    }

    private static ActionResult speechSpeak(Context app, String text) {
        if (TextUtils.isEmpty(text)) {
            return ActionResult.failed("SPEECH_SPEAK", "text empty");
        }
        try {
            TtsManager.get(app).speak(text);
            return ActionResult.ok("SPEECH_SPEAK", text);
        } catch (Exception e) {
            Log.e(TAG, "speech speak failed", e);
            return ActionResult.failed("SPEECH_SPEAK", e.getMessage());
        }
    }

    public static class ActionResult {
        public final boolean success;
        public final String action;
        public final String status;
        public final String message;
        public final long timestamp;
        public final Map<String, String> extras = new HashMap<>();

        private ActionResult(boolean success, String action, String status, String message) {
            this.success = success;
            this.action = action;
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public static ActionResult ok(String action, String message) {
            return new ActionResult(true, action, RESULT_OK, message);
        }

        public static ActionResult failed(String action, String message) {
            return new ActionResult(false, action, RESULT_FAILED, message);
        }

        public static ActionResult unsupported(String action, String message) {
            return new ActionResult(false, action, RESULT_UNSUPPORTED, message);
        }

        @Override
        public String toString() {
            return "ActionResult{" +
                    "success=" + success +
                    ", action='" + action + '\'' +
                    ", status='" + status + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
