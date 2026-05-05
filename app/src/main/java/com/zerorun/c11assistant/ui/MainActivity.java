package com.zerorun.c11assistant.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.zerorun.c11assistant.R;
import com.zerorun.c11assistant.manager.ConfigManager;
import com.zerorun.c11assistant.service.C11ForegroundService;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MainActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((Button)findViewById(R.id.btnGrantOverlay)).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        ((Button)findViewById(R.id.btnGrantBattery)).setOnClickListener(v -> requestIgnoreBattery());
        ((Button)findViewById(R.id.btnStartService)).setOnClickListener(v -> C11ForegroundService.start(this, false, -1));
        ((Button)findViewById(R.id.btnExecMain)).setOnClickListener(v -> C11ForegroundService.start(this, false, 0));
        ((Button)findViewById(R.id.btnExecSub)).setOnClickListener(v -> C11ForegroundService.start(this, false, 1));
        ((Button)findViewById(R.id.btnExport)).setOnClickListener(v -> exportConfig());
        ((Button)findViewById(R.id.btnImport)).setOnClickListener(v -> importConfig());
    }

    private void requestIgnoreBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = getSystemService(PowerManager.class);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
            }
        }
    }

    private void exportConfig() {
        try {
            File src = new File(getFilesDir(), "c11_config.json");
            File dst = new File(getExternalFilesDir(null), "c11_config_export.json");
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }

    private void importConfig() {
        try {
            File src = new File(getExternalFilesDir(null), "c11_config_export.json");
            File dst = new File(getFilesDir(), "c11_config.json");
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
