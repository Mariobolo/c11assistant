package com.leapmotor.c11assistant.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.leapmotor.c11assistant.R;
import com.leapmotor.c11assistant.manager.ConfigManager;
import com.leapmotor.c11assistant.model.ActionItem;
import com.leapmotor.c11assistant.model.ScreenConfig;
import com.leapmotor.c11assistant.service.C11ForegroundService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "c11_ui_prefs";
    private TextView tvFeedback;
    private TextView tvCurrentTab;
    private TextView tvStatusTime;
    private LinearLayout pageAbout;
    private View pageQuick;
    private View pageSettings;
    private Button tabQuick;
    private Button tabSettings;
    private Button tabAbout;
    private SharedPreferences sp;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enterFullScreen();

        sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        bindViews();
        bindTabs();
        bindQuickActions();
        bindSettings();
        initActionList();
        selectTab(0);
        updateTime();
    }

    private void bindViews() {
        tvFeedback = findViewById(R.id.tvFeedback);
        tvCurrentTab = findViewById(R.id.tvCurrentTab);
        tvStatusTime = findViewById(R.id.tvStatusTime);
        pageQuick = findViewById(R.id.pageQuick);
        pageSettings = findViewById(R.id.pageSettings);
        pageAbout = findViewById(R.id.pageAbout);
        tabQuick = findViewById(R.id.tabQuick);
        tabSettings = findViewById(R.id.tabSettings);
        tabAbout = findViewById(R.id.tabAbout);
    }

    private void bindTabs() {
        tabQuick.setOnClickListener(v -> selectTab(0));
        tabSettings.setOnClickListener(v -> selectTab(1));
        tabAbout.setOnClickListener(v -> selectTab(2));
    }

    private void selectTab(int tab) {
        tabQuick.setSelected(tab == 0);
        tabSettings.setSelected(tab == 1);
        tabAbout.setSelected(tab == 2);
        pageQuick.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        pageSettings.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        pageAbout.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        if (tab == 0) tvCurrentTab.setText(R.string.tab_quick_actions);
        else if (tab == 1) tvCurrentTab.setText(R.string.tab_settings);
        else tvCurrentTab.setText(R.string.tab_about);
    }

    private void bindQuickActions() {
        findViewById(R.id.btnQuick1).setOnClickListener(v -> {
            C11ForegroundService.start(this, false, -1);
            showFeedback("后台服务已启动");
        });
        findViewById(R.id.btnQuick2).setOnClickListener(v -> runAction("主屏任务", 0));
        findViewById(R.id.btnQuick3).setOnClickListener(v -> runAction("副屏任务", 1));
        findViewById(R.id.btnQuick4).setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            showFeedback("请在系统页面开启悬浮窗权限");
        });
        findViewById(R.id.btnQuick5).setOnClickListener(v -> exportConfig());
        findViewById(R.id.btnQuick6).setOnClickListener(v -> importConfig());
    }

    private void bindSettings() {
        setupSwitchItem(findViewById(R.id.itemAutoStart), getString(R.string.setting_auto_start), "auto_start", true);
        setupSwitchItem(findViewById(R.id.itemShowFeedback), getString(R.string.setting_show_feedback), "show_feedback", true);
        setupSwitchItem(findViewById(R.id.itemLargeMode), getString(R.string.setting_large_touch), "large_mode", true);
        setupSwitchItem(findViewById(R.id.itemSkipRunning), getString(R.string.setting_skip_running), "skip_running", false);
    }

    private void setupSwitchItem(View itemView, String title, String key, boolean defaultValue) {
        TextView tv = itemView.findViewById(R.id.tvSettingTitle);
        Switch sw = itemView.findViewById(R.id.switchSetting);
        tv.setText(title);
        sw.setChecked(sp.getBoolean(key, defaultValue));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean(key, isChecked).apply();
            showFeedback(title + (isChecked ? " 已开启" : " 已关闭"));
        });
    }

    private void initActionList() {
        RecyclerView rv = findViewById(R.id.rvActions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<ActionItem> actions = loadActions();
        rv.setAdapter(new ActionAdapter(actions, item -> runCustomAction(item)));
        rv.setHasFixedSize(true);
    }

    private List<ActionItem> loadActions() {
        List<ActionItem> list = new ArrayList<>();
        JSONObject root = new ConfigManager(this).load();
        JSONArray screens = root.optJSONArray("screens");
        if (screens != null) {
            for (int i = 0; i < screens.length(); i++) {
                JSONObject s = screens.optJSONObject(i);
                if (s == null) continue;
                ScreenConfig config = ScreenConfig.fromJson(s);
                for (ActionItem item : config.actions) {
                    if (item.id == null) item.id = "动作" + i;
                    list.add(item);
                }
            }
        }
        if (list.isEmpty()) {
            ActionItem demo = new ActionItem();
            demo.id = "高德回家";
            demo.type = "launch";
            demo.packageName = "com.autonavi.minimap";
            demo.delayMs = 300;
            list.add(demo);
        }
        return list;
    }

    private void runAction(String actionName, int index) {
        C11ForegroundService.start(this, false, index);
        showFeedback(actionName + " 已执行");
    }

    private void runCustomAction(ActionItem item) {
        // TODO: 在这里扩展高德地图联动、车机指令执行等业务逻辑
        showFeedback("执行动作: " + item.id);
    }

    private void updateTime() {
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        tvStatusTime.setText(df.format(new java.util.Date()));
        tvStatusTime.postDelayed(this::updateTime, 30_000);
    }

    private void requestIgnoreBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = getSystemService(PowerManager.class);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
            }
        }
    }

    private void exportConfig() {
        File src = new File(getFilesDir(), "c11_config.json");
        File dst = new File(getExternalFilesDir(null), "c11_config_export.json");
        copyFile(src, dst);
        showFeedback("配置已导出");
    }

    private void importConfig() {
        File src = new File(getExternalFilesDir(null), "c11_config_export.json");
        File dst = new File(getFilesDir(), "c11_config.json");
        copyFile(src, dst);
        showFeedback("配置已导入");
    }

    private void copyFile(File src, File dst) {
        try (FileInputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst, false)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        } catch (Exception e) {
            showFeedback("文件操作失败: " + e.getMessage());
        }
    }

    private void showFeedback(String msg) {
        if (sp.getBoolean("show_feedback", true)) tvFeedback.setText(msg);
    }

    private void enterFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
