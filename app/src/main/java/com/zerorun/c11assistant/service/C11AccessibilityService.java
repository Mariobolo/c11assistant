package com.leapmotor.c11assistant.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.leapmotor.c11assistant.manager.ActionExecutor;
import com.leapmotor.c11assistant.manager.AutomationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class C11AccessibilityService extends AccessibilityService {
    private static final String TAG = "C11AccessibilityService";
    private static C11AccessibilityService instance;

    private String currentPackageName = "";
    private String currentClassName = "";
    private String currentScene = "UNKNOWN";
    private long lastWindowChangedAt;

    public static C11AccessibilityService get() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        ActionExecutor.setAccessibilityService(this);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOWS_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                | AccessibilityEvent.TYPE_VIEW_CLICKED
                | AccessibilityEvent.TYPE_VIEW_FOCUSED
                | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 80;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
        Log.i(TAG, "accessibility service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        try {
            int type = event.getEventType();
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                handleWindowChange(event);
            } else if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                detectSceneFromRoot(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "event handling failed", e);
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "accessibility interrupted");
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        if (instance == this) instance = null;
        ActionExecutor.setAccessibilityService(null);
        return super.onUnbind(intent);
    }

    public String getCurrentPackageName() {
        return currentPackageName;
    }

    public String getCurrentClassName() {
        return currentClassName;
    }

    public String getCurrentScene() {
        return currentScene;
    }

    public long getLastWindowChangedAt() {
        return lastWindowChangedAt;
    }

    public boolean performClick(int x, int y) {
        return dispatchPathGesture(buildTapPath(x, y), 0L, 80L);
    }

    public boolean performLongPress(int x, int y, long durationMs) {
        return dispatchPathGesture(buildTapPath(x, y), 0L, Math.max(500L, durationMs));
    }

    public boolean performSwipe(int startX, int startY, int endX, int endY, long durationMs) {
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        return dispatchPathGesture(path, 0L, Math.max(120L, durationMs));
    }

    public boolean inputText(String text) {
        AccessibilityNodeInfo focused = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused == null) focused = getRootInActiveWindow();
        AccessibilityNodeInfo editable = findEditableNode(focused);
        if (editable == null) return false;

        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text == null ? "" : text);
        return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    public boolean clickByText(String text) {
        AccessibilityNodeInfo node = findNodeByText(text);
        return clickNode(node);
    }

    public boolean clickByViewId(String viewId) {
        AccessibilityNodeInfo node = findNodeByViewId(viewId);
        return clickNode(node);
    }

    public AccessibilityNodeInfo findNodeByText(String text) {
        if (TextUtils.isEmpty(text)) return null;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes == null || nodes.isEmpty()) return null;
        return nodes.get(0);
    }

    public AccessibilityNodeInfo findNodeByViewId(String viewId) {
        if (TextUtils.isEmpty(viewId)) return null;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return null;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
        if (nodes == null || nodes.isEmpty()) return null;
        return nodes.get(0);
    }

    public List<String> dumpVisibleTexts() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        collectVisibleTexts(root, out, 80);
        return out;
    }

    private void handleWindowChange(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        CharSequence cls = event.getClassName();
        String nextPackage = pkg == null ? "" : pkg.toString();
        String nextClass = cls == null ? "" : cls.toString();
        boolean changed = !TextUtils.equals(currentPackageName, nextPackage) || !TextUtils.equals(currentClassName, nextClass);
        currentPackageName = nextPackage;
        currentClassName = nextClass;
        lastWindowChangedAt = System.currentTimeMillis();
        String scene = detectSceneFromRoot(true);
        Log.i(TAG, "window package=" + currentPackageName + " class=" + currentClassName + " scene=" + scene);
        if (changed) {
            AutomationManager.get(this).onAccessibilityWindowChanged(currentPackageName, currentClassName, scene);
        }
    }

    private String detectSceneFromRoot(boolean notify) {
        String scene = detectScene(currentPackageName, dumpVisibleTexts());
        if (!TextUtils.equals(scene, currentScene)) {
            currentScene = scene;
            if (notify) AutomationManager.get(this).onSceneChanged(scene, currentPackageName);
        }
        return currentScene;
    }

    private String detectScene(String packageName, List<String> texts) {
        String pkg = packageName == null ? "" : packageName.toLowerCase(Locale.US);
        if (pkg.contains("autonavi") || pkg.contains("navigation") || containsAny(texts, "导航", "路线", "地图", "到达")) return "NAVIGATION";
        if (pkg.contains("music") || containsAny(texts, "音乐", "播放", "歌单", "歌词")) return "MUSIC";
        if (pkg.contains("setting") || containsAny(texts, "设置", "蓝牙", "网络", "显示")) return "SETTINGS";
        if (pkg.contains("around") || pkg.contains("avm") || containsAny(texts, "全景", "360", "泊车", "影像")) return "AROUND_VIEW";
        if (pkg.contains("launcher") || containsAny(texts, "桌面", "主页")) return "LAUNCHER";
        return "UNKNOWN";
    }

    private boolean dispatchPathGesture(Path path, long startDelayMs, long durationMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, startDelayMs, durationMs))
                .build();
        return dispatchGesture(gesture, null, null);
    }

    private Path buildTapPath(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        return path;
    }

    private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        if (root.isEditable() && root.isEnabled()) return root;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo found = findEditableNode(root.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable() && current.isEnabled()) return current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            current = current.getParent();
        }
        Rect rect = new Rect();
        node.getBoundsInScreen(rect);
        if (!rect.isEmpty()) return performClick(rect.centerX(), rect.centerY());
        return false;
    }

    private void collectVisibleTexts(AccessibilityNodeInfo node, List<String> out, int limit) {
        if (node == null || out.size() >= limit) return;
        if (node.isVisibleToUser()) {
            CharSequence text = !TextUtils.isEmpty(node.getText()) ? node.getText() : node.getContentDescription();
            if (!TextUtils.isEmpty(text)) out.add(text.toString());
        }
        for (int i = 0; i < node.getChildCount() && out.size() < limit; i++) {
            collectVisibleTexts(node.getChild(i), out, limit);
        }
    }

    private boolean containsAny(List<String> texts, String... needles) {
        if (texts == null || needles == null) return false;
        for (String text : texts) {
            if (text == null) continue;
            for (String needle : needles) {
                if (text.contains(needle)) return true;
            }
        }
        return false;
    }
}
