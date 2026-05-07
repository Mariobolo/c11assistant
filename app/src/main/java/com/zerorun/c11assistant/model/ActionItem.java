package com.leapmotor.c11assistant.model;

public class ActionItem {
    public String id;
    public String type;
    public boolean enabled = true;
    public String packageName;
    public String launchMode = "A";
    public int x, y, width = 960, height = 360;
    public float alpha = 1.0f;
    public boolean addCloseButton;
    public long delayMs;
    public boolean skipIfRunning;
}
