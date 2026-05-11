package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class ActionExecutor {
    private static final String TAG = "ActionExecutor";

    public static void execute(Context context, String action, String payload) {
        try {
            Log.i(TAG, "execute action=" + action + " payload=" + payload);
            if ("MEDIA_NEXT".equals(action)) {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) am.adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE, AudioManager.STREAM_MUSIC, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "execute failed", e);
        }
    }
}
