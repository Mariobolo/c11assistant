package com.leapmotor.c11assistant.manager;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

public class TtsManager {

    private static final String TAG = "TtsManager";

    private static final String ACTION_TTS = "com.iflytek.autofly.TtsService";
    private static final String TTS_PACKAGE = "com.iflytek.cutefly.speechclient.hmi";
    private static final String TTS_CLASS = "com.iflytek.autofly.voicecoreservice.tts.TtsService";

    private static final String EXTRA_OPERATION = "operation";
    private static final String EXTRA_TEXT = "text";
    private static final String EXTRA_PACKAGE = "package";
    private static final String EXTRA_PRIORITY = "priority";
    private static final String EXTRA_STREAM_TYPE = "streamType";
    private static final String EXTRA_AUDIO_FOCUS = "audioFocusDurationHint";

    private static final String OPERATION_PLAY = "PLAY";
    private static final String PRIORITY_HIGH = "high";
    private static final String STREAM_TYPE_MUSIC = "3";
    private static final String AUDIO_FOCUS_FOREVER = "forever";

    private static TtsManager instance;
    private final Context app;
    private AudioManager audioManager;
    private boolean enabled = true;
    private boolean muteOriginalTts = false;

    public static synchronized TtsManager get(Context context) {
        if (instance == null) {
            instance = new TtsManager(context.getApplicationContext());
        }
        return instance;
    }

    private TtsManager(Context context) {
        this.app = context;
        this.audioManager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setMuteOriginalTts(boolean mute) {
        this.muteOriginalTts = mute;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.putInt(app.getContentResolver(), "SPEECH_SPEAK", mute ? 0 : 1);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to set SPEECH_SPEAK", e);
        }
    }

    public boolean isMuteOriginalTts() {
        return muteOriginalTts;
    }

    public void speak(String text) {
        speak(text, null);
    }

    public void speak(String text, @Nullable String priority) {
        if (!enabled || TextUtils.isEmpty(text)) {
            Log.d(TAG, "TTS disabled or text empty, skipping: " + text);
            return;
        }

        try {
            Intent intent = new Intent(ACTION_TTS);
            intent.setClassName(TTS_PACKAGE, TTS_CLASS);
            intent.putExtra(EXTRA_OPERATION, OPERATION_PLAY);
            intent.putExtra(EXTRA_TEXT, text);
            intent.putExtra(EXTRA_PACKAGE, "leap");
            intent.putExtra(EXTRA_PRIORITY, TextUtils.isEmpty(priority) ? PRIORITY_HIGH : priority);
            intent.putExtra(EXTRA_STREAM_TYPE, STREAM_TYPE_MUSIC);
            intent.putExtra(EXTRA_AUDIO_FOCUS, AUDIO_FOCUS_FOREVER);

            app.startService(intent);
            Log.i(TAG, "TTS speak: " + text);
        } catch (Exception e) {
            Log.e(TAG, "Failed to speak TTS: " + text, e);
        }
    }

    public void speakIfEnabled(String text, String prefKey, boolean defaultValue) {
        if (SharedPreferencesUtils.getBoolean(app, prefKey, defaultValue)) {
            speak(text);
        }
    }

    public void speakWithDelay(String text, long delayMs) {
        if (delayMs <= 0) {
            speak(text);
            return;
        }
        new android.os.Handler().postDelayed(() -> speak(text), delayMs);
    }
}
