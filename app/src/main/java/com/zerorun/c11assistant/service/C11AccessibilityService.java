package com.leapmotor.c11assistant.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class C11AccessibilityService extends AccessibilityService {
    private static C11AccessibilityService instance;
    @Override protected void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    public static C11AccessibilityService get() { return instance; }
}
