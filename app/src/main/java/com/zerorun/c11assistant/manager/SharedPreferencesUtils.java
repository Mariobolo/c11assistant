package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesUtils {
    private static final String PREFS = "c11_automation";
    private SharedPreferencesUtils() {}

    public static boolean getBoolean(Context c, String key, boolean def) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, def);
    }

    public static void putBoolean(Context c, String key, boolean value) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    public static int getInt(Context c, String key, int def) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key, def);
    }

    public static void putInt(Context c, String key, int value) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(key, value).apply();
    }
}
